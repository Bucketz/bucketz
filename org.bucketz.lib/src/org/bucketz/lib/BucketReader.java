package org.bucketz.lib;

import java.io.IOException;
import java.util.stream.Stream;

import org.bucketz.Bucket;

public interface BucketReader<E>
{
    public Stream<E> read( Bucket bucket )
        throws IOException;
}
