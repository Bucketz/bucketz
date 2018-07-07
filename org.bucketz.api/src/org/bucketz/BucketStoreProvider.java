package org.bucketz;

public interface BucketStoreProvider
    extends BucketStoreFactory
{
    Bucketz.Type type();
    Bucketz.Provider provider();
}
