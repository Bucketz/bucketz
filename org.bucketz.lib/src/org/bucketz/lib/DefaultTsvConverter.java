package org.bucketz.lib;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.apache.felix.schematizer.Schema;
import org.apache.felix.schematizer.Schematizer;
import org.apache.felix.schematizer.StandardSchematizer;
import org.apache.felix.serializer.Serializer;
import org.bucketz.BucketDescriptor;
import org.bucketz.Codec;
import org.osgi.util.converter.Converter;

public class DefaultTsvConverter<D>
    implements Codec<D>
{
    final BucketDescriptor<D> descriptor;
    final Serializer serializer;

    private final String delimiter;
    private final String[] columns;
    private final String nullValue;
    private final Optional<Function<D, D>> preprocessor;

    public DefaultTsvConverter(
            String aDelimiter,
            String[] aColumnsSpecification,
            String aNullValue,
            Optional<Function<D, D>> aPreprocessor,
            BucketDescriptor<D> aDescriptor, 
            Serializer aSerializer )
    {
        delimiter = aDelimiter;
        columns = aColumnsSpecification;
        nullValue = aNullValue;
        preprocessor = aPreprocessor;

        descriptor = aDescriptor;
        serializer = aSerializer;
    }

    @Override
    public Coder<D> coder()
    {
        return entity -> {
            final String objectName = descriptor.type().getTypeName();
            final Schematizer schematizer = new StandardSchematizer().schematize( objectName, descriptor.type() );
            final Schema schema = schematizer.get( objectName );
            final Converter converter = schematizer.converterFor( objectName );

            final StringBuilder line = new StringBuilder();
            for( int i = 0; i < columns.length; i++ )
            {
                final Collection<?> collection = schema.valuesAt( columns[i], entity );
                final Object obj;
                if( collection != null && collection.size() == 1 )
                    obj = collection.iterator().next();
                else
                    obj = collection;
                final String value = converter.convert( obj ).sourceAsDTO().toString();
                final String toAppend = value == null ? DelimiterSeparatedValuesIO.NULL : value;
                line.append( toAppend );
                if( i < columns.length - 1 )
                    line.append( delimiter );
            }

            return line.toString();
        };
    }

    @Override
    public Decoder<D> decoder()
    {
        return line -> {

            final String[] fields = line.toString().split( delimiter );

            if( fields.length != columns.length )
            {
                // Wrong number of columns!
                // TODO log warning, find a better way to handle the error
                return null;
           }

            for (int i = 0; i < columns.length; i++)
                if (nullValue.equals(fields[i]))
                    fields[i] = null;
                
            final Map<String, String> m = new HashMap<>();
            for( int c = 0; c < columns.length; c++ )
                m.put( columns[c], fields[c] );

            final String objectName = descriptor.type().getTypeName();
            final Converter converter = new StandardSchematizer()
                    .schematize( objectName, descriptor.type() )
                    .converterFor( objectName );

            final D lineObject = converter.convert( m ).to( descriptor.type() );
            final D processedLineObject = preprocessor.isPresent() ? preprocessor.get().apply( lineObject ) : lineObject;

            return processedLineObject;
        };
    }
}
