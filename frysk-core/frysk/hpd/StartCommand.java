// This file is part of the program FRYSK.
//
// Copyright 2007, Red Hat Inc.
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

class StartCommand extends ParameterizedCommand {
    
    StartCommand() {
	super("start a process, run until the first executable instruction",
		"start <executable>",
		"The start command alllows the debuger to run a(any) program(s)"
		      + " that has(have) either been previously loaded via a load command"
		      + " if no parameters are given. To run an executable, just"
		      + " give the start command the path to the executable as a"
		      + " parameter.  In either case the debugger attaches immediately"
		      + " to the process and runs to the first executable instruction.");
    }
    
    private static class Runner implements TaskObserver.Attached {
	private final CLI cli;
	private CountDownLatch latch;
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
    
    public void interpret(CLI cli, Input cmd, Object options) {
	/* If the run command is given no args, check to see if 
	   any procs were loaded with the load command or loaded
	   when fhpd was started or loaded with the core command*/
	Iterator foo = cli.targetset.getTasks();
	if (cmd.size() < 1 && foo.hasNext()) {
	    if (cli.coreProcs.isEmpty() && cli.loadedProcs.isEmpty()) {
		cli.execCommand("kill");
		cli.execCommand("start");
		return;
	    }
	} else if (cmd.size() < 1 && !foo.hasNext()) {
	    cli.addMessage("No procs in targetset to run", 
		    Message.TYPE_NORMAL);
	    return;
	}
	
	// If a parameter was given the run command, go ahead and run it
	if (cmd.size() >= 1) {
	    
	    run(cli, cmd);
	    return;
	}
	
	/* If we made it here, a run command was given with no parameters
	 * and there should be either running procs or loaded procs or
	 * core procs
	 */

	/* This is the case where there are loaded procs */
	if (!cli.loadedProcs.isEmpty()) {
	    Set procSet = cli.loadedProcs.entrySet();
	    runProcs(cli, procSet);
	    synchronized (cli) {
		cli.loadedProcs.clear();
	    }
	}
	
	/* Check to see if there were procs loaded from a core command */
	if (!cli.coreProcs.isEmpty()) {
	    Set coreSet = cli.coreProcs.entrySet();
	    runProcs(cli, coreSet);
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
        cli.doAttach(runner.launchedTask.getProc());
	runner.launchedTask.requestUnblock(runner);
    }
    
    /*
     * runProcs does as the name implies, it runs procs found to be loaded by a
     * load or a core command.
     */
    private void runProcs(CLI cli, Set procs) {
	Iterator foo = procs.iterator();
	while (foo.hasNext()) {
	    Map.Entry me = (Map.Entry) foo.next();
	    Proc proc = (Proc) me.getKey();
	    Integer taskid = (Integer) me.getValue();
	    // Set the TaskID to be used to what was used when the
	    // proc was loaded with the core or load commands
	    synchronized (cli) {
		cli.taskID = taskid.intValue();
	    }
	    cli.execCommand("start " + proc.getExe());
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