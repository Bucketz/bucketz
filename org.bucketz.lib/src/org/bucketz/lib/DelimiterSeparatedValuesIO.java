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
import org.bucketz.BucketDescriptor;
import org.bucketz.BucketStore;
import org.bucketz.Codec;

/**
 * All Entities are in the same Bucket, but stores in the form of a Delimiter-Separated-Values
 * form. Tabs are the default delimiter. Each line therefore represents a single Entity, each field 
 * being separated by the delimiter (i.e. tab).
 * 
 * There is one and only one Bucket, which may have an inner path.
 */
public class DelimiterSeparatedValuesIO<E>
    implements BucketIO<E>
{
    public static final String NULL = "##NULL##";
    public static final String DELIMITER = "##D##";

    private Serializer serializer;
    private Codec<E> codec;
    private Optional<Function<E, E>> preprocessor = Optional.empty();

    private BucketDescriptor<E> descriptor;

    /* Tabs are the default */
    private String delimiter = "\t";
    private boolean includeHeaders = false;
    private String[] headers;
    private String[] columns;
    private String version;
    private String innerPath;
    private String simpleName;
    /* TSV is the default */
    private String format = BucketStore.Format.TSV.name();
    private BucketStore.Packaging packaging;
    private boolean doSort;
    private Comparator<E> comparator;

    final List<String> errors = new ArrayList<>();

    DelimiterSeparatedValuesIO( Class<E> aDTOClass ) {}

    public DelimiterSeparatedValuesIO<E> setSerializer( Serializer aSerializer )
    {
        serializer = aSerializer;
        return this;
    }

    public DelimiterSeparatedValuesIO<E> configureWith( BucketDescriptor<E> aDescriptor )
    {
        version = aDescriptor.version();
        packaging = aDescriptor.packaging();
        comparator = aDescriptor.comparator().orElse( null );
        doSort = comparator != null;

        final String bucketName = new StringBuilder()
                .append( aDescriptor.brn() )
                .append( "." )
                .append( format.toLowerCase() )
                .toString();
        try
        {
            final BucketName bp = BucketNameParser.newParser().parse( bucketName, packaging );
            innerPath = bp.innerPath;
            simpleName = bp.simpleName;
            format = bp.format;
        }
        catch ( Exception e )
        {
            // TODO Handle this error
        }
        descriptor = aDescriptor;
        return this;
    }

    public DelimiterSeparatedValuesIO<E> preprocess( Function<E, E> aPreprocessor )
    {
        preprocessor = Optional.ofNullable( aPreprocessor );
        return this;
    }

    public DelimiterSeparatedValuesIO<E> useTabDelimiters()
    {
        delimiter = "\t";
        return this;
    }

    public DelimiterSeparatedValuesIO<E> includeHeaders()
    {
        includeHeaders = true;
        return this;
    }

    public DelimiterSeparatedValuesIO<E> setColumns( String... aColumnList )
    {
        columns = aColumnList;
        return this;
    }

    private Codec<E> codec()
    {
        if (codec == null)
            codec = new DefaultTsvConverter<>( 
                    delimiter, 
                    columns, 
                    NULL, 
                    preprocessor, 
                    descriptor, 
                    serializer );

        return codec;
    }

    @Override
    public Codec.Coder<E> coder()
    {
        return codec().coder();
    }

    @Override
    public Codec.Decoder<E> decoder()
    {
        return codec().decoder();
    }

    @Override
    public Stream<E> read( Bucket bucket )
        throws IOException
    {
        errors.addAll( validateConfig() );
        if( !errors.isEmpty() )
            throw new IOException( errors.get( 0 ) );

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

            final List<E> lineObjects = new ArrayList<>();

            while( line != null )
            {
                E processedLineObject;
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

            final Stream<E> s = lineObjects.stream();
            return s;
        }
        catch (IOException e)
        {
            throw e;
        }
        catch ( Exception e )
        {
            throw new IOException( e );
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
    public List<Bucket> write( Stream<E> stream, String url )
        throws IOException
    {
        final List<String> errors = validateConfig();
        if( !errors.isEmpty() )
            throw new IOException( errors.get( 0 ) );

        try
        {
            final List<E> allItems = ( doSort ) ?
                    stream.sorted( comparator ).collect( Collectors.toList() ) :
                    stream.collect( Collectors.toList() );
            final List<String> lines = new ArrayList<>();
            for (E item : allItems)
            {
                final String string = codec().coder().encode( item );
                lines.add( string );
            }
            if( includeHeaders )
                lines.add( 0, headerLine() );
            final String content = lines.stream()
                    .collect( Collectors.joining( "\n" ) );

            final Bucket singleBucket = newBucket( content, url );

            final List<Bucket> bucketList = new ArrayList<>();
            bucketList.add( singleBucket );

            return bucketList;
        }
        catch (IOException e)
        {
            throw e;
        }
        catch ( Exception e )
        {
            throw new IOException( e );
        }
    }

    private Bucket newBucket( String content, String url )
    {
        final BucketStore.BucketDTO dto = new BucketStore.BucketDTO();
        final BucketStore.BucketContextDTO bucketContext = new BucketStore.BucketContextDTO();
        dto.context = bucketContext;
        dto.context.innerPath = innerPath;
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
