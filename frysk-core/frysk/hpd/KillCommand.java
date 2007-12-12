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

import java.util.ArrayList;
import java.util.Iterator;
import frysk.proc.Proc;
import frysk.proc.Task;
import java.util.List;

/**
 * KillCommand kills the processes in the current target set.
 */

public class KillCommand extends ParameterizedCommand {
    private static String full = "kill the processes that are currently in " +
    	"the current target set.  The processes are then reloaded and then " +
    	"ready to be run again.";

    KillCommand() {
	super("kill the current targetset", "kill", full);
    }

    public void interpret(CLI cli, Input cmd, Object options) {
	
	ArrayList saveProcs = new ArrayList();
	int procPID = 0;
	Iterator foo = cli.targetset.getTasks();
	while (foo.hasNext()) {
	    Task task = (Task) foo.next();
	    Proc proc = task.getProc();
	    if (proc.getPid() != procPID) {
		cli.addMessage("Killing process " + proc.getPid(),
		//	" that was created from " + proc.getExe(),
			Message.TYPE_NORMAL);
		// Save the procs we are killing so we can re-load them later
		saveProcs.add(proc.getExe());
		procPID = proc.getPid();
		// Now, call the Proc object to kill off the executable(s)
		proc.requestKill();
	    }
	}
	
	synchronized (cli) {
	    // Clear the running procs set
	    cli.runningProcs.clear();
	    // Clear the current targetset
	    cli.idManager.clearProcIDs();
	    // Clear the stepping engine structures
	    cli.steppingEngine.clear();
	    // Add back in the stepping observer for cli
	    cli.steppingEngine.addObserver(cli.steppingObserver);
	}
	// Now loop through and re-load all of the killed procs
	Iterator bar = saveProcs.iterator();
	while (bar.hasNext()) {
	    String cmdline = (String) bar.next();
	    cli.execCommand("load " + cmdline + "\n");
	}
    }

    int completer(CLI cli, Input input, int cursor, List completions) {
	return CompletionFactory.completeFileName(cli, input, cursor,
						  completions);
    }
}
