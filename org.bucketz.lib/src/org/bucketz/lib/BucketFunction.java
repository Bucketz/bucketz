package org.bucketz.lib;

@FunctionalInterface
public interface BucketFunction<D>
{
    String toBucket( D dto );
}
