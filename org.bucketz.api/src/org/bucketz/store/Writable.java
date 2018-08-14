package org.bucketz.store;

import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.osgi.dto.DTO;
import org.osgi.util.promise.Promise;

/**
 * A BucketStore can accept write operations represented by Writable.
 */
public interface Writable<D>
{
    /**
     * Push a Stream of DTOs to the BucketStore for persistence.
     */
    Promise<Boolean> push( Stream<D> aDataStream );

    /**
     * Push an incremental change to the BucketStore for persistence. A backreference to the
     * repository is required, as the object may require additional context when being
     * persisted. To persist in a MultiJson Bucket, for instance, the entire collection
     * of DTOs must be updated, not just the DTO that is incrementally changed.
     */
    Promise<Boolean> push( Increment<D> anIncrement, Supplier<Map<String, D>> repo );

    /**
     * Represents an incremental change. This is useful when the BucketStore should be
     * incrementally updated to reflect changes. The Increment object contains enough
     * information to allow the BucketStore to persist the incremental update.
     */
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
