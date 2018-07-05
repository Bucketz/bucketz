package org.bucketz.lib;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.apache.felix.serializer.Serializer;
import org.apache.felix.serializer.Writer;
import org.bucketz.BucketDescriptor;
import org.bucketz.BucketStore;
import org.bucketz.SingleObjectBucketDescriptor;
import org.osgi.dto.DTO;
import org.osgi.service.log.LogService;

public class BucketIOFactory<E>
{
    private static enum Profile { MULTI_TSV, MULTI_JSON, PARTITIONED_JSON, SINGLE }

    private Profile profile;

    public static class TsvConfig extends DTO
    {
        public BucketName bucketName;
        public boolean includeHeaders;
        public String[] headers;
        public String[] columns;
    }

    public static class TsvConfigFactory
    {
        private final TsvConfig config = new TsvConfig();

        public TsvConfigFactory includeHeaders( String... header )
        {
            config.headers = header;
            return this;
        }

        public TsvConfigFactory setColumns( String... aColumnList )
        {
            config.columns = aColumnList;
            return this;
        }

        public TsvConfig get()
        {
            return config;
        }
    }

    public static class MultiJsonConfig extends DTO
    {
        // Nothing for now!
    }

    public static class MultiJsonConfigFactory
    {
        private final MultiJsonConfig config = new MultiJsonConfig();

        public MultiJsonConfig get()
        {
            return config;
        }
    }

    public static class PartitionedJsonConfig<E>
    {
        public String bucketFilter;
        public BucketFunction<E> bucketFunction;
    }

    public static class PartitionedJsonConfigFactory<E>
    {
        private final PartitionedJsonConfig<E> config = new PartitionedJsonConfig<>();

        public PartitionedJsonConfigFactory<E> addBucketFilter( String aFilter )
        {
            config.bucketFilter = aFilter;
            return this;
        }

        public PartitionedJsonConfigFactory<E> setBucketFunction( BucketFunction<E> aBucketFunction )
        {
            config.bucketFunction = aBucketFunction;
            return this;
        }

        public PartitionedJsonConfig<E> get()
        {
            return config;
        }
    }

    public static class SingleObjectConfig extends DTO
    {
        public String bucketName;
    }

    public static class SingleObjectConfigFactory
    {
        private final SingleObjectConfig config = new SingleObjectConfig();

        public SingleObjectConfigFactory setBucketName( String aBucketName )
        {
            config.bucketName = aBucketName;
            return this;
        }

        public SingleObjectConfig get()
        {
            return config;
        }
    }

    private TsvConfig tsvConfig;
    @SuppressWarnings( "unused" ) // There are no values to configure at this time
    private MultiJsonConfig multiJsonConfig;
    private PartitionedJsonConfig<E> partitionedJsonConfig;
    private SingleObjectConfig singleObjectConfig;

    private Serializer serializer;
    private Writer writer;
    private LogService logger;
    private Class<E> dtoClass;

    private Function<E, E> preprocessor;
    private boolean preprocess;

    private BucketDescriptor<E> aggregateDescriptor;
    private SingleObjectBucketDescriptor<E> singleObjectDescriptor;

    private final List<String> errors = new ArrayList<>();

    public BucketIOFactory( Class<E> aDTOClass )
    {
        dtoClass = aDTOClass;
    }

    public BucketIOFactory<E> setSerializer( Serializer aSerializer )
    {
        return setSerializer( aSerializer, null );
    }

    public BucketIOFactory<E> setSerializer( Serializer aSerializer, Writer aWriter )
    {
        serializer = aSerializer;
        writer = aWriter;
        return this;
    }

    public BucketIOFactory<E> setLogService( LogService aLogService )
    {
        logger = aLogService;
        return this;
    }

    public BucketIOFactory<E> preprocess( Function<E, E> aPreprocessor )
    {
        preprocess = true;
        preprocessor = aPreprocessor;
        return this;
    }

