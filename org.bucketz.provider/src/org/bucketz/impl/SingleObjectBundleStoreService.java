package org.bucketz.impl;
//
//import java.net.URL;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Map;
//import java.util.stream.Stream;
//
//import org.osgi.framework.Bundle;
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
//import net.leangen.expedition.base.object.Try;
//import net.leangen.expedition.platform.bucketstore.BucketStore;
//import net.leangen.expedition.platform.bucketstore.BucketedSingleObjectDescriptor;
//import net.leangen.expedition.platform.bucketstore.BundleStore;
//import net.leangen.expedition.platform.bucketstore.SingleObjectBucketStore;
//import net.leangen.expedition.platform.datastore.DataStore;
//import net.leangen.expedition.platform.datastore.StateStore;
//import net.leangen.expedition.platform.repository.lib.config.BucketContextualizer;
//import net.leangen.expedition.platform.repository.lib.config.BucketPathConverter;
//import net.leangen.expedition.platform.repository.lib.config.BucketNameParser;
//
//@Component(
//        name = SingleObjectBundleStoreService.COMPONENT_NAME,
//        service = {
//                DataStore.class,
//                StateStore.class,
//                BucketStore.class,
//                SingleObjectBucketStore.class,
//                SingleObjectBucketStore.SingleObjectBundleStore.class,
//                BundleStore.class },
//        configurationPolicy = ConfigurationPolicy.REQUIRE,
//        configurationPid = SingleObjectBucketStore.SingleObjectBundleStore.PID,
//        immediate = true
//)
//public class SingleObjectBundleStoreService<E>
//    implements 
//        SingleObjectBucketStore<E>,
//        SingleObjectBucketStore.SingleObjectBundleStore<E>,
//        BundleStore<E>
//{
//    public static final String COMPONENT_NAME = SingleObjectBucketStore.SingleObjectBundleStore.PID;
//
//    private String name;
//
//    private long bundleId;
//    private String confinement;
//    private String location;
//    private String domain;
//    private String bc;
//    private String module;
//    private String bucketName;
//    private String version;
//
//    private String outerPath;
//
//    private BucketedSingleObjectDescriptor<E> descriptor;
//
//    @Reference private LogService logger;
//
//    @Activate
//    void activate( ComponentContext componentContext, SingleObjectBucketStore.Configuration configuration, Map<String, Object> properties )
//    {
//        bundleId = configuration.bundleId();
//        confinement = configuration.confinement();
//        location = configuration.location();
//        domain = configuration.domain();
//        bc = configuration.bc();
//        module = configuration.module();
//        version = configuration.version();
//        bucketName = configuration.bucketName();
//    }
//
//    void deactivate()
//    {
//        bundleId = -1;
//        confinement = null;
//        location = null;
//        domain = null;
//        bc = null;
//        module = null;
//        version = null;
//        bucketName = null;
//    }
//
//    @Reference
//    void bindDescriptor( BucketedSingleObjectDescriptor<E> aDescriptor, Map<String, Object> properties )
//    {
//        descriptor = aDescriptor;
//        name = new StringBuilder()
//                .append( descriptor.name().substring( 0, descriptor.name().lastIndexOf( "-" ) ) )
//                .append( "-SingleObjectBundleStore" )
//                .toString();
//
//        System.err.println( String.format( "SingleObjectBundleStore --> bindDescriptor: name=%s", name ) );
//    }
//
//    void unbindDescriptor( BucketedSingleObjectDescriptor<E> aDescriptor, Map<String, Object> properties )
//    {
//        name = null;
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
//        state.type = "SingleObjectBundleStore";
//        state.name = name();
//        return state;
//    }
//
//    @Override
//    public String url()
//    {
//        final Bundle bundle = FrameworkUtil.getBundle( getClass() ).getBundleContext().getBundle( bundleId );
//        final URL url = bundle.getEntry( location );
//        if (url == null)
//            return "ERROR";
//
//        return url.toString();
//    }
//
//    @Override
//    public String outerPath()
//    {
//        if (outerPath == null)
//        {
//            String path = new StringBuilder()
//                    .append( confinement )
//                    .append( "/" ).append( domain )
//                    .append( "/" ).append( bc )
//                    .append( "/" ).append( module )
//                    .append( "/" ).append( version )
//                    .toString();
//
//            if (path.startsWith( "/" ))
//                path = path.substring( 1 );
//
//            if (!path.isEmpty() && !path.endsWith( "/" ))
//                path += "/";
//
//            outerPath = path;
//        }
//
//        return outerPath;
//    }
//
//    @Override
//    public List<String> buckets()
//    {
//        final List<String> buckets = new ArrayList<>();
//        buckets.add( bucket() );
//        return buckets;
//    }
//
//    @Override
//    public String bucket()
//    {
//        return new StringBuilder()
//                .append( bucketName )
//                .toString();
//    }
//
//    @Override
//    public Promise<Stream<E>> pull()
//    {
//        final Deferred<Stream<E>> deferred = new Deferred<>();
//        new Thread(() -> {
//            try
//            {
//                final BucketNameParser parser = BucketNameParser.newParser();
//                final BucketPathConverter converter = BucketPathConverter.newConverter();
//                final BucketContextualizer contextualizer = BucketContextualizer.newContextualizer();
//
//                final Try<E> entity = parser.parse( bucket(), descriptor.packaging() )
//                    .map( b -> converter.convert( descriptor, b ) )
//                    .map( c -> contextualizer.contextualize( url(), c ) )
//                    .flatMap( b -> descriptor.read( b ) );
//
//                if( entity.isFailure() )
//                    deferred.fail( entity.getException() );
//                else
//                    deferred.resolve( Stream.of( entity.get() ) );
//            }
//            catch ( Throwable t )
//            {
//                logger.log( 
//                        FrameworkUtil.getBundle( getClass() ).getBundleContext().getServiceReference( getClass() ), 
//                        LogService.LOG_ERROR, 
//                        "An error occurred when reading from the SingleObjectBundleStore", 
//                        t );
//
//                deferred.fail( t );
//            }
//        }).start();
//
//        return deferred.getPromise();
//    }
//}
