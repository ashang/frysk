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

import frysk.event.EventLoop;
import java.util.LinkedList;
import java.util.List;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import lib.dwfl.Elf;
import lib.dwfl.ElfCommand;
import lib.dwfl.ElfData;
import lib.dwfl.ElfEHeader;
import lib.dwfl.ElfPHeader;
import lib.dwfl.ElfPrpsinfo;
import frysk.proc.Proc;
import frysk.proc.ProcId;

public class LinuxCoreHost extends DeadHost {

    CorefileStatus status = new CorefileStatus();

    boolean hasRefreshed = false;

    boolean exeSetToNull = false;

    protected File coreFile = null;

    protected File exeFile = null;

    Elf corefileElf;

    EventLoop eventLoop;

    private LinuxCoreHost(EventLoop eventLoop, File coreFile, boolean doRefresh) {

	try {
	    this.coreFile = coreFile.getCanonicalFile();
	} catch (IOException e) {
	    throw new RuntimeException(e);
	}
	this.eventLoop = eventLoop;
	try {
	    this.corefileElf = new Elf(coreFile.getPath(),
				       ElfCommand.ELF_C_READ);
	} catch (Exception e) {
	    throw new RuntimeException("Corefile " + this.coreFile + " is "
				       + "not a valid ELF core file.");
	}

	if ((corefileElf.getEHeader() == null) || 
	    (corefileElf.getEHeader().type != ElfEHeader.PHEADER_ET_CORE)) {
	    this.corefileElf.close();
	    throw new RuntimeException("'" + this.getName()
				       + "' is not a corefile.");
	}

	if (doRefresh)
	    this.sendRefresh();
    }

    public LinuxCoreHost(EventLoop eventLoop, File coreFile) {
	this(eventLoop, coreFile, true);
    }

    public LinuxCoreHost(EventLoop eventLoop, File coreFile, File exeFile) {
	this(eventLoop, coreFile, false);
	if (exeFile == null)
	    exeSetToNull = true;

	if (exeSetToNull == false)
	    if (exeFile.canRead() && exeFile.exists()) {
		try {
		    this.exeFile = exeFile.getCanonicalFile();
		} catch (IOException e) {
		    status.hasExe = false;
		    status.hasExeProblem = true;
		}
		status.hasExe = true;
		status.hasExeProblem = false;
	    } else {
		status.hasExe = false;
		status.hasExeProblem = true;
		status.message = "The user provided executable: "
		    + exeFile.getName() + " could not be accessed";
	    }

	this.sendRefresh();
    }

    public CorefileStatus getStatus() {
	return status;
    }

    private void sendRefresh() {
	if (this.hasRefreshed)
	    return;
	// Iterate (build) the /proc tree, passing each found PID to
	// procChanges where it can update the /proc tree.
	new DeconstructCoreFile(this.corefileElf);
	// Changes individual process.
	for (Iterator i = procPool.values().iterator(); i.hasNext();) {
	    LinuxCoreProc proc = (LinuxCoreProc) i.next();
	    proc.sendRefresh();
	}
	this.hasRefreshed = true;
    }

    private class DeconstructCoreFile {
	List addedProcs = new LinkedList();

	//HashMap removedProcs = (HashMap) ((HashMap) procPool).clone();
	Elf coreFileElf;

	ElfData noteData = null;

	DeconstructCoreFile(Elf coreFileElf) {
	    this.coreFileElf = coreFileElf;
	    status.coreName = coreFile.getName();
	    ElfEHeader eHeader = this.coreFileElf.getEHeader();

	    // Get number of program header entries.
	    long phSize = eHeader.phnum;
	    for (int i = 0; i < phSize; i++) {
		// Test if pheader is of types notes..
		ElfPHeader pHeader = coreFileElf.getPHeader(i);
		if (pHeader.type == ElfPHeader.PTYPE_NOTE) {
		    // if so, copy, break an leave.
		    noteData = coreFileElf.getRawData(pHeader.offset,
						      pHeader.filesz);
		    break;
		}
	    }

	    if (noteData != null)
		update(noteData);
	}

	Proc update(ElfData proc_pid) {
	    final ElfPrpsinfo coreProc = ElfPrpsinfo.decode(proc_pid);
	    final ProcId procId = new ProcId(coreProc.getPrPid());
	    // Currently there can only be one process per core file.
	    // What happens when we have two core files denoting the same
	    // process/pid? Leave the test here for now.

	    Proc proc = (Proc) procPool.get(procId);
	    if (proc == null) {
		// core file processes have no parents as thy are captured
		// in isolation, and reconstructed.
		proc = new LinuxCoreProc(proc_pid, LinuxCoreHost.this, procId);
	    }

	    addedProcs.add(proc);

	    if (exeFile == null)
		status.hasExe = false;
	    else {
		status.hasExe = true;
		status.exeName = exeFile.getName();
	    }
	    return proc;
	}

    }

    protected void finalize() throws Throwable {
	corefileElf = null;
    }
}
