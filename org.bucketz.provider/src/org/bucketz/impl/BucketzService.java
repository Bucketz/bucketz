package org.bucketz.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.bucketz.store.BucketStore;
import org.bucketz.store.BucketStores;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(name="org.bucketz.stores")
public class BucketzService
    implements BucketStores
{
    @Reference
    private List<BucketStore<?>> stores = new ArrayList<>();

    @Override
    public List<BucketStore<?>> list()
    {
        return stores.stream()
                .sorted( (s1,s2) -> s1.name().compareTo( s2.name() ) )
                .collect( Collectors.toList() );
    }
}
