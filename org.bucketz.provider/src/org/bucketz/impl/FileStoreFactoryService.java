package org.bucketz.impl;

import org.bucketz.BucketIO;
import org.bucketz.Bucketz;
import org.bucketz.UncheckedBucketException;
import org.bucketz.lib.BucketStoreFactoryImpl;
import org.bucketz.store.BucketDescriptor;
import org.bucketz.store.BucketStore;
import org.bucketz.store.BucketStoreFactory;
import org.bucketz.store.FileStore;
import org.bucketz.store.BucketDescriptor.Single;
import org.bucketz.store.BucketStore.Configuration;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.component.ComponentFactory;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(
        name = FileStoreFactoryService.COMPONENT_NAME,
        property = {
                Bucketz.Parameters.BUCKET_TYPE + "=" + Bucketz.TypeConstants.FILE
        })
public class FileStoreFactoryService
    implements BucketStoreFactory
{
    public static final String COMPONENT_NAME = PID + ".file";

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
