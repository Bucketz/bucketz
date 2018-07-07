package org.bucketz.store;

import org.bucketz.BucketIO;
import org.bucketz.Bucketz;
import org.bucketz.UncheckedBucketException;

public interface BucketStoreFactory
{
    static final String PID = "org.bucketz.store.factory";

    <D>BucketStore<D> newStore(
            BucketStore.Configuration usingConfiguration, 
            BucketDescriptor<D> aDescriptor,
            BucketIO<D> io )
        throws UncheckedBucketException;

    /**
     * For the special case of a Bucket that contains only a single object.
     */
    <D>BucketStore<D> newSingleObjectStore(
            BucketStore.Configuration usingConfiguration, 
            BucketDescriptor.Single<D> aDescriptor,
            BucketIO.Single<D> io )
        throws UncheckedBucketException;

    <D>void release( BucketStore<D> aStore )
        throws UncheckedBucketException;

    static interface ConfigurationBuilder
    {
        ConfigurationBuilder usingType( Bucketz.Type aType );
        ConfigurationBuilder fromLocation( String aLocation );
        ConfigurationBuilder setNameTo( String aName );
        ConfigurationBuilder setOuterPathTo( String anOuterPath );
        ConfigurationBuilder useDataFromBundle( long aBundleId );

        BucketStore.Configuration get();
    }

    static interface Available
    {
        static final String PID = BucketStoreFactory.PID + ".available";
    }
}
