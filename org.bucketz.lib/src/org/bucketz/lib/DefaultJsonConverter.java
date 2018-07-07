package org.bucketz.lib;

import org.apache.felix.serializer.Serializer;
import org.bucketz.Codec;
import org.bucketz.store.BucketDescriptor;

public class DefaultJsonConverter<D>
    implements Codec<D>
{
    final BucketDescriptor<D> descriptor;
    final Serializer serializer;

    public DefaultJsonConverter( BucketDescriptor<D> aDescriptor, Serializer aSerializer )
    {
        descriptor = aDescriptor;
        serializer = aSerializer;
    }

    @Override
    public Coder<D> coder()
    {
        return e -> serializer.serialize( e ).toString();
    }

    @Override
    public Decoder<D> decoder()
    {
        return s -> serializer.deserialize( descriptor.type() ).from( s );
    }
}
