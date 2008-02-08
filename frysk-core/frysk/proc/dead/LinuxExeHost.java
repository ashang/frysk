// This file is part of the program FRYSK.
//
// Copyright 2007, 2008 Red Hat Inc.
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

package frysk.proc.dead;

import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.LinkedList;

import lib.dwfl.Elf;
import lib.dwfl.ElfCommand;
import lib.dwfl.ElfData;
import lib.dwfl.ElfEHeader;
import lib.dwfl.ElfPHeader;

import frysk.event.EventLoop;
import frysk.proc.Proc;
import frysk.proc.ProcId;

public class LinuxExeHost extends DeadHost {

    protected File exeFile = null;
    EventLoop eventLoop = null;
    Elf exeFileElf = null;
    
    public LinuxExeHost(EventLoop eventLoop, File exeFile) {
	this.exeFile = exeFile;
	this.eventLoop = eventLoop;
	try {
	    this.exeFileElf = new Elf (exeFile.getPath(), ElfCommand.ELF_C_READ);
	} catch (Exception e)	{
	    throw new RuntimeException("ExeFile " + this.exeFile + " is "+ 
	    "not a valid ELF file.");
	}
	// Iterate (build) the /proc tree, passing each found PID to
	// procChanges where it can update the /proc tree.
	// Changes individual process.
	new DeconstructExeFile(this.exeFileElf);
	for (Iterator i = procPool.values().iterator(); i.hasNext();) {
	    LinuxExeProc proc = (LinuxExeProc) i.next();
	    proc.sendRefresh();
	}
    }

    private class DeconstructExeFile
    {
      List addedProcs = new LinkedList();
      Elf exeFileElf;
      ElfData noteData = null;

      DeconstructExeFile(Elf exeFileElf)
      {
        this.exeFileElf =  exeFileElf;
        ElfEHeader eHeader = this.exeFileElf.getEHeader();
        
        // Get number of program header entries.
        long phSize = eHeader.phnum;
        for (int i=0; i<phSize; i++)
  	{
  	  // Test if pheader is of types notes..
  	  ElfPHeader pHeader = exeFileElf.getPHeader(i);
  	  if (pHeader.type == ElfPHeader.PTYPE_NOTE)
  	    {
  	      // if so, copy, break and leave.
  	      noteData = exeFileElf.getRawData(pHeader.offset,pHeader.filesz);
  	      break;
  	    }
  	}

        if (noteData != null)
  	update(noteData);
      }

      Proc update (ElfData proc_pid) 
      {
        final ProcId procId = new ProcId(0);
        // Currently there can only be one process per core file.
        // What happens when we have two core files denoting the same
        // process/pid? Leave the test here for now.
     
        Proc proc = (Proc) procPool.get(procId);
        if (proc == null)
  	{
  	  // executable file processes have no parents as thy are captured
  	  // in isolation, and reconstructed.
  	  proc = new LinuxExeProc(proc_pid,LinuxExeHost.this,procId);
  	}

        addedProcs.add(proc);

        return proc;
      }
    }

    public String getName() {
	return exeFile.getName();
    }

    /**
     * finalize closes the file descriptor for the executable.
     */
    protected void finalize()
    {
	this.exeFileElf.close();
    }

}
