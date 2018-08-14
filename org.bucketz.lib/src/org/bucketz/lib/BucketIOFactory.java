package org.bucketz.lib;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import org.apache.felix.serializer.Serializer;
import org.apache.felix.serializer.Writer;
import org.bucketz.Bucket;
import org.bucketz.BucketIO;
import org.bucketz.BucketIO.BucketFunction;
import org.bucketz.UncheckedBucketException;
import org.bucketz.store.BucketDescriptor;
import org.osgi.service.log.LogService;

/**
 * To be used once only in a single thread only. Not thread safe.
 */
public class BucketIOFactory<D>
{
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

    private BucketIO.Profile profile;

    public static class TsvConfig
        implements BucketIO.Configuration.Tsv
    {
        public String[] headers;
        public String[] columns;

        @Override
        public BucketIO.Profile profile()
        {
            return BucketIO.Profile.MULTI_TSV;
        }

        @Override
        public Optional<String[]> headers()
        {
            return Optional.ofNullable( headers );
        }

        @Override
        public String[] columns()
        {
            return columns;
        }
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

        public BucketIO.Configuration.Tsv get()
        {
            return config;
        }
    }

    public static class MultiJsonConfig
        implements BucketIO.Configuration.MultiJson
    {
        @Override
        public BucketIO.Profile profile()
        {
            return BucketIO.Profile.MULTI_JSON;
        }

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
        implements BucketIO.Configuration.PartitionedJson<E>
    {
        public String bucketFilter;
        public BucketFunction<E> bucketFunction;

        @Override
        public BucketIO.Profile profile()
        {
            return BucketIO.Profile.PARTITIONED_JSON;
        }

        @Override
        public String bucketFilter()
        {
            return bucketFilter;
        }

        @Override
        public BucketFunction<E> bucketFunction()
        {
            return bucketFunction;
        }
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

    public static class SingleObjectConfig
        implements BucketIO.Configuration.SingleObject
    {
        public String bucketName;

        @Override
        public BucketIO.Profile profile()
        {
            return BucketIO.Profile.SINGLE;
        }

        @Override
        public String bucketName()
        {
            return bucketName;
        }
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

    private boolean isUsed;

    private BucketIO.Configuration.Tsv tsvConfig;
    @SuppressWarnings( "unused" ) // There are no values to configure at this time
    private BucketIO.Configuration.MultiJson multiJsonConfig;
    private BucketIO.Configuration.PartitionedJson<D> partitionedJsonConfig;
    private BucketIO.Configuration.SingleObject singleObjectConfig;

    private Serializer serializer;
    private Writer writer;
    private LogService logger;
    private Class<D> dtoClass;

    private Function<D, D> preprocessor;

    private BucketDescriptor<D> descriptor;
    private BucketDescriptor.Single<D> singleObjectDescriptor;

    private final List<String> errors = new ArrayList<>();

    private BucketIOFactory( Class<D> aDTOClass )
    {
        dtoClass = aDTOClass;
    }

    public BucketIOFactory<D> setSerializer( Serializer aSerializer )
    {
        assertNotUsed();
        return setSerializer( aSerializer, null );
    }

    public BucketIOFactory<D> setSerializer( Serializer aSerializer, Writer aWriter )
    {
        assertNotUsed();
        serializer = aSerializer;
        writer = aWriter;
        return this;
    }

    public BucketIOFactory<D> setLogService( LogService aLogService )
    {
        assertNotUsed();
        logger = aLogService;
        return this;
    }

    public BucketIOFactory<D> preprocess( Function<D, D> aPreprocessor )
    {
        assertNotUsed();
        preprocessor = aPreprocessor;
        return this;
    }

    public BucketIOFactory<D> configureWith( BucketDescriptor<D> aDescriptor )
    {
        assertNotUsed();
        if (aDescriptor instanceof BucketDescriptor.Single)
            singleObjectDescriptor = (BucketDescriptor.Single<D>)aDescriptor;
        else
            descriptor = aDescriptor;
        return this;
    }

    @SuppressWarnings( { "rawtypes", "unchecked" } )
    public BucketIOFactory<D> useConfiguration( BucketIO.ConfigurationProfile withConfiguration )
    {
        assertNotUsed();

        final BucketIO.Profile configurationProfile = withConfiguration.profile();
        switch( configurationProfile )
        {
            case MULTI_TSV :
                return useConfiguration( (BucketIO.Configuration.Tsv)withConfiguration );

            case MULTI_JSON :
                return useConfiguration( (BucketIO.Configuration.MultiJson)withConfiguration );

            case PARTITIONED_JSON :
                return useConfiguration( (BucketIO.Configuration.PartitionedJson)withConfiguration );

            case SINGLE :
                return useConfiguration( (BucketIO.Configuration.SingleObject)withConfiguration );

            default :
                // Do nothing. Error will be caught later.
                return this;
        }
    }

    public BucketIOFactory<D> useConfiguration( BucketIO.Configuration.Tsv withConfiguration )
    {
        assertNotUsed();
        tsvConfig = withConfiguration;
        profile = BucketIO.Profile.MULTI_TSV;
        return this;
    }

    public BucketIOFactory<D> useConfiguration( BucketIO.Configuration.MultiJson withConfiguration )
    {
        assertNotUsed();
        multiJsonConfig = withConfiguration;
        profile = BucketIO.Profile.MULTI_JSON;
        return this;
    }

    public BucketIOFactory<D> useConfiguration( BucketIO.Configuration.PartitionedJson<D> withConfiguration )
    {
        assertNotUsed();
        partitionedJsonConfig = withConfiguration;
        profile = BucketIO.Profile.PARTITIONED_JSON;
        return this;
    }

    public BucketIOFactory<D> useConfiguration( BucketIO.Configuration.SingleObject withConfiguration )
    {
        assertNotUsed();
        singleObjectConfig = withConfiguration;
        profile = BucketIO.Profile.SINGLE;
        return this;
    }

    public BucketIO<D> get()
        throws UncheckedBucketException
    {
        assertNotUsed();

        errors.addAll( validateCommonConfig() );
        if( !errors.isEmpty() )
            throw new UncheckedBucketException( errors.get( 0 ) );

        final BucketIO<D> io;
        if (profile == BucketIO.Profile.MULTI_TSV)
        {
            if (!validateTsvConfig().isEmpty() )
                throw new UncheckedBucketException( validateTsvConfig().get( 0 ) );

            io = newTabSeparatedValuesIO();
        }
        else if (profile == BucketIO.Profile.MULTI_JSON)
        {
            if (!validateMultiJsonConfig().isEmpty() )
                throw new UncheckedBucketException( validateMultiJsonConfig().get( 0 ) );

            io = newMultiJsonIO();
        }
        else if (profile == BucketIO.Profile.PARTITIONED_JSON)
        {
            if (!validatePartitionedJsonConfig().isEmpty() )
                throw new UncheckedBucketException( validatePartitionedJsonConfig().get( 0 ) );

            io = newPartitionedJsonIO();
        }
        else if (profile == BucketIO.Profile.SINGLE)
        {
            if (!validateSingleObjectJsonConfig().isEmpty() )
                throw new UncheckedBucketException( validateSingleObjectJsonConfig().get( 0 ) );

            io = newSingleObjectJsonIO();
        }
        else
            throw new UncheckedBucketException( "Could not instantiate a BucketReader" );

        isUsed = true;
        return io;
    }

    private List<String> validateCommonConfig()
    {
        final List<String> errors = new ArrayList<>();

        if( serializer == null )
            errors.add( "Serializer is not set" );
        if (profile == BucketIO.Profile.SINGLE)
        {
            if (singleObjectDescriptor == null)
                errors.add( "SingleObjectDescriptor is not set" );
        }
        else
        {
            if(profile != BucketIO.Profile.SINGLE && descriptor == null )
                errors.add( "BucketDescriptor is not set" );
        }

        return errors;
    }

    private DelimiterSeparatedValuesIO<D> newTabSeparatedValuesIO()
    {
        return new DelimiterSeparatedValuesIO<>(
                descriptor,
                "/t",
                tsvConfig.headers(),
                tsvConfig.columns(),
                preprocessor,
                serializer );
    }

    private List<String> validateTsvConfig()
    {
        final List<String> errors = new ArrayList<>();

        try
        {
            if (descriptor.format() != Bucket.Format.TSV)
                errors.add( String.format( "Expected TSV format, but was %s", descriptor.format() ) );

            if (descriptor.packaging() != Bucket.Packaging.MULTI)
                errors.add( String.format( "Expected MULTI packaging, but was %s", descriptor.packaging() ) );

            if (tsvConfig.columns() == null || tsvConfig.columns().length <= 0)
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
        return new MultiJsonIO<>( 
                dtoClass,
                descriptor,
                preprocessor,
                serializer,
                writer,
                logger );
    }

    private List<String> validateMultiJsonConfig()
    {
        final List<String> errors = new ArrayList<>();

        final String bucketName = new StringBuilder()
                .append( descriptor.brn() )
                .append( "." )
                .append( Bucket.Format.JSON.name().toLowerCase() )
                .toString();
        try
        {
            BucketNameParser.newParser().parse( bucketName, descriptor.packaging() );
        }
        catch ( Exception e )
        {
            errors.add( e.getMessage() );
        }

        try
        {
            if (descriptor.format() != Bucket.Format.JSON)
                errors.add( String.format( "Expected JSON format, but was %s", descriptor.format() ) );

            if (descriptor.packaging() != Bucket.Packaging.MULTI)
                errors.add( String.format( "Expected MULTI packaging, but was %s", descriptor.packaging() ) );

            if (!descriptor.containerName().isPresent())
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
        return new PartitionedJsonIO<>( 
                dtoClass,
                descriptor,
                partitionedJsonConfig.bucketFilter(),
                partitionedJsonConfig.bucketFunction(),
                preprocessor,
                serializer,
                writer );
    }

    private List<String> validatePartitionedJsonConfig()
    {
        final List<String> errors = new ArrayList<>();

        try
        {
            if (partitionedJsonConfig.bucketFunction() == null)
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
        return new SingleObjectJsonIO<>( 
                dtoClass,
                singleObjectDescriptor,
                singleObjectConfig.bucketName(),
                preprocessor,
                serializer,
                writer );
    }

    private List<String> validateSingleObjectJsonConfig()
    {
        final List<String> errors = new ArrayList<>();

        try
        {
            if (singleObjectConfig.bucketName() == null)
                errors.add( "BucketFunction is null" );
        }
        catch ( Exception e )
        {
            errors.add( e.getMessage() );
        }

        return errors;
    }    

    private void assertNotUsed()
    {
        if (isUsed)
            throw new IllegalStateException( "This Factory has already been used" );
    }
}
