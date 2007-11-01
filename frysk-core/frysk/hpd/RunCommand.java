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

class RunCommand extends Command {
    private static final String descr = "run program and immediately attach";
    // Used to synchronize with updateAttached method
    RunCommand() {
	super("run", descr, "run executable arguments*", descr);
    }

    private static class Runner implements TaskObserver.Attached {
	private final CLI cli;
	final CountDownLatch latch = new CountDownLatch(1);
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

    public void interpret(CLI cli, Input cmd) {
	if (cmd.size() < 1) {
	    cli.printUsage(cmd);
	    return;
	}
	Runner runner = new Runner(cli);
	Manager.host.requestCreateAttachedProc(cmd.parameters(), runner);
        try {
            runner.latch.await();
        } catch (InterruptedException e) {
            return;
        }
        // register with SteppingEngine
	cli.doAttach(runner.launchedTask);
        cli.getSteppingEngine().getBreakpointManager()
            .manageProcess(runner.launchedTask.getProc());
        cli.idManager.manageProc(runner.launchedTask.getProc(),
				 cli.idManager.reserveProcID());
        runner.launchedTask.requestUnblock(runner);
    }
}
