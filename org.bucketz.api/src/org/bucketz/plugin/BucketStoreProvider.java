package org.bucketz.plugin;

import org.bucketz.Bucketz;
import org.bucketz.store.BucketStoreFactory;

/**
 * Used by BundleStore implementors to declare available services.
 */
public interface BucketStoreProvider
    extends BucketStoreFactory
{
    static final String PID = BucketStoreFactory.PID + ".provider";
    static final String FACTORY = PID;
    static final String BUNDLE_PROVIDER_PID = PID + ".bundle";
    static final String FILE_PROVIDER_PID = PID + ".file";
    static final String CLOUD_PROVIDER_PID = PID + ".cloud";
    static final String EMPTY_PROVIDER_PID = PID + ".empty";

    Bucketz.Type type();
    Bucketz.Provider provider();
}
