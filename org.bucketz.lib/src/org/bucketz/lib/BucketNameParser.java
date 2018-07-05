package org.bucketz.lib;

import org.bucketz.BucketStore;

@FunctionalInterface
public interface BucketNameParser
{
    BucketName parse( String aBucketName, BucketStore.Packaging aPackaging )
        throws Exception;

    static BucketNameParser newParser()
    {
        return new Parser();
    }

    public static class Parser
        implements BucketNameParser
    {
        @Override
        public BucketName parse( String aBucketName, BucketStore.Packaging aPackaging )
            throws Exception
        {
            if (aBucketName == null || aBucketName.isEmpty())
                throw new Exception( "Bucket name cannot be null or empty" );

            String[] parts;
            String bn = aBucketName;
            final BucketName bp = new BucketName();
            if (aBucketName.contains( ":" ))
            {
                parts = bn.split( ":" );
                if (parts.length != 2)
                    throw new Exception( String.format( "Could not parse Bucket name %s", aBucketName ) );

                bn = parts[0];
                bp.packaging = parts[1].toUpperCase();
            }
            else
            {
                bp.packaging = aPackaging.name();
            }

            if (bn.contains( "/" ))
            {
                final int index = bn.lastIndexOf( "/" );
                bp.innerPath = bn.substring( 0, index );
                bn = bn.substring( index + 1 );
            }
            else
            {
                bp.innerPath = "";
            }

            if (bn.contains( "." ))
            {
                parts = bn.split( "\\." );
                if (parts.length != 2)
                    throw new Exception( String.format( "Could not parse Bucket name %s", aBucketName ) );

                bp.simpleName = parts[0];
                bp.format = parts[1].toUpperCase();
            }
            else
            {
                bp.simpleName = bn;
                bp.format = BucketStore.Format.JSON.name();
            }

            if (!bp.innerPath.isEmpty() && !bp.innerPath.endsWith( "/" ))
                bp.innerPath += "/";

            return bp;
        }        
    }
}
