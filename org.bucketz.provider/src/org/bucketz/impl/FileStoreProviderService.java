package org.bucketz.impl;

import org.bucketz.BucketIO;
import org.bucketz.Bucketz;
import org.bucketz.UncheckedBucketException;
import org.bucketz.lib.BucketStoreFactoryImpl;
import org.bucketz.plugin.BucketStoreProvider;
import org.bucketz.store.BucketDescriptor;
import org.bucketz.store.BucketStore;
import org.bucketz.store.FileStore;
import org.bucketz.store.BucketDescriptor.Single;
import org.bucketz.store.BucketStore.Configuration;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.component.ComponentFactory;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(
        name = BucketStoreProvider.FILE_PROVIDER_PID,
        factory = BucketStoreProvider.FACTORY)
public class FileStoreProviderService
    implements BucketStoreProvider
{
    @Reference(target="(" + ComponentConstants.COMPONENT_FACTORY + "=" + FileStore.PID + ")")
    private ComponentFactory cf;

    private BucketStoreFactoryImpl implementation;

    @Activate
    void activate()
    {
        implementation = new BucketStoreFactoryImpl(cf);
    }

    void deactivate()
    {
        implementation.dispose();
        implementation = null;
    }

    @Override
    public <D> BucketStore<D> newStore( 
            BucketStore.Configuration usingConfiguration, 
            BucketDescriptor<D> aDescriptor,
            BucketIO<D> aBucketIO )
            throws UncheckedBucketException
    {
        return implementation.newStore( usingConfiguration, aDescriptor, aBucketIO );
    }

    @Override
    public <D> BucketStore<D> newSingleObjectStore(
            Configuration usingConfiguration, 
            Single<D> aDescriptor,
            org.bucketz.BucketIO.Single<D> aBucketIO )
            throws UncheckedBucketException
    {
        return implementation.newSingleObjectStore( usingConfiguration, aDescriptor, aBucketIO );
    }

    @Override
    public <D> void release( BucketStore<D> aStore )
            throws UncheckedBucketException
    {
        implementation.release( aStore );
    }

    @Override
    public Bucketz.Type type()
    {
        return Bucketz.Type.FILE;
    }

    @Override
    public Bucketz.Provider provider()
    {
        return Bucketz.Provider.BUCKETZ;
    }
}
