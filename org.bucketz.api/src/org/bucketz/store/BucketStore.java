package org.bucketz.store;

import java.net.URI;
import java.util.List;
import java.util.stream.Stream;

import org.bucketz.Bucketz;
import org.bucketz.UncheckedBucketException;
import org.osgi.dto.DTO;
import org.osgi.util.promise.Promise;

/**
 * BucketStores tend to work well for smaller data sets that are kept in local live memory.
 * What "smaller" means is relative, but the main considerations are fairly low contention
 * and a small enough data set that the data can be kept in live memory.
 * In this approach, the Buckets contain the original data that is used to hydrate the live 
 * objects, then is no longer required. In modern computers, this tends to be easily within
 * the specifications of even a smaller server. Keeping objects in live memory is extremely
 * fast, so if you don't mind using live memory somewhat inefficiently, the benefits of the
 * simplicity and the speed can be well worth the cost.
 * 
 * A BucketStore, at least currently, is probably too simplistic to be used as the actual
 * reference for the objects, i.e. instantiating an object from the data following each read.
 * If there is some requirement to do so, likely BucketStore would not be appropriate.
 * 
 * A BucketStore can be read-only (the default), and optionally Writable. A read-only BucketStore can
 * still be updated, but the updates need to be done offline, usually manually, via some other
 * tool. This approach is can actually be much simpler than maintaining infrastructure and
 * worrying about consistency in cases where updates are rare. Since BucketStores are really
 * intended only to persist system state that is otherwise made available in live memory,
 * this actually turns out to be a very practical approach. However, in some cases--such as
 * updates that are frequent or complex enough to make the manual approach too difficult--when it is
 * desirable to have the system update itself, incremental updates can save the manual work.
 * Since the assumption is that the object is already updated in live memory, the incremental
 * update is simply an automation of the offline update. It should happen within a reasonable
 * amount of time, but does not need to occur in real time. Currently there is only a weak
 * check for consistency, as it is expected that there are not many updates to contend with.
 * 
 * In terms of concurrency and synchronization, the base assumption is that reading the 
 * stream may take some time, but should be a fairly rare operation. A BucketStore is used 
 * only as a backing mechanism for an in-memory repository. Thus, the repository will be 
 * hydrated only once upon startup. When a BucketStore is Writable,
 * writes may happen incrementally, and should be synchronized. Writes for an entire
 * stream may happen during an export operation, but should be a once-only occurrence.
 */
public interface BucketStore<D>
{
    public static final String VERSION = "1.0.0";

    /**
     * A unique name of the store. Should be human-consumable. 
     */
    String name();

    /**
     * The URI for the location of the BucketStore. Must end with "/" and does not
     * include the outer path.
     */
    public URI uri()
        throws UncheckedBucketException;

    /**
     * The outer path is common to all Buckets in this store. Must end with a "/", and does not include
     * the path part of the base URL, nor the inner path.
     */
    public String outerPath();

    /**
     * The type of BucketStore represented by this instance.
     */
    Bucketz.Type type();

    /**
     * List of all Buckets within the BucketStore. The values returned are the fully-qualified names 
     * of the Buckets. The expectation is that this should return immediately.
     */
    List<String> buckets();

    /**
     * Get a Stream of DTOs from this store. Depending on the size of the store and other factors
     * (such as if it is remote), may take some time, thus represented by a Promise.
     */
    Promise<Stream<D>> stream();

    default boolean isWritable()
    {
        return (this instanceof Writable);
    }

    @SuppressWarnings( "unchecked" )
    default Writable<D> asWritable()
    {
        if (isWritable())
            return (Writable<D>)this;

        throw new ClassCastException( "This Store is not Writable" );
    }

    static class BucketContextDTO
    {
        public String outerPath;
        public String innerPath;
        public String simpleName;
        public String format;
        public String packaging;
    }

    static class BucketDTO
        extends DTO
    {
        public String location;
        public BucketContextDTO context;
        public String content;
    }

    static @interface Configuration
    {
        String type() default Bucketz.TypeConstants.FILE;
        String location() default "~/data";
        String name();
        long bundleId();
        String outerPath();
    }
}
