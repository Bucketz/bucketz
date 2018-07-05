package org.bucketz.store;

import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.osgi.dto.DTO;

public interface Writable<D>
{
    void push( Stream<D> anDataStream )
        throws Exception;

    void push( Increment<D> anIncrement, Supplier<Map<String, D>> repo )
        throws Exception;

    static <E>Increment<E> newIncrement(  Increment.Type aType, E anEntity )
    {
        final IncrementDTO<E> dto = new IncrementDTO<>();
        dto.type = aType.name();
        dto.value = anEntity;
        final IncrementVo<E> vo = new IncrementVo<E>( dto, anEntity );
        return vo;
    }

    public static interface Increment<E>
    {
        public static enum Type { PUT, DELETE }
        Type type();
        E value();
    }

    public static class IncrementDTO<E>
        extends DTO
    {
        public String type;
        public E value;
    }

    public static class IncrementVo<E>
        extends IncrementDTO<E>
        implements Increment<E>
    {
        public IncrementVo( Writable.IncrementDTO<E> dto, E anEntity )
        {
            type = dto.type;
            value = anEntity;
        }

        @Override
        public Type type()
        {
            return Type.valueOf( type );
        }

        @Override
        public E value()
        {
            return value;
        }
    }
}
