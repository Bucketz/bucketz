package org.bucketz.lib;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.apache.felix.serializer.Serializer;
import org.apache.felix.serializer.Writer;
import org.bucketz.BucketDescriptor;
import org.bucketz.BucketIO;
import org.bucketz.BucketStore;
import org.bucketz.UncheckedBucketException;
import org.osgi.dto.DTO;
import org.osgi.service.log.LogService;

public class BucketIOFactory<D>
{
    private static enum Profile { MULTI_TSV, MULTI_JSON, PARTITIONED_JSON, SINGLE }

    public static <D>BucketIOFactory<D> newFactory( Class<D> dtoClass )
    {
        return new BucketIOFactory<>( dtoClass );
    }

    public static TsvConfigFactory newTsvConfigFactory()
    {
        return new TsvConfigFactory();
    }

    public static MultiJsonConfigFactory newMultiJsonConfigFactory()
    {
        return new MultiJsonConfigFactory();
    }

    public static <D>PartitionedJsonConfigFactory<D> newPartitionedJsonConfigFactory( Class<D> dtoClass )
    {
        return new PartitionedJsonConfigFactory<>();
    }

    public static SingleObjectConfigFactory newSingleObjectConfigFactory()
    {
        return new SingleObjectConfigFactory();
    }

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
    private PartitionedJsonConfig<D> partitionedJsonConfig;
    private SingleObjectConfig singleObjectConfig;

    private Serializer serializer;
    private Writer writer;
    private LogService logger;
    private Class<D> dtoClass;

    private Function<D, D> preprocessor;
    private boolean preprocess;

    private BucketDescriptor<D> aggregateDescriptor;
    private BucketDescriptor.Single<D> singleObjectDescriptor;

    private final List<String> errors = new ArrayList<>();

    private BucketIOFactory( Class<D> aDTOClass )
    {
        dtoClass = aDTOClass;
    }

    public BucketIOFactory<D> setSerializer( Serializer aSerializer )
    {
        return setSerializer( aSerializer, null );
    }

    public BucketIOFactory<D> setSerializer( Serializer aSerializer, Writer aWriter )
    {
        serializer = aSerializer;
        writer = aWriter;
        return this;
    }

    public BucketIOFactory<D> setLogService( LogService aLogService )
    {
        logger = aLogService;
        return this;
    }

    public BucketIOFactory<D> preprocess( Function<D, D> aPreprocessor )
    {
        preprocess = true;
        preprocessor = aPreprocessor;
        return this;
    }

    public BucketIOFactory<D> configureWith( BucketDescriptor<D> anAggregateDescriptor )
    {
        aggregateDescriptor = anAggregateDescriptor;
        return this;
    }

    public BucketIOFactory<D> useTabDelimited( TsvConfig withConfiguration )
    {
        tsvConfig = withConfiguration;
        profile = Profile.MULTI_TSV;
        return this;
    }

    public BucketIOFactory<D> useMultiJson( MultiJsonConfig withConfiguration )
    {
        multiJsonConfig = withConfiguration;
        profile = Profile.MULTI_JSON;
        return this;
    }

    public BucketIOFactory<D> usePartitionedJson( PartitionedJsonConfig<D> withConfiguration )
    {
        partitionedJsonConfig = withConfiguration;
        profile = Profile.PARTITIONED_JSON;
        return this;
    }

    public BucketIOFactory<D> useSingleObject( SingleObjectConfig withConfiguration )
    {
        singleObjectConfig = withConfiguration;
        profile = Profile.SINGLE;
        return this;
    }

    public BucketIO<D> get()
        throws UncheckedBucketException
    {
        errors.addAll( validateCommonConfig() );
        if( !errors.isEmpty() )
            throw new UncheckedBucketException( errors.get( 0 ) );

        final BucketIO<D> io;
        if (profile == Profile.MULTI_TSV)
        {
            if (!validateTsvConfig().isEmpty() )
                throw new UncheckedBucketException( validateTsvConfig().get( 0 ) );

            io = newTabSeparatedValuesIO();
        }
        else if (profile == Profile.MULTI_JSON)
        {
            if (!validateMultiJsonConfig().isEmpty() )
                throw new UncheckedBucketException( validateMultiJsonConfig().get( 0 ) );

            io = newMultiJsonIO();
        }
        else if (profile == Profile.PARTITIONED_JSON)
        {
            if (!validatePartitionedJsonConfig().isEmpty() )
                throw new UncheckedBucketException( validatePartitionedJsonConfig().get( 0 ) );

            io = newPartitionedJsonIO();
        }
        else if (profile == Profile.SINGLE)
        {
            if (!validateSingleObjectJsonConfig().isEmpty() )
                throw new UncheckedBucketException( validateSingleObjectJsonConfig().get( 0 ) );

            io = newSingleObjectJsonIO();
        }
        else
            throw new UncheckedBucketException( "Could not instantiate a BucketReader" );

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

    private DelimiterSeparatedValuesIO<D> newTabSeparatedValuesIO()
    {
        final DelimiterSeparatedValuesIO<D> io = new DelimiterSeparatedValuesIO<>( dtoClass )
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

    private MultiJsonIO<D> newMultiJsonIO()
    {
        final MultiJsonIO<D> io = new MultiJsonIO<>( dtoClass )
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

    private PartitionedJsonIO<D> newPartitionedJsonIO()
    {
        final PartitionedJsonIO<D> io = new PartitionedJsonIO<>( dtoClass )
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

    private SingleObjectJsonIO<D> newSingleObjectJsonIO()
    {
        final SingleObjectJsonIO<D> io = new SingleObjectJsonIO<>( dtoClass )
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
