// This file is part of the program FRYSK.
//
// Copyright 2005, Red Hat Inc.
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

package frysk.sys.proc;

import junit.framework.TestCase;

/**
 * Test the AuxvBuilder against a predefined set of <tt>auxv</tt>
 * buffers.
 *
 * The test buffers are obtained by running, and than massaging, the
 * output from <tt>prog/print/auxv</tt>
 */
public class TestAuxv
    extends TestCase
{
    static class Auxiliary
    {
	Auxiliary (int type, long val)
	{
	    this.type = type;
	    this.val = val;
	}
	int type;
	long val;
    }

    /**
     * Verify that the unpacked auxv matches expected.
     */
    private void check (int wordSize, boolean bigEndian,
			Auxiliary[] vec, byte[] auxv)
    {
	class Builder
	    extends AuxvBuilder
	{
	    int wordSize;
	    boolean bigEndian;
	    Auxiliary[] vec;
	    Builder (int wordSize, boolean bigEndian,
		     Auxiliary[] vec)
	    {
		this.wordSize = wordSize;
		this.vec = vec;
		this.bigEndian = bigEndian;
	    }
	    public void buildBuffer (int wordSize, boolean bigEndian,
				     int length, byte[] auxv)
	    {
		assertEquals ("word size", this.wordSize, wordSize);
		assertEquals ("big endian", this.bigEndian, bigEndian);
		assertEquals ("number elements", this.vec.length, length);
	    }
	    int index = 0;
	    public void buildAuxiliary (int index, int type, long val)
	    {
		assertEquals ("type", type, this.vec[index].type);
		assertEquals ("val", val, this.vec[index].val);
		assertEquals ("index", this.index, index);
		this.index++;
	    }
	}
	Builder builder = new Builder (wordSize, bigEndian, vec);
	builder.construct (auxv);
	assertEquals (builder.index, vec.length);
    }

    /**
     * Check that an AUXV taken from an IA-32 (Intel(R) Pentium(R) M
     * processor 1.10GHz) machine can be parsed.
     */
    public void testIA32 ()
    {
	check (4, false,
	       new Auxiliary[] {
		   new Auxiliary (32, 7947264L),
		   new Auxiliary (33, 7946240L),
		   new Auxiliary (16, 2951345087L),
		   new Auxiliary (6, 4096L),
		   new Auxiliary (17, 100L),
		   new Auxiliary (3, 134512692L),
		   new Auxiliary (4, 32L),
		   new Auxiliary (5, 7L),
		   new Auxiliary (7, 0L),
		   new Auxiliary (8, 0L),
		   new Auxiliary (9, 134513504L),
		   new Auxiliary (11, 500L),
		   new Auxiliary (12, 500L),
		   new Auxiliary (13, 500L),
		   new Auxiliary (14, 500L),
		   new Auxiliary (23, 0L),
		   new Auxiliary (15, 3216152987L),
		   new Auxiliary (0, 0)
	       },
	       new byte[] {
		   32, 0, 0, 0, 0, 68, 121, 0,
		   33, 0, 0, 0, 0, 64, 121, 0,
		   16, 0, 0, 0, -65, -13, -23, -81,
		   6, 0, 0, 0, 0, 16, 0, 0,
		   17, 0, 0, 0, 100, 0, 0, 0,
		   3, 0, 0, 0, 52, -128, 4, 8,
		   4, 0, 0, 0, 32, 0, 0, 0,
		   5, 0, 0, 0, 7, 0, 0, 0,
		   7, 0, 0, 0, 0, 0, 0, 0,
		   8, 0, 0, 0, 0, 0, 0, 0,
		   9, 0, 0, 0, 96, -125, 4, 8,
		   11, 0, 0, 0, -12, 1, 0, 0,
		   12, 0, 0, 0, -12, 1, 0, 0,
		   13, 0, 0, 0, -12, 1, 0, 0,
		   14, 0, 0, 0, -12, 1, 0, 0,
		   23, 0, 0, 0, 0, 0, 0, 0,
		   15, 0, 0, 0, -101, -103, -78, -65,
		   0, 0, 0, 0, 0, 0, 0, 0
	       });
    }

    /**
     * Check that an AUXV taken from an AMD64 (AMD Opteron(tm)
     * Processor 846) can be parsed.
     */
    public void testAMD64 ()
    {
	check (8, false,
	       new Auxiliary[] {
		   new Auxiliary (16, 126614527L),
		   new Auxiliary (6, 4096L),
		   new Auxiliary (17, 100L),
		   new Auxiliary (3, 4194368L),
		   new Auxiliary (4, 56L),
		   new Auxiliary (5, 8L),
		   new Auxiliary (7, 0L),
		   new Auxiliary (8, 0L),
		   new Auxiliary (9, 4195536L),
		   new Auxiliary (11, 2548L),
		   new Auxiliary (12, 2548L),
		   new Auxiliary (13, 2553L),
		   new Auxiliary (14, 2553L),
		   new Auxiliary (23, 0L),
		   new Auxiliary (15, 140737481582809L),
		   new Auxiliary (0, 0)
	       },
	       new byte[] {
		   16, 0, 0, 0, 0, 0, 0, 0, -1, -5, -117, 7, 0, 0, 0, 0,
		   6, 0, 0, 0, 0, 0, 0, 0, 0, 16, 0, 0, 0, 0, 0, 0,
		   17, 0, 0, 0, 0, 0, 0, 0, 100, 0, 0, 0, 0, 0, 0, 0,
		   3, 0, 0, 0, 0, 0, 0, 0, 64, 0, 64, 0, 0, 0, 0, 0,
		   4, 0, 0, 0, 0, 0, 0, 0, 56, 0, 0, 0, 0, 0, 0, 0,
		   5, 0, 0, 0, 0, 0, 0, 0, 8, 0, 0, 0, 0, 0, 0, 0,
		   7, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
		   8, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
		   9, 0, 0, 0, 0, 0, 0, 0, -48, 4, 64, 0, 0, 0, 0, 0,
		   11, 0, 0, 0, 0, 0, 0, 0, -12, 9, 0, 0, 0, 0, 0, 0,
		   12, 0, 0, 0, 0, 0, 0, 0, -12, 9, 0, 0, 0, 0, 0, 0,
		   13, 0, 0, 0, 0, 0, 0, 0, -7, 9, 0, 0, 0, 0, 0, 0,
		   14, 0, 0, 0, 0, 0, 0, 0, -7, 9, 0, 0, 0, 0, 0, 0,
		   23, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
		   15, 0, 0, 0, 0, 0, 0, 0, -39, -88, -104, -1, -1, 127, 0, 0,
		   0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
	       });
    }

    /**
     * Check that the AUXV from an IA-64 (Itanium 2) can be parsed.
     */
    public void testIA64 ()
    {
	check (8, false,
	       new Auxiliary[] {
		   new Auxiliary (32, -6917529027641014688L
				  /*11529215046068536928*/),
		   new Auxiliary (33, -6917529027641081856L
				  /*11529215046068469760L*/),
		   new Auxiliary (16, 0L),
		   new Auxiliary (6, 16384L),
		   new Auxiliary (17, 1024L),
		   new Auxiliary (3, 4611686018427387968L),
		   new Auxiliary (4, 56L),
		   new Auxiliary (5, 8L),
		   new Auxiliary (7, 2305843009213693952L),
		   new Auxiliary (8, 0L),
		   new Auxiliary (9, 4611686018427389344L),
		   new Auxiliary (11, 2548L),
		   new Auxiliary (12, 2548L),
		   new Auxiliary (13, 2553L),
		   new Auxiliary (14, 2553L),
		   new Auxiliary (23, 0L),
		   new Auxiliary (0, 0L)
	       },
	       new byte[] {
		   32, 0, 0, 0, 0, 0, 0, 0, 96, 6, 1, 0, 0, 0, 0, -96,
		   33, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -96,
		   16, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
		   6, 0, 0, 0, 0, 0, 0, 0, 0, 64, 0, 0, 0, 0, 0, 0,
		   17, 0, 0, 0, 0, 0, 0, 0, 0, 4, 0, 0, 0, 0, 0, 0,
		   3, 0, 0, 0, 0, 0, 0, 0, 64, 0, 0, 0, 0, 0, 0, 64,
		   4, 0, 0, 0, 0, 0, 0, 0, 56, 0, 0, 0, 0, 0, 0, 0,
		   5, 0, 0, 0, 0, 0, 0, 0, 8, 0, 0, 0, 0, 0, 0, 0,
		   7, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 32,
		   8, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
		   9, 0, 0, 0, 0, 0, 0, 0, -96, 5, 0, 0, 0, 0, 0, 64,
		   11, 0, 0, 0, 0, 0, 0, 0, -12, 9, 0, 0, 0, 0, 0, 0,
		   12, 0, 0, 0, 0, 0, 0, 0, -12, 9, 0, 0, 0, 0, 0, 0,
		   13, 0, 0, 0, 0, 0, 0, 0, -7, 9, 0, 0, 0, 0, 0, 0,
		   14, 0, 0, 0, 0, 0, 0, 0, -7, 9, 0, 0, 0, 0, 0, 0,
		   23, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
		   0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
	       });
    }

    /**
     * Check that an AUXV taken from a 32-bit PowerPC program running
     * on a 64-bit PowerPC machine (PPC970, altivec supported) can be
     * parsed.
     */
    public void testPPC32 ()
    {
	check (4, true,
	       new Auxiliary [] {
		   new Auxiliary (22, 22L),
		   new Auxiliary (22, 22L),
		   new Auxiliary (19, 128L),
		   new Auxiliary (20, 128L),
		   new Auxiliary (21, 0L),
		   new Auxiliary (33, 1048576L),
		   new Auxiliary (16, 3690987520L),
		   new Auxiliary (6, 4096L),
		   new Auxiliary (17, 100L),
		   new Auxiliary (3, 268435508L),
		   new Auxiliary (4, 32L),
		   new Auxiliary (5, 7L),
		   new Auxiliary (7, 0L),
		   new Auxiliary (8, 0L),
		   new Auxiliary (9, 268436304L),
		   new Auxiliary (11, 2548L),
		   new Auxiliary (12, 2548L),
		   new Auxiliary (13, 2553L),
		   new Auxiliary (14, 2553L),
		   new Auxiliary (23, 0L),
		   new Auxiliary (0, 0L)
	       },
	       new byte[] {
		   0, 0, 0, 22, 0, 0, 0, 22,
		   0, 0, 0, 22, 0, 0, 0, 22,
		   0, 0, 0, 19, 0, 0, 0, -128,
		   0, 0, 0, 20, 0, 0, 0, -128,
		   0, 0, 0, 21, 0, 0, 0, 0,
		   0, 0, 0, 33, 0, 16, 0, 0,
		   0, 0, 0, 16, -36, 0, 0, 0,
		   0, 0, 0, 6, 0, 0, 16, 0,
		   0, 0, 0, 17, 0, 0, 0, 100,
		   0, 0, 0, 3, 16, 0, 0, 52,
		   0, 0, 0, 4, 0, 0, 0, 32,
		   0, 0, 0, 5, 0, 0, 0, 7,
		   0, 0, 0, 7, 0, 0, 0, 0,
		   0, 0, 0, 8, 0, 0, 0, 0,
		   0, 0, 0, 9, 16, 0, 3, 80,
		   0, 0, 0, 11, 0, 0, 9, -12,
		   0, 0, 0, 12, 0, 0, 9, -12,
		   0, 0, 0, 13, 0, 0, 9, -7,
		   0, 0, 0, 14, 0, 0, 9, -7,
		   0, 0, 0, 23, 0, 0, 0, 0,
		   0, 0, 0, 0, 0, 0, 0, 0
	       });
    }

    /**
     * Check that an AUXV taken from a 64-bit PowerPC program running
     * on a 64-bit PowerPC machine (PPC970, altivec supported) can be
     * parsed.
     */
    public void testPPC64 ()
    {
	check (8, true,
	       new Auxiliary [] {
		   new Auxiliary (22, 22L),
		   new Auxiliary (22, 22L),
		   new Auxiliary (19, 128L),
		   new Auxiliary (20, 128L),
		   new Auxiliary (21, 0L),
		   new Auxiliary (33, 1048576L),
		   new Auxiliary (16, 3690987520L),
		   new Auxiliary (6, 4096L),
		   new Auxiliary (17, 100L),
		   new Auxiliary (3, 268435520L),
		   new Auxiliary (4, 56L),
		   new Auxiliary (5, 7L),
		   new Auxiliary (7, 0L),
		   new Auxiliary (8, 0L),
		   new Auxiliary (9, 268503768L),
		   new Auxiliary (11, 2548L),
		   new Auxiliary (12, 2548L),
		   new Auxiliary (13, 2553L),
		   new Auxiliary (14, 2553L),
		   new Auxiliary (23, 0L),
		   new Auxiliary (0, 0L)
	       },
	       new byte[] {
		   0, 0, 0, 0, 0, 0, 0, 22, 0, 0, 0, 0, 0, 0, 0, 22,
		   0, 0, 0, 0, 0, 0, 0, 22, 0, 0, 0, 0, 0, 0, 0, 22,
		   0, 0, 0, 0, 0, 0, 0, 19, 0, 0, 0, 0, 0, 0, 0, -128,
		   0, 0, 0, 0, 0, 0, 0, 20, 0, 0, 0, 0, 0, 0, 0, -128,
		   0, 0, 0, 0, 0, 0, 0, 21, 0, 0, 0, 0, 0, 0, 0, 0,
		   0, 0, 0, 0, 0, 0, 0, 33, 0, 0, 0, 0, 0, 16, 0, 0,
		   0, 0, 0, 0, 0, 0, 0, 16, 0, 0, 0, 0, -36, 0, 0, 0,
		   0, 0, 0, 0, 0, 0, 0, 6, 0, 0, 0, 0, 0, 0, 16, 0,
		   0, 0, 0, 0, 0, 0, 0, 17, 0, 0, 0, 0, 0, 0, 0, 100,
		   0, 0, 0, 0, 0, 0, 0, 3, 0, 0, 0, 0, 16, 0, 0, 64,
		   0, 0, 0, 0, 0, 0, 0, 4, 0, 0, 0, 0, 0, 0, 0, 56,
		   0, 0, 0, 0, 0, 0, 0, 5, 0, 0, 0, 0, 0, 0, 0, 7,
		   0, 0, 0, 0, 0, 0, 0, 7, 0, 0, 0, 0, 0, 0, 0, 0,
		   0, 0, 0, 0, 0, 0, 0, 8, 0, 0, 0, 0, 0, 0, 0, 0,
		   0, 0, 0, 0, 0, 0, 0, 9, 0, 0, 0, 0, 16, 1, 10, -40,
		   0, 0, 0, 0, 0, 0, 0, 11, 0, 0, 0, 0, 0, 0, 9, -12,
		   0, 0, 0, 0, 0, 0, 0, 12, 0, 0, 0, 0, 0, 0, 9, -12,
		   0, 0, 0, 0, 0, 0, 0, 13, 0, 0, 0, 0, 0, 0, 9, -7,
		   0, 0, 0, 0, 0, 0, 0, 14, 0, 0, 0, 0, 0, 0, 9, -7,
		   0, 0, 0, 0, 0, 0, 0, 23, 0, 0, 0, 0, 0, 0, 0, 0,
		   0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
	       });
    }
}
