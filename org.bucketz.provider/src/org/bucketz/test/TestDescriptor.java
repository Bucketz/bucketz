package org.bucketz.test;

import java.util.Optional;
import java.util.function.Function;

import org.bucketz.BucketDescriptor;

public class TestDescriptor
    implements BucketDescriptor<TestDTO>
{
    @Override
    public String name()
    {
        return "test";
    }

    @Override
    public String description()
    {
        return "Just for testing";
    }

    @Override
    public Class<TestDTO> type()
    {
        return TestDTO.class;
    }

    @Override
    public String version()
    {
        return "1.0.0";
    }

    @Override
    public Function<TestDTO, String> idExtractor()
    {
        return obj -> obj.id;
    }

    @Override
    public String brn()
    {
        return "test";
    }

    @Override
    public Optional<String> containerName()
    {
        return Optional.of( "test" );
    }
}
