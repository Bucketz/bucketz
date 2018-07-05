package org.bucketz.impl;
//
//import java.util.List;
//import java.util.concurrent.ConcurrentHashMap;
//import java.util.stream.Collectors;
//
//import org.osgi.service.component.annotations.Component;
//import org.osgi.service.component.annotations.Reference;
//import org.osgi.service.component.annotations.ReferenceCardinality;
//import org.osgi.service.component.annotations.ReferencePolicy;
//
//import net.leangen.expedition.platform.datastore.DataStore;
//import net.leangen.expedition.platform.datastore.DataStores;
//
//@Component( name = DataStores.COMPONENT_NAME )
//public class DataStoresService
//    implements DataStores
//{
//    private final ConcurrentHashMap<String, DataStore<?>> stores = new ConcurrentHashMap<>();
//
//    @Reference( cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC )
//    void bindStore( DataStore<?> aDataStore )
//    {
//        stores.put( aDataStore.name(), aDataStore );
//    }
//
//    void unbindStore( DataStore<?> aDataStore )
//    {
//        stores.remove( aDataStore.name() );
//    }
//
//    @Override
//    public List<DataStore<?>> stores()
//    {
//        return stores.values().stream()
//                .collect( Collectors.toList() );
//    }
//}
