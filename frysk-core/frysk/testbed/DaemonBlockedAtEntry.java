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

package frysk.testbed;

import java.io.File;
import frysk.proc.Action;
import frysk.proc.TaskObserver;
import frysk.proc.Task;
import frysk.proc.Manager;
import java.util.logging.Level;

/**
 * Creates an attached process halted at it's entry point address
 * (i.e., the program's first instruction).
 */
public class DaemonBlockedAtEntry {
    private final Task mainTask;
  
    private static class ExecBlockingObserver
	extends TaskObserverBase
	implements TaskObserver.Execed
    {
	private boolean fired = false;
	public void addedTo (Object o) {
	    super.addedTo(o);
	    Manager.eventLoop.requestStop();
	}
	public Action updateExeced (Task task) {
	    if (fired) {
		logger.log(Level.FINE,
			   "{0} first exec already occcured, continue\n",
			   this);
		// Only trigger the first time.
		return Action.CONTINUE;
	    }
	    logger.log(Level.FINE, "{0} first exec, blocking\n", this);
	    Manager.eventLoop.requestStop();
	    fired = true;
	    return Action.BLOCK;
	}
    }
    private final TaskObserver.Execed execBlockingObserver
	= new ExecBlockingObserver();
  
    /**
     * Create an attached process blocked at it's entry-point (i.e., just after
     * the exec).
     */
    public DaemonBlockedAtEntry(String[] argv) {
	// Create the child.
	ExecOffspring child = new ExecOffspring(new ExecCommand(argv));
	this.mainTask = child.findTaskUsingRefresh(true);
	// Add the exec observer that will block the task.
	mainTask.requestAddExecedObserver(execBlockingObserver);
	TestLib.assertRunUntilStop("add exec observer");
	// Run to the exec call.
	child.requestExec();
	TestLib.assertRunUntilStop("run to blocked exec");
    }
  
    /**
     * Create an attached process blocked at it's entry-point (i.e., just after
     * the exec).
     */
    public DaemonBlockedAtEntry(File program) {
	this(new String[] { program.getAbsolutePath() });
    }

    /**
     * Resume the attached process.
     */
    public void requestUnblock() {
	mainTask.requestUnblock(execBlockingObserver);
    }

    public void requestRemoveBlock() {
	mainTask.requestDeleteExecedObserver(execBlockingObserver);
    }
  
    public Task getMainTask () {
	return this.mainTask;
    }
}
