package org.bucketz.lib;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.bucketz.Bucket;
import org.bucketz.BucketPath;
import org.bucketz.BucketStore;
import org.bucketz.BucketStore.Format;
import org.bucketz.BucketStore.Packaging;

public class BucketFactory
{
    public static final Bucket newBucket( BucketStore.BucketDTO dto )
    {
        final BucketVo bucket = new BucketVo( dto );
        return bucket;
    }

    public static final class BucketVo
        extends BucketStore.BucketDTO
        implements Bucket
    {
        public BucketVo( BucketStore.BucketDTO dto )
        {
            final BucketStore.BucketContextDTO newContext = new BucketStore.BucketContextDTO();
            newContext.innerPath = dto.context.innerPath;
            newContext.outerPath = dto.context.outerPath;
            newContext.simpleName = dto.context.simpleName;
            newContext.format = dto.context.format;
            newContext.packaging = dto.context.packaging;
            context = newContext;
            content = dto.content;
            location = dto.location;
            descriminant = dto.descriminant;
        }

        @Override public BucketPath innerPath() { return toBucketPath( context.innerPath ); }
        @Override public BucketPath outerPath() { return toBucketPath( context.outerPath ); }
        @Override public String simpleName() { return context.simpleName; }
        @Override public Format format() { return Format.valueOf( context.format ); }
        @Override public Packaging packaging() { return Packaging.valueOf( context.packaging ); }
        @Override public String toString() { return fqn(); }
        @Override public Optional<String> content() { return Optional.ofNullable( content ); }
        @Override public Optional<String> descriminant() { return Optional.ofNullable( descriminant ); }
        @Override public URI location()
        {
            try
            {
                return new URI( location );
            }
            catch (URISyntaxException e)
            {
                throw new IllegalStateException( e );
            }
        }
        @Override public String fullPath()
        {
            return new StringBuilder()
                    .append( outerPath() )
                    .append( innerPath() )
                    .toString();
        }
        @Override public String fullName()
        {
            return new StringBuilder()
                    .append( innerPath() )
                    .append( simpleName() )
                    .append( "." ).append( context.format.toLowerCase() )
                    .toString();
        }
        @Override public String fqn()
        {
            return new StringBuilder()
                    .append( fullPath() )
                    .append( simpleName() )
                    .append( "." ).append( context.format.toLowerCase() )
                    .append( ":" ).append( context.packaging )
                    .toString();
        }
        @Override public URI asUri()
            throws IllegalStateException
        {
            try
            {
                final String fqn = fqn();
                final String urlString = new StringBuilder()
                        .append( location() )
                        .append(  fqn.substring( 0, fqn.lastIndexOf( ":" ) ) )
                        .toString();
                final URI uri = new URI( urlString );
                return uri;
            }
            catch ( URISyntaxException e )
            {
                throw new IllegalStateException( e );
            }
        }

        private BucketPath toBucketPath( String aPath )
        {
            final List<String> parts;
            if (aPath == null)
            {
                parts = Collections.emptyList();
            }
            else
            {
                final String[] pathParts = aPath.split( "/" );
                parts = new ArrayList<>( Arrays.asList( pathParts ) );
            }
            final BucketPathVo bp = new BucketPathVo( parts );
            return bp;
        }
    }

    public static final class BucketPathVo
        implements BucketPath
    {
        private final List<String> parts = new ArrayList<>();

        public BucketPathVo( List<String> aPartsList )
        {
            parts.addAll( aPartsList );
        }

        @Override
        public List<String> parts()
        {
            return parts.stream().collect( Collectors.toList() );
        }        
    }
}

