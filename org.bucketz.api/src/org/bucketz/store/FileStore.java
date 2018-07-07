package org.bucketz.store;

import org.bucketz.BucketStore;

public interface FileStore<D>
    extends BucketStore<D>, Writable<D>
{
    static final String PID = "org.bucketz.file";

    static @interface Configuration
    {
        String location();
    }
}
