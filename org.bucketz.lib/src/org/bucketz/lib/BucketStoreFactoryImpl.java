package org.bucketz.lib;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.bucketz.BucketIO;
import org.bucketz.Bucketz;
import org.bucketz.UncheckedBucketException;
import org.bucketz.store.BucketDescriptor;
import org.bucketz.store.BucketStore;
import org.bucketz.store.BucketStoreFactory;
import org.bucketz.store.BucketDescriptor.Single;
import org.bucketz.store.BucketStore.Configuration;
import org.osgi.service.component.ComponentFactory;
import org.osgi.service.component.ComponentInstance;
import org.osgi.util.converter.Converters;

public class BucketStoreFactoryImpl
    implements BucketStoreFactory
{
    private final ComponentFactory cf;
    private final ConcurrentHashMap<String, ComponentInstance> instances = new ConcurrentHashMap<>();

    public BucketStoreFactoryImpl( ComponentFactory aComponentFactory )
    {
        cf = aComponentFactory;
    }

    public void dispose()
    {
        if (!instances.isEmpty())
        {
            final ConcurrentHashMap<String, ComponentInstance> copy = new ConcurrentHashMap<>( instances );
            instances.clear();
            copy.values()
                .forEach( ci -> ci.dispose() );
        }
    }

    @Override
    public <D> BucketStore<D> newStore( 
            BucketStore.Configuration usingConfiguration, 
            BucketDescriptor<D> aDescriptor,
            BucketIO<D> aBucketIO )
            throws UncheckedBucketException
    {
        final Dictionary<String, Object> properties = new Hashtable<>();
        // Do in two steps due to erasure.
        @SuppressWarnings( "unchecked" )
        final Set<Map.Entry<String, Object>> set = Converters.standardConverter()
                .convert( usingConfiguration )
                .to( Map.class )
                .entrySet();
        set.forEach( e -> properties.put( e.getKey(), e.getValue() ) );
        properties.put( Bucketz.Parameters.DESCRIPTOR, aDescriptor );
        properties.put( Bucketz.Parameters.IO, aBucketIO );
        final ComponentInstance component = cf.newInstance( properties );
        @SuppressWarnings( "unchecked" )
        final BucketStore<D> store = (BucketStore<D>)component.getInstance();
        if (instances.containsKey(store.name()))
        {
            component.dispose();
            throw new UncheckedBucketException( "A BucketStore with the name '%s' is already registered." );
        }

        instances.put( store.name(), component );
        return store;
    }

    @Override
    public <D> BucketStore<D> newSingleObjectStore(
            Configuration usingConfiguration, 
            Single<D> aDescriptor,
            org.bucketz.BucketIO.Single<D> aBucketIO )
            throws UncheckedBucketException
    {
        return newStore( usingConfiguration, aDescriptor, aBucketIO );
    }

    @Override
    public <D> void release( BucketStore<D> aStore )
            throws UncheckedBucketException
    {
        final ComponentInstance component = instances.remove( aStore.name() );
        if (component != null)
            component.dispose();
    }
}
