// This file is part of the program FRYSK.
//
// Copyright 2006, 2007, 2008, Red Hat Inc.
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

package frysk.isa.syscalls;

import frysk.sys.AuditLibs;
import frysk.testbed.TestLib;
import frysk.isa.ISA;
import frysk.isa.ISAMap;
import frysk.testbed.IsaTestbed;

public class TestSyscallsWithAudit extends TestLib {
  
    private static final ISAMap machines = new ISAMap("syscall machines")
	.put(ISA.IA32, new Integer(AuditLibs.MACH_X86))
	.put(ISA.PPC32BE, new Integer(AuditLibs.MACH_PPC))
	.put(ISA.PPC64BE, new Integer(AuditLibs.MACH_PPC64))
	.put(ISA.X8664, new Integer(AuditLibs.MACH_86_64))
	;

    private void syscallTest(ISA isa) {
	int machine = ((Integer) machines.get(isa)).intValue();
	SyscallTable syscallTable
	    = SyscallTableFactory.getSyscallTable(isa);
	
	// Assume there are at most this many syscall numbers; check
	// it.
	int MAX_SYSCALL_NUM = 1024;
	assertNull("MAX_SYSCALL_NUM", AuditLibs.syscallToName(MAX_SYSCALL_NUM,
							      machine));

	for (int i = 0; i < MAX_SYSCALL_NUM; i++) {
	    String auditName = AuditLibs.syscallToName(i, machine);
	    if (auditName != null) {
		// Check that there's a corresponding entry in frysk's
		// by-number database.
		Syscall byNum = syscallTable.getSyscall(i);
		assertEquals("byNum-number", i, byNum.getNumber());
		assertEquals("byNum-name (" + i + ")", auditName,
			     byNum.getName());
		// Check that there's a corresponding entry in frysk's
		// by-name database.  There are a couple of syscalls
		// with the same name.  Test for auditNum, which is
		// the lowest number.
		int auditNum = AuditLibs.nameToSyscall(auditName, machine);
		Syscall byName = syscallTable.getSyscall(auditName);
		assertEquals("byName-number", auditNum, byName.getNumber());
		assertEquals("byName-name (" + i + ")", auditName,
			     byName.getName());
	    } else {
		// If the syscall is unknown, unknown syscall is
		// returned.
		Syscall syscall = syscallTable.getSyscall(i);
		assertEquals("number", i, syscall.getNumber());
		// Unfortunately auditlib doesn't seem to know all the
		//names.  String fryskName = syscall.getName();
		//assertEquals("no-name", "<" + i + ">", fryskName);
	    }
	}
    }


    public void testLinuxIA32() {
	syscallTest(ISA.IA32);
    }
    
    public void testLinuxPPC32() {
	syscallTest(ISA.PPC32BE);
    }
    
    public void testLinuxPPC64() {
	syscallTest(ISA.PPC64BE);
    }
    
    public void testLinuxX8664() {
	syscallTest(ISA.X8664);
    }

    public void testHost() {
	syscallTest(IsaTestbed.getISA());
    }
}
