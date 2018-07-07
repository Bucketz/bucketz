package org.bucketz;

public interface BucketIO<D>
    extends 
        BucketReader<D>, 
        BucketWriter<D>,
        Bucketizer<D>,
        Debucketizer<D>,
        Codec<D>
{
}
