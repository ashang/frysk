// This file is part of the program FRYSK.
//
// Copyright 2007 Red Hat Inc.
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
// modification, you must delete this exception statement from your// version and license this file solely under the GPL without
// exception.

package frysk.proc;

import frysk.event.Event;
import frysk.event.EventLoop;
import java.util.LinkedList;
import java.util.List;
import java.io.File;
import java.util.Iterator;

import lib.elf.Elf;
import lib.elf.ElfCommand;
import lib.elf.ElfData;
import lib.elf.ElfEHeader;
import lib.elf.ElfPHeader;
import lib.elf.ElfPrpsinfo;

public class LinuxCoreFileHost 
  extends Host 
{

  File coreFile;
  Elf corefileElf;
  EventLoop eventLoop;

  public LinuxCoreFileHost(EventLoop eventLoop, File coreFile)
  {
    this.coreFile = coreFile;
    this.eventLoop = eventLoop;

    try
      {
        this.corefileElf = new Elf (coreFile.getPath(), ElfCommand.ELF_C_READ);
      }
    catch (Exception e)
      {
        throw new RuntimeException("Corefile " + this.coreFile + " is "+ 
				   "not a valid ELF core file.");
      }
  }

  /**
   * The Constructor in Host.java needs a starting state.
   * As Host is abstract and cannot return a state specific
   * to its subclass, return here in the subclass
   */
  protected HostState getInitialState ()
  {
    return LinuxCoreFileHostState.initial (this);
  }
  
  void sendRefresh(boolean refreshAll) 
  {

    // Iterate (build) the /proc tree, passing each found PID to
    // procChanges where it can update the /proc tree.
    new DeconstructCoreFile(this.corefileElf);
    if (refreshAll)
      {
        // Changes individual process.
        for (Iterator i = procPool.values().iterator(); i.hasNext();)
          {
            LinuxCoreFileProc proc = (LinuxCoreFileProc) i.next();
            proc.sendRefresh();
          }
      }

  }

  void sendRefresh (final ProcId procId, final FindProc finder)
  {

    // Core files nevers never change 
    if (!(procPool.containsKey(procId)))
      {
        eventLoop.add(new Event()
        {
          public void execute ()
          {
            finder.procNotFound(procId, new RuntimeException(
                                                             "Couldn't find the proc"
                                                                 + procId));
          }
        });
        return;
      }

    
    LinuxCoreFileProc proc = (LinuxCoreFileProc) getProc(procId);
    proc.sendRefresh();
    
    eventLoop.add(new Event()
    {

      public void execute ()
      {
        finder.procFound(procId);
      }
    });

  } 


  void sendCreateAttachedProc(String stdin, String stdout,
			      String stderr, String[] args,
			      TaskObserver.Attached attached)
  {
  }


  protected Proc sendrecSelf() 
  {
    return null;
  }


  private class DeconstructCoreFile
  {
    List addedProcs = new LinkedList();
    //HashMap removedProcs = (HashMap) ((HashMap) procPool).clone();
    Elf coreFileElf;
    ElfData noteData = null;

    DeconstructCoreFile(Elf coreFileElf)
    {
      this.coreFileElf =  coreFileElf;
      ElfEHeader eHeader = this.coreFileElf.getEHeader();
      
      // Get number of program header entries.
      long phSize = eHeader.phnum;
      for (int i=0; i<phSize; i++)
	{
	  // Test if pheader is of types notes..
	  ElfPHeader pHeader = coreFileElf.getPHeader(i);
	  if (pHeader.type == ElfPHeader.PTYPE_NOTE)
	    {
	      // if so, copy, break an leave.
	      noteData = coreFileElf.getRawData(pHeader.offset,pHeader.filesz);
	      break;
	    }
	}

      if (noteData != null)
	update(noteData);
    }

    Proc update (ElfData proc_pid) 
    {
      final ElfPrpsinfo coreProc = ElfPrpsinfo.decode(proc_pid);
      final ProcId procId = new ProcId(coreProc.getPrPid());
      // Currently there can only be one process per core file.
      // What happens when we have two core files denoting the same
      // process/pid? Leave the test here for now.
   
      Proc proc = (Proc) procPool.get(procId);
      if (proc == null)
	{
	  // core file processes have no parents as thy are captured
	  // in isolation, and reconstructed.
	  proc = new LinuxCoreFileProc(proc_pid,LinuxCoreFileHost.this,procId);
	}

      addedProcs.add(proc);

      return proc;
    }
      
  }

  protected void finalize () throws Throwable
  {
    corefileElf = null;
  }
}
