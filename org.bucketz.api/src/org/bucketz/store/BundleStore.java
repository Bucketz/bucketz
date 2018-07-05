package org.bucketz.store;

/**
 * A BucketStore whereby the data is stored as files in a bundle. This type of
 * BucketStore is read-only. Updates to the data must be done manually offline.
 * 
 * The BundleStore is useful for rapid prototyping (as it completely eliminates
 * any framework requirements), or for data types that do not usually get updated.
 */
import org.bucketz.BucketStore;

public interface BundleStore<E>
    extends BucketStore<E>
{
    static final String PID = "org.bucketz.bundle";

    static @interface Configuration
    {
        String name();
        long bundleId();
        String location();
        String outerPath();
    }
}
