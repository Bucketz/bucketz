package org.bucketz.impl;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonPatch;
import javax.json.JsonPatchBuilder;
import javax.json.JsonReader;
import javax.json.JsonWriter;

import org.apache.felix.serializer.Serializer;
import org.bucketz.store.Patcher;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component
public class PatcherService
    implements Patcher
{
    @Reference private Serializer serializer;

    @Override
    public <D>D patch( D entity, List<Patch> patches )
    {
        final String jsonString = serializer.serialize( entity )
                .sourceAsDTO()
                .toString();
        final StringReader reader = new StringReader( jsonString );
        final JsonReader jsonReader = Json.createReader( reader );
        final JsonObject jsonObject = jsonReader.readObject();
        final JsonPatch jsonPatch = toJsonPatch( patches );
        final JsonObject patchedObject = jsonPatch.apply( jsonObject );
        final StringWriter writer = new StringWriter();
        final JsonWriter jsonWriter = Json.createWriter( writer );
        jsonWriter.writeObject( patchedObject );
        final String patchedJson = writer.toString();
        @SuppressWarnings( "unchecked" )
        final D patched = (D)serializer
                .deserialize( entity.getClass() )
                .from( patchedJson );
        return patched;
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
