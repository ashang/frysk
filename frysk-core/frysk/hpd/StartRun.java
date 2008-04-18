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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;


/**
 * Due to a lot of similar code in StartCommand/RunCommand this class was
 * created to consolidate most of the methods to one code base.
 */
abstract class StartRun extends ParameterizedCommand {
    
    final ArrayList procTaskDataList = new ArrayList();
    
    final ArrayList procProcCommand = new ArrayList();
    
    final ArrayList procProcArgs = new ArrayList();
    
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
		cli.getRunningProcs().add(proc);
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
			    HashSet procs = cli.getRunningProcs();
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
	// See if there are any tasks to be killed
	if (killProcs(cli)) {
	// See if there are any tasks in the current target set
	    TaskData taskData = null;
	    int counter = 0;
	    Iterator foo = procTaskDataList.iterator();
	    while (foo.hasNext()) {
		taskData = (TaskData) foo.next();
		String command = (String) procProcCommand.get(counter);
		String[] argList = (String[]) procProcArgs.get(counter);
		run(cli, cmd, command, argList, runToBreak, 
			taskData.getParentID());
		counter++;
	    }
	    procTaskDataList.clear();
	    procProcArgs.clear();
	    procProcCommand.clear();
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
		    cli.loadedProcs.containsKey(task.getProc())) {
		run(cli, cmd, task.getProc().getExeFile().getSysRootedPath(),
			task.getProc().getCmdLine(), runToBreak, taskData.getParentID());
		synchronized (cli) {
		    cli.loadedProcs.remove(task.getProc());
		}
	    }
	// Take care of core procs
	    else if (!cli.coreProcs.isEmpty() &&
		    cli.coreProcs.containsKey(task.getProc())) {
		run(cli, cmd, task.getProc().getExeFile().getSysRootedPath(), 
			task.getProc().getCmdLine(), runToBreak, taskData.getParentID());
		synchronized (cli) {
		    cli.coreProcs.remove(new Integer(taskData.getParentID()));
		}
	    }
	}
	
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
	    if (task.getProc().getPid() != oldPid && 
		    task.getProc().getPid() > 0) {
		procTaskDataList.add(taskData);
		procProcCommand.add(task.getProc().getExeFile().getSysRootedPath());
		procProcArgs.add(task.getProc().getCmdLine());
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
     * @param argList is a String array with the args from the commandline
     * @param runToBreak true if the process is to run to the first break point
     * 		or until it blows up("run" command), false if it should stop
     * 		at the first executable statement("start" command)
     * @param taskid the internal target set id that should be used for this process
     */
    private void run(CLI cli, Input cmd, String command, String[] argList, boolean runToBreak,
	    int taskid) {
	Runner runner = new Runner(cli);
	String startrun = "";
	if (runToBreak)
	    startrun = "running";
	else
	    startrun = "starting";
	
	switch (cmd.size()) {
	
	// No parameters were passed on the commandline, except maybe "--"
	case 0:
	    /** There is an exception to the rule of cmd.size() being 0 and that
	     * is when a "--" was entered, CLI removes that but the full command line
	     * string in the Input object has it.  So, if cmd.size() reports 0
	     * parameters, check to see if actually a "--" was entered. If it was
	     * the user wants no args passed to the process this run.
	     */
	    int index = cmd.getFullCommand().indexOf("--");
	    if (index != -1) {
		cli.addMessage(startrun + " with this commmand: " + 
			command, Message.TYPE_NORMAL);
		Manager.host.requestCreateAttachedProc(createArgList(null, argList), runner);
	    }
	    else {
		String[] temp = new String[1];
		if (command != "" || command != null) 
		    temp[0] = command;
		else
		    temp [0] = argList[0];
		cli.addMessage(startrun + " with this commmand: " + 
			   asString("", temp, cmd), Message.TYPE_NORMAL);
		Manager.host.requestCreateAttachedProc(createArgList(command, temp), runner);
	    }
	    break;
	    
	default:
	    String[] args = createArgList(command, cmd.stringArrayValue()); 
	    cli.addMessage(startrun + " with this command: " + 
			   asString(command, args, cmd), Message.TYPE_NORMAL);
	    
	    Manager.host.requestCreateAttachedProc(args, runner);
	}
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
     * createArgList takes a path to the executable and an argument list
     * and creates a String[] to pass to the create proc method.
     * @param command is a String with the path to the executable
     * @param argList is a String[] with an argument list
     * @return a String[] to pass to Manager.host.createAttachedProc
     */
    private String[] createArgList(String command, String[] argList) {
	// If command is null, the command is at the begging of argList
	// so we need to do nothing
	if (command == null) 
	    return argList;
	String[] args = new String[argList.length + 1];
	    args[0] = command;
	    if (args.length > 1) {
		for (int i = 0; i < argList.length; i++) {
		    if (i == argList.length - 1 && argList.equals("--"))
			args[i + 1] = "";
		    else
			args[i + 1] = argList[i];
		}
	    }
	    return args;
    }
    /**
     * asString takes an array of arguments and converts it to a single
     *    string of args as would be appropriate for a commandline
     * @param command is the String representing the full command path for
     * 		this process
     * @param argList is a String array of arguments to pass to the process to be run
     * @param cmd is the current Input command object
     * @return a String of args that are to be passed to the process
     *          on the commandline
     */

    private String asString(String command, String[] argList, Input cmd) {
	String[] args = cmd.stringArrayValue();
	// If there were no args on the commandline, see if there were any
	// on a previous run and if there were, use those
	if (args == null || args.length <= 0 || args == null)
	    args = argList;
	StringBuffer b = new StringBuffer(command);
	// No args entered, return just the command
	if (args == null)
	    return b.toString();
	// args were entered, append to the command
	for (int i = 0; i < args.length; i++) {
	    b.append(" ");
	    b.append(args[i]);
	}
	return b.toString();
    }
    
    int completer(CLI cli, Input input, int cursor, List completions) {
	return CompletionFactory.completeFileName(cli, input, cursor,
		completions);
    }
}
