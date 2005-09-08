// From the VENUS project.  Copyright 2004, 2005, Andrew Cagney
// Licenced under the terms of the Eclipse Public Licence.
// Licenced under the terms of the GNU CLASSPATH Licence.

package util.eio;

import junit.framework.TestCase;

public class TestLib
    extends TestCase
{
    static void checkArray (String what, byte[] array, int lo, int hi)
    {
	// check pre this gunk
	for (int i = lo; i < hi; i++) {
	    int a = array[i] & 0xff;
	    int v = -i & 0xff;
	    assertEquals (what + " array[" + i + "] == 0x"
			  + Integer.toHexString (a)
			  + " should be 0x"
			  + Integer.toHexString (v),
			  a, v);
	}
    }

    static void check (String what, byte[] array, int offset, int[] bytes)
    {
	checkArray (what, array, 0, offset);
	for (int i = 0; i < bytes.length; i++) {
	    int a = array[offset + i] & 0xff;
	    int b = bytes[i];
	    assertEquals (what + " array[" + offset + "+" + i + "] == 0x"
			  + Integer.toHexString (a)
			  + " should be 0x"
			  + Integer.toHexString (b),
			  a, b);
	}
	checkArray (what, array, offset + bytes.length, array.length);
    }

    static void clearArray (byte[] array)
    {
	for (int i = 0; i < array.length; i++) {
	    array[i] = (byte) -i;
	}
    }

    static void initArray (byte[] array)
    {
	for (int i = 0; i < array.length; i++) {
	    array[i] = (byte) (i + 1);
	}
    }

    byte[] orderedArray ()
    {
	byte[] array = new byte[256];
	for (int i = 0; i < array.length; i++) {
	    array[i] = (byte) (i + 1);
	}
	return array;
    }

    static String bytesToString (byte[] array, long loffset, int[] lengths)
    {
	StringBuffer string = new StringBuffer ();
	int offset = (int) loffset;
	string.append ("array[" + offset + "..]:");
	for (int l = 0; l < lengths.length; l++) {
	    int length = lengths[l];
	    string.append (' ');
	    for (int i = offset; i < offset + length; i++) {
		int b = array[i] & 0xff;
		// XXX: Fixme, better way to do this?
		if (b >= 0x00L && b <= 0x0fL)
		    string.append ("0" + Integer.toHexString (b));
		else
		    string.append (Integer.toHexString (b));
	    }
	    offset += length;
	}
	return string.toString ();
    }
}
