package org.bucketz;

import java.util.Collections;
import java.util.Comparator;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

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

    /**
     * For coding the serialized data into an object, and serializing the data.
     */
    Codec<D> codec();

    default BucketStore.Format format()
    {
        return BucketStore.Format.JSON;
    }

    default BucketStore.Packaging packaging()
    {
        return BucketStore.Packaging.MULTI;
    }

    default Optional<String> filter()
    {
        return Optional.empty();
    }

    default Bucketizer<D> bucketizer()
        throws Exception
    {
        return (s,url) -> Collections.emptyList();
    }

    default Debucketizer<D> debucketizer()
        throws Exception
    {
        return s -> Stream.empty();
    }
}
