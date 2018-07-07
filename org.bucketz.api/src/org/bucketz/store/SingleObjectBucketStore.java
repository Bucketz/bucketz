package org.bucketz.store;

import org.osgi.annotation.versioning.ProviderType;

/**
 * Not required, but provides a simpler and more intentional interface for interacting with 
 * the one and only object stored in the BucketStore.
 */
@ProviderType
public interface SingleObjectBucketStore<D>
    extends BucketStore<D>
{
    static final String SINGLE_BUCKET_NAME_PARAM = "bucketz.bucketName";

    static interface SingleObjectBundleStore<D>
        extends SingleObjectBucketStore<D>, BundleStore<D>
    {
        static final String PID = BundleStore.PID + ".single";
    }

    static interface SingleObjectFileStore<D>
        extends SingleObjectBucketStore<D>, FileStore<D>
    {
        static final String PID = FileStore.PID + ".single";
    }

    static interface SingleObjectCloudStore<D>
        extends SingleObjectBucketStore<D>, CloudStore<D>
    {
        static final String PID = CloudStore.PID + ".single";
    }

    /**
     * The one and only Bucket provided by this Store.
     */
    String bucket();

    /**
     * Same as for a regular BucketStore, but since there is one and only one Bucket,
     * the Bucket name is also provided with the configuration.
     */
    static @interface Configuration
    {
        long bundleId();
        String location();
        String bucketName();
    }
}
