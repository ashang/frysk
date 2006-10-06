// This file is part of the program FRYSK.
//
// Copyright 2005, 2006, Red Hat Inc.
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

package frysk.util;

import frysk.proc.Action;
import frysk.proc.Manager;
import frysk.proc.Proc;
import frysk.proc.ProcId;
import frysk.proc.ProcObserver;
import frysk.proc.ProcTasksObserver;
import frysk.proc.SyscallEventInfo;
import frysk.proc.Task;
import frysk.proc.TaskException;
import frysk.proc.TaskObserver;
import inua.util.PrintWriter;

import java.util.HashSet;
import java.util.Observable;
import java.util.Observer;

public class Ftrace
{
	// Where to send output.
	PrintWriter writer;
	// True if we're tracing children as well.
	boolean traceChildren;
	
	// Set of ProcId objects we trace; if traceChildren is set, we also
	// look for their children.
	HashSet tracedParents = new HashSet();

	// The number of processes we're tracing.
	int numProcesses;

	// Enter and exit handlers.
	SyscallHandler enterHandler;
	SyscallHandler exitHandler;

	public void setTraceChildren()
	{
		traceChildren = true;
	}

	public void addTracePid(int id)
	{
		tracedParents.add(new ProcId(id));
	}

	public void setWriter(PrintWriter writer)
	{
		this.writer = writer;
	}

	public void setEnterHandler(SyscallHandler handler)
	{
		this.enterHandler = handler;
	}

	public void setExitHandler(SyscallHandler handler)
	{
		this.exitHandler = handler;
	}

	private void init()
	{
		if (writer == null)
			writer = new PrintWriter(System.out);

		Manager.host.observableProcAddedXXX.addObserver(new Observer() {
			public void update(Observable observable, Object arg)
			{
				Proc proc = (Proc) arg;
				ProcId id = proc.getId();
				if (tracedParents.contains(id)
					|| (traceChildren
						&& tracedParents.contains(proc.getParent().getId())))
				{
					// In case we're tracing a new child, add it.
					tracedParents.add(proc.getId());
					// Weird API... unfortunately we can't fetch the
					// Proc's main task here, as it will be null.  Instead
					// we have to request it and handle it in a callback.
					new ProcTasksObserver(proc, new WaitForTask());
				}
			}
		});
	}

	public void trace(String[] command)
	{
		init();
		Manager.host.requestCreateAttachedProc(command, new AttachedObserver());
		// Manager.host.requestRefreshXXX(true);
		Manager.eventLoop.start();
	}

	public void trace()
	{
		init();
		Manager.host.requestRefreshXXX(true);
		Manager.eventLoop.start();
	}
	
	synchronized void handleTask (Task task)
	{
		task.requestAddSyscallObserver(new SyscallObserver());
		Proc proc = task.getProc();
		if (traceChildren)
			tracedParents.add(proc.getId());
		Manager.host.observableProcRemovedXXX.addObserver (new ProcRemovedObserver(proc));
		writer.println("Ftrace.main() Proc.getPid() " + proc.getPid());
		writer.println("Ftrace.main() Proc.getExe() " + proc.getExe());
		writer.flush();
		++numProcesses;
	}

	class WaitForTask implements ProcObserver.ProcTasks
	{
		public void addedTo(Object arg0) {
		}
		public void addFailed(Object arg0, Throwable arg1) {
		}
		public void deletedFrom(Object arg0) {
		}
		public void existingTask(Task arg) {
			taskAdded(arg);
		}
		public void taskAdded(Task task) {
			handleTask(task);
		}
		public void taskRemoved(Task arg0) {
		}
	}

	/**
	 * An observer to stop the eventloop when the traced process
	 * exits.
	 */
	private class ProcRemovedObserver implements Observer{
		int pid;
		
		ProcRemovedObserver(Proc proc){
			this.pid = proc.getPid();
		}
		
		public void update (Observable o, Object object)
		{
			Proc proc = (Proc) object;
			if (proc.getPid() == this.pid)
			{
				synchronized (Ftrace.this)
				{
					--numProcesses;
					if (numProcesses == 0)
						Manager.eventLoop.requestStop ();
				}
			}
		}
	}
	
	/**
	 * An observer that sets up things once frysk has set up
	 * the requested proc and attached to it.
	 */
	private class AttachedObserver implements TaskObserver.Attached{
		public Action updateAttached (Task task)
		{
			handleTask(task);
			task.requestUnblock(this);
			return Action.BLOCK;
		}
		
		public void addedTo (Object observable){}
		
		public void addFailed (Object observable, Throwable w){
			throw new RuntimeException("Failed to attach to created proc", w);
		}
		
		public void deletedFrom (Object observable){}
	}
	
	/**
	 * The syscallObserver added to the traced proc.
	 */
	private class SyscallObserver implements TaskObserver.Syscall{
		
		public Action updateSyscallEnter (Task task)
		{
			SyscallEventInfo syscallEventInfo;
			try
			{
				syscallEventInfo = task.getSyscallEventInfo ();
				if (enterHandler != null)
					enterHandler.handle(task, syscallEventInfo, SyscallEventInfo.ENTER);
			}
			catch (TaskException e) 
			{
				// XXX Abort? or what?
				System.err.println("Got task exception " + e);
			}
			return Action.CONTINUE;
		}
		
		public Action updateSyscallExit (Task task)
		{
			SyscallEventInfo syscallEventInfo;
			try
			{
				syscallEventInfo = task.getSyscallEventInfo ();
				if (exitHandler != null)
					exitHandler.handle(task, syscallEventInfo, SyscallEventInfo.EXIT);
			}
			catch (TaskException e) 
			{
				// XXX Abort? or what?
				System.err.println("Got task exception " + e);
			}
			return Action.CONTINUE;
		}
		
		public void addedTo (Object observable)
		{
			
		}
		
		public void addFailed (Object observable, Throwable w)  {
			throw new RuntimeException("Failed to add a Systemcall observer to the process",w);
		}
		
		public void deletedFrom (Object observable)
		{
			throw new RuntimeException("This has not yet been implemented");
		}
		
	}
	
}
