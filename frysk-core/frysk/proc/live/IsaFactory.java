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

package frysk.proc.live;

import java.util.Hashtable;
import java.io.File;
import lib.dwfl.Elf;
import lib.dwfl.ElfCommand;
import lib.dwfl.ElfEHeader;
import lib.dwfl.ElfEMachine;
import frysk.sys.ProcessIdentifier;
import frysk.rsl.Log;

public class IsaFactory {
    private static final Log fine = Log.fine(IsaFactory.class);

  private static IsaFactory factory;
  private Hashtable isaHash;
    
    IsaFactory() {
	isaHash = new Hashtable();
	isaHash.put(Integer.valueOf(ElfEMachine.EM_X86_64),
		    LinuxX8664.isaSingleton());
	isaHash.put(Integer.valueOf(ElfEMachine.EM_PPC64),
		    LinuxPPC64.isaSingleton());
	isaHash.put(Integer.valueOf(ElfEMachine.EM_386),
		    LinuxIA32.isaSingleton());
	isaHash.put(Integer.valueOf(ElfEMachine.EM_PPC),
		    LinuxPPC32.isaSingleton());
    }
  
  public static IsaFactory getSingleton()
  {
    if (factory == null)
      factory = new IsaFactory ();
    return factory;
  }

    /**
     * Obtain ISA of task via pid.
     *
     * XXX: Instead of reading /proc/PID/exe, and relying on its
     * presence, this code should do something like read
     * /proc/PID/auxv and the processor memory to directly figure out
     * what the ISA is.  There are already race coditions, such as
     * during termination, where /proc/PID/exe is no longer valid but
     * the processes memory is still readable.
     */
    Isa getIsa(ProcessIdentifier pid) {
	fine.log(this, "getIsa", pid);
	
	// FIXME: This should use task.proc.getExeFile().getSysRootedPath().  Only that
	// causes wierd failures; take a rain-check :-(
	File exe = new File("/proc/" + pid + "/exe");
	Elf elfFile = new Elf(exe, ElfCommand.ELF_C_READ);
	try {
	    ElfEHeader header = elfFile.getEHeader();
	    Isa isa = (Isa)isaHash.get(Integer.valueOf(header.machine));
	    if (isa == null)
		// A "can't happen".
		throw new RuntimeException ("Unknown machine type " + header.machine);
	    return isa;
	} finally {
	    elfFile.close();
	}
    }

  /**
   * Obtain ISA via ElfMachine Type.
   */
  public Isa getIsaForCoreFile(int machineType) 
  {

      // The lookup for corefile should return always the architecture
      // ISA that the core file represents, not 32on64.The isaHash lookup
      // tries to be a bit clever. Clobber it here for two cases.
      // XXX: This needs to be made for elegant.
      Isa isa = null;
      switch (machineType) {
      case ElfEMachine.EM_386:
	  isa = LinuxIA32.isaSingleton();
	  break;
      case ElfEMachine.EM_PPC:
	  isa = LinuxPPC32.isaSingleton();
	  break;
      default:
	  isa =  (Isa)isaHash.get(Integer.valueOf(machineType));
      }
      return isa;
  }
}
