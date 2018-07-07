package org.bucketz.impl;

import org.bucketz.Bucketz.Type;
import org.bucketz.store.BucketStore;
import org.bucketz.store.BucketStoreFactory;
import org.bucketz.UncheckedBucketException;
import org.osgi.dto.DTO;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ServiceScope;
import org.osgi.util.converter.Converters;

@Component(scope = ServiceScope.PROTOTYPE)
public class ConfigurationBuilderService
    implements BucketStoreFactory.ConfigurationBuilder
{
    private final ConfigurationDTO dto = new ConfigurationDTO();
    private boolean isUsed;

    @Override
    public BucketStoreFactory.ConfigurationBuilder usingType( Type aType )
    {
        assertNotUsed();
        dto.type = aType.name();
        return this;
    }

    @Override
    public BucketStoreFactory.ConfigurationBuilder fromLocation( String aLocation )
    {
        assertNotUsed();
        dto.location = aLocation;
        return this;
    }

    @Override
    public BucketStoreFactory.ConfigurationBuilder setNameTo( String aName )
    {
        assertNotUsed();
        dto.name = aName;
        return this;
    }

    @Override
    public BucketStoreFactory.ConfigurationBuilder setOuterPathTo( String anOuterPath )
    {
        assertNotUsed();
        dto.outerPath = anOuterPath;
        return this;
    }

    @Override
    public BucketStoreFactory.ConfigurationBuilder useDataFromBundle( long aBundleId )
    {
        assertNotUsed();
        dto.bundleId = aBundleId;
        return this;
    }

    @Override
    public BucketStore.Configuration get()
    {
        return Converters.standardConverter()
                .convert( dto )
                .to( BucketStore.Configuration.class );
    }

    private void assertNotUsed()
    {
        if (isUsed)
            throw new UncheckedBucketException( "This builder has already been used." );
    }

    public static class ConfigurationDTO
        extends DTO
    {
        public String type;
        public String location;
        public String name;
        public long bundleId = -1;
        public String outerPath;
    }
}
