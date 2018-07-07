package org.bucketz.impl;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import org.bucketz.BucketDescriptor;
import org.bucketz.BucketDescriptor.Single;
import org.bucketz.BucketIO;
import org.bucketz.BucketStore;
import org.bucketz.BucketStore.Configuration;
import org.bucketz.BucketStoreProvider;
import org.bucketz.Bucketz;
import org.bucketz.store.BundleStore;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.component.ComponentFactory;
import org.osgi.service.component.ComponentInstance;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.util.converter.Converters;

@Component(
        immediate = true)
public class BundleStoreProviderService
    implements BucketStoreProvider
{
    @Reference(target="(" + ComponentConstants.COMPONENT_FACTORY + "=" + BundleStore.PID + ")")
    private ComponentFactory cf;

    @Activate
    void activate()
    {
        toString();
    }

    void deactivate()
    {
        
    }

    @Override
    public <D> BucketStore<D> newStore( 
            BucketStore.Configuration usingConfiguration, 
            BucketDescriptor<D> aDescriptor,
            BucketIO<D> aBucketIO )
            throws Exception
    {
        final Dictionary<String, Object> properties = new Hashtable<>();
        // Do in two steps due to erasure.
        @SuppressWarnings( "unchecked" )
        final Set<Map.Entry<String, Object>> set = Converters.standardConverter()
                .convert( usingConfiguration )
                .to( Map.class )
                .entrySet();
        set.forEach( e -> properties.put( e.getKey(), e.getValue() ) );
        properties.put( "descriptor", aDescriptor );
        properties.put( "io", aBucketIO );
        final ComponentInstance component = cf.newInstance( properties );
        @SuppressWarnings( "unchecked" )
        final BucketStore<D> store = (BucketStore<D>)component.getInstance();
        return store;
    }

    @Override
    public <D> BucketStore<D> newSingleObjectStore(
            Configuration usingConfiguration, 
            Single<D> aDescriptor,
            org.bucketz.BucketIO.Single<D> aBucketIO )
            throws Exception
    {
        return newStore( usingConfiguration, aDescriptor, aBucketIO );
    }

    @Override
    public <D> void release( BucketStore<D> aStore )
            throws Exception
    {
        // TODO
    }

    @Override
    public Bucketz.Type type()
    {
        return Bucketz.Type.BUNDLE;
    }

    @Override
    public Bucketz.Provider provider()
    {
        return Bucketz.Provider.BUCKETZ;
    }
}