    public BucketIOFactory<E> configureWith( BucketDescriptor<E> anAggregateDescriptor )
    {
        aggregateDescriptor = anAggregateDescriptor;
        return this;
    }
    
    public BucketIOFactory<E> configureWith( SingleObjectBucketDescriptor<E> aSingleObjectDescriptor )
    {
        singleObjectDescriptor = aSingleObjectDescriptor;
        return this;
    }

    public BucketIOFactory<E> useTabDelimited( TsvConfig withConfiguration )
    {
        tsvConfig = withConfiguration;
        profile = Profile.MULTI_TSV;
        return this;
    }

    public BucketIOFactory<E> useMultiJson( MultiJsonConfig withConfiguration )
    {
        multiJsonConfig = withConfiguration;
        profile = Profile.MULTI_JSON;
        return this;
    }

    public BucketIOFactory<E> usePartitionedJson( PartitionedJsonConfig<E> withConfiguration )
    {
        partitionedJsonConfig = withConfiguration;
        profile = Profile.PARTITIONED_JSON;
        return this;
    }

    public BucketIOFactory<E> useSingleObject( SingleObjectConfig withConfiguration )
    {
        singleObjectConfig = withConfiguration;
        profile = Profile.SINGLE;
        return this;
    }

    public BucketIO<E> get()
        throws IOException
    {
        errors.addAll( validateCommonConfig() );
        if( !errors.isEmpty() )
            throw new IOException( errors.get( 0 ) );

        final BucketIO<E> io;
        if (profile == Profile.MULTI_TSV)
        {
            if (!validateTsvConfig().isEmpty() )
                throw new IOException( validateTsvConfig().get( 0 ) );

            io = newTabSeparatedValuesIO();
        }
        else if (profile == Profile.MULTI_JSON)
        {
            if (!validateMultiJsonConfig().isEmpty() )
                throw new IOException( validateMultiJsonConfig().get( 0 ) );

            io = newMultiJsonIO();
        }
        else if (profile == Profile.PARTITIONED_JSON)
        {
            if (!validatePartitionedJsonConfig().isEmpty() )
                throw new IOException( validatePartitionedJsonConfig().get( 0 ) );

            io = newPartitionedJsonIO();
        }
        else if (profile == Profile.SINGLE)
        {
            if (!validateSingleObjectJsonConfig().isEmpty() )
                throw new IOException( validateSingleObjectJsonConfig().get( 0 ) );

            io = newSingleObjectJsonIO();
        }
        else
            throw new IOException( "Could not instantiate a BucketReader" );

        return io;
    }

    private List<String> validateCommonConfig()
    {
        final List<String> errors = new ArrayList<>();

        if( serializer == null )
            errors.add( "Serializer is not set" );
        if (profile == Profile.SINGLE)
        {
            if (singleObjectDescriptor == null)
                errors.add( "SingleObjectDescriptor is not set" );
        }
        else
        {
            if(profile != Profile.SINGLE && aggregateDescriptor == null )
                errors.add( "AggregateDescriptor is not set" );
        }

        return errors;
    }

    private DelimiterSeparatedValuesIO<E> newTabSeparatedValuesIO()
    {
        final DelimiterSeparatedValuesIO<E> io = new DelimiterSeparatedValuesIO<>( dtoClass )
                .setSerializer( serializer )
                .configureWith( aggregateDescriptor )
                .setColumns( tsvConfig.columns )
                .useTabDelimiters();

        if (tsvConfig.includeHeaders)
            io.includeHeaders();
        if (preprocess)
            io.preprocess( preprocessor );

        return io;
    }

