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
import frysk.proc.ProcTasksAction;
import frysk.proc.Task;
import frysk.util.CountDownLatch;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import frysk.proc.TaskAttachedObserverXXX;

/**
 * Due to a lot of similar code in StartCommand/RunCommand this class was
 * created to consolidate most of the methods to one code base.
 */
abstract class StartRun extends ParameterizedCommand {
    
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
		    	run(cli, cmd, task.getProc(), runToBreak);
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
	    runProcs(cli, procSet, cmd, runToBreak);
	    synchronized (cli) {
		cli.loadedProcs.clear();
	    }
	}

	/* Check to see if there were procs loaded from a core command */
	if (!cli.coreProcs.isEmpty()) {
	    Set coreSet = cli.coreProcs.entrySet();
	    runProcs(cli, coreSet, cmd, runToBreak);
	    synchronized (cli) {
		cli.coreProcs.clear();
	    }
	}
    }

    private void run(CLI cli, Input cmd, Proc template, boolean runToBreak) {
	Runner runner = new Runner(cli);
	if (cmd.size() == 0) {
	    cli.addMessage("starting/running with this command: " + 
			   asString(template.getCmdLine()),
			   Message.TYPE_NORMAL);
	    Manager.host.requestCreateAttachedProc(template, runner);
	} else {
	    String[] args = new String[cmd.size() + 1];
	    args[0] = template.getCmdLine()[0];
	    for (int i = 1; i < args.length; i++)
		args[i] = cmd.parameter(i - 1);
	    cli.addMessage("starting/running with this command: " + 
			   asString(args), Message.TYPE_NORMAL);
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
	cli.doAttach(runner.launchedTask.getProc(), runToBreak);
	runner.launchedTask.requestUnblock(runner);
    }

    private String asString(String[] args) {
	StringBuffer b = new StringBuffer(args[0]);
	for (int i = 1; i < args.length; i++) {
	    b.append(" ");
	    b.append(args[i]);
	}
	return b.toString();
    }

    /**
     * runProcs does as the name implies, it runs procs found to be loaded by a
     * load or a core command.
     * 
     * @param cli is the current commandline interface object
     * @param procs is the set of procs to be run
     * @param cmd is the command object to use to start the proc(s)
     */
    private void runProcs(CLI cli, Set procs, Input cmd, boolean runToBreak) {
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
	    run(cli, cmd, proc, runToBreak);
	    synchronized (cli) {
		cli.taskID = -1;
	    }
	}
    }
    
    int completer(CLI cli, Input input, int cursor, List completions) {
	return CompletionFactory.completeFileName(cli, input, cursor,
		completions);
    }
}