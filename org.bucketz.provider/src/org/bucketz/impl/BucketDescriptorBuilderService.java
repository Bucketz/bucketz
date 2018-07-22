package org.bucketz.impl;

import java.util.Comparator;
import java.util.Optional;
import java.util.function.Function;

import org.bucketz.Bucket;
import org.bucketz.UncheckedBucketException;
import org.bucketz.store.BucketDescriptor;

public class BucketDescriptorBuilderService<D>
    implements BucketDescriptor.Builder<D>
{
    private final DescriptorData<D> data;

    private boolean isUsed;

    public BucketDescriptorBuilderService( Class<D> aDTOType )
    {
        // Set defaults
        data = new DescriptorData<>();
        data.type = aDTOType;
        data.version = "1.0.0";
        isUsed = false;
    }

    @Override
    public BucketDescriptor.Builder<D> setName( String aName )
    {
        assertNotUsed();
        data.name = aName;
        return this;
    }

    @Override
    public BucketDescriptor.Builder<D> describeAs( String aDescription )
    {
        assertNotUsed();
        data.description = aDescription;
        return this;
    }

    @Override
    public BucketDescriptor.Builder<D> setVersion( String aVersion )
    {
        assertNotUsed();
        data.version = aVersion;
        return this;
    }

    @Override
    public BucketDescriptor.Builder<D> extractIdUsing( Function<D, String> anIdExtractor )
    {
        assertNotUsed();
        data.idExtractor = anIdExtractor;
        return this;
    }

    @Override
    public BucketDescriptor.Builder<D> compareWith( Comparator<D> aComparator )
    {
        assertNotUsed();
        data.comparator = aComparator;
        return this;
    }

    @Override
    public BucketDescriptor.Builder<D> representWith( String aBundleRepresentativeName )
    {
        assertNotUsed();
        data.brn = aBundleRepresentativeName;
        return this;
    }

    @Override
    public BucketDescriptor.Builder<D> containWith( String aContainerName )
    {
        assertNotUsed();
        data.containerName = aContainerName;
        return this;
    }

    @Override
    public BucketDescriptor.Builder<D> formatAs( Bucket.Format aFormat )
    {
        assertNotUsed();
        data.format = aFormat;
        return this;
    }

    @Override
    public BucketDescriptor.Builder<D> packageAs( Bucket.Packaging aPackaging )
    {
        assertNotUsed();
        data.packaging = aPackaging;
        return this;
    }

    @Override
    public BucketDescriptor.Builder<D> filterWith( String aFilter )
    {
        assertNotUsed();
        data.filter = aFilter;
        return this;
    }

    @Override
    public BucketDescriptor<D> get()
    {
        validate();
        final DefaultBucketDescriptor<D> descriptor;
        if (data.packaging == Bucket.Packaging.SINGLE)
            descriptor = new DefaultSingleObjectBucketDescriptor<>( data );
        else
            descriptor = new DefaultBucketDescriptor<>( data );
        isUsed = true;
        return descriptor;
    }

    private void validate()
    {
        if (data.name == null || data.name.isEmpty())
            throw new UncheckedBucketException( "Name is missing" );
        if (data.description == null || data.description.isEmpty())
            throw new UncheckedBucketException( "Description is missing" );
        if (data.type == null)
            throw new UncheckedBucketException( "DTO data type is missing" );
        if (data.idExtractor == null )
            throw new UncheckedBucketException( "IDExtractor is missing" );
        if (data.brn == null)
            throw new UncheckedBucketException( "BundleRepresentativeName is missing" );
    }

    private void assertNotUsed()
    {
        if (isUsed)
            throw new UncheckedBucketException( "This builder has already been used" );
    }

    public static final class DescriptorData<D>
    {
        private String name;
        private String description;
        private Class<D> type;
        private String version;
        private Function<D, String> idExtractor;
        private Comparator<D> comparator;
        private Bucket.Format format;
        private Bucket.Packaging packaging;
        private String filter;
        private String brn;
        private String containerName;        
    }

    public static class DefaultBucketDescriptor<D>
        implements BucketDescriptor<D>
    {
        private final String name;
        private final String description;
        private final Class<D> type;
        private final String version;
        private final Function<D, String> idExtractor;
        private final Comparator<D> comparator;
        private final Bucket.Format format;
        private final Bucket.Packaging packaging;
        private final String filter;
        private final String brn;
        private final String containerName;        

        public DefaultBucketDescriptor( DescriptorData<D> data )
        {
            name = data.name;
            description = data.description;
            type = data.type;
            version = data.version;
            idExtractor = data.idExtractor;
            comparator = data.comparator;
            format = data.format;
            packaging = data.packaging;
            filter = data.filter;
            brn = data.brn;
            containerName = data.containerName;
        }

        @Override
        public String name()
        {
            return name;
        }

        @Override
        public String description()
        {
            return description;
        }

        @Override
        public Class<D> type()
        {
            return type;
        }

        @Override
        public String version()
        {
            return version;
        }

        @Override
        public Function<D, String> idExtractor()
        {
            return idExtractor;
        }

        @Override
        public Optional<Comparator<D>> comparator()
        {
            if (comparator != null)
                return Optional.of( comparator );

            return BucketDescriptor.super.comparator();
        }

        @Override
        public Bucket.Format format()
        {
            if (format != null)
                return format;

            return BucketDescriptor.super.format();
        }

        @Override
        public Bucket.Packaging packaging()
        {
            if (packaging != null)
                return packaging;

            return BucketDescriptor.super.packaging();
        }

        @Override
        public Optional<String> filter()
        {
            if (filter != null)
                return Optional.of( filter );

            return BucketDescriptor.super.filter();
        }

        @Override
        public String brn()
        {
            return brn;
        }

        @Override
        public Optional<String> containerName()
        {
            return Optional.ofNullable( containerName );
        }
    }

    public static class DefaultSingleObjectBucketDescriptor<D>
        extends DefaultBucketDescriptor<D>
        implements BucketDescriptor.Single<D>
    {
        public DefaultSingleObjectBucketDescriptor( DescriptorData<D> data )
        {
            super( data );
        }        
    }
}
