package org.bucketz.lib;

import org.bucketz.store.BucketDescriptor;
import org.bucketz.store.BucketStore;

@FunctionalInterface
public interface BucketPathConverter
{
    /**
     * Given a base URL and a path, convert to a BucketContextDTO.
     * 
     * @param BaseUrl The common base URL for all Buckets
     * @param Path The BucketPath relative to the BaseURL
     */
    BucketStore.BucketContextDTO convert( BucketDescriptor<?> aDescriptor, String outerPath, BucketName aPath );

    static Converter newConverter()
    {
        return new Converter();
    }

    static class Converter
        implements BucketPathConverter
    {
        @Override
        public BucketStore.BucketContextDTO convert( BucketDescriptor<?> aDescriptor, String anOuterPath, BucketName aPath )
        {
            final BucketStore.BucketContextDTO dto = new BucketStore.BucketContextDTO();
            dto.innerPath = aPath.innerPath;
            dto.simpleName = aPath.simpleName;
            dto.format = aPath.format;
            dto.packaging = aPath.packaging;
            dto.outerPath = anOuterPath;
            return dto;
        }        
    }
}
