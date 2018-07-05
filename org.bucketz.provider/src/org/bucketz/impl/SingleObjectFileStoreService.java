package org.bucketz.impl;
//
//import java.io.File;
//import java.io.FileOutputStream;
//import java.io.PrintWriter;
//import java.net.MalformedURLException;
//import java.nio.channels.FileLock;
//import java.nio.file.Path;
//import java.nio.file.Paths;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Map;
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
//import net.leangen.expedition.platform.bucketstore.BucketedSingleObjectDescriptor;
//import net.leangen.expedition.platform.bucketstore.FileStore;
//import net.leangen.expedition.platform.bucketstore.SingleObjectBucketStore;
//import net.leangen.expedition.platform.datastore.DataStore;
//import net.leangen.expedition.platform.datastore.StateStore;
//import net.leangen.expedition.platform.repository.lib.config.BucketContextualizer;
//import net.leangen.expedition.platform.repository.lib.config.BucketPathConverter;
//import net.leangen.expedition.platform.repository2.Mappable;
//import net.leangen.expedition.platform.repository.lib.config.BucketNameParser;
//
//@Component(
//        name = SingleObjectFileStoreService.COMPONENT_NAME,
//        service = {
//                DataStore.class,
//                StateStore.class,
//                BucketStore.class,
//                SingleObjectBucketStore.class,
//                SingleObjectBucketStore.SingleObjectFileStore.class,
//                FileStore.class },
//        configurationPolicy = ConfigurationPolicy.REQUIRE,
//        configurationPid = SingleObjectBucketStore.SingleObjectFileStore.PID,
//        immediate = true
//)
//public class SingleObjectFileStoreService<E>
//    implements
//        SingleObjectBucketStore<E>,
//        SingleObjectBucketStore.SingleObjectFileStore<E>,
//        FileStore<E>
//{
//    public static final String COMPONENT_NAME = SingleObjectBucketStore.SingleObjectFileStore.PID;
//
//    private String name;
//
//    private String location;
//    private String confinement;
//    private String domain;
//    private String bc;
//    private String module;
//    private String bucketName;
//    private String version;
//
//    private BucketedSingleObjectDescriptor<E> descriptor;
//
//    @Reference private LogService logger;
//
//    @Activate
//    void activate( ComponentContext componentContext, SingleObjectBucketStore.Configuration configuration, Map<String, Object> properties )
//    {
//        confinement = configuration.confinement();
//        location = configuration.location();
//        domain = configuration.domain();
//        bc = configuration.bc();
//        module = configuration.module();
//        version = configuration.version();
//        bucketName = configuration.bucketName();
//
//        if (properties.containsKey( "uuid" ))
//        {
//            name = new StringBuilder()
//                    .append( descriptor.name().substring( 0, descriptor.name().lastIndexOf( "-" ) ) )
//                    .append( "-" )
//                    .append( properties.get( "uuid" ) )
//                    .append( "-SingleObjectFileStore" )
//                    .toString();
//
//        }
//    }
//
//    void deactivate()
//    {
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
//                .append( "-SingleObjectFileStore" )
//                .toString();
//
//        System.err.println( String.format( "SingleObjectFileStore --> bindDescriptor: name=%s", name ) );
//    }
//
//    void unbindDescriptor( BucketedSingleObjectDescriptor<E> aDescriptor, Map<String, Object> properties )
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
//        state.type = "SingleObjectFileStore";
//        state.name = name();
//        return state;
//    }
//
//    @Override
//    public String url()
//    {
//        try
//        {
//            return entryPoint().toUri().toURL().toString();
//        }
//        catch ( MalformedURLException e )
//        {
//            logger.log( 
//                    FrameworkUtil.getBundle( getClass() ).getBundleContext().getServiceReference( getClass() ), 
//                    LogService.LOG_ERROR, 
//                    "An error occurred when building the object URL in the SingleObjectFileStore", 
//                    e );
//
//            return "ERROR";
//        }
//    }
//
//    private Path entryPoint()
//    {
//        final String baseLocation = location.replaceFirst( "^~", System.getProperty( "user.home" ) );
//        final Path entryPoint = new File( baseLocation ).toPath();
//        return entryPoint;
//    }
//
//    @Override
//    public String outerPath()
//    {
//        return outerPathInternal().toString();
//    }
//
//    private Path outerPathInternal()
//    {
//        final Path contextPath = Paths.get( confinement, domain, bc, module, version );
//        final Path base = entryPoint().resolve( contextPath );
//        return base;
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
//        try
//        {
//            final BucketNameParser parser = BucketNameParser.newParser();
//            final BucketPathConverter converter = BucketPathConverter.newConverter();
//            final BucketContextualizer contextualizer = BucketContextualizer.newContextualizer();
//
//            final Try<E> entity = parser.parse( bucket(), descriptor.packaging() )
//                .map( b -> converter.convert( descriptor, b ) )
//                .map( c -> contextualizer.contextualize( url(), c ) )
//                .flatMap( b -> descriptor.read( b ) );
//
//            if( entity.isFailure() )
//                deferred.fail( entity.getException() );
//            else
//                deferred.resolve( Stream.of( entity.get() ) );
//        }
//        catch ( Throwable t )
//        {
//            logger.log( 
//                    FrameworkUtil.getBundle( getClass() ).getBundleContext().getServiceReference( getClass() ), 
//                    LogService.LOG_ERROR, 
//                    "An error occurred when reading from the SingleObjectFileStore", 
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
//
//        new Thread(() -> {
//            try
//            {
//                final E singleEntity = anEntityStream.findAny()
//                        .orElseThrow( () -> new IllegalStateException( "Could not retrieve object" ) );
//                final Try<Confirmation> ok = descriptor.write( singleEntity, url() )
//                        .flatMap( b -> writeSingleBucket( b ) );
//
//                if (ok.isSuccess())
//                    deferred.resolve( ok.get() );
//                else
//                    deferred.fail( ok.getException() );
//            }
//            catch( Exception e )
//            {
//                deferred.fail( e );
//            }
//        }).start();
//        
//        return deferred.getPromise();
//    }
//
//    @Override
//    public Try<Confirmation> push( Increment<E> anIncrement, Mappable<E> aMappable )
//    {
//        final Try<BucketStore.Bucket> ok = descriptor.write( anIncrement.value(), url() );
//        if (ok.isFailure())
//            return Try.failure( ok.getException() );
//        final BucketStore.Bucket bucket = ok.get();
//        if (!bucket.content().isPresent())
//            return Try.failure( "No content to write" );
//
//        try
//        {
//            final Path outerPath = outerPathInternal();
//            final File bucketFile = new File( outerPath.toFile(), bucket.fullName() );
//            writeToFile( bucket.content().get(), bucketFile );
//            return Try.success( Confirmation.novo() );
//        }
//        catch ( Exception e )
//        {
//            return Try.failure( e );
//        }
//    }
//
//    private Try<Confirmation> writeSingleBucket( BucketStore.Bucket bucket )
//    {
//        try
//        {
//            final Path outerPath = outerPathInternal();
//            final File baseFile = outerPath.toFile();
//            final File bucketFile = new File( outerPath.toFile(), bucket.fullName() );
//            baseFile.mkdirs();
//            bucketFile.getParentFile().mkdirs();
//            bucketFile.createNewFile();
//            writeToFile( bucket.content().orElse( null ), bucketFile );
//
//            return Try.success( Confirmation.novo() );
//        }
//        catch ( Exception e )
//        {
//            return Try.failure( e );
//        }
//    }
//
//    private void writeToFile( String contents, File toFile )
//            throws Exception
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
