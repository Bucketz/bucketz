package org.bucketz.impl;
//
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
//import net.leangen.expedition.platform.bucketstore.old.BucketStoreDomain;
//import net.leangen.expedition.platform.bucketstore.old.BucketStores;
//
//@Component(
//        name = "net.leangen.expedition.platform.repository.filestores",
//        configurationPolicy = ConfigurationPolicy.REQUIRE,
//        immediate = true
//)
//public class FileStoresService
//    implements BucketStores
//{
//    private String location;
//    private ConcurrentHashMap<String, BucketStoreDomain> domains = new ConcurrentHashMap<>();
//
//    @Activate
//    void activate( BucketStores.Configuration config )
//    {
//        location = config.location();
//    }
//
//    @Reference( cardinality = ReferenceCardinality.AT_LEAST_ONE, policy = ReferencePolicy.DYNAMIC )
//    void addFileStoreDomain( BucketStoreDomain fdDomain, Map<String, Object> properties )
//    {
//        domains.put( fdDomain.name(), fdDomain );
//    }
//
//    void removeFileStoreDomain( BucketStoreDomain fdDomain, Map<String, Object> properties )
//    {
//        domains.remove( fdDomain.name() );
//    }
//
//    void deactivate()
//    {
//        location = null;
//        domains.clear();
//    }
//
//    @Override
//    public String location()
//    {
//        return location;
//    }
//
//    @Override
//    public List<BucketStoreDomain> domains()
//    {
//        return domains.values().stream()
//                .sorted( (d1,d2) -> d1.name().compareTo( d2.name() ) )
//                .collect( Collectors.toList() );
//    }
//
//    @Override
//    public BucketStoreDomain getByName( String aFileStoreName )
//    {
//        if( aFileStoreName == null || !domains.containsKey( aFileStoreName ) )
//            return FileStoreDomainService.error();
//
//        return domains.get( aFileStoreName );
//    }
//}
