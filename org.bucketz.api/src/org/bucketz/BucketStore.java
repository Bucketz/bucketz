package org.bucketz;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.stream.Stream;

import org.bucketz.store.Writable;
import org.osgi.dto.DTO;
import org.osgi.util.promise.Promise;

/**
 * BucketStores tend to work well for smaller data sets that are kept in local live memory.
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
 * A BucketStore can be read-only (the default), or Incremental. A read-only BucketStore can
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
 * check for consistency, as it is expected that there are not many updates to content with.
 */
public interface BucketStore<D>
{
    /**
     * A human-consumable name of the store.
     */
    String name();

    /**
     * The URI for the location of the BucketStore. Must end with "/" and does not
     * include the outer path.
     */
    public URI uri()
        throws URISyntaxException;

    /**
     * The outer path is common to all Buckets in this store. Must end with a "/", and does not include
     * the path part of the base URL, nor the inner path.
     */
    public String outerPath();

    /**
     * List of all Buckets within the BucketStore. The values returned are the fully-qualified names 
     * of the Buckets.
     */
    List<String> buckets();

    /**
     * Get a Stream of DTOs from this store.
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
        public String descriminant;
    }

    /**
     * Describes the format of the Bucket contents. Usually the contents are in JSON 
     * format, but not always. The format depends on the data schema. Very simple 
     * schemas are sometimes easier to store in a different format, particularly TSV.
     */
    static enum Format
    { 
        /**
         * If not specified, this is the default.
         */
        JSON, 

        /**
         * Tab-separated values.
         */
        TSV;
    }

    /**
     * A Bucket can contain a single object, or a collection of objects. To describe the contents,
     * we use the concept of a Bucket packaging.
     */
    static enum Packaging
    { 
        /**
         * A "Multi-Object Bucket" is a single Bucket that packages an entire collection of DTOs.
         * 
         * If not specified, this is the default packaging.
         */
        MULTI, 

        /**
         * A "Partitioned Bucket" is one DTO in a single collection, whereby each DTO is 
         * packaged as a single Bucket. In other words, instead of the set of DTOs being a 
         * single Bucket with a collection of DTOs, there is a collection of Buckets each 
         * with a single DTO.
         */
        PARTITIONED, 

        /**
         * A "SingleObject Bucket" is more rare, but still important. There are cases where we 
         * need a single object, which needs to be persisted. The purpose of the SingleObjectBucket 
         * is to just package and store this object “as is”.
         */
        SINGLE;
    }

    static @interface Configuration
    {
        String type() default Bucketz.Type.FILE;
        String location() default "~/data";
    }
}
