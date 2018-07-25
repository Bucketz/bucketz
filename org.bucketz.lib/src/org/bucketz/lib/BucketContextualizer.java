package org.bucketz.lib;

import java.net.URI;

import org.bucketz.Bucket;
import org.bucketz.UncheckedBucketException;
import org.bucketz.store.BucketStore;

/**
 * Used in an implementation to manipulate Buckets.
 */
@FunctionalInterface
public interface BucketContextualizer
{
    /**
     * Given a location and a context, produce a Bucket.
     */
    Bucket contextualize( URI aLocation, BucketStore.BucketContextDTO aBucketContext )
        throws UncheckedBucketException;

    static Contextualizer newContextualizer()
    {
        return new Contextualizer();// null );
    }

    static class Contextualizer
        implements BucketContextualizer
    {
        @Override
        public Bucket contextualize( URI aLocation, BucketStore.BucketContextDTO aBucketContext )
            throws UncheckedBucketException
        {
            final BucketStore.BucketDTO dto = new BucketStore.BucketDTO();
            dto.location = aLocation.toString();
            dto.context = aBucketContext;
            final Bucket b = BucketFactory.newBucket( dto );
            return b;
        }        
    }
}
