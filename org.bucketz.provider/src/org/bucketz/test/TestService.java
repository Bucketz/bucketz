package org.bucketz.test;

import org.apache.felix.serializer.Serializer;
import org.bucketz.BucketIO;
import org.bucketz.Bucketz;
import org.bucketz.lib.BucketIOFactory;
import org.bucketz.store.BucketStore;
import org.bucketz.store.BucketStoreFactory;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.log.LogService;

@Component(immediate=true)
public class TestService
{
    @Reference private BucketStoreFactory factory;
    @Reference(target="(" + Bucketz.Parameters.BUCKET_TYPE + "=FILE)")
    private BucketStoreFactory.Available availability;
    @Reference private BucketStoreFactory.ConfigurationBuilder builder;
    @Reference private LogService logger;
    @Reference private Serializer serializer;

    private BucketStore<TestDTO> store;

    @Activate
    void activate( ComponentContext context )
    {
        final TestDescriptor descriptor = new TestDescriptor();

        final BucketIO<TestDTO> io = BucketIOFactory.newFactory( TestDTO.class )
                .setLogService( logger )
                .setSerializer( serializer )
                .configureWith( descriptor )
                .useTabDelimited(
                        BucketIOFactory.newTsvConfigFactory()
                        .setColumns( "id", "creds" )
                        .get() )
                .get();

        final String name = descriptor.name();

        final BucketStore.Configuration config = builder
                .usingType( Bucketz.Type.BUNDLE )
                .fromLocation( "/data" )
                .setNameTo( name )
                .setOuterPathTo( "tesu.to/test/1.0.0" )
                .useDataFromBundle( context.getBundleContext().getBundle().getBundleId() )
                .get();
        try
        {
            store = factory.newStore( config, descriptor, io );
        }
        catch ( Exception e )
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    void deactivate()
    {
        factory.release( store );
    }
}
