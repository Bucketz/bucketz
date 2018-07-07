package org.bucketz;

public interface BucketStoreFactory
{
    <D>BucketStore<D> newStore(
            BucketStore.Configuration usingConfiguration, 
            BucketDescriptor<D> aDescriptor,
            BucketIO<D> io )
        throws Exception;

    /**
     * For the special case of a Bucket that contains only a single object.
     */
    <D>BucketStore<D> newSingleObjectStore(
            BucketStore.Configuration usingConfiguration, 
            BucketDescriptor.Single<D> aDescriptor,
            BucketIO.Single<D> io )
        throws Exception;

    <D>void release( BucketStore<D> aStore )
        throws Exception;

    static interface ConfigurationBuilder
    {
        ConfigurationBuilder usingType( Bucketz.Type aType );
        ConfigurationBuilder fromLocation( String aLocation );
        ConfigurationBuilder setNameTo( String aName );
        ConfigurationBuilder setOuterPathTo( String anOuterPath );
        ConfigurationBuilder useDataFromBundle( long aBundleId );

        BucketStore.Configuration get();
    }
}
