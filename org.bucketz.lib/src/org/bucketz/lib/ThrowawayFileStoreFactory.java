package org.bucketz.lib;

import java.net.URI;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.bucketz.BucketDescriptor;
import org.bucketz.store.FileStore;
import org.bucketz.store.SingleObjectBucketStore;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.log.LogService;
import org.osgi.util.promise.Deferred;
import org.osgi.util.promise.Promise;

public class ThrowawayFileStoreFactory<E>
{
    private final String fileStorePid;
    private final String singleObjectBucketStorePid;

    private ConfigurationAdmin cm;
    private Configuration config;
    private LogService logger;
    private boolean debug;

    private String location;
    private String pid;
    private String version;
    private boolean isSingleObject = false;
    private String singleBucketName;

    private ThrowawayFileStoreFactory( 
            String aFileStorePid,
            String aSingleObjectBucketStorePid )
    {
        fileStorePid = aFileStorePid;
        singleObjectBucketStorePid = aSingleObjectBucketStorePid;
    }

    public static <E>ThrowawayFileStoreFactory<E> newFactory(
            String aFileStorePid,
            String aSingleObjectBucketStorePid )
    {
        return new ThrowawayFileStoreFactory<>( aFileStorePid, aSingleObjectBucketStorePid );
    }

    public ThrowawayFileStoreFactory<E> setConfigurationAdmin( ConfigurationAdmin aConfigurationAdmin )
    {
        cm = aConfigurationAdmin;
        return this;
    }

    public ThrowawayFileStoreFactory<E> setLogService( LogService aLogService )
    {
        logger = aLogService;
        return this;
    }

    public ThrowawayFileStoreFactory<E> locatedAt( String aLocation )
    {
        location = aLocation;
        return this;
    }

    public ThrowawayFileStoreFactory<E> setPid( String aPid )
    {
        pid = aPid;
        return this;
    }

    public ThrowawayFileStoreFactory<E> setVersion( String aVersion )
    {
        version = aVersion;
        return this;
    }

    public ThrowawayFileStoreFactory<E> asSingleObject( String aSingleBucketName )
    {
        isSingleObject = true;
        singleBucketName = aSingleBucketName;
        return this;
    }

    public ThrowawayFileStoreFactory<E> debug()
    {
        debug = true;
        return this;
    }

    public Promise<FileStore<E>> get()
    {
        final Deferred<FileStore<E>> deferred = new Deferred<>();

        try
        {
            final List<String> errors = new ArrayList<>();
            if( cm == null )
                errors.add( "ConfigurationAdmin is not set" );
            if (location == null || location.isEmpty())
                errors.add( "Location not provided" );
            if( pid == null || pid.isEmpty() )
                errors.add( "PID not provided" );
            if( version == null )
                errors.add( "Version not provided" );
            if (isSingleObject && (singleBucketName == null || singleBucketName.isEmpty()))
                errors.add( "Configured as a single Bucket, but the Bucket name is not provided" );

            if( !errors.isEmpty() )
                throw new Exception( errors.get( 0 ) );

            final String uuid = UUID.randomUUID().toString();
            final String target = target();

            if (isSingleObject)
                config = cm.createFactoryConfiguration( singleObjectBucketStorePid, "?" );
            else
                config = cm.createFactoryConfiguration( fileStorePid, "?" );
            final Dictionary<String, Object> fileStoreConfigProperties = new Hashtable<>();
            fileStoreConfigProperties.put( "location", location );
            fileStoreConfigProperties.put( "version", version.toString() );
            // TODO descriminant here??
//            fileStoreConfigProperties.put( "confinement", confinement );
            fileStoreConfigProperties.put( "Descriptor.target", target );
            fileStoreConfigProperties.put( BucketDescriptor.TARGET_PID_PARAM, pid );
            fileStoreConfigProperties.put( BucketDescriptor.TARGET_VERSION_PARAM, version );
            fileStoreConfigProperties.put( "uuid", uuid );
            if (isSingleObject)
                fileStoreConfigProperties.put( SingleObjectBucketStore.SINGLE_BUCKET_NAME_PARAM, singleBucketName );
            config.update( fileStoreConfigProperties );

            deferred.resolveWith( waitForInstance( filter( uuid ) ) );
        }
        catch ( Exception e )
        {
            final String message = "Could not instantiate FileStore";
            if( logger != null )
                logger.log( LogService.LOG_ERROR, message );
            else
                e.printStackTrace();
            if( logger != null && debug )
                e.printStackTrace();
            deferred.fail( e );
        }

        return deferred.getPromise();
    }

