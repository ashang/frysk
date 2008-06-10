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

package frysk.hpd;

import frysk.proc.Action;
import frysk.proc.Manager;
import frysk.proc.Proc;
import frysk.proc.ProcTasksObserver;
import frysk.proc.Task;
import frysk.proc.TaskAttachedObserverXXX;
import frysk.proc.ProcTasksAction;
import frysk.util.CountDownLatch;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;


/**
 * Due to a lot of similar code in StartCommand/RunCommand this class was
 * created to consolidate most of the methods to one code base.
 */
abstract class StartRun extends ParameterizedCommand {
    
    final HashMap procTaskDataList = new HashMap();
    
    final HashMap procProcCommand = new HashMap();
    
    final HashMap procProcArgs = new HashMap();
    
    PTSet userSet;
    
    StartRun(String command, String help1, String help2) {
	super(command, help1, help2);
    }

    private static class Runner implements TaskAttachedObserverXXX {
	final CLI cli;
	CountDownLatch latch;
	Task launchedTask;

	Runner(CLI cli) {
	    this.cli = cli;
	}

	public Action updateAttached(final Task task) {
	    final Proc proc = task.getProc();
	    synchronized (this) {
		launchedTask = task;
	    }
	    synchronized (cli) {
		cli.runningProcs.add(proc);
	    }
	    new ProcTasksAction(proc, new ProcTasksObserver() {
		public void existingTask(Task task) {
		}

		public void addedTo(Object observable) {
		}

		public void addFailed(Object observable, Throwable w) {
		}

		public void deletedFrom(Object observable) {
		}

		public void taskAdded(Task task) {
		}

		public void taskRemoved(Task task) {
		    if (proc.getChildren().size() == 0) {
			synchronized (cli) {
			    HashSet procs = cli.runningProcs;
			    procs.remove(proc);
			}
		    }
		}
	    });
	    latch.countDown();
	    // Keep task blocked until the SteppingEngine notifies us that its
	    // instruction observers, etc. have been inserted.
	    return Action.BLOCK;
	}

	public void addedTo(Object observable) {
	}

	public void addFailed(Object observable, Throwable w) {
	    System.out.println("couldn't get it done:" + w);
	}

	public void deletedFrom(Object observable) {
	}
    }
    
    public void interpretCmd(CLI cli, Input cmd, Object options,
			     boolean runToBreak) {
	
	userSet = cli.getCommandPTSet(cmd);
	setParams(cmd, cli);
	// See if there are any tasks to be killed
	if (killProcs(cli)) {
	// See if there are any tasks in the current target set
	    TaskData taskData = null;
	    int counter = 0;
	    Iterator foo = userSet.getTaskData();
	    while (foo.hasNext()) {
		taskData = (TaskData) foo.next();
		int parentID = taskData.getParentID();
		String command = (String) cli.loadedProcs.get(new Integer(parentID));
		run(cli, cmd, command, runToBreak, 
			taskData.getParentID());
		counter++;
	    }
	    return;
	}
	// Take care of loaded procs
	Iterator foo = userSet.getTaskData();
	if (!foo.hasNext()) {
	    cli.addMessage("No procs in targetset to run", Message.TYPE_NORMAL);
	    return;
	}
	while (foo.hasNext()) {
	    TaskData taskData = (TaskData) foo.next();
	    Task task = taskData.getTask();
	    if (!cli.loadedProcs.isEmpty() && 
		    cli.loadedProcs.containsKey(new Integer(taskData.getParentID()))) {
		run(cli, cmd, task.getProc().getExeFile().getSysRootedPath(),
			runToBreak, taskData.getParentID());
		synchronized (cli) {
		    cli.loadedProcs.remove(task.getProc());
		}
	    }
	    // Take care of core procs  
	    else if (!cli.coreProcs.isEmpty() &&
		    cli.coreProcs.containsKey(new Integer(taskData.getParentID()))) {
		run(cli, cmd, task.getProc().getExeFile().getSysRootedPath(), 
			runToBreak, taskData.getParentID());
		synchronized (cli) {
		    cli.coreProcs.remove(new Integer(taskData.getParentID()));
		}
	    }
	}
	
    }
    
