package org.bucketz.store;

import org.bucketz.BucketStore;

public interface FileStore<E>
    extends BucketStore<E>, Writable<E>
{
    static final String PID = "org.bucketz.file";

    static @interface Configuration
    {
        String location();
    }
}
