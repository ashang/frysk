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

import java.util.Iterator;
import java.util.LinkedList;

import frysk.debuginfo.DebugInfoFrame;
import frysk.proc.Task;

public class StepFinishCommand extends Command {

    private static final String full = "The finish command defines a "
	    + "'step-out' command, which \n"
	    + "is used when the user has what they need from stepping \n"
	    + "through a function, and would like to quickly return to\n"
	    + "the calling function to continue debugging there.";

    StepFinishCommand() {
	super("finish", "Step out of function", "finish", full);
    }

    public void interpret(CLI cli, Input cmd) {
	PTSet ptset = cli.getCommandPTSet(cmd);
	if (cmd.size() == 1 && cmd.parameter(0).equals("-help")) {
	    cli.printUsage(cmd);
	    return;
	}
	LinkedList taskList = new LinkedList();
	Iterator taskIter = ptset.getTasks();
	while (taskIter.hasNext()) {
	    taskList.add(taskIter.next());
	}
	if (cli.steppingObserver != null) {
	    cli.getSteppingEngine().stepOut(taskList);

	    synchronized (cli.steppingObserver.getMonitor()) {
		try {
		    // XXX This looks racy.
		    cli.steppingObserver.getMonitor().wait();
		} catch (InterruptedException ie) {
		}
	    }
	    taskIter = ptset.getTasks();
	    while (taskIter.hasNext()) {
		Task task = (Task) taskIter.next();
		DebugInfoFrame rf = cli.getTaskFrame(task);

		if (rf.getLines().length == 0)
		    cli.addMessage("Task stopped at address 0x"
			    + Long.toHexString(rf.getAdjustedAddress()),
			    Message.TYPE_NORMAL);
		else
		    cli.addMessage("Task stopped at line "
			    + rf.getLines()[0].getLine() + " in file "
			    + rf.getLines()[0].getFile(), Message.TYPE_NORMAL);
	    }
	} else
	    cli.addMessage("Not attached to any process",
		    Message.TYPE_ERROR);
    }

}
