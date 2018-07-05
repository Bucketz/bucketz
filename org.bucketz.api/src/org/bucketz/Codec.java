package org.bucketz;

/**
 * Codes/decodes a Bucket to and from a string representation in its configured Format.
 * The default is JSON <-> DTO.
 */
public interface Codec<D>
{
    Coder<D> coder();
    Decoder<D> decoder();

    @FunctionalInterface
    static interface Coder<D>
    {
        String encode(D d)
            throws Exception;
    }

    @FunctionalInterface
    static interface Decoder<D>
    {
        D decode(String string)
            throws Exception;
    }
}
