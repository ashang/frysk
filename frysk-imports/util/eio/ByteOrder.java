// From the VENUS project.  Copyright 2004, 2005, Andrew Cagney
// Licenced under the terms of the Eclipse Public Licence.
// Licenced under the terms of the GNU CLASSPATH Licence.

package util.eio;

public final class ByteOrder
{
    public static final ByteOrder BIG_ENDIAN = new ByteOrder ();
    public static final ByteOrder LITTLE_ENDIAN = new ByteOrder ();
    public static ByteOrder nativeOrder ()
    {
	return BIG_ENDIAN;
    }
    public String toString ()
    {
	if (this == BIG_ENDIAN)
	    return "BIG_ENDIAN";
	else if (this == LITTLE_ENDIAN)
	    return "LITTLE_ENDIAN";
	else
	    return null;
    }
}
