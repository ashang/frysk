// This file is part of the program FRYSK.
//
// Copyright 2005, 2006, 2007, Red Hat Inc.
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

package frysk.proc.ptrace;

import frysk.proc.TaskObserver;
import frysk.proc.Proc;
import frysk.proc.TaskId;
import frysk.proc.Task;
import java.util.logging.Level;
import frysk.proc.Manager;
import frysk.proc.TaskEvent;
import inua.eio.ByteBuffer;
import inua.eio.ByteOrder;
import frysk.proc.IsaFactory;
import frysk.proc.Isa;
import frysk.sys.Ptrace.AddressSpace;
import frysk.sys.Errno;
import frysk.sys.Ptrace;
import frysk.sys.Sig;
import frysk.sys.Signal;

/**
 * A Linux Task tracked using PTRACE.
 */

public class LinuxTask
    extends Task
{
    /**
     * Create a new unattached Task.
     */
    public LinuxTask (Proc proc, TaskId id)
    {
	super(proc, id, LinuxTaskState.detachedState());
    }
    /**
     * Create a new attached clone of Task.
     */
    public LinuxTask (Task task, TaskId clone)
    {
	// XXX: shouldn't need to grub around in the old task's state.
	super(task, clone,
	      LinuxTaskState.clonedState(((LinuxTask)task).getState ()));
    }
    /**
     * Create a new attached main Task of Proc.
     */
    public LinuxTask (Proc proc, TaskObserver.Attached attached)
    {
	super(proc, attached, LinuxTaskState.mainState());
    }


    /**
     * Return the memory byte-buffer.
     */
    protected ByteBuffer sendrecMemory ()
    {
	logger.log(Level.FINE, "Begin fillMemory\n", this);
	ByteOrder byteOrder = getIsa().getByteOrder();
	ByteBuffer memory
	    = new AddressSpaceByteBuffer(getTid(), AddressSpace.DATA);
	memory.order(byteOrder);
	logger.log(Level.FINE, "End fillMemory\n", this); 
	return memory;
    }
    
    /**
     * Return the ISA's register-bank byte-buffers.
     */
    protected ByteBuffer[] sendrecRegisterBanks () 
    {
	return getIsa().getRegisterBankBuffers(getTid());
    }
    /**
     * Return the Task's ISA.
     *
     * XXX: This code locally, and not the IsaFactory, and definitly
     * not via a PID should be determining the ISA of the process.
     */
    protected Isa sendrecIsa ()
    {
	logger.log(Level.FINE, "{0} sendrecIsa\n", this);
	IsaFactory factory = IsaFactory.getSingleton();
	return factory.getIsa(getTid());
    }

    /**
     * (internal) This task cloned creating the new Task cloneArg.
     */
    void processClonedEvent (Task clone)
    {
	set(oldState().handleClonedEvent(this, clone));
    }
    /**
     * (internal) This Task forked creating an entirely new child process
     * containing one (the fork) task.
     */
    void processForkedEvent (Task fork)
    {
	set(oldState().handleForkedEvent(this, fork));
    }
    /**
     * (internal) This task stopped.
     */
    void processStoppedEvent ()
    {
	set(oldState().handleStoppedEvent(this));
    }
    /**
     * (internal) This task encountered a trap.
     */
    void processTrappedEvent ()
    {
	set(oldState().handleTrappedEvent(this));
    }
    /**
     * (internal) This task received a signal.
     */
    void processSignaledEvent (int sig)
    {
	set(oldState().handleSignaledEvent(this, sig));
    }
    /**
     * (internal) The task is in the process of terminating. If SIGNAL, VALUE is
     * the signal, otherwize it is the exit status.
     */
    void processTerminatingEvent (boolean signal, int value)
    {
	set(oldState().handleTerminatingEvent(this, signal, value));
    }
    /**
     * (internal) The task has disappeared (due to an exit or some other error
     * operation).
     */
    void processDisappearedEvent (Throwable arg)
    {
	set(oldState().handleDisappearedEvent(this, arg));
    }
    /**
     * (internal) The task is performing a system call.
     */
    void processSyscalledEvent ()
    {
	set(oldState().handleSyscalledEvent(this));
    }
    /**
     * (internal) The task has terminated; if SIGNAL, VALUE is the signal,
     * otherwize it is the exit status.
     */
    void processTerminatedEvent (boolean signal, int value)
    {
	set(oldState().handleTerminatedEvent(this, signal, value));
    }
    /**
     * (internal) The task has execed, overlaying itself with another program.
     */
    void processExecedEvent ()
    {
	set(oldState().handleExecedEvent(this));
    }

    /**
     * Must inject disappeared events back into the event loop so that
     * they can be processed in sequence. Calling
     * receiveDisappearedEvent directly would cause a recursive state
     * transition.
     */
    protected void postDisappearedEvent (final Throwable arg)
    {
	logger.log(Level.FINE, "{0} postDisappearedEvent\n", this);
	Manager.eventLoop.add(new TaskEvent()
	    {
		final Throwable w = arg;
		public void execute ()
		{
		    processDisappearedEvent(w);
		}
	    });
    }

    public void sendContinue (int sig)
    {
	logger.log(Level.FINE, "{0} sendContinue\n", this);
	step_send = false;
	sig_send = sig;
        incrementMod();
	try
	    {
		Ptrace.cont(getTid(), sig);
	    }
	catch (Errno.Esrch e)
	    {
		postDisappearedEvent(e);
	    }
    }
    public void sendSyscallContinue (int sig)
    {
	logger.log(Level.FINE, "{0} sendSyscallContinue\n", this);
	step_send = false;
	sig_send = sig;
        incrementMod();
	try
	    {
		Ptrace.sysCall(getTid(), sig);
	    }
	catch (Errno.Esrch e)
	    {
		postDisappearedEvent(e);
	    }
    }

    public void sendStepInstruction (int sig)
    {
	logger.log(Level.FINE, "{0} sendStepInstruction\n", this);
	step_send = true;
	sig_send = sig;
        incrementMod();
	syscall_sigret = getIsa().isAtSyscallSigReturn(this);
	try
	    {
		Ptrace.singleStep(getTid(), sig);
	    }
	catch (Errno.Esrch e)
	    {
		postDisappearedEvent(e);
	    }
    }

    public void sendStop ()
    {
	logger.log(Level.FINE, "{0} sendStop\n", this);
	Signal.tkill(getTid(), Sig.STOP);
    }

    private int ptraceOptions;
    public void sendSetOptions ()
    {
	logger.log(Level.FINE, "{0} sendSetOptions\n", this);
	try
	    {
		// XXX: Should be selecting the trace flags based on the
		// contents of .observers.
		ptraceOptions |= Ptrace.optionTraceClone();
		ptraceOptions |= Ptrace.optionTraceFork();
		ptraceOptions |= Ptrace.optionTraceExit();
		// ptraceOptions |= Ptrace.optionTraceSysgood (); not set by default
		ptraceOptions |= Ptrace.optionTraceExec();
		Ptrace.setOptions(getTid(), ptraceOptions);
	    }
	catch (Errno.Esrch e)
	    {
		postDisappearedEvent(e);
	    }
    }

    public void sendAttach ()
    {
	logger.log(Level.FINE, "{0} sendAttach\n", this);
	try
	    {
		Ptrace.attach(getTid());

		/*
		 * XXX: Linux kernel has a 'feature' that if a process is already
		 * stopped and ptrace requests that it be stopped (again) in order to
		 * attach to it, the signal (SIGCHLD) notifying frysk of the attach's
		 * pending waitpid event isn't generated.
		 */

		/*
		 * XXX: This line sends another signal to frysk notifying about the
		 * attach's pending waitpid regardless of whether the task is running or
		 * stopped. This avoids hangs on attaching to a stopped process. Bug
		 * 3316.
		 */
		frysk.sys.Signal.tkill(frysk.sys.Tid.get(), Sig.CHLD);
	    }
	catch (Errno.Eperm e)
	    {
		logger.log(Level.FINE, "{" + e.toString()
			   + "} Cannot attach to process\n");
	    }
	catch (Errno.Esrch e)
	    {
		postDisappearedEvent(e);
	    }
    }

    public void sendDetach (int sig)
    {
	logger.log(Level.FINE, "{0} sendDetach\n", this);
	Ptrace.detach(getTid(), sig);
    }

    public void startTracingSyscalls ()
    {
	logger.log(Level.FINE, "{0} startTracingSyscalls\n", this);
	ptraceOptions |= Ptrace.optionTraceSysgood();
	this.sendSetOptions();
    }

    public void stopTracingSyscalls ()
    {
	logger.log(Level.FINE, "{0} stopTracingSyscalls\n", this);
	ptraceOptions &= ~ (Ptrace.optionTraceSysgood());
	this.sendSetOptions();
    }
}
