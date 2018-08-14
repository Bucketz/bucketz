package org.bucketz.store;

import java.util.Comparator;
import java.util.Optional;
import java.util.function.Function;

import org.bucketz.Bucket;
import org.osgi.annotation.versioning.ProviderType;

/**
 * Implement this interface to describe how a DTO is used by a BucketStore.
 * To use a DTO with a BucketStore, an instance of this interface is required,
 * either by implementing it directly, or by using a Builder.
 */
@ProviderType
public interface BucketDescriptor<D>
{
    static final String TARGET_PID_PARAM = "target.pid";
    static final String TARGET_VERSION_PARAM = "target.version";

    static final String DEFAULT_CONTAINER_NAME = "data";

    /**
     * A short human-readable name for reporting purposes only.
     */
    String name();

    /**
     * A short human-readable description for reporting purposes only.
     */
    String description();

    /**
     * The type of data object the Bucket is storing.
     */
    Class<D> type();

    /**
     * The version of the data object, or "0.0.0" if none is provided.
     */
    String version();

    /**
     * Function to extract an ID from a Bucket. Since Buckets are stored as
     * key/value pairs, the ID (key) needs to be extractable from the data object.
     */
    Function<D, String> idExtractor();

    /**
     * An optional Comparator that will order the data within the Buckets.
     */
    default Optional<Comparator<D>> comparator()
    {
        return Optional.empty();
    }

    /**
     * The BucketRepresentativeName, or BRN.
     * 
     * Depending on the Packaging of the Bucket, the Bucket is represented by some name that
     * will allow its reconstruction:
     *   - Multi: [InnerPath/]SimpleName
     *   - Partitioned: The part of the InnerPath that is not included in the partition name
     *   - Single: [InnerPath/]SimpleName
     */
    String brn();

    /**
     * The container name is used when multiple objects are stored in the same Bucket.
     */
    Optional<String> containerName();

    default Bucket.Format format()
    {
        return Bucket.Format.JSON;
    }

    default Bucket.Packaging packaging()
    {
        return Bucket.Packaging.MULTI;
    }

    default Optional<String> filter()
    {
        return Optional.empty();
    }

    static interface Single<D>
        extends BucketDescriptor<D>
    {
        default Optional<String> containerName()
        {
            return Optional.empty();
        }

        default Bucket.Format format()
        {
            return Bucket.Format.JSON;
        }

        default Bucket.Packaging packaging()
        {
            return Bucket.Packaging.SINGLE;
        }        
    }

    /**
     * Programmatically builds a BucketDescriptor.
     */
    public interface Builder<D>
    {
        static interface Factory
        {
            <D>Builder<D> newBuilder( Class<D> forDTOType );
        }

        Builder<D> setName( String aName );
        Builder<D> describeAs( String aDescription );
        Builder<D> setVersion( String aVersion );
        Builder<D> extractIdUsing( Function<D, String> anIdExtractor );
        Builder<D> compareWith( Comparator<D> aComparator );
        Builder<D> representWith( String aBundleRepresentativeName );
        Builder<D> containWith( String aContainerName );
        Builder<D> formatAs( Bucket.Format aFormat );
        Builder<D> packageAs( Bucket.Packaging packaging );
        Builder<D> filterWith( String aFilter );

        BucketDescriptor<D> get();
    }
}
