package org.bucketz.impl;

import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.bucketz.Bucket;
import org.bucketz.BucketIO;
import org.bucketz.Bucketz;
import org.bucketz.Bucketz.Type;
import org.bucketz.lib.BucketContextualizer;
import org.bucketz.lib.BucketNameParser;
import org.bucketz.lib.BucketPathConverter;
import org.bucketz.store.BucketDescriptor;
import org.bucketz.store.BucketStore;
import org.bucketz.store.BundleStore;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.log.LogService;
import org.osgi.util.promise.Deferred;
import org.osgi.util.promise.Promise;
import org.osgi.util.promise.Promises;

/**
 * Implementation note: We expect that operations with the bundle will be very quick,
 * so IO operations are not executed concurrently.
 * 
 * This service is EffectivelyMutable.
 */
@Bucketz.Provide(type=Bucketz.TypeConstants.BUNDLE)
@Component(
        name = BundleStoreService.COMPONENT_NAME,
        service = {
                BucketStore.class,
                BundleStore.class },
        factory = BundleStore.PID
)
public class BundleStoreService<D>
    implements BundleStore<D>
{
    public static final String COMPONENT_NAME = BundleStore.PID;

    private String name;

    private long bundleId;
    private String location;
    private String outerPath;

    private BucketDescriptor<D> descriptor;
    private BucketIO<D> io;

    private final List<String> buckets = new ArrayList<>();
    private URI uri;
    
    @Reference private LogService logger;

    @SuppressWarnings( "unchecked" )
    @Activate
    void activate( ComponentContext context, BundleStore.Configuration configuration, Map<String, Object> properties )
        throws Exception
    {
        name = configuration.name();
        bundleId = configuration.bundleId();
        location = configuration.location();
        outerPath = configuration.outerPath();

        if (outerPath.startsWith( "/" ))
            outerPath = outerPath.substring( 1 );

        if (!outerPath.isEmpty() && !outerPath.endsWith( "/" ))
            outerPath += "/";

        descriptor = (BucketDescriptor<D>)properties.get( Bucketz.Parameters.DESCRIPTOR );
        io = (BucketIO<D>)properties.get( Bucketz.Parameters.IO );

        initialize( context );
    }

    private void initialize( ComponentContext context )
        throws Exception
    {
        initializeUri( context );
        initializeBuckets();
    }

    @Override
    public String name()
    {
        return name;
    }

    @Override
    public List<String> buckets()
    {
        return buckets.stream().collect( Collectors.toList() );
    }

    /*
     * Since reading the list of Buckets is a read-only operation for a BundleStore, and
     * the list will never change, we can compute once and create a read-only copy.
     */
    private void initializeBuckets()
    {
        final List<String> bucketList;

        final Bundle bundle = FrameworkUtil.getBundle( getClass() ).getBundleContext().getBundle( bundleId );
        final URI uri = uri();

        final String uriString = uri.toString();
        final String searchPath = new StringBuilder()
                .append( location )
                .append( "/" )
                .append( outerPath() )
                .toString();
        final Enumeration<URL> urls = bundle.findEntries( searchPath, "*", true );
        if (urls == null)
            bucketList = Collections.emptyList();
        else
            bucketList = Collections.list( urls ).stream()
                .map( u -> u.toString() )
                .map( u -> u.replace( uriString + outerPath, "" ) )
                .filter( b -> !b.endsWith( "/" ) )
                .filter( b -> !b.isEmpty() )
                .filter( b -> !( !b.contains( "/" ) && ( descriptor.packaging() == Bucket.Packaging.PARTITIONED ) ) )
                .filter( b -> b.startsWith( descriptor.brn() ) )
                .filter( b -> (descriptor.filter().isPresent()) ? b.matches( descriptor.filter().get() ) : true )
                .collect( Collectors.toList() );

        buckets.addAll( bucketList );
    }

    @Override
    public URI uri()
    {
        return uri;
    }

    /*
     * The URI does not change, so we can compute the value once upon startup.
     */
    private void initializeUri( ComponentContext context )
        throws Exception
    {
        final Bundle bundle;
        if (bundleId == -1)
            // Use this bundle
            bundle = context.getBundleContext().getBundle();
        else
            bundle = context.getBundleContext().getBundle( bundleId );

        final URL url = bundle.getEntry( location );
        if (url == null)
            throw new IllegalStateException( String.format( "Could not find entry at %s", location ) );

        uri = url.toURI();
    }

    @Override
    public String outerPath()
    {
        return outerPath;
    }

    @Override
    public Type type()
    {
        return Bucketz.Type.BUNDLE;
    }

    @Override
    public Promise<Stream<D>> stream()
    {
        final Deferred<Stream<D>> deferred = new Deferred<>();

        try
        {
            final BucketNameParser parser = BucketNameParser.newParser();
            final BucketPathConverter converter = BucketPathConverter.newConverter();
            final BucketContextualizer contextualizer = BucketContextualizer.newContextualizer();

            final Stream<D> stream = buckets().stream()
                    .map( bn -> parser.parse( bn, descriptor.packaging() ) )
                    .map( bn -> converter.convert( descriptor, outerPath, bn ) )
                    .map( c -> contextualizer.contextualize( uri(), c ) )
                    .flatMap( b -> io.debucketize( b ) );

            deferred.resolve( stream );
        }
        catch ( Exception e )
        {
            logger.log( 
                    FrameworkUtil.getBundle( getClass() ).getBundleContext().getServiceReference( getClass() ), 
                    LogService.LOG_ERROR, 
                    "An error occurred when reading from the FileStore", 
                    e );

            deferred.fail( e );
        }

        return deferred.getPromise();
    }

    @Override
    public Promise<Integer> size()
    {
        return stream()
                .then( p -> {
                    return Promises.resolved((int)p.getValue().count());
                });
    }
}
