package org.bucketz;

import java.util.List;
import java.util.stream.Stream;

/**
 * Given a Stream of DTOs as input, provide a list of Buckets.
 */
@FunctionalInterface
public interface Bucketizer<D>
{
    List<Bucket> bucketize( Stream<D> aDTOStream, String aLocation, String anOuterPath )
            throws UncheckedBucketException;
}
