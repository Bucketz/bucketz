package org.bucketz.export.impl;
//
//import java.net.URI;
//import java.nio.file.Path;
//import java.nio.file.Paths;
//
//import org.osgi.service.cm.ConfigurationAdmin;
//import org.osgi.service.log.LogService;
//import org.osgi.util.promise.Deferred;
//import org.osgi.util.promise.Promise;
//
//import net.leangen.expedition.platform.bucketstore.FileStore;
//import net.leangen.expedition.platform.repository.lib.config.StoreWriter;
//import net.leangen.expedition.platform.repository2.Exportable;
//
//public class FileStoreWriter<E>
//    implements StoreWriter<E>
//{
//    private final Exportable<E> exportable;
//    private final String location;
//    private final ConfigurationAdmin cm;
//    private final LogService logger;
//
//    public FileStoreWriter(
//            Exportable<E> anExportable,
//            String aLocation,
//            ConfigurationAdmin aConfigurationAdmin,
//            LogService aLogger )
//    {
//        exportable = anExportable;
//        location = aLocation;
//        cm = aConfigurationAdmin;
//        logger = aLogger;
//    }
//
//    @SuppressWarnings( "unchecked" )
//    public Promise<URI> write()
//    {
//        final FileStoreFactory<?> factory = FileStoreFactory.newFactory()
//                .setConfigurationAdmin( cm )
//                .setLogService( logger )
//                .locatedAt( location )
//                .confinedTo( exportable.confinement() )
//                .usingDomain( exportable.domain() )
//                .usingBoundedContext( exportable.bc() )
//                .usingModule( exportable.module() )
//                .setPid( exportable.target() )
//                .setVersion( exportable.version() )
//                .debug();
//
//        if (exportable.brn().isPresent())
//            factory.asSingleObject( exportable.brn().get() );
//
//        return factory.get()
//                .then( p -> writeWith( (FileStore<E>)p.getValue() ) )
//                .then( p -> factory.cleanup( p.getValue() ) );
//    }
//
//    private Promise<URI> writeWith( FileStore<E> store )
//    {
//        try
//        {
//            return store.push( exportable.cursor().stream() )
//                    .then( p -> toUri( location ) );
//        }
//        catch (Exception e )
//        {
//            final Deferred<URI> deferred = new Deferred<>();
//            deferred.fail( e );
//            return deferred.getPromise();
//        }
//    }
//
//    private Promise<URI> toUri( String aLocation )
//    {
//        final Deferred<URI> deferred = new Deferred<>();
//
//        try
//        {
//            final Path path = Paths.get( aLocation );
//            final URI uri = path.toUri();
//            deferred.resolve( uri );
//        }
//        catch (Exception e)
//        {
//            deferred.fail( e );
//        }
//
//        return deferred.getPromise();
//    }
//}
