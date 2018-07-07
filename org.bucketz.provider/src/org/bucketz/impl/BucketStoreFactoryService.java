package org.bucketz.impl;

import org.bucketz.BucketDescriptor;
import org.bucketz.BucketIO;
import org.bucketz.BucketStore;
import org.bucketz.BucketStoreFactory;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.component.ComponentFactory;
import org.osgi.service.component.annotations.Reference;

public class BucketStoreFactoryService
    implements BucketStoreFactory
{

    @Reference(target="(" + ComponentConstants.COMPONENT_NAME + "= " + "your.component" + ")")
    private ComponentFactory cf;
    
    @Override
    public <D> BucketStore<D> newStore( 
            BucketStore.Configuration usingConfiguration, 
            BucketDescriptor<D> aDescriptor,
            BucketIO<D> io )
            throws Exception
    {
        return null;
    }

    @Override
    public <D> void release( BucketStore<D> aStore )
            throws Exception
    {
    }
}
