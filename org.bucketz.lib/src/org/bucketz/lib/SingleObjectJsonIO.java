package org.bucketz.lib;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
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
 * This class is immutable and therefore thread safe.
 */
public class SingleObjectJsonIO<D>
    implements BucketIO<D>
{
    private final Serializer serializer;
    private final Writer writer;
    private final Function<D, D> preprocessor;
    private final Codec<D> codec;
    private final boolean preprocess;

    private final Class<D> dtoClass;

    private final String version;
    private final String innerPath;
    private final String simpleName;
    private final String format;

    private Bucket.Packaging packaging;

    SingleObjectJsonIO( 
            Class<D> aDTOClass,
            BucketDescriptor.Single<D> aDescriptor,
            String aBucketName,
            Function<D, D> aPreprocessor,
            Serializer aSerializer,
            Writer aWriter)
    {
        dtoClass = aDTOClass;
        version = aDescriptor.version();
        packaging = aDescriptor.packaging();
        final BucketName bp = BucketNameParser.newParser().parse( aBucketName, packaging );
        innerPath = bp.innerPath;
        simpleName = bp.simpleName;
        format = bp.format;
        preprocessor = aPreprocessor;
        preprocess = preprocessor != null;
        serializer = aSerializer;
        codec = new DefaultJsonConverter<>( aDescriptor, serializer );
        writer = aWriter;
    }

    @SuppressWarnings( "rawtypes" )
    @Override
    public Stream<D> debucketize( Bucket bucket )
        throws UncheckedBucketException
    {
        final List<String> errors = validateConfig();
        if( !errors.isEmpty() )
            throw new UncheckedBucketException( errors.get( 0 ) );

        final URI bucketUri = bucket.asUri();
        try
        {
            final Map m = serializer
                    .deserialize( Map.class )
                    .from( bucketUri.toURL().openStream() );

            final String objectName = dtoClass.getTypeName();
            final Converter converter = new StandardSchematizer()
                    .schematize( objectName, dtoClass )
                    .converterFor( objectName );

            final D dto = converter.convert( m ).to( dtoClass );
            final D processedDTO = preprocess ? preprocessor.apply( dto ) : dto;

            return Stream.of( processedDTO );
        }
        catch ( Exception e )
        {
            throw new UncheckedBucketException( e );
        }
    }

    @Override
    public List<Bucket> bucketize( Stream<D> stream, String url, String outerPath )
        throws UncheckedBucketException
    {
        final List<String> errors = validateConfig();

        if( !errors.isEmpty() )
            throw new UncheckedBucketException( errors.get( 0 ) );

        try
        {
            final D singleDTO = stream
                    .collect( Collectors.toList() )
                    .get( 0 );

            final String content = (writer != null ) ?
                    serializer.serialize( singleDTO )
                        .writeWith( writer )
                        .sourceAsDTO()
                        .toString() :
                    serializer.serialize( singleDTO )
                        .sourceAsDTO()
                        .toString();

            final List<Bucket> buckets = new ArrayList<>();
            final Bucket bucket = newBucket( content, outerPath, url );
            buckets.add( bucket );
            return buckets;
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
        if( innerPath == null )
            errors.add( "Inner Path is not set" );
        if( simpleName == null )
            errors.add( "Bucket SimpleName is not set" );
        if( format == null )
            errors.add( "Format is not set" );
        if( packaging == null )
            errors.add( "Packaing is not set" );
        if (preprocess && preprocessor == null)
            errors.add( "Preprocessor is invalid" );

        return errors;
    }

    private Bucket newBucket( String content, String outerPath, String location )
    {
        final BucketStore.BucketDTO dto = new BucketStore.BucketDTO();
        final BucketStore.BucketContextDTO bucketContext = new BucketStore.BucketContextDTO();
        dto.context = bucketContext;
        dto.context.innerPath = innerPath;
        dto.context.outerPath = outerPath;
        dto.context.simpleName = simpleName;
        dto.context.format = format;
        dto.context.packaging = packaging.name();
        dto.content = content;
        dto.location = location;
        final Bucket b = BucketFactory.newBucket( dto );
        return b;
    }
}
