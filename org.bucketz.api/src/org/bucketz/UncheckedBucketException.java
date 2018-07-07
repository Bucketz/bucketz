package org.bucketz;

/**
 * Use unchecked Exceptions so we can benefit from Java8 Streams.
 */
public class UncheckedBucketException
    extends RuntimeException
{
    private static final long serialVersionUID = 1L;

    public UncheckedBucketException()
    {
        super();
    }

    public UncheckedBucketException( String message, Throwable cause )
    {
        super( message, cause );
    }

    public UncheckedBucketException( String message )
    {
        super( message );
    }

    public UncheckedBucketException( Throwable cause )
    {
        super( cause );
    }
}
