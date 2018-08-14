package org.bucketz.lib;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.felix.serializer.Serializer;
import org.bucketz.Bucket;
import org.bucketz.BucketIO;
import org.bucketz.Codec;
import org.bucketz.UncheckedBucketException;
import org.bucketz.store.BucketDescriptor;
import org.bucketz.store.BucketStore;

/**
 * All Entities are in the same Bucket, but stored in Delimiter-Separated-Value form.
 * Tabs are the default delimiter. Each line therefore represents a single DTO, each field 
 * being separated by the delimiter (i.e. tab).
 * 
 * There is one and only one Bucket, which may have an inner path.
 * 
 * This class is immutable and therefore thread safe.
 */
public class DelimiterSeparatedValuesIO<D>
    implements BucketIO<D>
{
    public static final String NULL = "##NULL##";
    public static final String DELIMITER = "##D##";

    private final Serializer serializer;
    private final Codec<D> codec;
    private final Optional<Function<D, D>> preprocessor;

    private final BucketDescriptor<D> descriptor;

    private final String delimiter;
    private final boolean includeHeaders;
    private final String[] headers;
    private final String[] columns;
    private final String version;
    private final String innerPath;
    private final String simpleName;
    private final String format;
    private final Bucket.Packaging packaging;
    private final boolean doSort;
    private final Comparator<D> comparator;

    private final List<String> errors = new ArrayList<>();

    DelimiterSeparatedValuesIO( 
            BucketDescriptor<D> aDescriptor,
            String aDelimiter,
            Optional<String[]> aHeadersArray,
            String[] aColumnsArray,
            Function<D, D> aPreprocessor,
            Serializer aSerializer )
        throws UncheckedBucketException
    {
        version = aDescriptor.version();
        packaging = aDescriptor.packaging();
        format = aDescriptor.format().name();
        comparator = aDescriptor.comparator().orElse( null );
        doSort = comparator != null;

        final String bucketName = new StringBuilder()
                .append( aDescriptor.brn() )
                .append( "." )
                .append( format.toLowerCase() )
                .toString();
        final BucketName bp = BucketNameParser.newParser().parse( bucketName, packaging );
        innerPath = bp.innerPath;
        simpleName = bp.simpleName;
        descriptor = aDescriptor;

        delimiter = aDelimiter;
        includeHeaders = aHeadersArray.isPresent();
        headers = aHeadersArray.orElse( null );
        columns = aColumnsArray;
        preprocessor = Optional.ofNullable( aPreprocessor );
        codec = new DefaultTsvConverter<>( 
                delimiter, 
                columns, 
                NULL, 
                preprocessor, 
                descriptor );
        serializer = aSerializer;
    }

    private Codec<D> codec()
    {
        return codec;
    }

    @Override
    public Codec.Coder<D> coder()
    {
        return codec().coder();
    }

    @Override
    public Codec.Decoder<D> decoder()
    {
        return codec().decoder();
    }

    @Override
    public Stream<D> debucketize( Bucket bucket )
        throws UncheckedBucketException
    {
        errors.addAll( validateConfig() );
        if( !errors.isEmpty() )
            throw new UncheckedBucketException( errors.get( 0 ) );

        final URI bucketUri = bucket.asUri();

        // If this is not the named Bucket, then ignore it.
        // This is not a failure. Just return an empty stream.
        if( !isBucket( bucketUri.toString() ) )
            return Stream.empty();

        try
        {
            final InputStreamReader in = new InputStreamReader( bucketUri.toURL().openStream() );
            final LineNumberReader reader = new LineNumberReader( in );

            String line = reader.readLine();
            if( includeHeaders && line != null )
                line = reader.readLine();

            final List<D> lineObjects = new ArrayList<>();

            while( line != null )
            {
                D processedLineObject;
                try
                {
                    processedLineObject = codec().decoder().decode( line );
                }
                catch ( Exception e )
                {
                    throw new IOException( String.format( "LINE %s: %s", reader.getLineNumber(), e.getMessage() ), e );
                }
                lineObjects.add( processedLineObject );
                line = reader.readLine();
            }

            final Stream<D> s = lineObjects.stream();
            return s;
        }
        catch ( Exception e )
        {
            throw new UncheckedBucketException( e );
        }
    }

    private List<String> validateConfig()
    {
        final List<String> errors = new ArrayList<>();
        if( delimiter == null )
            errors.add( "Delimiter is not set" );
        if( serializer == null )
            errors.add( "Serializer is not set" );
        if( !areHeadersValid() )
            errors.add( "Headers are not valid" );
        if( doSort && comparator == null )
            errors.add( "Sort is configured, but comparator is invalid" );
        if( version == null )
            errors.add( "Version is not set" );
        if( simpleName == null )
            errors.add( "Bucket name is not set" );
        if( packaging == null )
            errors.add( "Bucket packaging is not set" );

        return errors;
    }

    /*
     * TODO: is this acceptable? Or should we match against the full name?
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
    public List<Bucket> bucketize( Stream<D> stream, String url, String outerPath, Object o )
        throws UncheckedBucketException
    {
        final List<String> errors = validateConfig();
        if( !errors.isEmpty() )
            throw new UncheckedBucketException( errors.get( 0 ) );

        try
        {
            final List<D> allItems = ( doSort ) ?
                    stream.sorted( comparator ).collect( Collectors.toList() ) :
                    stream.collect( Collectors.toList() );
            final List<String> lines = new ArrayList<>();
            for (D item : allItems)
            {
                final String string = codec().coder().encode( item );
                lines.add( string );
            }
            if( includeHeaders )
                lines.add( 0, headerLine() );
            final String content = lines.stream()
                    .collect( Collectors.joining( "\n" ) );

            final Bucket singleBucket = newBucket( content, outerPath, url );

            final List<Bucket> bucketList = new ArrayList<>();
            bucketList.add( singleBucket );

            return bucketList;
        }
        catch ( Exception e )
        {
            throw new UncheckedBucketException( e );
        }
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

    private String headerLine()
    {
        final StringBuilder headerLine = new StringBuilder();
        for( int i = 0; i < columns.length; i++ )
        {
            headerLine.append( headers[i] );
            if( i < columns.length - 1 )
                headerLine.append( delimiter );
        }

        return headerLine.toString();
    }

    private boolean areHeadersValid()
    {
        if( includeHeaders && ( headers == null || headers.length != columns.length ) )
            return false;

        return true;
    }
}
