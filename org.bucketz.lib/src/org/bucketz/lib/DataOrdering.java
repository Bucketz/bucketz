package org.bucketz.lib;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DataOrdering
{
    private static final Map<Class<?>, Class<?>> boxedClasses;
    static
    {
        final Map<Class<?>, Class<?>> m = new HashMap<>();
        m.put( int.class, Integer.class );
        m.put( long.class, Long.class );
        m.put( double.class, Double.class );
        m.put( float.class, Float.class );
        m.put( boolean.class, Boolean.class );
        m.put( char.class, Character.class );
        m.put( byte.class, Byte.class );
        m.put( void.class, Void.class );
        m.put( short.class, Short.class );
        m.put( String.class, String.class );
        m.put( Map.class, Map.class );
        m.put( Number.class, Number.class );
        boxedClasses = Collections.unmodifiableMap( m );
    }

    private static Class<?> primitiveToBoxed( Class<?> cls )
    {
        final Class<?> boxed = boxedClasses.get( cls );
        if( boxed != null )
            return boxed;
        else
            return cls;
    }

    private static boolean isPrimitiveType( Class<?> type )
    {
        return boxedClasses.containsValue( primitiveToBoxed( type ) );
    }

    public static Map<String, List<String>> extractOrderingRules( Class<?> fromType )
    {
        return extractOrderingRules( fromType, "/" );
    }

    private static Map<String, List<String>> extractOrderingRules( Class<?> fromType, String path )
    {
        final Map<String, List<String>> rules = new HashMap<>();
        final List<String> fields = new ArrayList<>();
        rules.put( path, fields );

        for( Field f : fromType.getFields() )
        {
            if( Modifier.isStatic( f.getModifiers() ) )
                continue;

            final String fieldName = f.getName();
            fields.add( fieldName );

            final Class<?> fieldType = f.getType();
            if( Collection.class.isAssignableFrom( fieldType ) )
            {
                if( hasCollectionTypeAnnotation( f ) )
                {
                    final Class<?> fieldCollectionType = collectionTypeOf( f );
                    if( !isPrimitiveType( fieldCollectionType ) )
                    {
                        final String fieldPath = appendToPath( path, fieldName );
                        final Map<String, List<String>> fieldRules = extractOrderingRules( fieldCollectionType, fieldPath );
                        rules.putAll( fieldRules );
                    }
                }
            }
            else if( !isPrimitiveType( fieldType ) )
            {
                final String fieldPath = appendToPath( path, fieldName );
                final Map<String, List<String>> fieldRules = extractOrderingRules( fieldType, fieldPath );
                rules.putAll( fieldRules );
            }
        }

        return rules;
    }

    public static String appendToPath( String basePath, String additionalPath )
    {
        String basePart = basePath;
        if( basePart.endsWith( "/" ) )
            basePart = basePart.substring( 0, basePart.length() - 1 );
        return new StringBuilder()
                .append( basePart )
                .append( "/" )
                .append( additionalPath )
                .toString();
    }

    public static boolean hasCollectionTypeAnnotation( Field field )
    {
        if (field == null)
            return false;

        Annotation[] annotations = field.getAnnotations();
        if( annotations.length == 0 )
            return false;

        return Arrays.stream( annotations )
            .map( a -> a.annotationType().getName() )
            .anyMatch( a -> "CollectionType".equals( a.substring( a.lastIndexOf( "." ) + 1 ) ) );
    }

    public static Class<?> collectionTypeOf( Field field )
    {
        final Annotation[] annotations = field.getAnnotations();

        final Annotation annotation = Arrays.stream( annotations )
            .filter(a -> "CollectionType".equals( a.annotationType().getName().substring(a.annotationType().getName().lastIndexOf(".") + 1) ) )
            .findFirst()
            .get();

        try
        {
            final Method m = annotation.annotationType().getMethod( "value" );
            Class<?> value = (Class<?>)m.invoke( annotation, (Object[])null );
            return value;            
        }
        catch( Exception e )
        {
            return null;
        }
    }

    public static boolean isCollectionType(Class<?> clazz) {
        return Collection.class.isAssignableFrom(clazz);        
    }
}
