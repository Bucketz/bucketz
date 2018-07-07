package org.bucketz.lib;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import org.bucketz.Bucketz;
import org.bucketz.store.BucketDescriptor;
import org.bucketz.store.BucketStore;
import org.bucketz.store.BundleStore;
import org.bucketz.store.CloudStore;
import org.bucketz.store.FileStore;
import org.bucketz.store.SingleObjectBucketStore;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;

public class BucketStoreActivator
{
    private ConfigurationAdmin cm;
    private ComponentContext componentContext;

    private BucketStore.Configuration configuration;

    private long bundleId = -1;
    private String pid;
    private String version;
    private String path;
    private String descriminant;
    private boolean isSingleObject = false;
    private String singleBucketName;

    private BucketStoreActivator() {}

    public static BucketStoreActivator newActivator()
    {
        return new BucketStoreActivator();
    }

    public BucketStoreActivator setConfigurationAdmin( ConfigurationAdmin aConfigurationAdmin )
    {
        cm = aConfigurationAdmin;
        return this;
    }

    public BucketStoreActivator withComponentContext( ComponentContext aComponentContext )
    {
        componentContext = aComponentContext;
        return this;
    }

    public BucketStoreActivator usingConfiguration( BucketStore.Configuration aConfiguration )
    {
        configuration = aConfiguration;
        return this;
    }

    public BucketStoreActivator setPathTo( String aPath )
    {
        path = aPath;
        return this;
    }

    public BucketStoreActivator setPid( String aPid )
    {
        pid = aPid;
        return this;
    }

    public BucketStoreActivator setVersion( String aVersion )
    {
        version = aVersion;
        return this;
    }

    public BucketStoreActivator useDescriminant( String aDescriminant )
    {
        descriminant = aDescriminant;
        return this;
    }

    public BucketStoreActivator asSingleObject( String aSingleBucketName )
    {
        isSingleObject = true;
        singleBucketName = aSingleBucketName;
        return this;
    }

    public void activate()
        throws Exception
    {
        if (configuration != null && componentContext != null && Bucketz.Type.BUNDLE.equals( configuration.type() ))
            bundleId = componentContext.getBundleContext().getBundle().getBundleId();

        final List<String> errors = new ArrayList<>();
        if( cm == null )
            errors.add( "ConfigurationAdmin is not set" );
        if( componentContext == null )
            errors.add( "Invalid ComponentContext" );
        if (configuration == null)
            errors.add( "Configuration is missing" );
        if (configuration != null && configuration.location() == null || configuration.location().isEmpty())
            errors.add( "Location not provided" );
        if (configuration != null && (Bucketz.Type.CLOUD.equals(configuration.type()))  && bundleId != -1)
            errors.add( "Configured as a CloudStore, but the bundle ID is set" );
        if( path == null || path.isEmpty() )
            errors.add( "Path not provided" );
        if( pid == null || pid.isEmpty() )
            errors.add( "PID not provided" );
        if( version == null || version.isEmpty() )
            errors.add( "Version not provided" );
        if (isSingleObject && (singleBucketName == null || singleBucketName.isEmpty()))
            errors.add( "Configured as a single Bucket, but the Bucket name is not provided" );

        if( !errors.isEmpty() )
            throw new IllegalStateException( errors.get( 0 ) );

        final StringBuilder target = new StringBuilder()
                .append( "(&" )
                    .append( "(" ).append( BucketDescriptor.TARGET_PID_PARAM ).append( "=" ).append( pid ).append( ")" )
                    .append( descriminant != null ? appendDescriminant() : "" )
                    .append( "(" ).append( BucketDescriptor.TARGET_VERSION_PARAM ).append( "=" ).append( version.toString() ).append( ")" )
                .append( ")" );

        final Configuration bucketStoreConfig;
        if( bundleId != -1 )
        {
            if (isSingleObject)
                bucketStoreConfig = cm.createFactoryConfiguration( SingleObjectBucketStore.SingleObjectBundleStore.PID, "?" );
            else
                bucketStoreConfig = cm.createFactoryConfiguration( BundleStore.PID, "?" );                
        }
        else if (Bucketz.Type.CLOUD.equals(configuration.type()))
        {
            if (isSingleObject)
                bucketStoreConfig = cm.createFactoryConfiguration( SingleObjectBucketStore.SingleObjectCloudStore.PID, "?" );
            else
                bucketStoreConfig = cm.createFactoryConfiguration( CloudStore.PID, "?" );                
        }
        else // File is the default
        {
            if (isSingleObject)
                bucketStoreConfig = cm.createFactoryConfiguration( SingleObjectBucketStore.SingleObjectFileStore.PID, "?" );
            else
                bucketStoreConfig = cm.createFactoryConfiguration( FileStore.PID, "?" );
        }
        final Dictionary<String, Object> bucketStoreConfigProperties = new Hashtable<>();
        if( bundleId != -1 )
            bucketStoreConfigProperties.put( "bundleId", componentContext.getBundleContext().getBundle().getBundleId() );
        bucketStoreConfigProperties.put( "location", configuration.location() );
        bucketStoreConfigProperties.put( "path", path );
        bucketStoreConfigProperties.put( "version", version );
        bucketStoreConfigProperties.put( "descriminant", descriminant );
        bucketStoreConfigProperties.put( "Descriptor.target", target.toString() );
        bucketStoreConfigProperties.put( BucketDescriptor.TARGET_PID_PARAM, pid );
        bucketStoreConfigProperties.put( BucketDescriptor.TARGET_VERSION_PARAM, version.toString() );
        if (isSingleObject)
            bucketStoreConfigProperties.put( SingleObjectBucketStore.SINGLE_BUCKET_NAME_PARAM, singleBucketName );
        bucketStoreConfig.update( bucketStoreConfigProperties );
    }

    private String appendDescriminant()
    {
        return new StringBuilder()
                .append( "(descriminant=" )
                .append( descriminant )
                .append( ")" )
                .toString();
    }
}
