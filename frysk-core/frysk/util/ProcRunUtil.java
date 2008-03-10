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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import frysk.proc.Action;
import frysk.proc.Manager;
import frysk.proc.Proc;
import frysk.proc.ProcObserver;
import frysk.proc.ProcTasksObserver;
import frysk.proc.Task;
import frysk.proc.TaskObserver;
import frysk.proc.TaskObserver.Attached;
import frysk.proc.TaskObserver.Cloned;
import frysk.proc.TaskObserver.Execed;
import frysk.proc.TaskObserver.Forked;
import frysk.proc.TaskObserver.Instruction;
import frysk.proc.TaskObserver.Signaled;
import frysk.proc.TaskObserver.Syscalls;
import frysk.proc.TaskObserver.Terminated;
import frysk.proc.TaskObserver.Terminating;
import gnu.classpath.tools.getopt.Option;

/**
 * Framework to be used for frysk utilities that, a) Accept pids, executable
 * paths, core files & b) Require tasks to be stopped,
 * 
 * Utilities must define a event.ProcEvent to execute.
 */
public class ProcRunUtil {

    ForkedObserver forkedObserver = new ForkedObserver();

    AttachedObserver attachedObserver = new AttachedObserver();

    private RunningUtilOptions options;
    private TaskObserver[] observers;

    public static class RunningUtilOptions {
	boolean followForks = true;
    }
    
    public static final RunningUtilOptions DEFAULT = new RunningUtilOptions();
    
    public ProcRunUtil(String utilName, String usage, String[] args,
	    TaskObserver[] observers, Option[] customOptions,
	    RunningUtilOptions options) {

	this.options = options;
	this.observers = observers;

	//Set up commandline parser
	CommandlineParser parser = new CommandlineParser(utilName) {
	    // @Override
	    public void parsePids(Proc[] procs) {
		for (int i = 0; i < procs.length; i++) {
		    addObservers(procs[i]);
		}
	    }

	    // @Override
	    public void parseCommand(Proc command) {

		//Exctact arguments and executable from give Proc object
		ArrayList commandAndArguments;
		String[] line = command.getCmdLine();
		commandAndArguments = new ArrayList();
		for (int i = 0; i < line.length; i++){
		    commandAndArguments.add(line[i]);
		}
		
		//Create a live process using the extracted command.
		// attachedObserver will handle adding of the remaining
		// observers.
		Manager.host.requestCreateAttachedProc(
			(String[]) commandAndArguments.toArray(new String[0]),
			attachedObserver);
	    }
	};

	// Add options
	for (int i = 0; i < customOptions.length; i++) {
	    parser.add(customOptions[i]);
	}

	parser.parse(args);
    }

    private void addObservers(Proc proc) {
	new ProcTasksObserver(proc, tasksObserver);
    }
    
    private void addObservers(Task task) {
	
	for (int i = 0; i < observers.length; i++) {
	    this.addTaskObserver(observers[i], task);
	}
	if(options.followForks){
	    this.addTaskObserver(forkedObserver, task);
	    observationRequested(task);
	}
    }
    
    private HashMap observationCounters = new HashMap();
    synchronized private void observationRequested(Task task) {
	Integer i = (Integer)observationCounters.get(task);
	if (i == null)
	    i = new Integer(1);
	else
	    i = new Integer(i.intValue() + 1);
	observationCounters.put(task, i);
    }

    synchronized private void observationRealized(Task task) {
	Integer i = (Integer)observationCounters.get(task);
	// must be non-null
	int j = i.intValue();
	if (j == 1) {
	    // Store a dummy into the map to detect errors.
	    observationCounters.put(task, new Object());
	    task.requestUnblock(attachedObserver);
	}
	else
	    observationCounters.put(task, new Integer(--j));
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
	    observationRealized((Task) observable);
	}

	public void deletedFrom(Object observable) {
	}
    }

    class AttachedObserver implements TaskObserver.Attached {
	private Set procs = new HashSet();

	public synchronized Action updateAttached(Task task) {
	    Proc proc = task.getProc();
	    if (!procs.contains(proc)) {
		procs.add(proc);
		addObservers(proc);
	    }
	    return Action.BLOCK;
	}

	public void addedTo(Object observable) {
	}

	public void deletedFrom(Object observable) {
	}

	public void addFailed(Object observable, Throwable w) {
	    throw new RuntimeException("Failed to attach to created proc", w);
	}
    }
    
    ProcObserver.ProcTasks tasksObserver = new ProcObserver.ProcTasks()
    {
	int taskCount = 0;
	public void existingTask (Task task)
	{
	    taskCount++;
	    addObservers(task);

	    if (task == task.getProc().getMainTask()) {
		// Unblock forked and cloned observer, which blocks
		// main task after the fork or clone, to give us a
		// chance to pick it up.
		task.requestUnblock(forkedObserver);
	    }
	}

	public void taskAdded (Task task)
	{
	    taskCount++;
	    addObservers(task);
	}

	public void taskRemoved (Task task)
	{
	    taskCount--;
	    if(taskCount == 0){
		Manager.eventLoop.requestStop();
	    }
	}

	public void addedTo (Object observable)	{}
	public void addFailed (Object observable, Throwable arg1) {}
	public void deletedFrom (Object observable) {}
    };

    //XXX: this is to handle adding observers according to their types
    //     since task does not provide overloaded functions for adding
    //     observers.
    private void addTaskObserver(TaskObserver observer, Task task){
	boolean handled = false;
	
	if(observer instanceof TaskObserver.Attached){
	    task.requestAddAttachedObserver((Attached) observer);
	    handled = true;
	}
	if(observer instanceof TaskObserver.Cloned){
	    task.requestAddClonedObserver((Cloned) observer);
	    handled = true;
	}
	if(observer instanceof TaskObserver.Forked){
	    task.requestAddForkedObserver((Forked) observer);
	    handled = true;
	}
	if(observer instanceof TaskObserver.Execed){
	    task.requestAddExecedObserver((Execed) observer);
	    handled = true;
	}
	if(observer instanceof TaskObserver.Terminating){
	    task.requestAddTerminatingObserver((Terminating) observer);
	    handled = true;
	}
	if(observer instanceof TaskObserver.Terminated){
	    task.requestAddTerminatedObserver((Terminated) observer);
	    handled = true;
	}
	if(observer instanceof TaskObserver.Syscalls){
	    task.requestAddSyscallsObserver((Syscalls) observer);
	    handled = true;
	}
	if(observer instanceof TaskObserver.Signaled){
	    task.requestAddSignaledObserver((Signaled) observer);
	    handled = true;
	}
	if(observer instanceof TaskObserver.Instruction){
	    task.requestAddInstructionObserver((Instruction) observer);
	    handled = true;
	}
	
	if(!handled){
	    throw new RuntimeException("Observer type not handled");
	}
    }

    public void start() {
	Manager.eventLoop.run();
    }
}