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
import frysk.proc.ProcObserver.ProcTasks;
import frysk.proc.ProcTasksObserver;
import frysk.proc.Task;
import frysk.proc.TaskObserver;
import frysk.util.CountDownLatch;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Due to a lot of similar code in StartCommand/RunCommand this class was
 * created to consolidate most of the methods to one code base.
 */
class StartRun extends ParameterizedCommand {
    
    boolean runToBreak = false;
    
    StartRun(String command, String help1, String help2) {
	super(command, help1, help2);
    }

    static class Runner implements TaskObserver.Attached {
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
	    new ProcTasksObserver(proc, new ProcTasks() {
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
    
    /**
     * interpretRun is called from RunCommand to run a process until
     * its first break point(if any) or until it blows up of finishes.
     * 
     * @param cli is the current command line interface object
     * @param cmd is the command to be run
     * @param options is not used at this point
     */
    public void interpretRun(CLI cli, Input cmd, Object options) {
	runToBreak = true;
	interpretCmd(cli, cmd, options);
	return;
    }
   
    /**
     * interpretStart is called from StartCommand to start a process and
     * run it to the first executable statement.
     * 
     * @param cli is the current command line interface object
     * @param cmd is the command to be started
     * @param options is not used at this point
     */
    public void interpretStart(CLI cli, Input cmd, Object options)  {
	runToBreak = false;
	interpretCmd(cli, cmd, options);
	return;
    }
    
    public void interpret(CLI cli, Input cmd, Object options) {
	return;
    }

    public void interpretCmd(CLI cli, Input cmd, Object options) {
	// See if there are any running tasks, if so, process them
	// if there are not any procs loaded with the load/core commands
	Iterator foo = cli.targetset.getTaskData();
	if (foo.hasNext()) {
	    if (cli.coreProcs.isEmpty() && cli.loadedProcs.isEmpty()) {
		// Clear the parameters for this process
		int oldPid = -1;
		TaskData taskData = null;
		while (foo.hasNext()) {
		    taskData = (TaskData) foo.next();
		    Task task = taskData.getTask();
		    // Need only one kill per PID(proc)
		    if (task.getProc().getPid() == oldPid) {
			continue;
		    } else {
			cli.execCommand("kill\n");
			int taskid = taskData.getParentID();
			synchronized (cli) {
				cli.taskID = taskid;
			}
			String paramList = getParameters(cmd, task);
		    	Input newcmd = new Input(task.getProc().getExe() + " " +
			    paramList);
		    	run(cli, newcmd);
		    	synchronized (cli) {
				cli.taskID = -1;
				cli.loadedProcs.clear();
			}
		    	oldPid = task.getProc().getPid();
		    }
		}
		return;
	    }
	} else {
	    cli.addMessage("No procs in targetset to run", Message.TYPE_NORMAL);
	    return;
	}

	/*
	 * If we made it here, a run command was issued and there are no running
	 * procs, so, see if there were loaded procs or core procs
	 */

	/* This is the case where there are loaded procs */
	if (!cli.loadedProcs.isEmpty()) {
	    Set procSet = cli.loadedProcs.entrySet();
	    runProcs(cli, procSet, cmd);
	    synchronized (cli) {
		cli.loadedProcs.clear();
	    }
	}

	/* Check to see if there were procs loaded from a core command */
	if (!cli.coreProcs.isEmpty()) {
	    Set coreSet = cli.coreProcs.entrySet();
	    runProcs(cli, coreSet, cmd);
	    synchronized (cli) {
		cli.coreProcs.clear();
	    }
	}
    }

    private void run(CLI cli, Input cmd) {
	Runner runner = new Runner(cli);
	Manager.host.requestCreateAttachedProc(cmd.stringArrayValue(), runner);
	while (true) {
	    try {
		runner.latch = new CountDownLatch(1);
		runner.latch.await();
		break;
	    } catch (InterruptedException e) {
	    }
	}
	// register with SteppingEngine et.al.
	cli.doAttach(runner.launchedTask.getProc(), runToBreak);
	runner.launchedTask.requestUnblock(runner);
    }

    /**
     * runProcs does as the name implies, it runs procs found to be loaded by a
     * load or a core command.
     * 
     * @param cli is the current commandline interface object
     * @param procs is the set of procs to be run
     * @param cmd is the command object to use to start the proc(s)
     */
    private void runProcs(CLI cli, Set procs, Input cmd) {
	Iterator foo = procs.iterator();
	int ctr = 0;
	while (foo.hasNext()) {
	    ctr++;
	    Map.Entry me = (Map.Entry) foo.next();
	    Proc proc = (Proc) me.getKey();
	    Integer taskid = (Integer) me.getValue();
	    // Set the TaskID to be used to what was used when the
	    // proc was loaded with the core or load commands
	    synchronized (cli) {
		cli.taskID = taskid.intValue();
	    }
	    Input newcmd = new Input(proc.getExe() + " " +
		    getParameters(cmd, proc.getMainTask()));
	    cli.addMessage("starting/running with this command: " + 
		    newcmd, Message.TYPE_NORMAL);
	    run(cli, newcmd);
	    synchronized (cli) {
		cli.taskID = -1;
	    }
	}
    }
    
    /**
     * getParameters figures out what parameters to send back to start/run the process with;
     * 	if no parameters are entered, use the previous ones if any were entered; if
     *  parameters were entered, use those.
     *  
     * @param cmd is the Input object containing the command and any paramaters
     * @param task is the Task object of the process that was previously run
     * @return a String containing the parameters to be used in starting/running
     *         the process
     */

    private String getParameters(Input cmd, Task task) {
	if (cmd.size() < 1) {
	    // No params entered, use this proc's previous params(if any)
	    Proc proc = task.getProc();
	    return parseParameters(proc.getCmdLine(), true);
	} else//Proc had no previous params, send back empty param list
	    // There were parameters entered, use those
	    return parseParameters(cmd.stringArrayValue(), false);
    }
    
    /**
     * parseParameters takes a String array and returns a space-delimited String
     * 
     * @param parameters is the String array to convert
     * @param which indicates whether or not to skip the first parameter
     * @return a String of the parameters separated by spaces
     */
    private String parseParameters(String[] parameters, boolean which) {
	if (parameters == null || parameters.length <= 0)
		return "";
	int i;
	if (which)
	    // In this case skip the first parameter which is the path to the process
	    i = 1;
	else
	    // In this case, get all of the parameters which does not include the process
	    i = 0;
	String paramList = "";
	for (int j = i; j < parameters.length; j++) 
	    paramList = paramList + parameters[j] + " ";
	    return paramList;
    }
    
    int completer(CLI cli, Input input, int cursor, List completions) {
	return CompletionFactory.completeFileName(cli, input, cursor,
		completions);
    }
}