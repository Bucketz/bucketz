package org.bucketz.export.impl;
//
//import java.net.URI;
//import java.util.HashSet;
//import java.util.List;
//import java.util.Set;
//
//import org.osgi.service.cm.ConfigurationAdmin;
//import org.osgi.service.component.annotations.Component;
//import org.osgi.service.component.annotations.Reference;
//import org.osgi.service.log.LogService;
//import org.osgi.util.promise.Deferred;
//import org.osgi.util.promise.Promise;
//
//import net.leangen.expedition.platform.repository2.Exportable;
//import net.leangen.expedition.platform.repository2.admin.Exporter;
//
//@Component(
//        name = FileStoreExporter.COMPONENT_NAME,
//        service = { Exporter.class, Exporter.FileStore.class } )
//public class FileStoreExporter
//    implements Exporter.FileStore
//{
//    public static final String COMPONENT_NAME = PID;
//
//    @Reference private ConfigurationAdmin cm;
//    @Reference private LogService logger;
//
//    public Promise<URI> export( List<Exportable<?>> exportables, String toLocation)
//    {
//        final Deferred<URI> deferred = new Deferred<>();
//        try
//        {
//            Set<URI> results = new HashSet<>();
//
//            for (Exportable<?> exportable : exportables)
//            {
//                final FileStoreWriter<?> writer = 
//                        new FileStoreWriter<>( 
//                                exportable,
//                                toLocation,
//                                cm,
//                                logger );
//                writer.write()
//                        .then( p -> { results.add( p.getValue() ); return null; } );
//            }
//
//            if (results.size() != 1)
//                throw new Exception( "Unexpected result" );
//
//            deferred.resolve( results.iterator().next() );
//        }
//        catch ( Exception e )
//        {
//            deferred.fail( e );
//        }
//
//        return deferred.getPromise();
//    }
//}
