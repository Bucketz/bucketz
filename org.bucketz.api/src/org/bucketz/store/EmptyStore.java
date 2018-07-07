package org.bucketz.store;

import org.bucketz.BucketStore;
import org.osgi.annotation.versioning.ProviderType;

/**
 * Used when there is no initial data, and no intent to actually store the data.
 */
@ProviderType
public interface EmptyStore<D>
    extends BucketStore<D>
{
}
