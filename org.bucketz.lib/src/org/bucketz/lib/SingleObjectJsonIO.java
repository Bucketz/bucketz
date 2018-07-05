package org.bucketz.lib;

import java.io.IOException;
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
import org.bucketz.BucketStore;
import org.bucketz.Codec;
import org.bucketz.SingleObjectBucketDescriptor;
import org.osgi.util.converter.Converter;

public class SingleObjectJsonIO<E>
    implements BucketIO<E>
{
    private Serializer serializer;
    private Writer writer;
    private Function<E, E> preprocessor;
    private Codec<E> codec;
    private boolean preprocess;

    private final Class<E> dtoClass;

    private String version;
    private String innerPath;
    private String simpleName;
    private String format;

    private BucketStore.Packaging packaging;

    SingleObjectJsonIO( Class<E> aDTOClass )
    {
        dtoClass = aDTOClass;
    }

    public SingleObjectJsonIO<E> setSerializer( Serializer aSerializer )
    {
        return setSerializer( serializer, null );
    }

    public SingleObjectJsonIO<E> setSerializer( Serializer aSerializer, Writer aWriter )
    {
        serializer = aSerializer;
        writer = aWriter;
        return this;
    }

    public SingleObjectJsonIO<E> configureWith( SingleObjectBucketDescriptor<E> aDescriptor )
    {
        version = aDescriptor.version();
        packaging = aDescriptor.packaging();
        codec = new DefaultJsonConverter<>( aDescriptor, serializer );
        return this;
    }

    public SingleObjectJsonIO<E> setBucketName( String aBucketName )
    {
        try
        {
            final BucketName bp = BucketNameParser.newParser().parse( aBucketName, packaging );
            innerPath = bp.innerPath;
            simpleName = bp.simpleName;
            format = bp.format;
        }
        catch ( Exception e )
        {
            // TODO Handle error
            e.printStackTrace();
        }

        return this;
    }

    public SingleObjectJsonIO<E> preprocess( Function<E, E> aPreprocessor )
    {
        preprocess = true;
        preprocessor = aPreprocessor;
        return this;
    }

    @SuppressWarnings( "rawtypes" )
    @Override
    public Stream<E> read( Bucket bucket )
        throws IOException
    {
        final List<String> errors = validateConfig();
        if( !errors.isEmpty() )
            throw new IOException( errors.get( 0 ) );

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

            final E entity = converter.convert( m ).to( dtoClass );
            final E processedEntity = preprocess ? preprocessor.apply( entity ) : entity;

            return Stream.of( processedEntity );
        }
        catch (IOException e )
        {
            throw e;
        }
        catch ( Exception e )
        {
            throw new IOException( e );
        }
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
            final E singleEntity = stream
                    .collect( Collectors.toList() )
                    .get( 0 );

            final String content = (writer != null ) ?
                    serializer.serialize( singleEntity )
                        .writeWith( writer )
                        .sourceAsDTO()
                        .toString() :
                    serializer.serialize( singleEntity )
                        .sourceAsDTO()
                        .toString();

            final List<Bucket> buckets = new ArrayList<>();
            final Bucket bucket = newBucket( content, url );
            buckets.add( bucket );
            return buckets;
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

    private Bucket newBucket( String content, String location )
    {
        final BucketStore.BucketDTO dto = new BucketStore.BucketDTO();
        final BucketStore.BucketContextDTO bucketContext = new BucketStore.BucketContextDTO();
        dto.context = bucketContext;
        dto.context.innerPath = innerPath;
        dto.context.simpleName = simpleName;
        dto.context.format = format;
        dto.context.packaging = packaging.name();
        dto.content = content;
        dto.location = location;
        dto.descriminant = null; //TODO
        final Bucket b = BucketFactory.newBucket( dto );
        return b;
    }
}