    private Promise<FileStore<E>> waitForInstance( String aTarget )
    {
        final Deferred<FileStore<E>> deferred = new Deferred<>();

        try
        {
            final BundleContext bundleContext = FrameworkUtil.getBundle( getClass() ).getBundleContext();

            // Wait a second for the system to create the FileStore
            pause(100);
            ServiceReference<?>[] results = bundleContext.getServiceReferences( FileStore.class.getName(), aTarget );

            // If the FileStore ServiceReference has not yet been created, try a few more times before giving up
            if (results == null)
            {
                int numTries = 0;
                while (results == null && numTries++ < 3)
                {
                    pause(100);
                    results = bundleContext.getServiceReferences( FileStore.class.getName(), aTarget );
                }

                if (results == null)
                {
                    // Failed to locate the FileStore.
                    throw new Exception( "Could not locate the FileStore. Giving up." );
                }
            }

            if (results.length != 1)
                throw new Exception( "Could not locate a unique FileStore. Giving up." );

            Object service = bundleContext.getService( results[0] );

            // If the FileStore ServiceReference has not yet been created, try a few more times before giving up
            if (service == null)
            {
                int numTries = 0;
                while (service == null && numTries++ < 3)
                {
                    pause(100);
                    service = bundleContext.getService( results[0] );
                }

                if (service == null)
                {
                    // Failed to locate the FileStore.
                    throw new Exception( "Could not locate the FileStore. Giving up." );
                }
            }

            if ( !(service instanceof FileStore))
                throw new Exception( String.format( "Expected FileStore, but got %s.", service.getClass() ) );

            @SuppressWarnings( "unchecked" )
            final FileStore<E> fileStore = (FileStore<E>)service;
            deferred.resolve( fileStore );
        }
        catch ( Exception e )
        {
            deferred.fail( e );
        }

        return deferred.getPromise();
    }

    private String target()
    {
        return buildFilter( Optional.empty() );
    }

    private String filter( String uuid )
    {
        return buildFilter( Optional.of( uuid ) );
    }

    private String buildFilter( Optional<String> uuid )
    {
        final String uuidString = uuid.isPresent() ? 
                "(uuid=" + uuid.get() + ")" :
                "";
        return new StringBuilder()
                .append( "(&" )
                    .append( "(" ).append( BucketDescriptor.TARGET_PID_PARAM ).append( "=" ).append( pid ).append( ")" )
                    // TODO confinement
//                    .append( "(confinement=" ).append( confinement ).append( ")" )
                    .append( "(" ).append( BucketDescriptor.TARGET_VERSION_PARAM ).append( "=" ).append( version.toString() ).append( ")" )
                    // Add a UUID to make this instance unique. It will be a throwaway service to be dismantled when done.
                    .append( uuidString )
                .append( ")" )
                .toString();
    }

    private void pause( int milliseconds )
    {
        try
        {
            TimeUnit.MILLISECONDS.sleep( milliseconds );
        }
        catch ( InterruptedException e )
        {
        }
    }

    public Promise<URI> cleanup( URI aLocation)
    {
        final Deferred<URI> deferred = new Deferred<>();

        try
        {
            config.delete();
            deferred.resolve( aLocation );
        }
        catch (Exception e )
        {
            deferred.fail( e );
        }

        return deferred.getPromise();
    }
}
