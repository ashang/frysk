// This file is part of INUA.  Copyright 2004, 2005, Andrew Cagney
//
// INUA is free software; you can redistribute it and/or modify it
// under the terms of the GNU General Public License as published by
// the Free Software Foundation; version 2 of the License.
//
// INUA is distributed in the hope that it will be useful, but
// WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
// General Public License for more details.
// 
// You should have received a copy of the GNU General Public License
// along with INUA; if not, write to the Free Software Foundation,
// Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
// 
// In addition, as a special exception, Andrew Cagney. gives You the
// additional right to link the code of INUA with code not covered
// under the GNU General Public License ("Non-GPL Code") and to
// distribute linked combinations including the two, subject to the
// limitations in this paragraph. Non-GPL Code permitted under this
// exception must only link to the code of INUA through those well
// defined interfaces identified in the file named EXCEPTION found in
// the source code files (the "Approved Interfaces"). The files of
// Non-GPL Code may instantiate templates or use macros or inline
// functions from the Approved Interfaces without causing the
// resulting work to be covered by the GNU General Public
// License. Only Andrew Cagney may make changes or additions to the
// list of Approved Interfaces. You must obey the GNU General Public
// License in all respects for all of the INUA code and other code
// used in conjunction with INUA except the Non-GPL Code covered by
// this exception. If you modify this file, you may extend this
// exception to your version of the file, but you are not obligated to
// do so. If you do not wish to provide this exception without
// modification, you must delete this exception statement from your
// version and license this file solely under the GPL without
// exception.
package inua.eio;

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
