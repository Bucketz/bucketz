package org.bucketz.impl;
//
//import java.util.Collections;
//import java.util.List;
//import java.util.Map;
//import java.util.stream.Stream;
//
//import org.osgi.service.component.annotations.Component;
//import org.osgi.service.component.annotations.ConfigurationPolicy;
//import org.osgi.service.component.annotations.Reference;
//import org.osgi.service.log.LogService;
//import org.osgi.util.promise.Deferred;
//import org.osgi.util.promise.Promise;
//
//import net.leangen.expedition.platform.bucketstore.BucketStore;
//import net.leangen.expedition.platform.bucketstore.EmptyStore;
//import net.leangen.expedition.platform.datastore.DataStore;
//import net.leangen.expedition.platform.datastore.StateStore;
//import net.leangen.expedition.platform.repository2.AggregateDescriptor;
//
//@BucketStore.Provide(type=BucketStore.Type.EMPTY)
//@Component(
//        name = EmptyStoreService.COMPONENT_NAME,
//        service = {
//                DataStore.class,
//                StateStore.class, 
//                EmptyStore.class },
//        configurationPolicy = ConfigurationPolicy.REQUIRE,
//        configurationPid = EmptyStore.PID,
//        immediate = true
//)
//public class EmptyStoreService<E>
//    implements EmptyStore<E>
//{
//    public static final String COMPONENT_NAME = EmptyStore.PID;
//
//    private String name;
//
//    @Reference private LogService logger;
//
//    @Reference
//    void bindDescriptor( AggregateDescriptor<E> aDescriptor, Map<String, Object> properties )
//    {
//        name = new StringBuilder()
//                .append( aDescriptor.name().substring( 0, aDescriptor.name().lastIndexOf( "-" ) ) )
//                .append( "-EmptyStore" )
//                .toString();
//    }
//
//    void unbindDescriptor( AggregateDescriptor<E> aDescriptor, Map<String, Object> properties )
//    {
//        name = null;
//    }
//
//    @Override
//    public String name()
//    {
//        return name;
//    }
//
//    @Override
//    public List<String> buckets()
//    {
//        return Collections.emptyList();
//    }
//
//    @Override
//    public String url()
//    {
//        return null;
//    }
//
//    @Override
//    public String outerPath()
//    {
//        return null;
//    }
//
//    @Override
//    public Promise<Stream<E>> pull()
//    {
//        final Deferred<Stream<E>> deferred = new Deferred<>();
//        deferred.resolve( Stream.empty() );
//        return deferred.getPromise();
//    }
//
//    @Override
//    public DataStore.Info info()
//    {
//        final DataStore.Info state = new DataStore.Info();
//        state.type = "EmptyStore";
//        state.name = name();
//        return state;
//    }
//
//}
