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

import java.util.logging.Level;
import java.util.logging.Logger;

import lib.elf.Elf;
import lib.elf.ElfCommand;
import lib.elf.ElfEHeader;
import lib.elf.ElfEMachine;
import lib.elf.ElfException;
import lib.elf.ElfFileException;

public class IsaFactory
{
  private static IsaFactory factory;
    static final Logger logger = Logger.getLogger ("frysk");//.proc");

  static IsaFactory getFactory()
  {
    if (factory == null)
      factory = new IsaFactory ();
    return factory;
  }

  /** Obtain ISA of task via pid. 
   */
  private Isa getIsa(int pid, Task task) 
    throws TaskException
  {
    Elf elfFile;
    logger.log (Level.FINE, "{0} getIsa\n", this);
    
    try 
      {
	elfFile = new Elf(pid, ElfCommand.ELF_C_READ);
      }
    catch (ElfFileException e) 
      {
	throw new TaskFileException(e.getMessage(), task, e.getFileName(), e);
      }
    catch (ElfException e) 
      {
	throw new TaskException("getting task's executable", e);
      }
    try
      {
	
	ElfEHeader header = elfFile.getEHeader();

	switch (header.machine) 
	  {
	  case ElfEMachine.EM_386:
	    return LinuxIa32.isaSingleton ();
	  case ElfEMachine.EM_PPC:
	    return LinuxPPC.isaSingleton ();
	  case ElfEMachine.EM_PPC64:
	    return LinuxPPC64.isaSingleton ();
	  case ElfEMachine.EM_X86_64:
	    return LinuxEMT64.isaSingleton ();
	  default: 
	    throw new TaskException("Unknown machine type " + header.machine);
	  }
      }
    finally 
      {
	elfFile.close();
      }
  }

  public  Isa getIsa(int pid) 
    throws TaskException 
  {
    return getIsa(pid, null);
  }
  
  public Isa getIsa(Task task)
    throws TaskException
  {
    return getIsa(task.getTid(), task);
  }
  
}
