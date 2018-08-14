package org.bucketz;

import java.util.stream.Stream;

/**
 * Given a Bucket, return the Stream of DTOs it contains.
 */
@FunctionalInterface
public interface Debucketizer<D>
{
    Stream<D> debucketize( Bucket bucket )
            throws UncheckedBucketException;
}
