package org.bucketz.store;

import org.osgi.annotation.versioning.ProviderType;

@ProviderType
public interface CloudStore<D>
    extends BucketStore<D>, Writable<D>
{
    static final String PID = "org.bucketz.cloud";

    /**
     * Used to indicate that the CloudStore backend is ready.
     */
    static interface Ready {}
}
