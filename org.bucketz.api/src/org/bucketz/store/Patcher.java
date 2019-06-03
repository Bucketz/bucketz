package org.bucketz.store;

import java.util.List;

import org.osgi.dto.DTO;

public interface Patcher
{
    static enum PatchOperation { add, replace, remove, ERROR }

    public static class Patch
        extends DTO
    {
        public String op;
        public String path;
        public String value;
    }

    <D>D patch( D entity, List<Patch> patches );
//    @SuppressWarnings( "unchecked" )
//    public static <D>D patch( D entity, List<Patch> patches )
//    {
//        if (patches == null || patches.isEmpty())
//            return entity;
//
//        final Map<String, Object> map = Converters.standardConverter()
//                .convert( entity )
//                .sourceAsDTO()
//                .to( Map.class );
//
//        
////        for( String path : updates.keySet() )
////            patch( map, path, updates.get( path ) );
////
////        final D updated = Converters.standardConverter()
////                .convert( map )
////                .targetAsDTO()
////                .to( (Class<D>)entity.getClass() );
////
////        return updated;
//    }

//    @SuppressWarnings( "unchecked" )
//    private static Object patch( Map<String, Object> original, String path, Object value )
//    {
//        if (path.contains( "/" ))
//        {
//            final int idx = path.indexOf( "/" );
//            final String nextPart = path.substring( 0, idx );
//            final String remainder = path.substring( idx + 1 );
//            final Object obj = original.get( nextPart );
//            if (!(obj instanceof Map))
//                throw new IllegalStateException( String.format( "No field for path %s", path ) );
//            final Map<String, Object> map = (Map<String, Object>)obj;
//            return patch( map, remainder, value );
//        }
//
//        original.put( path, value );
//        return original;
//    }
//  if (value instanceof Map)
//  {
//      final String childPath = path + "/" + 
//      final Map<String, Object> child = (Map<String, Object>)value;
//      for( String path : updates.keySet() )
//          patch( map, path, updates.get( path ) );
//  }
}
