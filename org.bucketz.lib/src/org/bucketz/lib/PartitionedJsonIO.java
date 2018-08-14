package org.bucketz.lib;

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
import org.bucketz.BucketIO;
import org.bucketz.Codec;
import org.bucketz.UncheckedBucketException;
import org.bucketz.store.BucketDescriptor;
import org.bucketz.store.BucketStore;
import org.osgi.util.converter.Converter;

/**
 * The Partitioned packaging splits up each DTO into its own file.
 * There is exactly one DTO per file. The file structure may have some
 * organizational hierarchy, or it may be flat.
 * 
 * When reading, either the list of provided Bucket names or the BucketFilter 
 * determines whether or not the file is part of the AR. When writing, the 
 * BucketFuction will transform the DTO into a Bucket name.
 * 
 * This class is immutable and therefore thread safe.
 */
public class PartitionedJsonIO<D>
    implements BucketIO<D>
{
    private final Serializer serializer;
    private final Writer writer;
    private final Codec<D> codec;

    private final Function<D, D> preprocessor;
    private final boolean preprocess;

    private final String version;
    private final Bucket.Packaging packaging;
    private final Set<Pattern> bucketFilters = new HashSet<>();

    private final Class<D> dtoClass;

    private BucketIO.BucketFunction<D> bucketFunction;

    PartitionedJsonIO( 
            Class<D> aDTOClass,
            BucketDescriptor<D> aDescriptor,
            String aFilter,
            BucketIO.BucketFunction<D> aBucketFunction,
            Function<D, D> aPreprocessor,
            Serializer aSerializer,
            Writer aWriter )
    {
        dtoClass = aDTOClass;
        version = aDescriptor.version();
        packaging = aDescriptor.packaging();
        final Pattern p = Pattern.compile( aFilter );
        bucketFilters.add( p );
        bucketFunction = aBucketFunction;
        preprocess = true;
        preprocessor = aPreprocessor;
        serializer = aSerializer;
        codec = new DefaultJsonConverter<>( aDescriptor, serializer );
        writer = aWriter;
    }

    @Override
    public List<Bucket> bucketize( Stream<D> stream, String url, String outerPath, Object o )
        throws UncheckedBucketException
    {
        final List<String> errors = validateConfig();
        if( !errors.isEmpty() )
            throw new UncheckedBucketException( errors.get( 0 ) );

        try
        {
            final List<Bucket> buckets = stream
                .collect( Collectors.toMap( 
                        e -> bucketFunction.toBucket( e ), 
                        e -> serialize( e ) ) )
                .entrySet().stream()
                .map( newBucket( outerPath, url ) )
                .collect( Collectors.toList() );

            return buckets;
        }
        catch ( Exception e )
        {
            throw new UncheckedBucketException( e );
        }
    }

    private String serialize( D dto )
    {
        return (writer != null ) ?
                serializer
                    .serialize( dto )
                    .writeWith( writer )
                    .sourceAsDTO()
                    .toString() :
                serializer
                    .serialize( dto )
                    .sourceAsDTO()
                    .toString();
    }

    @Override
    @SuppressWarnings( "rawtypes" )
    public Stream<D> debucketize( Bucket bucket )
        throws UncheckedBucketException
    {
        final List<String> errors = validateConfig();
        if( !errors.isEmpty() )
            throw new UncheckedBucketException( errors.get( 0 ) );

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

            D dto = converter.convert( m ).to( dtoClass );
            if( preprocess )
                dto = preprocessor.apply( dto );

            Stream<D> s = Stream.of( dto );

            return s;
        }
        catch ( Exception e )
        {
            throw new UncheckedBucketException( e );
        }
    }

    @Override
    public Coder<D> coder()
    {
        return codec.coder();
    }

    @Override
    public Decoder<D> decoder()
    {
        return codec.decoder();
    }

    private List<String> validateConfig()
    {
        final List<String> errors = new ArrayList<>();
        if( serializer == null )
            errors.add( "Serializer is not set" );
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

    private Function<Map.Entry<String, String>, Bucket> newBucket( String outerPath, String url )
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
                bucketContext.outerPath = outerPath;
            }
            catch ( Exception ex )
            {
                // TODO Handle this error
            }

            bucketContext.packaging = packaging.name();
            dto.context = bucketContext;
            dto.content = e.getValue();
            dto.location = url;
            final Bucket b = BucketFactory.newBucket( dto );
            return b;
        };
    };
}
