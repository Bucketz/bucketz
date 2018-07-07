package org.bucketz.store;

import org.bucketz.BucketStore;
import org.osgi.annotation.versioning.ProviderType;

@ProviderType
public interface CloudStore<D>
    extends BucketStore<D>, Writable<D>
{
    static final String PID = "org.bucketz.cloud";

    /**
     * Used to indicate that the CloudStore backend is ready.
     * If a service (usually an AggregateDescriptor) depends on the CloudStore, 
     * it should not be started until the implementation is ready.
     * 
     * This is because it can depend (very indirectly) on URL Handlers.
     */
    static interface Ready {}

    static @interface Configuration
    {
        String location();
    }
}
