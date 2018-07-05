package org.bucketz.lib;

import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;

import org.bucketz.Bucket;

public interface BucketWriter<E>
{
    public List<Bucket> write( Stream<E> stream, String aUrl )
        throws IOException;
}
