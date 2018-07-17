package org.bucketz;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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

    @FunctionalInterface
    public interface BucketFunction<D>
    {
        String toBucket( D dto );
    }

    static enum Profile { MULTI_TSV, MULTI_JSON, PARTITIONED_JSON, SINGLE }

    static interface ConfigurationProfile
    {
        Profile profile();
    }

    static interface Configuration
    {
        static interface Tsv
            extends ConfigurationProfile
        {
            Optional<String[]> headers();
            String[] columns();
        }

        static interface MultiJson
            extends BucketIO.ConfigurationProfile
        {
            // Nothing for now!
        }

        static interface PartitionedJson<E>
            extends ConfigurationProfile
        {
            String bucketFilter();
            BucketFunction<E> bucketFunction();
        }

        static interface SingleObject
            extends ConfigurationProfile
        {
            String bucketName();
        }        
    }
}
