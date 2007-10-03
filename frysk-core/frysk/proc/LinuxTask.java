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
import inua.eio.PtraceByteBuffer;
import inua.eio.ByteBuffer;
import inua.eio.ByteOrder;

/**
 * Linux implementation of Task.
 */

public class LinuxTask
    extends Task
{
    // XXX: For moment wire in standard 32-bit little-endian memory
    // map.  This will be replaced by a memory map created using
    // information from /proc/PID/maps.
    private void setupMapsXXX ()
    {
	// XXX: For writing at least, PTRACE must be used as /proc/mem
	// cannot be written to.
	memory = new PtraceByteBuffer (id.id, PtraceByteBuffer.Area.DATA,
				       0xffffffffl);
	memory.order (ByteOrder.LITTLE_ENDIAN);
	// XXX: For moment wire in a standard 32-bit little-endian
	// register set.
	registerBank = new ByteBuffer[] {
	    new PtraceByteBuffer (id.id, PtraceByteBuffer.Area.USR)
	};
	registerBank[0].order (ByteOrder.LITTLE_ENDIAN);
    }

    /**
     * Create a detached task.  For detached tasks, RUNNING makes no
     * sennse.
     */
    LinuxTask (Proc process, TaskId id)
    {
	super (process, id);
	setupMapsXXX ();
    }

    /**
     * Create an attached, possibly running, task.
     */
    LinuxTask (Proc process, TaskId id, boolean running)
    {
	super (process, id, running);
	setupMapsXXX ();
    }

    protected void sendContinue (int sig)
    {
	try {
	    if (traceSyscall)
		Ptrace.sysCall (getTid (), sig);
	    else
		Ptrace.cont (getTid (), sig);
	}
	catch (Errno.Esrch e) {
	    performZombied ();
	}
    }
    protected void sendStepInstruction (int sig)
    {
	try {
	    Ptrace.singleStep (getTid (), sig);
	}
	catch (Errno.Esrch e) {
	    performZombied ();
	}
    }
    protected void sendStop ()
    {
	Signal.tkill (id.hashCode (), Sig.STOP);
    }
    protected void sendSetOptions ()
    {
	try {
	    // XXX: Should be selecting the trace flags based on the
	    // contents of .observers.
	    long options = 0;
	    options |= Ptrace.optionTraceClone ();
	    options |= Ptrace.optionTraceFork ();
	    options |= Ptrace.optionTraceExit ();
	    if (traceSyscall)
		options |= Ptrace.optionTraceSysgood ();
	    options |= Ptrace.optionTraceExec ();
	    Ptrace.setOptions (getTid (), options);
	}
	catch (Errno.Esrch e) {
	    performZombied ();
	}
    }
    protected void sendAttach ()
    {
	try {
	    Ptrace.attach (getTid ());
	}
	catch (Errno.Esrch e) {
	    performZombied ();
	}
    }
    protected void sendDetach (int sig)
    {
	Ptrace.detach (getTid (), sig);
    }
    protected Isa sendrecIsa ()
    {
	return LinuxIa32.isaSingleton ();
    }
    public String toString ()
    {
	return "Linux" + super.toString ();
    }
}
