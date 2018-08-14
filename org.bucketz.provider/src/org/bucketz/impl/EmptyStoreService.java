package org.bucketz.impl;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.bucketz.Bucketz;
import org.bucketz.UncheckedBucketException;
import org.bucketz.store.BucketStore;
import org.bucketz.store.EmptyStore;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.log.LogService;
import org.osgi.util.promise.Promise;
import org.osgi.util.promise.Promises;

@Bucketz.Provide(type=Bucketz.TypeConstants.EMPTY)
@Component(
      name = EmptyStoreService.COMPONENT_NAME,
      service = {
              BucketStore.class,
              EmptyStore.class },
      factory = EmptyStore.PID
)
public class EmptyStoreService<D>
    implements EmptyStore<D>
{
    public static final String COMPONENT_NAME = EmptyStore.PID;

    private String name;

    private String location;
    private String outerPath;

    @Reference private LogService logger;

    @Activate
    void activate( EmptyStore.Configuration configuration, Map<String, Object> properties )
    {
        name = configuration.name();
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
        return Collections.emptyList();
    }

    @Override
    public URI uri()
            throws UncheckedBucketException
    {
        try
        {
            return new URI( location );
        }
        catch ( URISyntaxException e )
        {
            throw new UncheckedBucketException( e );
        }
    }

    @Override
    public String outerPath()
    {
        return "/";
    }

    @Override
    public Bucketz.Type type()
    {
        return Bucketz.Type.EMPTY;
    }

    @Override
    public Promise<Stream<D>> stream()
    {
        return Promises.resolved( Stream.empty() );
    }
}
