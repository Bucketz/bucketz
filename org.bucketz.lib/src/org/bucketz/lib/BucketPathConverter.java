package org.bucketz.lib;

import org.bucketz.UncheckedBucketException;
import org.bucketz.UncheckedInterruptedException;
import org.bucketz.store.BucketDescriptor;
import org.bucketz.store.BucketStore;

@FunctionalInterface
public interface BucketPathConverter
{
    /**
     * Given a base BucketDescriptor, an Outer Path, and a BucketName, convert to a BucketContextDTO.
     */
    BucketStore.BucketContextDTO convert( BucketDescriptor<?> aDescriptor, String outerPath, BucketName aBucketName )
        throws UncheckedBucketException;

    static Converter newConverter()
    {
        return new Converter();
    }

    static class Converter
        implements BucketPathConverter
    {
        public BucketStore.BucketContextDTO convert( BucketDescriptor<?> aDescriptor, String anOuterPath, BucketName aBucketName )
            throws UncheckedBucketException
        {
            if (Thread.interrupted())
                throw new UncheckedInterruptedException();

            final BucketStore.BucketContextDTO dto = new BucketStore.BucketContextDTO();
            dto.innerPath = aBucketName.innerPath;
            dto.simpleName = aBucketName.simpleName;
            dto.format = aBucketName.format;
            dto.packaging = aBucketName.packaging;
            dto.outerPath = anOuterPath;
            return dto;
        }        
    }
}
