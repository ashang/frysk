// This file is part of the program FRYSK.
//
// Copyright 2005, 2006, 2007 Red Hat Inc.
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
import frysk.proc.Proc;
import frysk.proc.ProcId;
import frysk.proc.Task;
import frysk.proc.Manager;
import frysk.proc.FindProc;

class AttachCommand
    extends Command {
    private class ProcFinder implements FindProc {
	Proc proc = null;

	boolean procSearchFinished = false;

	public synchronized void procFound(ProcId procId) {
	    proc = Manager.host.getProc(procId);
	    procSearchFinished = true;
	    notifyAll();
	}

	public synchronized void procNotFound(ProcId procId, Exception e) {
	    proc = null;
	    procSearchFinished = true;
	    notifyAll();
	}
    }

    private static final String full = "The attach command causes the debugger "
	    + "to attach to an existing\n"
	    + "process(es), making it possible to continue the process' "
	    + "execution under\n"
	    + "debugger control. The command applies at the process level; all "
	    + "threads\n"
	    + "corresponding to the process will be attached by the operation. "
	    + "It is\n"
	    + "the user's responsibility to ensure that the process(es) "
	    + "actually is\n" + "executing the specified executable.";

    AttachCommand() {
	super("attach", "Attach to a running process.",
	      "attach [executable] pid [-task tid]", full);
    }

    public void parse(CLI cli, Input cmd) {
	int pid = 0;
	int tid = 0;
	if (cmd.size() == 1 && cmd.parameter(0).equals("-help")) {
	    cli.printUsage(cmd);
	    return;
	}

	if (cmd.size() < 1) {
	    cli.printUsage(cmd);
	    return;
	}

	for (int idx = 0; idx < cmd.size(); idx++) {
	    if (cmd.parameter(idx).equals("-task")) {
		idx += 1;
		tid = Integer.parseInt(cmd.parameter(idx));
	    } else if (cmd.parameter(idx).indexOf('-') == 0) {
		cli.printUsage(cmd);
		return;
	    } else if (cmd.parameter(idx).matches("[0-9]+"))
		pid = Integer.parseInt(cmd.parameter(idx));
	}

	ProcFinder findProc = new ProcFinder();
	Manager.host.requestFindProc(new ProcId(pid), findProc);
	synchronized (findProc) {
	    while (!findProc.procSearchFinished) {
		try {
		    findProc.wait();
		} catch (InterruptedException ie) {
		    findProc.proc = null;
		}
	    }
	}
	if (findProc.proc == null) {
	    cli.addMessage("Couldn't find process " + pid, Message.TYPE_ERROR);
	    return;
	}
	int procID = cli.idManager.reserveProcID();
	Task task = null;
	if (pid == tid || tid == 0)
	    task = findProc.proc.getMainTask();
	else
	    for (Iterator i = findProc.proc.getTasks().iterator(); i.hasNext();) {
		task = (Task) i.next();
		if (task.getTid() == tid)
		    break;
	    }
	cli.doAttach(pid, findProc.proc, task);
        cli.getSteppingEngine().getBreakpointManager()
            .manageProcess(findProc.proc);
	cli.idManager.manageProc(findProc.proc, procID);

    }
}
