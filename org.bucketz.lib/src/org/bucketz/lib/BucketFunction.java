package org.bucketz.lib;

@FunctionalInterface
public interface BucketFunction<E>
{
    String toBucket( E entity );
}