    /**
     * setParams will take the passed parameters by the run/start commands
     * and makes them the new current parameters.
     * 
     * @param params is a String[] containing the new params
     * @param cli is the current commandline interface object
     */
    private void setParams(Input cmd, CLI cli) {

	Iterator foo = userSet.getTaskData();
	TaskData taskData = null;
	while (foo.hasNext()) {
	    taskData = (TaskData) foo.next();
	    int parentID = taskData.getParentID();
	    switch (cmd.size()) {

		// No parameters were passed on the commandline, except maybe "--"
		case 0:
		    /**
		     * There is an exception to the rule of cmd.size() being 0 and that
		     * is when a "--" was entered, CLI removes that but the full command
		     * line string in the Input object has it. So, if cmd.size() reports
		     * 0 parameters, check to see if actually a "--" was entered. If it
		     * was the user wants no args passed to the process this run.
		     * 
		     * Leave the command as is if "--" was not passed.
		     */
		    int index = cmd.getFullCommand().indexOf("--");
		    if (index != -1) {
			String[] command = new String[1];
			String[] oldCommand = (String[]) cli.ptsetParams.get(new Integer(parentID));
			command[0] = oldCommand[0];
			// Set proc params to null so we won't run with them again
			cli.ptsetParams.put(new Integer(parentID), command);
		    }
		    break;
		    
		    // Params were passed
		default:
		    cli.ptsetParams.put(new Integer(parentID),
			    makeCommand(cmd.stringArrayValue(), parentID, cli));
		    break;
	    }
	}	      
    }
    
    /**
     * makeCommand takes the previous command and a new set of parameters and creates a new
     * command.  This new command is stored in the cli.ptsetParams HashMap.
     * 
     * @param params is the String array of parameters passed via a run or start command
     * @return returns a String[]
     */
    
    private String[] makeCommand(String[] params, int parentID, CLI cli) {
	String[] newParams = new String[params.length + 1];
	String[] oldParams = (String[]) cli.ptsetParams.get(new Integer(parentID));
	newParams[0] = oldParams[0];
	for (int i = 1; i < newParams.length; i++)
	    newParams[i] = params[i-1];
	return newParams;
    }
    
    /**
     * killProcs loops through the current target set to see if there are any
     * tasks to kill, the philosophy being that all tasks should be killed before
     * 
     * @param cli is the command line object
     * @return true if there were procs to kill, false if there weren't
     */
    
    private boolean killProcs(CLI cli) {
	Iterator foo = userSet.getTaskData();
	// No procs in target set return false
	if (!foo.hasNext()) {
	    return false;
	}
	TaskData taskData = null;
	int oldPid = -1;
	while (foo.hasNext()) {
	    taskData = (TaskData) foo.next();
	    Task task = taskData.getTask();
	    // Kill a proc only once
	    if (task.getProc().getPid() != oldPid &&
		    // Don't kill loaded procs, don't have a PID assigned yet
		    task.getProc().getPid() > 0 &&
		    // Don't kill core procs either, they have the old PID number
		    cli.coreProcs.get(new Integer(taskData.getParentID())) == null) {
		cli.execCommand("kill " + task.getProc().getPid() + "\n");
		oldPid = task.getProc().getPid();
	    }
	}
	if (procTaskDataList.isEmpty())
	    return false;
	
	return true;
    }

    /**
     * run takes a passed proc from the target set and runs it
     * 
     * @param cli is the commandline object
     * @param cmd is the command object with all the parameters
     * @param command is a string containing the command to run
     * @param runToBreak true if the process is to run to the first break point
     * 		or until it blows up("run" command), false if it should stop
     * 		at the first executable statement("start" command)
     * @param taskid the internal target set id that should be used for this process
     */
    private void run(CLI cli, Input cmd, String command, boolean runToBreak,
	    int taskid) {
	
	Runner runner = new Runner(cli);
	String startrun = "";
	if (runToBreak)
	    startrun = "running";
	else
	    startrun = "starting";

	cli.addMessage(startrun
		+ " with this command: "
		+ asString((String[]) cli.ptsetParams.get(new Integer(
			taskid))), Message.TYPE_NORMAL);
	Manager.host.requestCreateAttachedProc((String[]) cli.ptsetParams
		.get(new Integer(taskid)), runner);

	while (true) {
	    try {
		runner.latch = new CountDownLatch(1);
		runner.latch.await();
		break;
	    } catch (InterruptedException e) {
	    }
	}
	// register with SteppingEngine et.al.
	// Make sure we use the old task id for the new one
	synchronized (cli) {
	    cli.taskID = taskid;
	}
	cli.doAttach(runner.launchedTask.getProc(), runToBreak);
	runner.launchedTask.requestUnblock(runner);
	synchronized (cli) {
	    cli.taskID = -1;
	}
    }
    
    /**
     * asString takes an array of parameters and converts it to a single
     *    string of command/params as would be appropriate for a commandline
     * @param params is a String array with the first position in the array is
     *         the command to be run and the rest of the positions are params
     *         to pass to the command(if any)
     * @return a String containing the command to be run follwed by its parameters
     *         (if there are any)
     */

    private String asString(String[] params) {

	StringBuffer b = new StringBuffer(params[0]);
	// No params entered, return just the command
	if (params.length == 1)
	    return b.toString();
	// params were entered, append to the command
	for (int i = 1; i < params.length; i++) {
	    b.append(" ");
	    b.append(params[i]);
	}
	return b.toString();
    }
    
    int completer(CLI cli, Input input, int cursor, List completions) {
	return CompletionFactory.completeFileName(cli, input, cursor,
		completions);
    }
}
