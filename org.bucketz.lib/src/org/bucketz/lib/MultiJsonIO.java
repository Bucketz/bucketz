package org.bucketz.lib;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
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
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.log.LogService;
import org.osgi.util.converter.Converter;

/**
 * A MultiJson packaging puts all the entities into one single JSON Bucket.
 * For this reason, the Bucket contains "Multi" JSON contents.
 * 
 * There is one and only one Bucket, which has a known name.
 * 
 * This class is immutable and therefore thread safe.
 */
public class MultiJsonIO<D>
    implements BucketIO<D>
{
    private final Serializer serializer;
    private final org.apache.felix.serializer.Writer writer;
    private final LogService logger;
    private final Codec<D> codec;
    private final Function<D, D> preprocessor;
    private final boolean preprocess;

    private final Class<D> dtoClass;

    private final String arrayName;// = "data";
    private final String version;
    private final String innerPath;
    private final String simpleName;
    private final String format;
    private final Bucket.Packaging packaging;
    private final boolean doSort;
    private final Comparator<D> comparator;

    MultiJsonIO( 
            Class<D> aDTOClass,
            BucketDescriptor<D> aDescriptor,
            Function<D, D> aPreprocessor,
            Serializer aSerializer,
            Writer aWriter,
            LogService aLogService )
        throws UncheckedBucketException
    {
        dtoClass = aDTOClass;
        version = aDescriptor.version();
        packaging = aDescriptor.packaging();
        arrayName = aDescriptor.containerName().orElseGet( null );
        comparator = aDescriptor.comparator().orElse( null );
        doSort = comparator != null;

        final String bucketName = new StringBuilder()
                .append( aDescriptor.brn() )
                .append( "." )
                .append( Bucket.Format.JSON.name().toLowerCase() )
                .toString();
        final BucketName bp = BucketNameParser.newParser().parse( bucketName, packaging );
        innerPath = bp.innerPath;
        simpleName = bp.simpleName;
        format = bp.format;
        preprocessor = aPreprocessor;
        preprocess = aPreprocessor != null;
        serializer = aSerializer;
        codec = new DefaultJsonConverter<>( aDescriptor, serializer );
        writer = aWriter;
        logger = aLogService;
    }

    @SuppressWarnings( { "rawtypes", "unchecked" } )
    @Override
    public Stream<D> debucketize( Bucket bucket )
        throws UncheckedBucketException
    {
        final List<String> errors = validateConfig();
        if( !errors.isEmpty() )
            throw new UncheckedBucketException( errors.get( 0 ) );

        final URI bucketUri = bucket.asUri();

        // If this is not the named Bucket, then ignore it.
        // This is not a failure. Just return an empty stream.
        if( !isBucket( bucketUri.toString() ) )
            return Stream.empty();

        try
        {
            final Map m = serializer
                    .deserialize( Map.class )
                    .from( new NullCapturingInputStream( bucketUri.toURL().openStream() ) );

            final Object arrayObject = m.get( arrayName );
            if( arrayObject == null || !( arrayObject instanceof List ) )
                throw new IOException( "Invalid data format for bucket '" + bucket.fqn() + "'. "
                        + "Must be formatted as an object with a single value name '" + arrayName + "' "
                                + "containing an array of the desired object type." );

            final List<D> list = (List<D>)arrayObject;

            final Converter converter = new StandardSchematizer()
                    .schematize( arrayName, dtoClass )
                    .converterFor( arrayName );

            Stream<D> s = list.stream()
                    .map( o -> converter.convert( o ).to( dtoClass ) )
                    .map( o -> preprocess ? preprocessor.apply( o ) : o );

            return s;
        }
        catch ( Exception e )
        {
            if ((e instanceof NullCapturingInputStream.NullCapturedException) || (e.getCause() instanceof NullCapturingInputStream.NullCapturedException))
            {
                warn( e, String.format( "Bucket contains no data: %s", bucket.fqn() ) );
                return Stream.empty();
            }
            else
            {
                throw new UncheckedBucketException( String.format( "Could not parse data for bucket: %s.", bucket.fqn() ), e );
            }
        }
    }

    private void warn( Exception e, String message )
    {
        if( logger != null )
            logger.log( 
                    FrameworkUtil.getBundle( getClass() ).getBundleContext().getServiceReference( getClass() ), 
                    LogService.LOG_WARNING, 
                    message, 
                    e );
        else
            e.printStackTrace();
    }

    /*
     * TODO: Is this sufficient? Or should we match the full name?
     */
    private boolean isBucket( String aBucketString )
    {
        final String bucketName = new StringBuilder()
                .append( simpleName )
                .append( "." )
                .append( format.toLowerCase() )
                .toString();
        return aBucketString.endsWith( bucketName );
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
            final List<Bucket> buckets = new ArrayList<>();
            final Map<String, List<D>> output = new LinkedHashMap<>();
            final List<D> allItems = ( doSort ) ?
                    stream.sorted( comparator ).collect( Collectors.toList() ) :
                    stream.collect( Collectors.toList() );
            output.put( arrayName, allItems );
            final String content = toString( arrayName, output );
            buckets.add( newBucket( content, outerPath, url ) );

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
        if( doSort && comparator == null )
            errors.add( "Sort is configured, but comparator is invalid" );
        if( version == null )
            errors.add( "Version is not set" );
        if( simpleName == null )
            errors.add( "Bucket SimpleName is not set" );
        if( format == null )
            errors.add( "Bucket format is not set" );
        if( packaging == null )
            errors.add( "Bucket packaging is not set" );
        if (preprocess && preprocessor == null)
            errors.add( "Preprocessor is invalid" );

        return errors;
    }

    private Bucket newBucket( String content, String outerPath, String url )
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
        dto.location = url;
        final Bucket b = BucketFactory.newBucket( dto );
        return b;
    }

    private String toString( String arrayName, Map<String,List<D>> output )
    {
        if( writer == null )
            return serializer.serialize( output )
                .sourceAsDTO()
                .toString();

        final Map<String, Comparator<?>> arrayRules = writer.arrayOrderingRules();
        final Map<String, Comparator<?>> originalArrayRules = writer.arrayOrderingRules().entrySet().stream()
                .collect( Collectors.toMap( 
                        e -> e.getKey(), 
                        e -> e.getValue() ) );
        final Map<String, Comparator<?>> updatedArrayRules = writer.arrayOrderingRules().entrySet().stream()
                .collect( Collectors.toMap(
                        e -> updatePath( arrayName, e.getKey() ), 
                        e -> e.getValue() ) );

        arrayRules.clear();
        arrayRules.putAll( updatedArrayRules );

        final Map<String, List<String>> mapRules = writer.mapOrderingRules();
        final Map<String, List<String>> originalMapRules = writer.mapOrderingRules().entrySet().stream()
                .collect( Collectors.toMap( 
                        e -> e.getKey(), 
                        e -> e.getValue() ) );
        final Map<String, List<String>> updatedMapRules = writer.mapOrderingRules().entrySet().stream()
                .collect( Collectors.toMap( 
                        e -> updatePath( arrayName, e.getKey() ), 
                        e -> e.getValue() ) );

        mapRules.clear();
        mapRules.putAll( updatedMapRules );

        final String serialized = serializer.serialize( output )
                .writeWith( writer )
                .sourceAsDTO()
                .toString();

        // Now reset the rules back to their original states
        arrayRules.clear();
        arrayRules.putAll( originalArrayRules );
        mapRules.clear();
        mapRules.putAll( originalMapRules );        

        return serialized;
    }

    private String updatePath( String arrayName, String oldPath )
    {
        String newPath = "/" + arrayName + oldPath;
        if( newPath.endsWith( "/" ) )
            newPath = newPath.substring( 0, newPath.length() - 1 );
        return newPath;
    }
}
