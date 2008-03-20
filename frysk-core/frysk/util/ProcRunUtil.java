// This file is part of the program FRYSK.
//
// Copyright 2008, Red Hat Inc.
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

import java.util.HashSet;

import frysk.proc.Action;
import frysk.proc.Manager;
import frysk.proc.Proc;
import frysk.proc.ProcTasksAction;
import frysk.proc.ProcTasksObserver;
import frysk.proc.Task;
import frysk.proc.TaskAttachedObserverXXX;
import frysk.proc.TaskObserver;
import gnu.classpath.tools.getopt.OptionGroup;

/**
 * Framework to be used for frysk utilities that, a) Accept pids, executable
 * paths, core files & b) Require tasks to be stopped,
 * 
 * Utilities must define a event.ProcEvent to execute.
 */
public class ProcRunUtil {

    private final HashSet knownTasks = new HashSet();
    
    ForkedObserver forkedObserver = new ForkedObserver();
    AttachedObserver attachedObserver = new AttachedObserver();

    private RunUtilOptions options;

    public static interface ProcRunObserver extends
    TaskAttachedObserverXXX, // this is only notified if the process is newly created by the util
    TaskObserver.Forked,
    TaskObserver.Execed,
    ProcTasksObserver, // this is only notified if the util attaches to an existing process 
    TaskObserver.Cloned,
    TaskObserver.Terminated
    {}
    
    private final ProcRunObserver procRunObserver;
    
    public static class RunUtilOptions {
	boolean followForks = true;
    }
    
    public static final RunUtilOptions DEFAULT = new RunUtilOptions();

    public ProcRunUtil(String utilName, String usage, String[] args,
	    final ProcRunObserver procRunObserver, OptionGroup[] customOptions,
	    RunUtilOptions options) {
	
	this.procRunObserver = procRunObserver;
	this.options = options;
	
	CommandlineParser parser = new CommandlineParser(utilName, customOptions) {
	    // @Override
	    public void parsePids(Proc[] procs) {
		for (int i = 0; i < procs.length; i++) {
		    addObservers(procs[i]);
		    new ProcTasksAction(procs[i], procRunObserver);
		}
	    }
	    
	    // @Override
	    public void parseCommand(Proc command) {
		Manager.host.requestCreateAttachedProc(command, attachedObserver);
	    }
	};
	
	parser.parse(args);
	
    }
    
    private void addObservers(Proc proc) {
	new ProcTasksAction(proc, tasksObserver);
    }
    
    private void addObservers(Task task) {
	if (knownTasks.add(task)) {
	    task.requestAddClonedObserver(procRunObserver);
	    task.requestAddExecedObserver(procRunObserver);
	    task.requestAddForkedObserver(forkedObserver);
	    task.requestAddTerminatedObserver(procRunObserver);

	    if (this.options.followForks) {
		task.requestAddForkedObserver(forkedObserver);
	    }
	}
    }
    
    class ForkedObserver implements TaskObserver.Forked {
	public Action updateForkedOffspring(Task parent, Task offspring) {
	    addObservers(offspring.getProc());
	    return Action.BLOCK;
	}

	public Action updateForkedParent(Task parent, Task offspring) {
	    return Action.CONTINUE;
	}

	public void addFailed(Object observable, Throwable w) {
	}

	public void addedTo(Object observable) {
	}

	public void deletedFrom(Object observable) {
	}
    }

    class AttachedObserver implements TaskAttachedObserverXXX {
	
	public synchronized Action updateAttached(Task task) {
	
	    procRunObserver.updateAttached(task);
	    Proc proc = task.getProc();
	    addObservers(proc);
	    
	    return Action.BLOCK;
	}

	public void addedTo(Object observable) {
	    procRunObserver.addedTo(observable);
	}

	public void deletedFrom(Object observable) {
	    procRunObserver.deletedFrom(observable);
	}

	public void addFailed(Object observable, Throwable w) {
	    procRunObserver.addFailed(observable, w);
	    throw new RuntimeException("Failed to attach to created proc", w);
	}
    }
    
    ProcTasksObserver tasksObserver = new ProcTasksObserver()
    {
	
	public void existingTask (Task task)
	{
	    addObservers(task);

	    if (task == task.getProc().getMainTask()) {
		// Unblock forked and cloned observer, which blocks
		// main task after the fork or clone, to give us a
		// chance to pick it up.
		task.requestUnblock(forkedObserver);
		task.requestUnblock(attachedObserver);
		
	    }
	}

	public void taskAdded (Task task)
	{
	    addObservers(task);
	}

	public void taskRemoved (Task task)
	{
	    knownTasks.remove(task);
	    if(knownTasks.size() == 0){
		Manager.eventLoop.requestStop();
	    }
	}

	public void addedTo (Object observable)	{}
	public void addFailed (Object observable, Throwable arg1) {}
	public void deletedFrom (Object observable) {}
    };

    public void start() {
	Manager.eventLoop.run();
    }
}