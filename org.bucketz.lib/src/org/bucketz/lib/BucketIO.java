package org.bucketz.lib;

import org.bucketz.Codec;
import org.bucketz.lib.BucketIOFactory.MultiJsonConfigFactory;
import org.bucketz.lib.BucketIOFactory.PartitionedJsonConfigFactory;
import org.bucketz.lib.BucketIOFactory.SingleObjectConfigFactory;
import org.bucketz.lib.BucketIOFactory.TsvConfigFactory;

public interface BucketIO<D>
    extends BucketReader<D>, BucketWriter<D>, Codec<D>
{
    public static <D>BucketIOFactory<D> newFactory( Class<D> dtoClass )
    {
        return new BucketIOFactory<>( dtoClass );
    }

    public static TsvConfigFactory newTsvConfigFactory()
    {
        return new TsvConfigFactory();
    }

    public static MultiJsonConfigFactory newMultiJsonConfigFactory()
    {
        return new MultiJsonConfigFactory();
    }

    public static <D>PartitionedJsonConfigFactory<D> newPartitionedJsonConfigFactory( Class<D> dtoClass )
    {
        return new PartitionedJsonConfigFactory<>();
    }

    public static SingleObjectConfigFactory newSingleObjectConfigFactory()
    {
        return new SingleObjectConfigFactory();
    }
}
