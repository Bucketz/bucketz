package org.bucketz.lib;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

/**
 * Captures a processing error so that the error can be properly handled.
 * Not thread safe. Assumed to be running in a single thread.
 */
public class NullCapturingInputStream
    extends InputStream
{
    public static class NullCapturedException
        extends IOException
    {
        private static final long serialVersionUID = 1L;        
    }

    private final InputStream delegate;
    private boolean isInitiated = false;

    public NullCapturingInputStream( InputStream aDelegate )
    {
        delegate = aDelegate;
    }

    @Override
    public int read()
            throws IOException
    {
        return delegate.read();
    }

    @Override
    public int read( byte[] b )
            throws IOException
    {
        return delegate.read( b );
    }

    @Override
    public int read( byte[] b, int off, int len )
            throws IOException
    {
        final int result = delegate.read( b, off, len );
        final byte[] nullTest = Arrays.copyOf(b, 4);
        final String s = new String(nullTest);
        if (!isInitiated && "null".equals( s ))
        {
            isInitiated = true;
            throw new NullCapturedException();            
        }
        else
        {
            isInitiated = true;
        }

        return result;
    }

    @Override
    public long skip( long n )
            throws IOException
    {
        return delegate.skip( n );
    }

    @Override
    public int available()
            throws IOException
    {
        return delegate.available();
    }

    @Override
    public void close()
            throws IOException
    {
        delegate.close();
    }

    @Override
    public synchronized void mark( int readlimit )
    {
        delegate.mark( readlimit );
    }

    @Override
    public synchronized void reset()
            throws IOException
    {
        delegate.reset();
    }

    @Override
    public boolean markSupported()
    {
        return delegate.markSupported();
    }
}
