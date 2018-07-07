package org.bucketz.impl;

import java.util.concurrent.ConcurrentHashMap;

import org.bucketz.BucketIO;
import org.bucketz.Bucketz;
import org.bucketz.plugin.BucketStoreProvider;
import org.bucketz.UncheckedBucketException;
import org.bucketz.store.BucketDescriptor;
import org.bucketz.store.BucketStore;
import org.bucketz.store.BucketStoreFactory;
import org.bucketz.store.BucketDescriptor.Single;
import org.bucketz.store.BucketStore.Configuration;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.component.ComponentFactory;
import org.osgi.service.component.ComponentInstance;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.log.LogService;

@Component(immediate=true)
public class BucketStoreFactoryService
    implements BucketStoreFactory
{
    private final ConcurrentHashMap<String, BucketStoreProvider> providers = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ComponentFactory> factories = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ComponentInstance> instances = new ConcurrentHashMap<>();

    @Reference private LogService logger;

    @Reference(
            cardinality = ReferenceCardinality.MULTIPLE,
            policy = ReferencePolicy.DYNAMIC,
            target = "(" + ComponentConstants.COMPONENT_FACTORY + "=" + BucketStoreProvider.FACTORY + ")" )
    void bindStoreProviderComponentFactory( ComponentFactory cf )
    {
        final ComponentInstance component = cf.newInstance(null);
        final BucketStoreProvider provider = (BucketStoreProvider)component.getInstance();
        final String typeOfFactory = provider.type().name();
        if (instances.containsKey(typeOfFactory))
        {
            component.dispose();
            logger.log( 
                    FrameworkUtil.getBundle( getClass() ).getBundleContext().getServiceReference( getClass() ), 
                    LogService.LOG_WARNING, 
                    String.format( "Provider of type '%s' already registered. Ignoring.", typeOfFactory ) );
            return;
        }

        providers.put( typeOfFactory, provider );
        instances.put( typeOfFactory, component );
        factories.put( typeOfFactory, cf );
    }

    void unbindStoreProviderComponentFactory( ComponentFactory cf )
        throws Exception
    {
        final String typeOfFactory = factories.entrySet().stream()
                .filter( e -> cf == e.getValue() )
                .map( e -> e.getKey() )
                .findFirst()
                .orElseThrow( () -> new Exception("Could not determine ComponentFactory type") );
        final ComponentInstance instance = instances.get( typeOfFactory );
        if (instance != null)
            instance.dispose();
        providers.remove( typeOfFactory );
        instances.remove( typeOfFactory );
        factories.remove( typeOfFactory );
    }

    // TODO: do I really need to do this??
//    void deactivate()
//    {
//        if (!instances.isEmpty())
//        {
//            final ConcurrentHashMap<String, ComponentInstance> copy = new ConcurrentHashMap<>( instances );
//            instances.clear();
//            copy.values()
//                .forEach( ci -> ci.dispose() );
//        }
//    }

    @Override
    public <D> BucketStore<D> newStore( 
            BucketStore.Configuration usingConfiguration, 
            BucketDescriptor<D> aDescriptor,
            BucketIO<D> io )
            throws UncheckedBucketException
    {
        return get(usingConfiguration).newStore( usingConfiguration, aDescriptor, io );
    }

    @Override
    public <D> BucketStore<D> newSingleObjectStore(
            Configuration usingConfiguration, Single<D> aDescriptor,
            org.bucketz.BucketIO.Single<D> io )
            throws UncheckedBucketException
    {
        return get(usingConfiguration).newSingleObjectStore( usingConfiguration, aDescriptor, io );
    }

    @Override
    public <D> void release( BucketStore<D> aStore )
            throws UncheckedBucketException
    {
        get(aStore.type().name()).release( aStore );
    }

    private BucketStoreProvider get(Configuration config)
        throws UncheckedBucketException
    {
        return get(config.type());
    }

    private BucketStoreProvider get(String typeOfFactory)
        throws UncheckedBucketException
    {
        if (typeOfFactory == null || typeOfFactory.isEmpty())
            throw new UncheckedBucketException( "Factory type not provided" );

        try
        {
            Bucketz.Type.valueOf( typeOfFactory );
        }
        catch ( Exception e )
        {
            throw new UncheckedBucketException( String.format( "Could not determine BucketStore type: %s", typeOfFactory ) );
        }

        if (!providers.containsKey( typeOfFactory ))
            throw new UncheckedBucketException( String.format( "No provided of type '%s' registered", typeOfFactory ) );

        final BucketStoreProvider provider = providers.get( typeOfFactory );
        return provider;
    }
}
