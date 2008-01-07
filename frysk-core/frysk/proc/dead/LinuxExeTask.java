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
// modification, you must delete this exception statement from your
// version and license this file solely under the GPL without
// exception.

package frysk.proc.dead;

import inua.eio.ArrayByteBuffer;
import inua.eio.ByteBuffer;
import frysk.bank.RegisterBanks;
import frysk.proc.TaskId;
import frysk.isa.ISA;

import lib.dwfl.*;

public class LinuxExeTask extends DeadTask {
    private final long pc;
    LinuxExeProc proc = null;
    TaskId id = null;

    protected LinuxExeTask(LinuxExeProc proc, TaskId id) {
	super(proc, id);
	this.proc = proc;
	this.id = id;
	// Compute a Fake PC.  XXX should be done in Proc instead of
	// creating Elf object in the Task itself.
	Elf e = null;
	long pc;
	try {
	    e = new Elf(getProc().getExe(), ElfCommand.ELF_C_READ);
	    ElfEHeader h = e.getEHeader();
	    pc = h.entry;
	} catch (ElfException ee) {
	    // Nice try, just give up.
	    pc = 0;
	} finally {
	    if (e != null)
		e.close();
	}
	this.pc = pc;
    }
  
    public long getPC() {
	return pc;
    }

  protected ISA sendrecISA() {
      return ((LinuxExeProc)getProc()).sendrecISA();
  }

  protected ByteBuffer sendrecMemory () {
    return this.proc.sendrecMemory();
  }
  
  protected RegisterBanks sendrecRegisterBanks() {
      ByteBuffer[] bankBuffers = new ByteBuffer[4];
      // Create an empty page
      byte[] emptyBuffer = new byte[4096];
      for (int i = 0; i < emptyBuffer.length; i++)
	  emptyBuffer[i] = 0;
      bankBuffers[0] = new ArrayByteBuffer(emptyBuffer);
      bankBuffers[1] = new ArrayByteBuffer(emptyBuffer);
      bankBuffers[2] = new ArrayByteBuffer(emptyBuffer);
      bankBuffers[3] = new ArrayByteBuffer(emptyBuffer);
      return CorefileRegisterBanksFactory.create
      	  (getISA(), bankBuffers);
  }
}
