package org.bucketz;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * A helper for Bucket IO operations. This class operates like a configuration for
 * a Bucket, which provides operations for serializing and deserializing a DTO
 * to and from a Bucket. It attempts to provide reasonable defaults when possible.
 */
public interface BucketIO<D>
    extends 
        Bucketizer<D>,
        Debucketizer<D>,
        Codec<D>
{
    /**
     * Special case of a Single-object Bucket.
     */
    static interface Single<D>
        extends BucketIO<D>
    {
        D debucketizeObject( Bucket bucket )
                throws UncheckedBucketException;

        Bucket bucketizeObject( D aDTO, String aUrl, String anOuterPath )
                throws UncheckedBucketException;

        default Bucketizer<D> bucketizer()
                throws UncheckedBucketException
        {
            return (s,url,op) -> {
                final D singleDTO = s.findAny().orElse( null );
                if (singleDTO == null)
                    throw new IllegalArgumentException( "No DTO provided" );
                final Bucket bucket = this.bucketizeObject( singleDTO, url, op );
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

    /**
     * Provides the configuration for a Bucket, depending on the type.
     */
    static interface Configuration
    {
        /**
         * A tab-separated value Bucket.
         */
        static interface Tsv
            extends ConfigurationProfile
        {
            /**
             * An optional line of headers with header names provided as a String array.
             * If provided, the length must match the length of columns.
             */
            Optional<String[]> headers();

            /**
             * T
             */
            String[] columns();
        }

        /**
         * A multi-valued Bucket in which the DTO is serialized as JSON.
         */
        static interface MultiJson
            extends BucketIO.ConfigurationProfile
        {
            // Nothing for now!
        }

        /**
         * A partitioned Bucket in which the DTO is serialized as JSON.
         */
        static interface PartitionedJson<D>
            extends ConfigurationProfile
        {
            /**
             * A regex, provided as a String, that will determine whether or not a
             * Bucket name belongs to this Partitioned Bucket or not.
             */
            String bucketFilter();

            /**
             * The BucketFunction for the DTO type.
             */
            BucketFunction<D> bucketFunction();
        }

        /**
         * Special case for a single object.
         */
        static interface SingleObject
            extends ConfigurationProfile
        {
            /**
             * The name of the single Bucket.
             */
            String bucketName();
        }        
    }
}
