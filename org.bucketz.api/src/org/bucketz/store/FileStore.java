package org.bucketz.store;

public interface FileStore<D>
    extends BucketStore<D>, Writable<D>
{
    static final String PID = "org.bucketz.file";
}
