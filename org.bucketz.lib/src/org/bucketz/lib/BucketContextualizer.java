package org.bucketz.lib;

import java.net.URI;

import org.bucketz.Bucket;
import org.bucketz.BucketStore;

@FunctionalInterface
public interface BucketContextualizer
{
    Bucket contextualize( URI aLocation, BucketStore.BucketContextDTO aBucketContext );

    static Contextualizer newContextualizer()
    {
        return new Contextualizer();// null );
    }

    static class Contextualizer
        implements BucketContextualizer
    {
        @Override
        public Bucket contextualize( URI aLocation, BucketStore.BucketContextDTO aBucketContext )
        {
            final BucketStore.BucketDTO dto = new BucketStore.BucketDTO();
            dto.location = aLocation.toString();
            dto.context = aBucketContext;
            final Bucket b = BucketFactory.newBucket( dto );
            return b;
        }        
    }
}