    private List<String> validateTsvConfig()
    {
        final List<String> errors = new ArrayList<>();

        final String bucketName = new StringBuilder()
                .append( aggregateDescriptor.brn() )
                .append( "." )
                .append( BucketStore.Format.TSV.name().toLowerCase() )
                .toString();

        try
        {
             tsvConfig.bucketName = BucketNameParser.newParser()
                     .parse( bucketName, aggregateDescriptor.packaging() );
        }
        catch ( Exception e )
        {
            errors.add( e.getMessage() );
        }

        try
        {
            final BucketStore.Format format = BucketStore.Format.valueOf( tsvConfig.bucketName.format );
            if (format != BucketStore.Format.TSV)
                errors.add( "" );

            final BucketStore.Packaging packaging = BucketStore.Packaging.valueOf( tsvConfig.bucketName.packaging );
            if (packaging != BucketStore.Packaging.MULTI)
                errors.add( String.format( "TSV format is configured, but packging is %s", packaging.name() ) );

            if (tsvConfig.columns == null || tsvConfig.columns.length <= 0)
                errors.add( "TSV format is configured, but no columns have been set" );
        }
        catch ( Exception e )
        {
            errors.add( e.getMessage() );
        }

        return errors;
    }

    private MultiJsonIO<E> newMultiJsonIO()
    {
        final MultiJsonIO<E> io = new MultiJsonIO<>( dtoClass )
                .setLogService( logger )
                .setSerializer( serializer, writer )
                .configureWith( aggregateDescriptor );

        if (preprocess)
            io.preprocess( preprocessor );

        return io;
    }

    private List<String> validateMultiJsonConfig()
    {
        final List<String> errors = new ArrayList<>();

        final String bucketName = new StringBuilder()
                .append( aggregateDescriptor.brn() )
                .append( "." )
                .append( BucketStore.Format.JSON.name().toLowerCase() )
                .toString();
        try
        {
            BucketNameParser.newParser().parse( bucketName, aggregateDescriptor.packaging() );
        }
        catch ( Exception e )
        {
            errors.add( e.getMessage() );
        }

        try
        {
            if (aggregateDescriptor.format() != BucketStore.Format.JSON)
                errors.add( String.format( "Expected JSON format, but was %s", aggregateDescriptor.format() ) );

            if (aggregateDescriptor.packaging() != BucketStore.Packaging.MULTI)
                errors.add( String.format( "Expected MULTI packaging, but was %s", aggregateDescriptor.packaging() ) );

            if (!aggregateDescriptor.containerName().isPresent())
                errors.add( "Missing container name. MULTI-packaging requires a container name." );
        }
        catch ( Exception e )
        {
            errors.add( e.getMessage() );
        }

        return errors;
    }

    private PartitionedJsonIO<E> newPartitionedJsonIO()
    {
        final PartitionedJsonIO<E> io = new PartitionedJsonIO<>( dtoClass )
                .setSerializer( serializer, writer )
                .configureWith( aggregateDescriptor )
                .addBucketFilter( partitionedJsonConfig.bucketFilter )
                .setBucketFunction( partitionedJsonConfig.bucketFunction );

        if (preprocess)
            io.preprocess( preprocessor );

        return io;
    }

    private List<String> validatePartitionedJsonConfig()
    {
        final List<String> errors = new ArrayList<>();

        try
        {
            if (partitionedJsonConfig.bucketFunction == null)
                errors.add( "BucketFunction is null" );
        }
        catch ( Exception e )
        {
            errors.add( e.getMessage() );
        }

        return errors;
    }

    private SingleObjectJsonIO<E> newSingleObjectJsonIO()
    {
        final SingleObjectJsonIO<E> io = new SingleObjectJsonIO<>( dtoClass )
                .setSerializer( serializer, writer )
                .configureWith( singleObjectDescriptor )
                .setBucketName( singleObjectConfig.bucketName );

        if (preprocess)
            io.preprocess( preprocessor );

        return io;
    }

    private List<String> validateSingleObjectJsonConfig()
    {
        final List<String> errors = new ArrayList<>();

        try
        {
            if (singleObjectConfig.bucketName == null)
                errors.add( "BucketFunction is null" );
        }
        catch ( Exception e )
        {
            errors.add( e.getMessage() );
        }

        return errors;
    }    
}
