package org.bucketz.impl;
//
//import java.io.File;
//import java.io.FileOutputStream;
//import java.io.IOException;
//import java.io.PrintWriter;
//import java.net.MalformedURLException;
//import java.nio.channels.FileLock;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.nio.file.Paths;
//import java.util.Collections;
//import java.util.List;
//import java.util.Map;
//import java.util.stream.Collectors;
//import java.util.stream.Stream;
//
//import org.osgi.framework.FrameworkUtil;
//import org.osgi.service.component.ComponentContext;
//import org.osgi.service.component.annotations.Activate;
//import org.osgi.service.component.annotations.Component;
//import org.osgi.service.component.annotations.ConfigurationPolicy;
//import org.osgi.service.component.annotations.Reference;
//import org.osgi.service.log.LogService;
//import org.osgi.util.promise.Deferred;
//import org.osgi.util.promise.Promise;
//
//import net.leangen.expedition.base.object.Confirmation;
//import net.leangen.expedition.base.object.Try;
//import net.leangen.expedition.platform.bucketstore.BucketStore;
//import net.leangen.expedition.platform.bucketstore.BucketedAggregateDescriptor;
//import net.leangen.expedition.platform.bucketstore.FileStore;
//import net.leangen.expedition.platform.datastore.DataStore;
//import net.leangen.expedition.platform.datastore.Incremental;
//import net.leangen.expedition.platform.datastore.StateStore;
//import net.leangen.expedition.platform.repository.lib.config.BucketContextualizer;
//import net.leangen.expedition.platform.repository.lib.config.BucketPathConverter;
//import net.leangen.expedition.platform.repository.lib.config.BucketNameParser;
//import net.leangen.expedition.platform.repository2.Mappable;
//
//@BucketStore.Provide(type=BucketStore.Type.FILE)
//@Component(
//        name = FileStoreService.COMPONENT_NAME,
//        service = {
//                DataStore.class,
//                StateStore.class, 
//                BucketStore.class,
//                FileStore.class },
//        configurationPolicy = ConfigurationPolicy.REQUIRE,
//        configurationPid = FileStore.PID,
//        immediate = true
//)
//public class FileStoreService<E>
//    implements FileStore<E>
//{
//    public static final String COMPONENT_NAME = FileStore.PID;
//
//    private String name;
//
//    private String location;
//    private String confinement;
//    private String domain;
//    private String bc;
//    private String module;
//    private String version;
//
//    private BucketedAggregateDescriptor<E> descriptor;
//
//    @Reference private LogService logger;
//
//    @Activate
//    void activate( ComponentContext componentContext, FileStore.Configuration configuration, Map<String, Object> properties )
//    {
//        location = configuration.location();
//        confinement = configuration.confinement();
//        domain = configuration.domain();
//        bc = configuration.bc();
//        module = configuration.module();
//        version = configuration.version();
//
//        if (properties.containsKey( "uuid" ))
//        {
//            name = new StringBuilder()
//                    .append( descriptor.name().substring( 0, descriptor.name().lastIndexOf( "-" ) ) )
//                    .append( "-" )
//                    .append( properties.get( "uuid" ) )
//                    .append( "-FileStore" )
//                    .toString();
//
//        }
//    }
//
//    void deactivate()
//    {
//        location = null;
//        confinement = null;
//        domain = null;
//        bc = null;
//        module = null;
//        version = null;
//    }
//
//    @Reference
//    void bindDescriptor( BucketedAggregateDescriptor<E> aDescriptor, Map<String, Object> properties )
//    {
//        descriptor = aDescriptor;
//        name = new StringBuilder()
//                .append( descriptor.name().substring( 0, descriptor.name().lastIndexOf( "-" ) ) )
//                .append( "-FileStore" )
//                .toString();
//
//        System.err.println( String.format( "FileStore --> bindDescriptor: name=%s", name ) );
//    }
//
//    void unbindDescriptor( BucketedAggregateDescriptor<E> aDescriptor, Map<String, Object> properties )
//    {
//        descriptor = null;
//    }
//
//    @Override
//    public String name()
//    {
//        return name;
//    }
//
//    @Override
//    public DataStore.Info info()
//    {
//        final DataStore.Info state = new DataStore.Info();
//        state.type = "FileStore";
//        state.name = name();
//        return state;
//    }
//
//    @Override
//    public List<String> buckets()
//    {
//        try
//        {
//            final Path base = baseAndOuterPath();
//            if( !Files.exists( base ) && !Files.isDirectory( base ) )
//                return Collections.emptyList();
//
////            final List<Path> paths = Files.walk( base ).collect( Collectors.toList() );
////            for (Path path : paths)
////            {
////                if (Files.isDirectory( path ))
////                    continue;
////                final String p = path.toFile().toURI().toString();
////                final String u = p.replace( base.toFile().toURI().toString(), "" );
////                final boolean isPathEmpty = !u.contains( "/" );
////                if (isPathEmpty && (descriptor.packaging() == BucketStore.Packaging.PARTITIONED))
////                    // Cannot process a Partitioned Bucket with an empty path
////                    continue;
////                if (!u.startsWith( descriptor.brn() ))
////                    continue;
////
////                if (descriptor.filter().isPresent())
////                    if (u.matches( descriptor.filter().get() ))
////                        toString(); // matches
////                    else
////                        toString(); // no match
////            }
//
//            return Files.walk( base )
//                    .parallel()
//                    .filter( p -> !Files.isDirectory( p ) )
//                    .map( p -> p.toFile().toURI().toString() )
//                    .map( u -> u.replace( base.toFile().toURI().toString(), "" ) )
//                    .filter( b -> !b.isEmpty() )
//                    .filter( b -> !( !b.contains( "/" ) && ( descriptor.packaging() == BucketStore.Packaging.PARTITIONED ) ) )
//                    .filter( b -> b.startsWith( descriptor.brn() ) )
//                    .filter( b -> (descriptor.filter().isPresent()) ? b.matches( descriptor.filter().get() ) : true )
//                    .collect( Collectors.toList() );
//        }
//        catch ( IOException e )
//        {
//            logger.log( 
//                    FrameworkUtil.getBundle( getClass() ).getBundleContext().getServiceReference( getClass() ), 
//                    LogService.LOG_ERROR, 
//                    "An error occurred when attempting to generate the bucket list for the FileStore", 
//                    e );
//
//            return Collections.emptyList();
//        }
//    }
//
//    @Override
//    public String url()
//    {
//        try
//        {
//            return baseLocation().toUri().toURL().toString();
//        }
//        catch ( MalformedURLException e )
//        {
//            logger.log( 
//                    FrameworkUtil.getBundle( getClass() ).getBundleContext().getServiceReference( getClass() ), 
//                    LogService.LOG_ERROR, 
//                    "An error occurred when attempting to generate the URL of the FileStore", 
//                    e );
//
//            return null;
//        }
//    }
//
//    private Path baseLocation()
//    {
//        final String baseLocation = location.replaceFirst( "^~", System.getProperty( "user.home" ) );
//        final Path base = new File( baseLocation ).toPath();
//        return base;
//    }
//
//    private Path baseAndOuterPath()
//    {
//        return baseLocation().resolve( outerPathInternal() );
//    }
//
//    private Path outerPathInternal()
//    {
//        return Paths.get( confinement, domain, bc, module, version );        
//    }
//
//    @Override
//    public String outerPath()
//    {
//        String path = outerPathInternal().toUri().toString();
//
//        if (!path.isEmpty() && !path.endsWith( "/" ))
//            path += "/";
//
//        return path;
//    }
//
//    @Override
//    public Promise<Stream<E>> pull()
//    {
//        final Deferred<Stream<E>> deferred = new Deferred<>();
//        try
//        {
//            final BucketNameParser parser = BucketNameParser.newParser();
//            final BucketPathConverter converter = BucketPathConverter.newConverter();
//            final BucketContextualizer contextualizer = BucketContextualizer.newContextualizer();
//
//            final Stream<E> stream = buckets().stream()
//                    .map( b -> parser.parse( b, descriptor.packaging() ) )
//                    .filter( t -> t.isSuccess() )
//                    .map( t -> t.get() )
//                    .map( p -> converter.convert( descriptor, p ) )
//                    .map( dto -> contextualizer.contextualize( url(), dto ) )
//                    .map( b -> descriptor.debucketizer().debucketize( b ) )
//                    .flatMap( t -> t.get() );
//
//            deferred.resolve( stream );
//        }
//        catch ( Throwable t )
//        {
//            logger.log( 
//                    FrameworkUtil.getBundle( getClass() ).getBundleContext().getServiceReference( getClass() ), 
//                    LogService.LOG_ERROR, 
//                    "An error occurred when reading from the FileStore", 
//                    t );
//
//            deferred.fail( t );
//        }
//
//        return deferred.getPromise();
//    }
//
//    @Override
//    public Promise<Confirmation> push( Stream<E> anEntityStream )
//    {
//        final Deferred<Confirmation> deferred = new Deferred<>();
//        try
//        {
//            final List<BucketStore.Bucket> buckets = descriptor
//                    .bucketizer()
//                    .bucketize( anEntityStream, url() )
//                    .orElseThrow( () -> new Exception() );
//
//            for( BucketStore.Bucket bucket : buckets )
//            {
//                final Path base = baseAndOuterPath();
//                final File bucketFile = new File( base.toFile(), bucket.fullName() );
//                bucketFile.getParentFile().mkdirs();
//                bucketFile.createNewFile();
//                writeToFile( bucket.content().get(), bucketFile );
//            }
//        }
//        catch ( Throwable t )
//        {
//            logger.log( 
//                    FrameworkUtil.getBundle( getClass() ).getBundleContext().getServiceReference( getClass() ), 
//                    LogService.LOG_ERROR, 
//                    "An error occurred when reading from the FileStore", 
//                    t );
//
//            deferred.fail( t );
//        }
//
//        return deferred.getPromise();
//    }
//
//    @Override
//    public Try<Confirmation> push( Increment<E> anIncrement, Mappable<E> aMappable )
//    {
//        final Try<List<BucketStore.Bucket>> ok = descriptor.bucketizer().bucketize( Stream.of( anIncrement.value() ), url() );
//        if (ok.isFailure())
//            return Try.failure( ok.getException() );
//        final List<BucketStore.Bucket> bucketList = ok.get();
//        if (bucketList.size() != 1)
//            return Try.failure( "An error occurred when attempting to process the Bucket" );
//        final BucketStore.Bucket bucket = bucketList.get( 0 );
//        if (!bucket.content().isPresent())
//            return Try.failure( "No content to write" );
//
//        if ( Incremental.Increment.Type.PUT == anIncrement.type() )
//            return put( bucket, anIncrement, aMappable );
//        else
//            return delete( bucket, anIncrement, aMappable );
//    }
//
////    /**
////     * Assume TSV for now.
////     */
////    private void exportTable( Path base, Supplier<Stream<E>> anEntitySupplier )
////            throws Exception
////    {
////        final List<Table> tables = descriptor
////                .tableizer()
////                .tableize( anEntitySupplier.get() )
////                .orElseThrow( () -> new Exception() );
////
////        for( int i = 0; i < tables.size(); i++ )
////        {
////            final Table table = tables.get( i );
////            final String fileName = ( table.name().isEmpty() ) ? tableName( i, tables.size() ) : table.name();
////            final File bucketFile = new File( base.toFile(), fileName );
////            bucketFile.getParentFile().mkdirs();
////            bucketFile.createNewFile();
////            final PrintWriter writer = new PrintWriter( bucketFile );
////
////            if( !table.headers().isEmpty() )
////                writeLine( table.headers(), writer );
////
////            final Iterator<List<String>> it = table.rows().iterator();
////            while( it.hasNext() )
////            {
////                final List<String> row = it.next();
////                writeLine( row, writer );
////                if( it.hasNext() )
////                    writer.print( "\n" );
////            }
////
////            writer.close();
////        }
////    }
////
////    private void writeLine( List<String> values, PrintWriter writer )
////        throws Exception
////    {
////        final Iterator<String> it = values.iterator();
////        while( it.hasNext() )
////        {
////            writer.write( it.next() );
////            if( it.hasNext() )
////                writer.write( "\t" );
////        }
////    }
////
////    private String tableName( int index, int size )
////    {
////        return new StringBuilder()
////                .append( "ExportedTable-" )
////                .append( LocalDate.now() )
////                .append( ( size > 1 ) ? "-" + index : "" )
////                .append( ".tsv" )
////                .toString();
////    }
//
//    private Try<Confirmation> put( Bucket bucket, Incremental.Increment<E> anIncrement, Mappable<E> aMappable )
//    {
//        final Try<Confirmation> result;
//        final String id = descriptor.idExtractor().apply( anIncrement.value() );
//
//        // If the bucket is PARTITIONED, then only the single bucket gets written
//        if (bucket.packaging() == Packaging.PARTITIONED)
//        {
//            result = writeSingleBucket( bucket );
//        }
//        // If the bucket is MULTI, then the updated object needs to be inserted into the map, and
//        // the entire map gets written to the file.
//        else if (bucket.packaging() == Packaging.MULTI)
//        {
//            final Stream<E> stream = aMappable.asMap().entrySet().stream()
//                    .map( e -> id.equals(e.getKey()) ? anIncrement.value() : e.getValue() );
//            result = writeMultiBucket( stream );
//        }
//        // Huh??
//        else
//        {
//            return Try.failure( String.format( "Could not process based on Packaging type %s", bucket.packaging() ) );
//        }
//
//        return result.map( r -> Confirmation.novo( id ) );
//    }
//
//    private Try<Confirmation> delete( Bucket bucket, Incremental.Increment<E> anIncrement, Mappable<E> aMappable )
//    {
//        final Try<Confirmation> result;
//        final String id = descriptor.idExtractor().apply( anIncrement.value() );
//
//        // If the bucket is PARTITIONED, then we just need to delete the file representing the Bucket
//        if (bucket.packaging() == Packaging.PARTITIONED)
//        {
//            result = deleteSingleBucket( bucket );
//        }
//        // If the bucket is MULTI, then the deleted object needs to be removed from the map, and
//        // the entire map gets written to the file. So, although it is a DELETE operation, we
//        // still need to WRITE to the Store.
//        else if (bucket.packaging() == Packaging.MULTI)
//        {
//            final Stream<E> stream = aMappable.asMap().entrySet().stream()
//                    .filter( e -> !id.equals(e.getKey()) )
//                    .map( e -> e.getValue() );
//            result = writeMultiBucket( stream );
//        }
//        // Huh??
//        else
//        {
//            return Try.failure( String.format( "Could not process based on Packaging type %s", bucket.packaging() ) );
//        }
//
//        return result.map( r -> Confirmation.novo( id ) );
//    }
//
//    private Try<Confirmation> writeSingleBucket( BucketStore.Bucket bucket )
//    {
//        try
//        {
//            final Path base = baseAndOuterPath();
//            final Path bucketFilePath = base.resolve( bucket.fullName() );
//            final Path parent = bucketFilePath.getParent();
//            if (parent != null) // null will be returned if the path has no parent
//                Files.createDirectories(parent);
//            if (!Files.exists( bucketFilePath ) )
//                Files.createFile( bucketFilePath );
//            final File bucketFile = bucketFilePath.toFile();
//            writeToFile( bucket.content().get(), bucketFile );
//            return Try.success( Confirmation.novo() );
//        }
//        catch ( Exception e )
//        {
//            return Try.failure( e );
//        }
//    }
//
//    private Try<Confirmation> deleteSingleBucket( BucketStore.Bucket bucket )
//    {
//        try
//        {
//            final Path base = baseAndOuterPath();
//            final File bucketFile = new File( base.toFile(), bucket.fullName() );
//            if (bucketFile.delete())
//                return Try.success( Confirmation.novo() ); 
//            return Try.failure( String.format( "Could not remove file: %s", bucketFile.getPath() ) );
//        }
//        catch ( Exception e )
//        {
//            return Try.failure( e );
//        }
//    }
//
//    private Try<Confirmation> writeMultiBucket( Stream<E> stream )
//    {
//        try
//        {
//            final List<Bucket> bucketList = descriptor
//                    .bucketizer()
//                    .bucketize( stream, url() )
//                    .orElseThrow( () -> new Exception( "No entities found" ) );
//            if (bucketList.size() != 1)
//                return Try.failure( "Could not convert to Bucket" );
//            final BucketStore.Bucket bucket = bucketList.get( 0 );
//            if (!bucket.content().isPresent())
//                return Try.failure( "No content to write" );
//
//            final Path base = baseAndOuterPath();
//            final File bucketFile = new File( base.toFile(), bucket.fullName() );
//            writeToFile( bucket.content().get(), bucketFile );
//            return Try.success( Confirmation.novo() );
//        }
//        catch ( Exception e )
//        {
//            return Try.failure( e );
//        }
//    }
//
//    private void writeToFile( String contents, File toFile )
//        throws Exception
//    {
//        final FileOutputStream out = new FileOutputStream( toFile );
//        try (PrintWriter writer = new PrintWriter( out ))
//        {
//            try(FileLock lock = out.getChannel().lock())
//            {
//                writer.write( contents );                    
//            }
//        }
//    }
//}
