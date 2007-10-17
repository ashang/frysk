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

import java.io.File;
import java.util.Iterator;
import frysk.proc.FindProc;
import lib.dwfl.Elf;
import lib.dwfl.ElfCommand;

import frysk.event.EventLoop;
import frysk.proc.Host;
import frysk.proc.Proc;
import frysk.proc.ProcId;
import frysk.proc.TaskObserver.Attached;

public class LinuxExeHost extends Host {

    protected File exeFile = null;
    EventLoop eventLoop = null;
    Elf exeFileElf = null;
    private boolean hasRefreshed;
    
    public LinuxExeHost(EventLoop eventLoop, File exeFile)
    {
	this.exeFile = exeFile;
	this.eventLoop = eventLoop;
	try
	{
	    this.exeFileElf = new Elf (exeFile.getPath(), ElfCommand.ELF_C_READ);
	}
	catch (Exception e)
	{
	    throw new RuntimeException("ExeFile " + this.exeFile + " is "+ 
	    "not a valid ELF file.");
	}
	
	this.sendRefresh(true);
    }
    
    protected void sendCreateAttachedProc(String stdin, String stdout,
	    String stderr, String[] args, Attached attached) {

    }

    /**
     * sendRefresh refreshes the list of processes.
     * 
     * @param refreshAll is a boolean, true=refresh, false=not
     */
    protected void sendRefresh(boolean refreshAll) {	
	if (this.hasRefreshed)
	    return;
	// Iterate (build) the /proc tree, passing each found PID to
	// procChanges where it can update the /proc tree.
	// Changes individual process.
	Proc newProc = new LinuxExeProc(this, null ,new ProcId(0));
	newProc.getClass();
	
	for (Iterator i = procPool.values().iterator(); i.hasNext();)
	{
	    LinuxExeProc proc = (LinuxExeProc) i.next();
	    proc.sendRefresh();
	}
	this.hasRefreshed = true;
    }

    /**
     * sendRefresh refreshes a list of PIDs for this process, although
     * at this point in time for an executable none have been assigned.
     */
    protected void sendRefresh(ProcId procId, FindProc finder) {
	return;
    }

    /**
     * sendrecSelf sends a point to frysk back, but no need for this 
     * for an executable.
     */
    protected Proc sendrecSelf() {
	return null;
    }
    /**
     * finalize closes the file descriptor for the executable.
     */
    protected void finalize()
    {
	this.exeFileElf.close();
    }

}
