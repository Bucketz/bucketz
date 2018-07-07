package org.bucketz;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public interface BucketIO<D>
    extends 
        Bucketizer<D>,
        Debucketizer<D>,
        Codec<D>
{
    static interface Single<D>
        extends BucketIO<D>
    {
        D debucketizeObject( Bucket bucket )
                throws UncheckedBucketException;

        Bucket bucketizeObject( D aDTO, String aUrl )
                throws UncheckedBucketException;

        default Bucketizer<D> bucketizer()
                throws UncheckedBucketException
        {
            return (s,url) -> {
                final D singleDTO = s.findAny().orElse( null );
                if (singleDTO == null)
                    throw new IllegalArgumentException( "No DTO provided" );
                final Bucket bucket = this.bucketizeObject( singleDTO, url );
                final List<Bucket> list = new ArrayList<>();
                list.add( bucket );
                return list;
            };
        }

        default Debucketizer<D> debucketizer()
                throws UncheckedBucketException
        {
            return b -> {
                if (b == null)
                    throw new IllegalArgumentException( "No Bucket provided" );
                final D object = debucketizeObject( b );
                final Stream<D> stream = Stream.of( object );
                return stream;
            };
        }
    }
}
