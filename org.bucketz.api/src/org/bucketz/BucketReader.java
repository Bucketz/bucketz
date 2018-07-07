package org.bucketz;

import java.util.stream.Stream;

public interface BucketReader<D>
{
    public Stream<D> read( Bucket bucket )
        throws UncheckedBucketException;
}
