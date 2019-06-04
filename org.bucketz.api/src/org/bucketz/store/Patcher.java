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
}
