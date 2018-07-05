package org.bucketz.lib;

import org.apache.felix.serializer.Serializer;
import org.bucketz.BucketDescriptor;
import org.bucketz.Codec;

public class DefaultJsonConverter<E>
    implements Codec<E>
{
    final BucketDescriptor<E> descriptor;
    final Serializer serializer;

    public DefaultJsonConverter( BucketDescriptor<E> aDescriptor, Serializer aSerializer )
    {
        descriptor = aDescriptor;
        serializer = aSerializer;
    }

    @Override
    public Coder<E> coder()
    {
        return e -> serializer.serialize( e ).toString();
    }

    @Override
    public Decoder<E> decoder()
    {
        return s -> serializer.deserialize( descriptor.type() ).from( s );
    }
}
