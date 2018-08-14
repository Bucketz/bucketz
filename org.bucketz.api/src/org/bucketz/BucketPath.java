package org.bucketz;

import java.util.List;

/**
 * A Bucket can have an inner path and an outer path. Both of these can,
 * for simplicity, be represented as a BucketPath, which contains a List
 * of Strings.
 */
public interface BucketPath
{
    List<String> parts();

    /**
     * Returns the BucketPath as a String. Should return parts() joined
     * by a "/". 
     */
    String toString();
}
