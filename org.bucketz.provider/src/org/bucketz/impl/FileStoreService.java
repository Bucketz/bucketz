package org.bucketz.impl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.bucketz.Bucket;
import org.bucketz.BucketIO;
import org.bucketz.Bucketz;
import org.bucketz.UncheckedBucketException;
import org.bucketz.lib.BucketContextualizer;
import org.bucketz.lib.BucketNameParser;
import org.bucketz.lib.BucketPathConverter;
import org.bucketz.store.BucketDescriptor;
import org.bucketz.store.BucketStore;
import org.bucketz.store.FileStore;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.log.LogService;
import org.osgi.util.promise.Deferred;
import org.osgi.util.promise.Promise;
import org.osgi.util.promise.Promises;

/**
 * Implementation note: We expect that operations with the file system will be very quick,
 * so IO operations are executed synchronously.
 * 
 * Since we do not anticipate a heavy number of transactions (most are done in-memory), we
 * use synchronization, guarding with "this" for simplicity. For even more simplicity, we
 * use course-grained synchronization for IO at the method level. If this proves to be to
 * contentious, synchronization should be made more fine-grained. At this time, however,
 * it would be a premature optimization.
 */
@Bucketz.Provide(type=Bucketz.TypeConstants.FILE)
@Component(
      name = FileStoreService.COMPONENT_NAME,
      service = {
              BucketStore.class,
              FileStore.class },
      factory = FileStore.PID
)
public class FileStoreService<D>
    implements FileStore<D>
{
    public static final String COMPONENT_NAME = FileStore.PID;

    private String name;

    private String location;
    private String outerPath;

    private BucketDescriptor<D> descriptor;
    private BucketIO<D> io;
    private Path baseLocation;
    private Path baseAndOuterPath;
    private URI uri;

    @Reference private LogService logger;

    @SuppressWarnings( "unchecked" )
    @Activate
    void activate( ComponentContext componentContext, FileStore.Configuration configuration, Map<String, Object> properties )
        throws Exception
    {
        name = configuration.name();
        location = configuration.location();
        outerPath = configuration.outerPath();

        if (outerPath.startsWith( "/" ))
            outerPath = outerPath.substring( 1 );

        if (!outerPath.isEmpty() && !outerPath.endsWith( "/" ))
            outerPath += "/";

        descriptor = (BucketDescriptor<D>)properties.get( Bucketz.Parameters.DESCRIPTOR );
        io = (BucketIO<D>)properties.get( Bucketz.Parameters.IO );

        final String base = location.replaceFirst( "^~", System.getProperty( "user.home" ) );
        baseLocation = new File( base ).toPath();
        uri = baseLocation.toUri();
        final Path outerPathPath = Paths.get( outerPath );
        baseAndOuterPath = baseLocation.resolve( outerPathPath );
    }

    @Override
    public String name()
    {
        return name;
    }

    @Override
    public Bucketz.Type type()
    {
        return Bucketz.Type.FILE;
    }

    @Override
    public synchronized List<String> buckets()
    {
        try
        {
            final Path base = baseAndOuterPath;
            if( !Files.exists( base ) && !Files.isDirectory( base ) )
                return Collections.emptyList();

//            final List<Path> paths = Files.walk( base ).collect( Collectors.toList() );
//            for (Path path : paths)
//            {
//                if (Files.isDirectory( path ))
//                    continue;
//                final String p = path.toFile().toURI().toString();
//                final String u = p.replace( base.toFile().toURI().toString(), "" );
//                final boolean isPathEmpty = !u.contains( "/" );
//                if (isPathEmpty && (descriptor.packaging() == BucketStore.Packaging.PARTITIONED))
//                    // Cannot process a Partitioned Bucket with an empty path
//                    continue;
//                if (!u.startsWith( descriptor.brn() ))
//                    continue;
//
//                if (descriptor.filter().isPresent())
//                    if (u.matches( descriptor.filter().get() ))
//                        toString(); // matches
//                    else
//                        toString(); // no match
//            }

            return Files.walk( base )
                    .parallel()
                    .filter( p -> !Files.isDirectory( p ) )
                    .map( p -> p.toFile().toURI().toString() )
                    .map( u -> u.replace( base.toFile().toURI().toString(), "" ) )
                    .filter( b -> !b.isEmpty() )
                    .filter( b -> !( !b.contains( "/" ) && ( descriptor.packaging() == Bucket.Packaging.PARTITIONED ) ) )
                    .filter( b -> b.startsWith( descriptor.brn() ) )
                    .filter( b -> (descriptor.filter().isPresent()) ? b.matches( descriptor.filter().get() ) : true )
                    .collect( Collectors.toList() );
        }
        catch ( IOException e )
        {
            logger.log( 
                    FrameworkUtil.getBundle( getClass() ).getBundleContext().getServiceReference( getClass() ), 
                    LogService.LOG_ERROR, 
                    "An error occurred when attempting to generate the bucket list for the FileStore", 
                    e );

            return Collections.emptyList();
        }
    }

    @Override
    public URI uri()
    {
        return uri;
    }

    @Override
    public String outerPath()
    {
        return outerPath;
    }

    @Override
    public synchronized Promise<Stream<D>> stream()
    {
        final Deferred<Stream<D>> deferred = new Deferred<>();

        try
        {
            final BucketNameParser parser = BucketNameParser.newParser();
            final BucketPathConverter converter = BucketPathConverter.newConverter();
            final BucketContextualizer contextualizer = BucketContextualizer.newContextualizer();

            final Stream<D> stream = buckets().stream()
                    .map( bn -> parser.parse( bn, descriptor.packaging() ) )
                    .map( bn -> converter.convert( descriptor, outerPath, bn ) )
                    .map( c -> contextualizer.contextualize( uri(), c ) )
                    .flatMap( b -> io.debucketize( b ) );

            deferred.resolve( stream );
        }
        catch ( Exception e )
        {
            logger.log( 
                    FrameworkUtil.getBundle( getClass() ).getBundleContext().getServiceReference( getClass() ), 
                    LogService.LOG_ERROR, 
                    "An error occurred when reading from the FileStore", 
                    e );

            deferred.fail( e );
        }

        return deferred.getPromise();
    }

    @Override
    public Promise<Integer> size()
    {
        return stream()
                .then( p -> {
                    return Promises.resolved((int)p.getValue().count());
                });
    }

    @Override
    public synchronized Promise<Boolean> push( Stream<D> aDTOStream )
    {
        final Deferred<Boolean> deferred = new Deferred<>();

        try
        {
            final List<Bucket> buckets = io.bucketize( aDTOStream, uri().toString(), outerPath );

            for( Bucket bucket : buckets )
            {
                final Path base = baseAndOuterPath;
                final File bucketFile = new File( base.toFile(), bucket.fullName() );
                bucketFile.getParentFile().mkdirs();
                bucketFile.createNewFile();
                writeToFile( bucket.content().get(), bucketFile );
            }

            deferred.resolve( true );
        }
        catch ( Exception e )
        {
            deferred.fail( e );
        }

        return deferred.getPromise();
    }

    @Override
    public synchronized Promise<Boolean> push( Increment<D> anIncrement, Supplier<Map<String, D>> repo )
    {
        final Deferred<Boolean> deferred = new Deferred<>();

        try
        {
            final List<Bucket> bucketList = io.bucketize(
                    Stream.of( anIncrement.value() ),
                    uri().toString(),
                    outerPath );
            if( bucketList.size() != 1 )
                throw new UncheckedBucketException(
                        "An error occurred when attempting to process the Bucket" );
            final Bucket bucket = bucketList.get( 0 );
            if( !bucket.content().isPresent() )
                throw new UncheckedBucketException( "No content to write" );
            if( Increment.Type.PUT == anIncrement.type() )
                put( bucket, anIncrement, repo );
            else
                delete( bucket, anIncrement, repo );

            deferred.resolve( true );
        }
        catch ( Exception e )
        {
            deferred.fail( e );
        }

        return deferred.getPromise();
    }

//    /**
//     * Assume TSV for now.
//     */
//    private void exportTable( Path base, Supplier<Stream<E>> anDTOSupplier )
//            throws Exception
//    {
//        final List<Table> tables = descriptor
//                .tableizer()
//                .tableize( anDTOSupplier.get() )
//                .orElseThrow( () -> new Exception() );
//
//        for( int i = 0; i < tables.size(); i++ )
//        {
//            final Table table = tables.get( i );
//            final String fileName = ( table.name().isEmpty() ) ? tableName( i, tables.size() ) : table.name();
//            final File bucketFile = new File( base.toFile(), fileName );
//            bucketFile.getParentFile().mkdirs();
//            bucketFile.createNewFile();
//            final PrintWriter writer = new PrintWriter( bucketFile );
//
//            if( !table.headers().isEmpty() )
//                writeLine( table.headers(), writer );
//
//            final Iterator<List<String>> it = table.rows().iterator();
//            while( it.hasNext() )
//            {
//                final List<String> row = it.next();
//                writeLine( row, writer );
//                if( it.hasNext() )
//                    writer.print( "\n" );
//            }
//
//            writer.close();
//        }
//    }
//
//    private void writeLine( List<String> values, PrintWriter writer )
//        throws Exception
//    {
//        final Iterator<String> it = values.iterator();
//        while( it.hasNext() )
//        {
//            writer.write( it.next() );
//            if( it.hasNext() )
//                writer.write( "\t" );
//        }
//    }
//
//    private String tableName( int index, int size )
//    {
//        return new StringBuilder()
//                .append( "ExportedTable-" )
//                .append( LocalDate.now() )
//                .append( ( size > 1 ) ? "-" + index : "" )
//                .append( ".tsv" )
//                .toString();
//    }

    private void put( Bucket bucket, Increment<D> anIncrement, Supplier<Map<String, D>> repo )
        throws UncheckedBucketException
    {
        final String id = descriptor.idExtractor().apply( anIncrement.value() );

        // If the bucket is PARTITIONED or is a SINGLE Bucket, then only the single bucket gets written
        if (bucket.packaging() == Bucket.Packaging.PARTITIONED || bucket.packaging() == Bucket.Packaging.SINGLE)
        {
            writeSingleBucket( bucket );
        }
        // If the bucket is MULTI, then the updated object needs to be inserted into the map, and
        // the entire map gets written to the file.
        else if (bucket.packaging() == Bucket.Packaging.MULTI)
        {
            final Stream<D> stream = repo.get().entrySet().stream()
                    .map( e -> id.equals(e.getKey()) ? anIncrement.value() : e.getValue() );
            writeMultiBucket( stream );
        }
        // Huh??
        else
        {
            throw new UncheckedBucketException( String.format( "Could not process based on Packaging type %s", bucket.packaging() ) );
        }
    }

    private void delete( Bucket bucket, Increment<D> anIncrement, Supplier<Map<String, D>> repo )
        throws UncheckedBucketException
    {
        final String id = descriptor.idExtractor().apply( anIncrement.value() );

        // If the bucket is PARTITIONED or SINGLE, then we just need to delete the file representing the Bucket
        if (bucket.packaging() == Bucket.Packaging.PARTITIONED || bucket.packaging() == Bucket.Packaging.SINGLE)
        {
            deleteSingleBucket( bucket );
        }
        // If the bucket is MULTI, then the deleted object needs to be removed from the map, and
        // the entire map gets written to the file. So, although it is a DELETE operation, we
        // still need to WRITE to the Store.
        else if (bucket.packaging() == Bucket.Packaging.MULTI)
        {
            final Stream<D> stream = repo.get().entrySet().stream()
                    .filter( e -> !id.equals(e.getKey()) )
                    .map( e -> e.getValue() );
            writeMultiBucket( stream );
        }
        // Huh??
        else
        {
            throw new UncheckedBucketException( String.format( "Could not process based on Packaging type %s", bucket.packaging() ) );
        }
    }

    private void writeSingleBucket( Bucket bucket )
        throws UncheckedBucketException
    {
        try
        {
            final Path base = baseAndOuterPath;
            final Path bucketFilePath = base.resolve( bucket.fullName() );
            final Path parent = bucketFilePath.getParent();
            if (parent != null) // null will be returned if the path has no parent
                Files.createDirectories(parent);
            if (!Files.exists( bucketFilePath ) )
                Files.createFile( bucketFilePath );
            final File bucketFile = bucketFilePath.toFile();
            writeToFile( bucket.content().get(), bucketFile );
        }
        catch ( Exception e )
        {
            throw new UncheckedBucketException( e );
        }
    }

    private void deleteSingleBucket( Bucket bucket )
        throws UncheckedBucketException
    {
        try
        {
            final Path base = baseAndOuterPath;
            final File bucketFile = new File( base.toFile(), bucket.fullName() );
            if (!bucketFile.delete())
                throw new UncheckedBucketException( String.format( "Could not remove file: %s", bucketFile.getPath() ) );
        }
        catch( UncheckedBucketException e )
        {
            throw e;
        }
        catch ( Exception e )
        {
            throw new UncheckedBucketException( e );
        }
    }

    private void writeMultiBucket( Stream<D> stream )
        throws UncheckedBucketException
    {
        try
        {
            final List<Bucket> bucketList = io.bucketize( stream, uri().toString(), outerPath );
            if (bucketList.size() != 1)
                throw new UncheckedBucketException( "Could not convert to Bucket" );
            final Bucket bucket = bucketList.get( 0 );
            if (!bucket.content().isPresent())
                throw new UncheckedBucketException( "No content to write" );

            final Path base = baseAndOuterPath;
            final File bucketFile = new File( base.toFile(), bucket.fullName() );
            writeToFile( bucket.content().get(), bucketFile );
        }
        catch( UncheckedBucketException e )
        {
            throw e;
        }
        catch ( Exception e )
        {
            throw new UncheckedBucketException( e );
        }
    }

    private void writeToFile( String contents, File toFile )
        throws Exception
    {
        final FileOutputStream out = new FileOutputStream( toFile );
        try (PrintWriter writer = new PrintWriter( out ))
        {
            try(FileLock lock = out.getChannel().lock())
            {
                writer.write( contents );                    
            }
        }
    }
}
