package org.bucketz.store;

import org.osgi.annotation.versioning.ProviderType;

/**
 * Used when there is no initial data, and no intent to actually store the data.
 */
@ProviderType
public interface EmptyStore<D>
    extends BucketStore<D>
{
    static final String PID = "org.bucketz.empty";
}
