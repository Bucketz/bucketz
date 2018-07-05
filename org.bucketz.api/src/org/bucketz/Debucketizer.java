package org.bucketz;

import java.util.stream.Stream;

/**
 * Given an bucket, return the Stream of DTOs it contains.
 */
@FunctionalInterface
public interface Debucketizer<D>
{
    Stream<D> debucketize( Bucket bucket )
            throws Exception;
}
