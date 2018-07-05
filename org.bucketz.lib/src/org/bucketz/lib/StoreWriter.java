package org.bucketz.lib;

import java.net.URI;

import org.osgi.util.promise.Promise;

/**
 * Writes all the data managed by a given DataStore to disk, which is a necessary step when
 * exporting data.
 */
public interface StoreWriter<E>
{
    Promise<URI> write();
}
