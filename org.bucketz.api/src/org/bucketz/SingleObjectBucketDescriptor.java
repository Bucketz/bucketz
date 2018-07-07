package org.bucketz;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.osgi.annotation.versioning.ProviderType;

/**
 * A specialized descriptor for a service backed by a SingleObjectRepository.
 */
@ProviderType
public interface SingleObjectBucketDescriptor<D>
    extends BucketDescriptor<D>
{
    D read( Bucket bucket )
            throws UncheckedBucketException;

    Bucket write( D anEntity, String aUrl )
            throws UncheckedBucketException;

    default Optional<String> containerName()
    {
        return Optional.empty();
    }

    default BucketStore.Format format()
    {
        return BucketStore.Format.JSON;
    }

    default BucketStore.Packaging packaging()
    {
        return BucketStore.Packaging.SINGLE;
    }

    default Bucketizer<D> bucketizer()
        throws UncheckedBucketException
    {
        return (s,url) -> {
            final D singleEntity = s.findAny().orElse( null );
            if (singleEntity == null)
                throw new IllegalArgumentException( "No entity provided" );
            final Bucket bucket = write( singleEntity, url );
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
            final D object = read( b );
            final Stream<D> stream = Stream.of( object );
            return stream;
        };
    }
}
