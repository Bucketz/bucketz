package org.bucketz;

import java.net.URI;
import java.util.Optional;

import org.bucketz.BucketStore.Format;
import org.bucketz.BucketStore.Packaging;

public interface Bucket
{
    /**
     * The location of the BucketStore. It is included as part of the Bucket for convenience only.
     */
    URI location();

    /**
     * Same as the common outer path, provided for convenience.
     */
    BucketPath outerPath();

    /**
     * The path part of the bucketName. Should end with a "/".
     */
    BucketPath innerPath();

    /**
     * The outer path appended with the inner path, provided as a String.
     */
    String fullPath();

    String simpleName();

    Format format();

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
}
