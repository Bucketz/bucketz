package org.bucketz;

public class UncheckedInterruptedException
    extends UncheckedBucketException
{
    private static final long serialVersionUID = 1L;

    public UncheckedInterruptedException()
    {
        super();
    }

    public UncheckedInterruptedException( String message, Throwable cause )
    {
        super( message, cause );
    }

    public UncheckedInterruptedException( String message )
    {
        super( message );
    }

    public UncheckedInterruptedException( Throwable cause )
    {
        super( cause );
    }
}
