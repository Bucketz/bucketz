package org.bucketz.impl;

import org.bucketz.BucketDescriptor;
import org.bucketz.UncheckedBucketException;
import org.osgi.service.component.annotations.Component;

@Component
public class BucketDescriptorBuilderFactory
    implements BucketDescriptor.Builder.Factory
{
    @Override
    public <D> BucketDescriptor.Builder<D> newBuilder( Class<D> forDTOType )
    {
        if (forDTOType == null)
            throw new UncheckedBucketException( "Type must be provided" );

        return new BucketDescriptorBuilderService<>( forDTOType );
    }
}
