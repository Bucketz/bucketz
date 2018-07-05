package org.bucketz.lib;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.felix.schematizer.StandardSchematizer;
import org.apache.felix.serializer.Serializer;
import org.apache.felix.serializer.Writer;
import org.bucketz.Bucket;
import org.bucketz.BucketDescriptor;
import org.bucketz.BucketStore;
import org.bucketz.Codec;
import org.osgi.util.converter.Converter;

/**
 * The Partitioned packaging splits up each Entity into its own file.
 * There is exactly one Entity per file. The file structure may have some
 * organizational hierarchy, or it may be flat.
 * 
 * When reading, either the list of provided Bucket names or the BucketFilter 
 * determines whether or not the file is part of the AR. When writing, the 
 * BucketFuction will transform the entity into a Bucket name.
 */
public class PartitionedJsonIO<E>
    implements BucketIO<E>
{
    private Serializer serializer;
    private Writer writer;
    private Codec<E> codec;

    private Function<E, E> preprocessor;
    private boolean preprocess;

    private String confinement;
    private String version;
    private BucketStore.Packaging packaging;
    private Set<Pattern> bucketFilters = new HashSet<>();

    private final Class<E> dtoClass;

    private BucketFunction<E> bucketFunction;

    PartitionedJsonIO( Class<E> aDTOClass )
    {
        dtoClass = aDTOClass;
    }

    public PartitionedJsonIO<E> setSerializer( Serializer aSerializer )
    {
        serializer = aSerializer;
        return this;
    }

    public PartitionedJsonIO<E> setSerializer( Serializer aSerializer, Writer aWriter )
    {
        serializer = aSerializer;
        writer = aWriter;
        return this;
    }

    public PartitionedJsonIO<E> configureWith( BucketDescriptor<E> aDescriptor )
    {
        version = aDescriptor.version();
        packaging = aDescriptor.packaging();
        codec = new DefaultJsonConverter<>( aDescriptor, serializer );
        return this;
    }

    public PartitionedJsonIO<E> addBucketFilter( String aFilter )
    {
        final Pattern p = Pattern.compile( aFilter );
        bucketFilters.add( p );
        return this;
    }

    public PartitionedJsonIO<E> setBucketFunction( BucketFunction<E> aBucketFunction )
    {
        bucketFunction = aBucketFunction;
        return this;
    }

    public PartitionedJsonIO<E> preprocess( Function<E, E> aPreprocessor )
    {
        preprocess = true;
        preprocessor = aPreprocessor;
        return this;
    }

    @Override
    public List<Bucket> write( Stream<E> stream, String url )
        throws IOException
    {
        final List<String> errors = validateConfig();
        if( !errors.isEmpty() )
            throw new IOException( errors.get( 0 ) );

        try
        {
            final List<Bucket> buckets = stream
                .collect( Collectors.toMap( 
                        e -> bucketFunction.toBucket( e ), 
                        e -> serialize( e ) ) )
                .entrySet().stream()
                .map( newBucket( url ) )
                .collect( Collectors.toList() );

            return buckets;
        }
        catch ( Exception e )
        {
            throw new IOException( e );
        }
    }

    private String serialize( E entity )
    {
        return (writer != null ) ?
                serializer
                    .serialize( entity )
                    .writeWith( writer )
                    .sourceAsDTO()
                    .toString() :
                serializer
                    .serialize( entity )
                    .sourceAsDTO()
                    .toString();
    }

    @Override
    @SuppressWarnings( "rawtypes" )
    public Stream<E> read( Bucket bucket )
        throws IOException
    {
        final List<String> errors = validateConfig();
        if( !errors.isEmpty() )
            throw new IOException( errors.get( 0 ) );

        final URI bucketUri = bucket.asUri();

        // If this is not a "watched" bucket, then ignore it.
        // This is not a failure. Just return an empty stream.
        if( !matchesBucketFilters.test( bucketUri.toString() ) )
            return Stream.empty();

        try
        {
            final Map m = serializer
                    .deserialize( Map.class )
                    .from( bucketUri.toURL().openStream() );

            final String objectName = dtoClass.getTypeName();
            final Converter converter = new StandardSchematizer()
                    .schematize( objectName, dtoClass )
                    .converterFor( objectName );

            E entity = converter.convert( m ).to( dtoClass );
            if( preprocess )
                entity = preprocessor.apply( entity );

            Stream<E> s = Stream.of( entity );

            return s;
        }
        catch ( IOException e )
        {
            throw e;
        }
        catch ( Exception e )
        {
            throw new IOException( e );
        }
    }

    @Override
    public Coder<E> coder()
    {
        return codec.coder();
    }

    @Override
    public Decoder<E> decoder()
    {
        return codec.decoder();
    }

    private List<String> validateConfig()
    {
        final List<String> errors = new ArrayList<>();
        if( serializer == null )
            errors.add( "Serializer is not set" );
        if( confinement == null )
            errors.add( "Confinement is not set" );
        if( version == null )
            errors.add( "Version is not set" );
        if( packaging == null )
            errors.add( "Bucket packaging is not set" );
        if( bucketFunction == null )
            errors.add( "BucketFunction is not set" );
        if (preprocess && preprocessor == null)
            errors.add( "Preprocessor is invalid" );

        return errors;
    }

    private Predicate<String> matchesBucketFilters = s -> {
        return bucketFilters.stream()
                .anyMatch( p -> p.matcher( s ).matches() );
    };

    private Function<Map.Entry<String, String>, Bucket> newBucket( String url )
    {
        return e -> {
            final BucketStore.BucketDTO dto = new BucketStore.BucketDTO();
            final BucketStore.BucketContextDTO bucketContext = new BucketStore.BucketContextDTO();

            try
            {
                final BucketName bp = BucketNameParser.newParser().parse( e.getKey(), packaging );
                bucketContext.innerPath = bp.innerPath;
                bucketContext.simpleName = bp.simpleName;
                bucketContext.format = bp.format;
            }
            catch ( Exception ex )
            {
                // TODO Handle this error
            }

            bucketContext.packaging = packaging.name();
            dto.context = bucketContext;
            dto.content = e.getValue();
            dto.location = url;
            dto.descriminant = ""; // TODO
            final Bucket b = BucketFactory.newBucket( dto );
            return b;
        };
    };
}
