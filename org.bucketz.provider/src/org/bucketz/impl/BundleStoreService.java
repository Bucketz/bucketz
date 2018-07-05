package org.bucketz.impl;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.bucketz.BucketDescriptor;
import org.bucketz.BucketStore;
import org.bucketz.Bucketz;
import org.bucketz.store.BundleStore;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.log.LogService;
import org.osgi.util.promise.Deferred;
import org.osgi.util.promise.Promise;

@Bucketz.Provide(type=Bucketz.Type.BUNDLE)
@Component(
        name = BundleStoreService.COMPONENT_NAME,
        service = {
                BucketStore.class,
                BundleStore.class },
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        configurationPid = BundleStore.PID,
        immediate = true
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

    @Reference private LogService logger;

    @Activate
    void activate( ComponentContext componentContext, BundleStore.Configuration configuration, Map<String, Object> properties )
    {
        name = configuration.name();
        bundleId = configuration.bundleId();
        location = configuration.location();
        outerPath = configuration.outerPath();

        if (outerPath.startsWith( "/" ))
            outerPath = outerPath.substring( 1 );

        if (!outerPath.isEmpty() && !outerPath.endsWith( "/" ))
            outerPath += "/";
    }

    void deactivate()
    {
        name = null;
        bundleId = -1;
        location = null;
        outerPath = null;
    }

    @Reference
    void bindDescriptor( BucketDescriptor<D> aDescriptor )
    {
        descriptor = aDescriptor;
        name = new StringBuilder()
                .append( descriptor.name().substring( 0, descriptor.name().lastIndexOf( "-" ) ) )
                .append( "-BundleStore" )
                .toString();

        System.err.println( String.format( "BundleStore --> bindDescriptor: name=%s", name ) );
    }

    void unbindDescriptor( BucketDescriptor<D> aDescriptor )
    {
        name = null;
        descriptor = null;
    }

    @Override
    public String name()
    {
        return name;
    }

    @Override
    public List<String> buckets()
    {
        final Bundle bundle = FrameworkUtil.getBundle( getClass() ).getBundleContext().getBundle( bundleId );
        URI uri;
        try
        {
            uri = uri();
        }
        catch ( URISyntaxException e )
        {
            // TODO log something
            return Collections.emptyList();
        }

        final String uriString = uri.toString();
        final String searchPath = new StringBuilder()
                .append( location )
                .append( "/" )
                .append( outerPath() )
                .toString();
        final Enumeration<URL> urls = bundle.findEntries( searchPath, "*", true );
        if (urls == null)
            return Collections.emptyList();

        return Collections.list( urls ).stream()
                .map( u -> u.toString() )
                .map( u -> u.replace( uriString + outerPath, "" ) )
                .filter( u -> !u.endsWith( "/" ) )
                .collect( Collectors.toList() );
    }

    @Override
    public URI uri()
        throws URISyntaxException
    {
        final Bundle bundle = FrameworkUtil.getBundle( getClass() ).getBundleContext().getBundle( bundleId );
        final URL url = bundle.getEntry( location );
        if (url == null)
            throw new URISyntaxException( "", String.format( "Could not find entry at %s", location ) );

        return url.toURI();
    }

    @Override
    public String outerPath()
    {
        return outerPath;
    }

    @Override
    public Promise<Stream<D>> stream()
    {
        final Deferred<Stream<D>> deferred = new Deferred<>();
        new Thread(() -> {
            try
            {
//                final BucketNameParser parser = BucketNameParser.newParser();
//                final BucketPathConverter converter = BucketPathConverter.newConverter();
//                final BucketContextualizer contextualizer = BucketContextualizer.newContextualizer();
//
//                final Stream<E> stream = buckets().stream()
//                        .map( b -> parser.parse( b, descriptor.packaging() ) )
//                        .filter( t -> t.isSuccess() )
//                        .map( t -> t.get() )
//                        .map( p -> converter.convert( descriptor, p ) )
//                        .map( c -> contextualizer.contextualize( url(), c ) )
//                        .map( b -> descriptor.debucketizer().debucketize( b ) )
//                        .flatMap( t -> t.get() );

//                deferred.resolve( stream );
            }
            catch ( Throwable t )
            {
                logger.log( 
                        FrameworkUtil.getBundle( getClass() ).getBundleContext().getServiceReference( getClass() ), 
                        LogService.LOG_ERROR, 
                        "An error occurred when reading from the FileStore", 
                        t );

                deferred.fail( t );
            }
        }).start();

        return deferred.getPromise();
    }
}
