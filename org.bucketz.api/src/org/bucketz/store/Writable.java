package org.bucketz.store;

import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.osgi.dto.DTO;
import org.osgi.util.promise.Promise;

public interface Writable<D>
{
    Promise<Boolean> push( Stream<D> anDataStream );

    Promise<Boolean> push( Increment<D> anIncrement, Supplier<Map<String, D>> repo );

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
