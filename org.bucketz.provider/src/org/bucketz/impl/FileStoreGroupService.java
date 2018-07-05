package org.bucketz.impl;
//
//import java.util.Collections;
//import java.util.List;
//import java.util.Map;
//import java.util.concurrent.ConcurrentHashMap;
//import java.util.stream.Collectors;
//
//import org.osgi.service.component.annotations.Activate;
//import org.osgi.service.component.annotations.Component;
//import org.osgi.service.component.annotations.ConfigurationPolicy;
//import org.osgi.service.component.annotations.Reference;
//import org.osgi.service.component.annotations.ReferenceCardinality;
//import org.osgi.service.component.annotations.ReferencePolicy;
//
//import net.leangen.expedition.platform.bucketstore.BucketStore;
//import net.leangen.expedition.platform.bucketstore.old.BucketStoreGroup;
//
//@Component(
//        name = "net.leangen.expedition.platform.repository.filestore.group",
//        configurationPolicy = ConfigurationPolicy.REQUIRE,
//        immediate = true
//)
//public class FileStoreGroupService
//    implements BucketStoreGroup
//{
//    private static BucketStoreGroup ERROR;
//
//    static BucketStoreGroup error()
//    {
//        if( ERROR == null )
//        {
//            ERROR = new BucketStoreGroup()
//            {
//                @Override public String name() { return "ERROR"; }
//                @Override public String domain() { return "ERROR"; }
//                @Override public List<BucketStore<?>> stores() { return Collections.emptyList(); }
//                @Override public BucketStore<?> getByName( String aStoreName ) { return null; }//FileStoreService.error(); }
//            };
//        }
//
//        return ERROR;
//    }
//
//    private String name;
//    private String domain;
//    private ConcurrentHashMap<String, BucketStore<?>> stores = new ConcurrentHashMap<>();
//
//    @Activate
//    void activate( BucketStoreGroup.Configuration config )
//    {
//        name = config.name();
//        domain = config.domain();
//    }
//
//    @Reference( cardinality = ReferenceCardinality.AT_LEAST_ONE, policy = ReferencePolicy.DYNAMIC )
//    void addFileStore( BucketStore<?> bucketStore, Map<String, Object> properties )
//    {
//        stores.put( bucketStore.name(), bucketStore );
//    }
//
//    void removeFileStore( BucketStore<?> bucketStore, Map<String, Object> properties )
//    {
//        stores.remove( bucketStore.name() );
//    }
//
//    void deactivate()
//    {
//        name = null;
//        stores.clear();
//    }
//
//    @Override
//    public String name()
//    {
//        return name;
//    }
//
//    @Override
//    public String domain()
//    {
//        return domain;
//    }
//
//    @Override
//    public List<BucketStore<?>> stores()
//    {
//        return stores.values().stream()
//                .collect( Collectors.toList() );
//    }
//
//    @Override
//    public BucketStore<?> getByName( String aStoreName )
//    {
//        if( aStoreName == null || !stores.containsKey( aStoreName ) )
////            return FileStoreService.error();
//            return null;
//
//        return stores.get( aStoreName );
//    }
//}
