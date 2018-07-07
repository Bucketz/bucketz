package org.bucketz;

import java.util.List;
import java.util.stream.Stream;

public interface BucketWriter<D>
{
    public List<Bucket> write( Stream<D> stream, String aUrl )
        throws UncheckedBucketException;
}
