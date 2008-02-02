// This file is part of the program FRYSK.
//
// Copyright 2005, 2006, 2007, 2008 Red Hat Inc.
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

//import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import frysk.stepping.SteppingEngine;
import frysk.proc.Task;

class GoCommand extends ParameterizedCommand {
    private static String full = "Continue running a process, returning "
	    + "without blocking.  The go command\n"
	    + "resumes execution of a collection of processes. The prompt will "
	    + "then be\n"
	    + "returned so that the user can issue further commands; "
	    + "execution\n" + "continues behind the scene.";

    GoCommand() {
	super("Continue a process.", "go", full);
    } 

    void interpret(CLI cli, Input cmd, Object options) {
	PTSet ptset = cli.getCommandPTSet(cmd);
	if (cli.steppingObserver != null) {
	    Iterator taskIter = ptset.getTasks();
	    SteppingEngine steppingEngine = cli.getSteppingEngine();

	    while (taskIter.hasNext()) {
		Task task = (Task) taskIter.next();
		if (!steppingEngine.isTaskRunning(task)) {
		    /* Try to continue task, if an exception occurs it is 
		     * probably because it is already running and previously
		     * has not been marked as such.  Until the
		     * method task.getStateFIXME is fixed, this may be the best we
		     * can do for now.
		     */
		    try {
			if (CLI.notRunningProc(task.getProc().getPid(), cli.getLoadedProcs()) || 
				CLI.notRunningProc(task.getProc().getPid(), cli.getCoreProcs())) {
				cli.addMessage("Cannot use 'go' on a loaded or core file, must " +
					"use 'start' first",
					Message.TYPE_ERROR);
				continue;
			}
			steppingEngine.continueExecution(task);
			cli.addMessage("Running process " + task.getProc().getPid(),
				Message.TYPE_NORMAL);
		    } catch (Exception e) {
			// OK, caught an exception, try to set the task to running
			try {
			    steppingEngine.setTaskRunning(task);
			} catch (Exception err) {
			    cli.addMessage("Process " + task.getProc().getPid() + " already running",
				    Message.TYPE_NORMAL);
			}
		    }
		}
	    }
	} else
	    cli.addMessage("Not attached to any process", Message.TYPE_ERROR);
    }
    
    int completer(CLI cli, Input input, int cursor, List completions) {
	return -1;
    }
}
