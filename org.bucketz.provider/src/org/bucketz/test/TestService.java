package org.bucketz.test;

import org.apache.felix.serializer.Serializer;
import org.bucketz.BucketIO;
import org.bucketz.BucketStore;
import org.bucketz.BucketStoreFactory;
import org.bucketz.BucketStoreProvider;
import org.bucketz.Bucketz;
import org.bucketz.lib.BucketIOFactory;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.log.LogService;

@Component(immediate=true)
public class TestService
{
    @Reference private BucketStoreProvider provider;
    @Reference private BucketStoreFactory.ConfigurationBuilder builder;
    @Reference private LogService logger;
    @Reference private Serializer serializer;

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
//                new StringBuilder()
//                .append( descriptor.name().substring( 0, descriptor.name().lastIndexOf( "-" ) ) )
//                .append( "-BundleStore" )
//                .toString();

        final BucketStore.Configuration config = builder
                .usingType( Bucketz.Type.BUNDLE )
                .fromLocation( "/data" )
                .setNameTo( name )
                .setOuterPathTo( "tesu.to/test/1.0.0" )
                .useDataFromBundle( context.getBundleContext().getBundle().getBundleId() )
                .get();
        try
        {
            provider.newStore( config, descriptor, io );
        }
        catch ( Exception e )
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
