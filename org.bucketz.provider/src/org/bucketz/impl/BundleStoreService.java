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

import org.bucketz.Bucket;
import org.bucketz.BucketIO;
import org.bucketz.Bucketz;
import org.bucketz.Bucketz.Type;
import org.bucketz.UncheckedBucketException;
import org.bucketz.lib.BucketContextualizer;
import org.bucketz.lib.BucketName;
import org.bucketz.lib.BucketNameParser;
import org.bucketz.lib.BucketPathConverter;
import org.bucketz.store.BucketDescriptor;
import org.bucketz.store.BucketStore;
import org.bucketz.store.BundleStore;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.log.LogService;
import org.osgi.util.promise.Deferred;
import org.osgi.util.promise.Promise;

import aQute.bnd.annotation.headers.ProvideCapability;

//Need to provide the capability for the resolver
@ProvideCapability(
        ns = "osgi.service",
        value = "objectClass:List<String>=\"org.osgi.service.component.ComponentFactory\"" )
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

    @Reference private LogService logger;

    @SuppressWarnings( "unchecked" )
    @Activate
    void activate( BundleStore.Configuration configuration, Map<String, Object> properties )
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
    }

    void deactivate()
    {
        name = null;
        bundleId = -1;
        location = null;
        outerPath = null;
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
        final URI uri = uri();

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
        throws UncheckedBucketException
    {
        final Bundle bundle = FrameworkUtil.getBundle( getClass() ).getBundleContext().getBundle( bundleId );
        final URL url = bundle.getEntry( location );
        if (url == null)
            throw new UncheckedBucketException( String.format( "Could not find entry at %s", location ) );

        try
        {
            return url.toURI();
        }
        catch ( URISyntaxException e )
        {
            throw new UncheckedBucketException( e );
        }
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
        new Thread(() -> {
            try
            {
                final BucketNameParser parser = BucketNameParser.newParser();
                final BucketPathConverter converter = BucketPathConverter.newConverter();
                final BucketContextualizer contextualizer = BucketContextualizer.newContextualizer();

                for( String bucketName : buckets() )
                {
                    final BucketName bn = parser.parse( bucketName, descriptor.packaging() );
                    final BucketStore.BucketContextDTO ctx = converter.convert( descriptor, outerPath, bn );
                    final Bucket bucket = contextualizer.contextualize( uri(), ctx );
                    final List<D> e = io.debucketize( bucket )
                            .collect( Collectors.toList() );
                    e.toString();
                }

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
        }).start();

        return deferred.getPromise();
    }
}
