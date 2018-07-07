package org.bucketz.lib;

import org.osgi.dto.DTO;

/**
 * Can be used in an implementation as a tool for manipulating
 * multiple values relating to a Bucket name at once. 
 */
public class BucketName
    extends DTO
{
    public String innerPath;
    public String simpleName;
    public String format;
    public String packaging;
}
