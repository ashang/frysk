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

package lib.unwind;

import frysk.testbed.TearDownProcess;

import frysk.junit.TestCase;

public class TestUnwind
    extends TestCase
{

    public void tearDown ()
    {
	TearDownProcess.tearDown ();
    }
  
    class TestAddressSpace extends AddressSpace {
	TestAddressSpace(Unwind unwind, ByteOrder byteOrder) {
	    super(unwind, byteOrder);
	}
	public int accessReg(Number regnum, byte[] fpvalp, boolean write) {
	    return 0;
	}
	public int accessMem (long addr, byte[] valp, boolean write) {
	    return 0;
	}
	public long getReg(Number regnum) {
	    return 0;
	}
	public void setReg(Number regnum, long regval) {
	}
	public int findProcInfo(long ip, boolean needUnwindInfo,
				ProcInfo procInfo) {
	    return -1;
	}
	public int getDynInfoListAddr (byte[] dilap) {
	    return 0;
	}
	public void putUnwindInfo (ProcInfo procInfo) {
	}
	public int resume (Cursor cursor) {
	    return 0;
	}
    }


    public void testCreateAddress() {  
	AddressSpace addr = new TestAddressSpace(new UnwindX8664(),
						 ByteOrder.DEFAULT);
	assertNotNull("AddressSpace should not be null", addr);
    }
  
    public void testCreateCursor() {
	Unwind unwind = new UnwindX8664();
	AddressSpace addr = new TestAddressSpace(unwind, ByteOrder.DEFAULT);
	Cursor cursor = addr.createCursor();
	assertNotNull("Cursor should not be null", cursor);
    }
}
