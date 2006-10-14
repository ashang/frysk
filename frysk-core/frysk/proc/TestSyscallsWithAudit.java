// This file is part of the program FRYSK.
//
// Copyright 2006, Red Hat Inc.
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

package frysk.proc;

import frysk.sys.AuditLibs;

public class TestSyscallsWithAudit
  extends TestLib
{
  
  public void testLinuxIa32()
  {
    int machine = AuditLibs.MACH_X86;
    Isa isa = LinuxIa32.isaSingleton();
    syscallTest(machine, isa);
  }

  public void testLinuxPPC()
  {
    int machine = AuditLibs.MACH_PPC;
    Isa isa = LinuxPPC.isaSingleton();
    syscallTest(machine, isa);
  }

  public void testLinuxPPC64()
  {
    int machine = AuditLibs.MACH_PPC64;
    Isa isa = LinuxPPC64.isaSingleton();
    syscallTest(machine, isa);
  }

  public void testLinuxX86_64()
  {
    int machine = AuditLibs.MACH_86_64;
    Isa isa = LinuxX8664.isaSingleton();
    syscallTest(machine, isa);
  }

  private void syscallTest(int machine, Isa isa)
  {
    Syscall[] syscallList = isa.getSyscallList();
    int highestNum = 0;

    // We assume there are at most this many syscall numbers
    int MAX_SYSCALL_NUM = 1024;
    for (int i = 0; i < MAX_SYSCALL_NUM; i++)
      {
	String auditName = AuditLibs.syscallToName(i, machine);
	if (auditName != null)
	  {
	    highestNum = i;
	    int auditNum = AuditLibs.nameToSyscall(auditName, machine);
	    // XXX There are a couple of syscalls with the same name...
	    // Below we test for auditNum, which is the lowest number.
	    // assertEquals("auditlib sanity", i, auditNum);

	    Syscall syscall = syscallList[i];
	    String fryskName = syscall.getName();
	    int fryskNum = syscall.getNumber();
	    
	    assertEquals("number", i, fryskNum);
	    assertEquals("name (" + i + ")", auditName, fryskName);

	    Syscall syscallByName = isa.syscallByName(auditName);
	    // XXX There are a couple of syscalls with the same name
	    // Below we test for auditNum, not i.
	    // assertEquals("byName", syscall, syscallByName);
	    assertEquals("byName-name (" + i + ")",
			 auditName, syscallByName.getName());
	    assertEquals("byName-number",
			 auditNum, syscallByName.getNumber());
	  }
	else
	  {
	    if (i < syscallList.length)
	      {
		Syscall syscall = syscallList[i];
		int fryskNum = syscall.getNumber();
		assertEquals("number", i, fryskNum);

		// Unfortunately auditlib doesn't seem to know all the names.
		//String fryskName = syscall.getName();
		//assertEquals("no-name", "<" + i + ">", fryskName);
	      }
	  }
      }

    // Extra sanity check of MAX_SYSCALL_NUM assumption.
    assertNull("MAX_SYSCALL_NUM", AuditLibs.syscallToName(MAX_SYSCALL_NUM,
							  machine));

    // We should have names up to the highest number auditlib knows about.
    assertEquals("max-syscall-num", highestNum, syscallList.length - 1);
  }
}
