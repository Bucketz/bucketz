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

        Bucket bucketizeObject( D anEntity, String aUrl )
                throws UncheckedBucketException;

        default Bucketizer<D> bucketizer()
                throws UncheckedBucketException
        {
            return (s,url) -> {
                final D singleEntity = s.findAny().orElse( null );
                if (singleEntity == null)
                    throw new IllegalArgumentException( "No entity provided" );
                final Bucket bucket = this.bucketizeObject( singleEntity, url );
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
