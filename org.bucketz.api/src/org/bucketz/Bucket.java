package org.bucketz;

import java.net.URI;
import java.util.Optional;

/**
 * The Bucket is the main abstraction. A DTO is serialized into a Bucket, and deserialized
 * from a Bucket.
 */
public interface Bucket
{
    /**
     * The location of the BucketStore that contains this Bucket. It is included as part of the 
     * Bucket for convenience.
     */
    URI location();

    /**
     * Same as the common outer path for all the Buckets in a given BucketStore, provided here 
     * for convenience. Must end with a "/".
     */
    BucketPath outerPath();

    /**
     * The path part of the bucketName. Must not start with a "/", but must end with a "/".
     */
    BucketPath innerPath();

    /**
     * The outer path appended with the inner path, provided as a String.
     */
    String fullPath();

    /**
     * The "simple" part of the name. See {@link Bucketz} for details.
     */
    String simpleName();

    /**
     * The Format of this Bucket.
     */
    Format format();

    /**
     * The Packaging for this Bucket.
     */
    Packaging packaging();

    /**
     * The "full name" of the Bucket, i.e. the inner-path/simpleName.format.
     */
    String fullName();

    /**
     * The fully-qualified name of the Bucket:
     *   outer-path/inner-path/simpleName.format:packaging
     */
    String fqn();

    /**
     * The URI of the Bucket. Constructed by removing the ":packaging" part of the FQN
     * and converting to a URI.
     * 
     * @throws UncheckedBucketException if the Bucket is not properly configured and the URI
     *            cannot be constructed.
     */
    URI asUri();

    /**
     * Same as fqn();
     */
    String toString();

    /**
     * The content of the Bucket as a serialized String.
     * This is Optional because the contents may not yet have been read from its Store.
     */
    Optional<String> content();

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
}
