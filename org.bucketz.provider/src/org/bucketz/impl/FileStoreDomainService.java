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
//import net.leangen.expedition.platform.bucketstore.old.BucketStoreDomain;
//import net.leangen.expedition.platform.bucketstore.old.BucketStoreGroup;
//
//@Component(
//        name = "net.leangen.expedition.platform.repository.filestore.domain",
//        configurationPolicy = ConfigurationPolicy.REQUIRE,
//        immediate = true
//)
//public class FileStoreDomainService
//    implements BucketStoreDomain
//{
//    private static BucketStoreDomain ERROR;
//
//    static BucketStoreDomain error()
//    {
//        if( ERROR == null )
//        {
//            ERROR = new BucketStoreDomain()
//            {                
//                @Override public String name() { return "ERROR"; }
//                @Override public List<BucketStoreGroup> groups() { return Collections.emptyList(); }
//                @Override public BucketStoreGroup getByName( String aGroupName ) { return FileStoreGroupService.error(); }
//            };
//        }
//
//        return ERROR;
//    }
//
//    private String name;
//    private ConcurrentHashMap<String, BucketStoreGroup> groups = new ConcurrentHashMap<>();
//
//    @Activate
//    void activate( BucketStoreDomain.Configuration config )
//    {
//        name = config.name();
//    }
//
//    @Reference( cardinality = ReferenceCardinality.AT_LEAST_ONE, policy = ReferencePolicy.DYNAMIC )
//    void addFileStoreGroup( BucketStoreGroup fdGroup, Map<String, Object> properties )
//    {
//        groups.put( fdGroup.name(), fdGroup );
//    }
//
//    void removeFileStoreGroup( BucketStoreGroup fdGroup, Map<String, Object> properties )
//    {
//        groups.remove( fdGroup.name() );
//    }
//
//    void deactivate()
//    {
//        name = null;
//        groups.clear();
//    }
//
//    @Override
//    public String name()
//    {
//        return name;
//    }
//
//    @Override
//    public List<BucketStoreGroup> groups()
//    {
//        return groups.values().stream()
//                .collect( Collectors.toList() );
//    }
//
//    @Override
//    public BucketStoreGroup getByName( String aGroupName )
//    {
//        if( aGroupName == null || !groups.containsKey( aGroupName ) )
//            return FileStoreGroupService.error();
//
//        return groups.get( aGroupName );
//    }
//}
