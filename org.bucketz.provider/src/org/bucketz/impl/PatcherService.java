package org.bucketz.impl;

import static org.osgi.util.converter.ConverterFunction.CANNOT_HANDLE;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonPatch;
import javax.json.JsonPatchBuilder;
import javax.json.JsonString;
import javax.json.JsonValue;
import javax.json.JsonValue.ValueType;

import org.apache.felix.serializer.Serializer;
import org.bucketz.store.Patcher;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ServiceScope;
import org.osgi.util.converter.Converter;
import org.osgi.util.converter.Converters;
import org.osgi.util.converter.TypeReference;

@Component(scope = ServiceScope.PROTOTYPE)
public class PatcherService
    implements Patcher
{
    @Reference private Serializer serializer;

    private final Converter converter = Converters.newConverterBuilder()
            .rule(JsonValue.class, this::toJsonValue)
            .rule(this::toScalar)
            .build();

    @Override
    public <D>D patch( D original, List<Patch> patches )
    {
        final JsonValue jsonValue = converter
                .convert(original)
                .sourceAsDTO()
                .to(JsonValue.class);
        final JsonObject jsonObject = jsonValue.asJsonObject();
        final JsonPatch jsonPatch = toJsonPatch( patches );
        final JsonObject patchedObject = jsonPatch.apply( jsonObject );
        @SuppressWarnings( "unchecked" )
        final D patched = (D)converter
                .convert(patchedObject)
                .targetAsDTO()
                .to(original.getClass());
        return patched;
    }

    private JsonValue toJsonValue(Object value, Type targetType)
    {
        if (value == null)
        {
           return JsonValue.NULL;
        }
        else if (value instanceof String)
        {
            return Json.createValue(value.toString());
        }
        else if (value instanceof Boolean)
        {
            return ((Boolean) value) ? JsonValue.TRUE : JsonValue.FALSE;
        }
        else if (value instanceof Number)
        {
            final Number n = (Number) value;
            if (value instanceof Float || value instanceof Double)
            {
                return Json.createValue(n.doubleValue());
            }
            else if (value instanceof BigDecimal)
            {
                return Json.createValue((BigDecimal) value);
            }
            else if (value instanceof BigInteger)
            {
                return Json.createValue((BigInteger) value);
            }
            else {
                return Json.createValue(n.longValue());
            }
        }
        else if (value instanceof Collection || value.getClass().isArray())
        {
            return toJsonArray(value);
        }
        else
        {
            return toJsonObject(value);
        }
    }

    private JsonArray toJsonArray(Object o)
    {
        final List<?> l = converter.convert(o).to(List.class);
        final JsonArrayBuilder builder = Json.createArrayBuilder();
        l.forEach(v -> builder.add(toJsonValue(v, JsonValue.class)));
        return builder.build();
    }

    private JsonObject toJsonObject(Object o)
    {
        final Map<String, Object> m = converter.convert(o).to(new TypeReference<Map<String, Object>>(){});
        final JsonObjectBuilder jsonBuilder = Json.createObjectBuilder();
        m.entrySet().stream().forEach(e -> jsonBuilder.add(e.getKey(), toJsonValue(e.getValue(), JsonValue.class)));
        return jsonBuilder.build();
    }

    private Object toScalar(Object o, Type t)
    {
        if (o instanceof JsonNumber)
        {
            final JsonNumber jn = (JsonNumber) o;
            return converter.convert(jn.bigDecimalValue()).to(t);
        }
        else if (o instanceof JsonString)
        {
            final JsonString js = (JsonString) o;
            return converter.convert(js.getString()).to(t);
        }
        else if (o instanceof JsonValue)
        {
            final JsonValue jv = (JsonValue) o;
            if (jv.getValueType() == ValueType.NULL)
            {
                return null;
            }
            else if (jv.getValueType() == ValueType.TRUE)
            {
                return converter.convert(Boolean.TRUE).to(t);
            }
            else if (jv.getValueType() == ValueType.FALSE) {
                return converter.convert(Boolean.FALSE).to(t);
            }
        }

        return CANNOT_HANDLE;
    }

    private JsonPatch toJsonPatch( List<Patch> patches )
    {
        final JsonPatchBuilder builder = Json.createPatchBuilder();
        for( Patcher.Patch patch : patches )
        {
            final Patcher.PatchOperation op = Patcher.PatchOperation.valueOf( patch.op );
            switch( op )
            {
                case add:
                    builder.add( patch.path, patch.value );
                    break;

                case replace:
                    builder.replace( patch.path, patch.value );
                    break;

                case remove:
                    builder.remove( patch.path );
                    break;

                default :
                    // Ignore
                    break;
            }
        }

        final JsonPatch jsonPatch = builder.build();
        return jsonPatch;
    }
}
