package org.bucketz.store;

import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.bucketz.UncheckedBucketException;
import org.osgi.dto.DTO;

public interface Writable<D>
{
    void push( Stream<D> anDataStream )
        throws UncheckedBucketException;

    void push( Increment<D> anIncrement, Supplier<Map<String, D>> repo )
        throws UncheckedBucketException;

    static <D>Increment<D> newIncrement( Increment.Type aType, D aDTO )
    {
        final IncrementDTO<D> dto = new IncrementDTO<>();
        dto.type = aType.name();
        dto.value = aDTO;
        final IncrementVo<D> vo = new IncrementVo<D>( dto, aDTO );
        return vo;
    }

    public static interface Increment<D>
    {
        public static enum Type { PUT, DELETE }
        Type type();
        D value();
    }

    public static class IncrementDTO<D>
        extends DTO
    {
        public String type;
        public D value;
    }

    public static class IncrementVo<D>
        extends IncrementDTO<D>
        implements Increment<D>
    {
        public IncrementVo( Writable.IncrementDTO<D> dto, D aDTO )
        {
            type = dto.type;
            value = aDTO;
        }

        @Override
        public Type type()
        {
            return Type.valueOf( type );
        }

        @Override
        public D value()
        {
            return value;
        }
    }
}
