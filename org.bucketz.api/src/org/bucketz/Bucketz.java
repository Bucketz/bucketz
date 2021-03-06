package org.bucketz;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.osgi.namespace.implementation.ImplementationNamespace;

import aQute.bnd.annotation.headers.ProvideCapability;
import aQute.bnd.annotation.headers.RequireCapability;

/**
 * A BucketStore is a data store in which DTOs are stored in groupings called "Buckets".
 * 
 * The BucketStore has a location, represented by a URI. It could be backed by a file system, a
 * bundle, a specialed persistence application, or someplace remote in the Cloud.
 * 
 * A BucketStore is divided into Buckets. Each Bucket holds one or more persisted DTOs.
 * The Bucket can have a hierarchical location within the BucketStore, represented by the
 * Bucket's path. The path has two potentially empty parts: the outer part and the inner part.
 * The outer part is common to all Buckets in the BucketStore, while the inner part is specific
 * to a Bucket, though not necessarily unique (Buckets can potentially share a path).
 * The full path is therefore OUTER_PATH/INNER_PATH/, always ending with a "/".
 * 
 * The Bucket name is PATH/SIMPLE_NAME.
 * 
 * A Bucket has contents that are stored in a given format, the natural format being JSON.
 * The chosen format depends on the DTO. Very simple DTOs are sometimes easier to store 
 * in a different format, particularly tab-separated values (TSV).
 * 
 * A Bucket can contain a single object, or a collection of objects. To describe the contents, 
 * we use the concept of a Bucket packaging:
 * 
 *   - Multi-Object Bucket: a single Bucket packages an entire collection of DTOs. A Multi-Object 
 *     Bucket (or Multi-Bucket) has a "container" that holds the data. The name of the container 
 *     is the "container name". The default container name is "data".
 *     
 *   - Partitioned Buckets: as with the Multi-Object Bucket, represents an entire collection of DTOs. 
 *     However, instead of packaging the DTOs into the same Bucket as a single collection, each DTO is 
 *     packaged as a single Bucket. In other words, instead of the set of DTOs being a single Bucket 
 *     with a collection of DTOs, there is a collection of Buckets each with a single DTO.
 *     
 *   - SingleObjectBucket: more rare, but still important. There are cases where we need a 
 *     single object, which needs to be persisted. The purpose of the SingleObjectBucket is to 
 *     just package and store this object “as is”.
 * 
 * Each Bucket has a Codec, which either encodes the DTO into a data representation of the given
 * format, or decodes the data representation into a DTO. When the format is JSON, these transformations
 * are trivial.
 * 
 * Bucketizing and Debucketizing is a means of converting a DTO into a Bucket, and vice-versa. 
 * The essential problem is:
 * 
 *   - Bucketization: Given a DTO, how do I transform it into a Bucket?
 *   
 *   - Debucketization: Given a Bucket, how do I transform it into a DTO?
 */
public interface Bucketz
{
    static final String NAMESPACE = ImplementationNamespace.IMPLEMENTATION_NAMESPACE;
    static final String CAP = "bucketz";
    static final String CONFIG = "bucketz.config";
    static final String VERSION = "1.0.0";

    /**
     * The type of BucketStore.
     */
    static enum Type { EMPTY, BUNDLE, FILE, CLOUD }

    /**
     * The provider of the BucketStore. The default is the internal "BUCKETZ", but there
     * is a known implementation of a Firebase Bucket as well.
     */
    static enum Provider { BUCKETZ, FIREBASE }

    static final class TypeConstants
    {
        private TypeConstants() {}

        /**
         * Represents an Empty BucketStore. Used when no backing persistence is required.
         */
        public static final String EMPTY = "empty";

        /**
         * Read-only BundleStore.
         */
        public static final String BUNDLE = "bundle";

        /**
         * FileStore for persisting to the local file system.
         */
        public static final String FILE = "file";

        /**
         * CloudStore for persistence to a cloud-based system (such as Firebase).
         */
        public static final String CLOUD = "cloud";

        public static final class Provider
        {
            public static final String BUCKETZ = "bucketz";
            public static final String FIREBASE = "Firebase";

            private Provider() {}
        }
    }

    static final class Parameters
    {
        private Parameters() {}

        public static final String DESCRIPTOR = "descriptor";
        public static final String IO = "io";
        public static final String BUCKET_TYPE = "org.bucketz.type";
    }

    //
    // Annotations for CAP/REQs
    //

    @RequireCapability(
            ns = NAMESPACE,
            filter = "(&"
                        + "(" + NAMESPACE + "=" + CAP + ")"
                        + "(type=${type})"
                        + "(provider=${provider})"
                        + "${frange;" + VERSION + "}"
                     + ")"
    )
    @Retention(RetentionPolicy.CLASS)
    public static @interface Require
    {
        String type();
        String provider() default TypeConstants.Provider.BUCKETZ;
    }

    @ProvideCapability(
            ns = NAMESPACE, 
            name = CAP,
            version = VERSION )
    public static @interface Provide
    {
        String type();
        String provider() default TypeConstants.Provider.BUCKETZ;
    }

    static class Commands
    {
        private Commands() {}

        static final String CAP = "bucketz.commands";

        //
        // Annotations for CAP/REQs
        //

        @RequireCapability(
                ns = NAMESPACE,
                filter = "(&"
                            + "(" + NAMESPACE + "=" + CAP + ")"
                            + "${frange;" + VERSION + "}"
                         + ")"
        )
        @Retention(RetentionPolicy.CLASS)
        public static @interface Require {}

        @ProvideCapability(
                ns = NAMESPACE, 
                name = CAP,
                version = VERSION )
        public static @interface Provide {}        
    }
}
