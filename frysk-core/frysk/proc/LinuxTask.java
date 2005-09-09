// This file is part of FRYSK.
//
// Copyright 2005, Red Hat Inc.
//
// FRYSK is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
//
// FRYSK is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with FRYSK; if not, write to the Free Software
// Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA

/**
 * Linux implementation of Task.
 */

package frysk.proc;

import frysk.sys.Errno;
import frysk.sys.Ptrace;
import frysk.sys.Sig;
import frysk.sys.Signal;
import util.eio.ByteBuffer;
import util.eio.PtraceByteBuffer;

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
