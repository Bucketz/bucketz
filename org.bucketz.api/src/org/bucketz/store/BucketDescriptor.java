package org.bucketz.store;

import java.util.Comparator;
import java.util.Optional;
import java.util.function.Function;

import org.bucketz.Bucket;
import org.osgi.annotation.versioning.ProviderType;

/**
 * Notes about naming.
 * 
 * Due to the way Repositories and Stores are designed, many components get instantiated,
 * which need to be addressable by name. Without some convention, confusion will quickly set in.
 * Therefore, we use the following conventions:
 * 
 *  - An AggregateRoot will use a component.name based on the PID of the AggregateRoot
 *    and the Version of the schema: component.name = PID:VERSION.
 *    
 *  - Each supporting object for an AggregateRoot will have a component.name based on the PID and VERSION of the
 *    component.name of the AggregateRoot, appended with an appropriate type indicator:
 *      - PID-VERSION-Descriptor
 *      - PID-VERSION-Repository
 *      - PID-VERSION-Store
 *      - PID-VERSION-Index
 *    
 *  - Each instance of supporting object will have a name based on its Confinement component.name: 
 *      - Confinement-PID-VERSION-Descriptor
 *      - Confinement-PID-VERSION-Repository
 *      - Confinement-PID-VERSION-Store
 *      - Confinement-PID-VERSION-Index
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

    // TODO: It should be possible to simply annotate simple data types
//    static @interface Describe
//    {
//        String name();
//        String description();
//        String version();
//        String id();
//        String brn();
//        String containerName();
//        String format();
//        String packaging();
//        String filter();
//    }

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
