package org.bucketz.command;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.felix.service.command.Descriptor;
import org.bucketz.Bucketz;
import org.bucketz.store.BucketStore;
import org.bucketz.store.BucketStores;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Bucketz.Commands.Provide
@Component(
    service=BucketzCommands.class, 
    property = {
        "osgi.command.scope=bucketz",
        "osgi.command.function=info",
        "osgi.command.function=list" },
	name="org.bucketz.commands"
)
public class BucketzCommands
{
    @Reference private BucketStores stores;

    @Descriptor( "List all the currently-active BucketStores" )
    public List<String> list()
        throws Exception
    {
        final List<String> storeNames = stores.list().stream()
                .map( store -> store.name() )
                .collect( Collectors.toList() );

        final List<String> lines = new ArrayList<>();
        lines.add( "BucketStores" );
        lines.add( "-----------------------------------------" );
        lines.addAll( storeNames );
        lines.add( "" );
        return lines;
    }

    @Descriptor( "Provide information about a BucketStore" )
    public List<String> info(
            @Descriptor( "The name of the BucketStore" )
            String storeName )
        throws Exception
    {
        final BucketStore<?> store = stores.list().stream()
                .filter( bs -> storeName.equals( bs.name() ) )
                .findFirst()
                .orElseThrow( () -> new Exception( String.format( "No BucketStore found with name: %s", storeName ) ) );

        final List<String> lines = new ArrayList<>();
        lines.add( String.format( "BucketStore: %s", storeName ) );
        lines.add( "-----------------------------------------" );
        lines.add( "  Store Type:  " + store.type() );
        lines.add( "  Location:    " + store.uri() );
        lines.add( "  Outer Path:  " + store.outerPath() );
        lines.add( "  Is Writable? " + store.isWritable() );
        lines.add( "  Buckets:     " + store.buckets() );
        return lines;
    }
}
