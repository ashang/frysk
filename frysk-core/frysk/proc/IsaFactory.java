// This file is part of the program FRYSK.
//
// Copyright 2006, 2007, Red Hat Inc.
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

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Hashtable;

import frysk.Config;

import lib.elf.Elf;
import lib.elf.ElfCommand;
import lib.elf.ElfEHeader;
import lib.elf.ElfEMachine;
import lib.elf.ElfException;
import lib.elf.ElfFileException;

public class IsaFactory
{
  private static IsaFactory factory;
  static final Logger logger = Logger.getLogger("frysk.proc");
  private Hashtable isaHash;
    
  IsaFactory() 
  {
    isaHash = new Hashtable();
    isaHash.put(Integer.valueOf(ElfEMachine.EM_X86_64),
		LinuxX8664.isaSingleton());
    isaHash.put(Integer.valueOf(ElfEMachine.EM_PPC64),
		LinuxPPC64.isaSingleton());
    // XXX: This should not be looking at the configuration target
    // string when selecting the ISA.
    if (Config.getTargetCpuXXX().equals ("x86_64")) 
      {
	isaHash.put(Integer.valueOf(ElfEMachine.EM_386),
		    LinuxIa32On64.isaSingleton());
      }
    else
      {
	isaHash.put(Integer.valueOf(ElfEMachine.EM_386),
		    LinuxIa32.isaSingleton());
      }
    if (Config.getTargetCpuXXX().equals("powerpc64"))
      {
	isaHash.put(Integer.valueOf(ElfEMachine.EM_PPC),
		    LinuxPPC32On64.isaSingleton());
      }
    else
      {
	isaHash.put(Integer.valueOf(ElfEMachine.EM_PPC),
		    LinuxPPC.isaSingleton());
      }
  }
  
  static IsaFactory getFactory()
  {
    if (factory == null)
      factory = new IsaFactory ();
    return factory;
  }

  /**
   * Obtain ISA of task via pid.
   *
   * XXX: Instead of reading /proc/PID/exe, and relying on its
   * presence, this code should do something like read /proc/PID/auxv
   * and the processor memory to directly figure out what the ISA is.
   * There are already race coditions, such as during termination,
   * where /proc/PID/exe is no longer valid but the processes memory
   * is still readable.
   */
  private Isa getIsa(int pid, Task task) 
  {
    Elf elfFile;
    logger.log (Level.FINE, "{0} getIsa\n", this);
    
    try {
	elfFile = new Elf(pid, ElfCommand.ELF_C_READ);
    }
    catch (ElfFileException e) {
	throw new RuntimeException ("getting task's executable", e);
    }
    catch (ElfException e) {
	throw new RuntimeException ("getting task's executable", e);
    }

    try {
	ElfEHeader header = elfFile.getEHeader();
	Isa isa = (Isa)isaHash.get(Integer.valueOf(header.machine));
	if (isa == null)
	    // A "can't happen".
	    throw new RuntimeException ("Unknown machine type " + header.machine);
	return isa;
    }
    finally {
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
      switch (machineType)
	  {
	  case ElfEMachine.EM_386:
	      isa = LinuxIa32.isaSingleton();
	      break;
	  case ElfEMachine.EM_PPC:
	      isa = LinuxPPC.isaSingleton();
	      break;
	  default:
	      isa =  (Isa)isaHash.get(Integer.valueOf(machineType));
	  }
      return isa;
  }

  public  Isa getIsa(int pid) 
  {
    return getIsa(pid, null);
  }
  
  public Isa getIsa(Task task)
  {
    return getIsa(task.getTid(), task);
  }
  
}
