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

import frysk.dwfl.DwflCache;
import frysk.proc.Proc;
import frysk.proc.Manager;
import frysk.proc.FindProc;
import java.util.List;

public class AttachCommand extends ParameterizedCommand {

    private class ProcFinder implements FindProc {
	Proc proc = null;

	boolean procSearchFinished = false;

	public synchronized void procFound(Proc proc) {
	    this.proc = proc;
	    procSearchFinished = true;
	    notifyAll();
	}

	public synchronized void procNotFound(int pid) {
	    proc = null;
	    procSearchFinished = true;
	    notifyAll();
	}
    }

    AttachCommand() {
	super("Attach to a running process.",
	      "attach <pid> ... [ -sysroot Path ]",
	      ("The attach command causes the debugger to attach to an"
	       + " existing process(es), making it possible to continue"
	       + " the process' execution under debugger control.  The"
	       + " command applies at the process level; all threads"
	       + " corresponding to the process will be attached by the"
	       + " operation.  It is the user's responsibility to ensure"
	       + " that the process(es) actually is executing the specified"
	       + " executable."));
        add(new CommandOption("sysroot", "pathname to use as a sysroot",
        "Pathname") {
            void parse(String args, Object options) {
        	((Options)options).sysroot = args;
            }
        });
    }


    private static class Options {
	String sysroot = "/";
    }
    Object options() {
	return new Options();
    }
    
    public void interpret(CLI cli, Input cmd, Object options) {
	Options o = (Options)options;

	if (cmd.size() == 0) {
	    throw new InvalidCommandException("Missing process ID");
	}
	for (int i = 0; i < cmd.size(); i++) {
	    int pid = Integer.parseInt(cmd.parameter(i));
	    ProcFinder findProc = new ProcFinder();
	    Manager.host.requestProc(pid, findProc);
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
		cli.outWriter.print("Couldn't find process ");
		cli.outWriter.println(pid);
		continue;
	    }
	    
	    attach(findProc.proc, cli, o.sysroot);
	}
    }

    public static void attach(Proc proc, CLI cli) {
    attach(proc, cli, null);
    }

    public static void attach(Proc proc, CLI cli, String sysroot) {
	DwflCache.setSysroot(proc.getMainTask(), sysroot);
	cli.doAttach(proc, false);
    }

    int completer(CLI cli, Input input, int base, List completions) {
	return -1;
    }
}
