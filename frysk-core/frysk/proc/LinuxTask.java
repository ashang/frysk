// This file is part of the program FRYSK.
//
// Copyright 2005, Red Hat Inc.
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

import frysk.sys.Errno;
import frysk.sys.Ptrace;
import frysk.sys.Sig;
import frysk.sys.Signal;
import inua.eio.ByteBuffer;
import inua.eio.PtraceByteBuffer;

/**
 * Linux implementation of Task.
 */

public class LinuxTask
    extends Task
{
    protected int pid;
    protected boolean ptraceAttached;
    
    private void addMemoryAndRegisters ()
    {
	memory = new PtraceByteBuffer (pid, PtraceByteBuffer.Area.DATA);
	registerBank = new ByteBuffer[] {
	    new PtraceByteBuffer (pid, PtraceByteBuffer.Area.USR)
	};
    }
    
    LinuxTask (Proc process, TaskId id)
    {
	super (process, id);
	this.pid = id.hashCode ();
	addMemoryAndRegisters ();
    }

    LinuxTask (Proc process, TaskId id,
	  boolean runnable)
    {
	super (process, id, runnable);
	this.pid = id.hashCode ();
	addMemoryAndRegisters ();
    }

    private void appendZombiedEvent ()
    {
	Manager.eventLoop.appendEvent (new TaskEvent.Zombied (this.id));
    }

    protected void sendContinue (int sig)
    {
	try {
	    if (traceSyscall)
		Ptrace.sysCall (pid, sig);
	    else
		Ptrace.cont (pid, sig);
	}
	catch (Errno.Esrch e) {
	    appendZombiedEvent ();
	}
    }
    protected void sendStepInstruction (int sig)
    {
	try {
	    Ptrace.singleStep (pid, sig);
	}
	catch (Errno.Esrch e) {
	    appendZombiedEvent ();
	}
    }
    protected void sendStop ()
    {
	Signal.tkill (id.hashCode (), Sig.STOP);
    }
    protected void sendSetOptions ()
    {
	try {
	    long options = 0;
	    options |= Ptrace.optionTraceClone ();
	    if (traceFork)
		options |= Ptrace.optionTraceFork ();
	    if (traceExit)
		options |= Ptrace.optionTraceExit ();
	    if (traceSyscall)
		options |= Ptrace.optionTraceSysgood ();
	    options |= Ptrace.optionTraceExec ();
	    Ptrace.setOptions (pid, options);
	}
	catch (Errno.Esrch e) {
	    appendZombiedEvent ();
	}
    }
    protected void sendAttach ()
    {
	try {
	    Ptrace.attach (pid);
	}
	catch (Errno.Esrch e) {
	    appendZombiedEvent ();
	}
    }
    public String toString ()
    {
	return "Linux" + super.toString ();
    }
}
