// This file is part of the program FRYSK.
//
// Copyright 2007, Red Hat Inc.
//
// FRYSK is free software; you can redistribute it and/or modify it
// under the terms of the GNU General Public License as published by
// the Free Software Foundation; version 2 of the License.
//
// FRYSK is distributed in the hope that it will be useful, but
// WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
// General Public License for more details.
// 
// You should have received a copy of the GNU General Public License
// along with FRYSK; if not, write to the Free Software Foundation,
// Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
// 
// In addition, as a special exception, Red Hat, Inc. gives You the
// additional right to link the code of FRYSK with code not covered
// under the GNU General Public License ("Non-GPL Code") and to
// distribute linked combinations including the two, subject to the
// limitations in this paragraph. Non-GPL Code permitted under this
// exception must only link to the code of FRYSK through those well
// defined interfaces identified in the file named EXCEPTION found in
// the source code files (the "Approved Interfaces"). The files of
// Non-GPL Code may instantiate templates or use macros or inline
// functions from the Approved Interfaces without causing the
// resulting work to be covered by the GNU General Public
// License. Only Red Hat, Inc. may make changes or additions to the
// list of Approved Interfaces. You must obey the GNU General Public
// License in all respects for all of the FRYSK code and other code
// used in conjunction with FRYSK except the Non-GPL Code covered by
// this exception. If you modify this file, you may extend this
// exception to your version of the file, but you are not obligated to
// do so. If you do not wish to provide this exception without
// modification, you must delete this exception statement from your
// version and license this file solely under the GPL without
// exception.

package frysk.junit;

import junit.framework.AssertionFailedError;
import java.math.BigInteger;

/**
 * Test additional assert()s added to TestCase.
 */

public class TestAsserts
    extends TestCase
{
    public void testByteArrayNull() {
	assertEquals("byte array", (byte[])null, (byte[])null);
    }
    public void testByteArrayLeftNull() {
	AssertionFailedError e = null;
	try {
	    assertEquals("byte array", (byte[])null, new byte[0]);
	} catch (AssertionFailedError a) {
	    e = a;
	}
	assertNotNull("exception", e);
    }
    public void testByteArrayRightNull() {
	AssertionFailedError e = null;
	try {
	    assertEquals("byte array", new byte[0], (byte[])null);
	} catch (AssertionFailedError a) {
	    e = a;
	}
	assertNotNull("exception", e);
    }
    public void testByteArrayDifferentLength() {
	AssertionFailedError e = null;
	try {
	    assertEquals("byte array", new byte[0], new byte[1]);
	} catch (AssertionFailedError a) {
	    e = a;
	}
	assertNotNull("exception", e);
    }
    public void testByteArrayDifferentContent() {
	AssertionFailedError e = null;
	try {
	    assertEquals("byte array", new byte[] { 1 }, new byte[] { 2 });
	} catch (AssertionFailedError a) {
	    e = a;
	}
	assertNotNull("exception", e);
    }
    public void testByteArrayEquals() {
	assertEquals("empty byte array", new byte[0], new byte[0]);
	assertEquals("full byte array", new byte[] { 1 }, new byte[] { 1 });
    }

    public void testBigIntegerNull() {
	assertEquals("null", (BigInteger)null, (BigInteger)null);
    }
    public void testBigIntegerRightNull() {
	AssertionFailedError e = null;
	try {
	    assertEquals("null", BigInteger.valueOf(1), null);
	} catch (AssertionFailedError a) {
	    e = a;
	}
	assertNotNull("exception", e);
    }
    public void testBigIntegerLeftNull() {
	AssertionFailedError e = null;
	try {
	    assertEquals("null", null, BigInteger.valueOf(1));
	} catch (AssertionFailedError a) {
	    e = a;
	}
	assertNotNull("exception", e);
    }
    public void testBigIntegerEquals() {
	assertEquals("one", BigInteger.valueOf(1), BigInteger.valueOf(1));
    }
    public void testBigIntegerDifferent() {
	AssertionFailedError e = null;
	try {
	    assertEquals("not equals", BigInteger.valueOf(1),
			 BigInteger.valueOf(2));
	} catch (AssertionFailedError a) {
	    e = a;
	}
	assertNotNull("exception", e);
    }
    public void testLongBigIntegerEquals() {
	// Test the plumming.
	assertEquals("one", 1, BigInteger.valueOf(1));
    }

    public void testStringArrayNull() {
	assertEquals("String array", (String[])null, (String[])null);
    }
    public void testStringArrayLeftNull() {
	AssertionFailedError e = null;
	try {
	    assertEquals("String array", (String[])null, new String[0]);
	} catch (AssertionFailedError a) {
	    e = a;
	}
	assertNotNull("exception", e);
    }
    public void testStringArrayRightNull() {
	AssertionFailedError e = null;
	try {
	    assertEquals("String array", new String[0], (String[])null);
	} catch (AssertionFailedError a) {
	    e = a;
	}
	assertNotNull("exception", e);
    }
    public void testStringArrayDifferentLength() {
	AssertionFailedError e = null;
	try {
	    assertEquals("String array", new String[0], new String[1]);
	} catch (AssertionFailedError a) {
	    e = a;
	}
	assertNotNull("exception", e);
    }
    public void testStringArrayDifferentContent() {
	AssertionFailedError e = null;
	try {
	    assertEquals("String array", new String[] { "1" }, new String[] { "2" });
	} catch (AssertionFailedError a) {
	    e = a;
	}
	assertNotNull("exception", e);
    }
    public void testStringArrayEquals() {
	assertEquals("empty String array", new String[0], new String[0]);
	assertEquals("full String array", new String[] { "1" }, new String[] { "1" });
    }
}
