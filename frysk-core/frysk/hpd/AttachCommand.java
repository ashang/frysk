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

import java.text.ParseException;
import java.util.ArrayList;
import frysk.proc.ProcId;
import java.util.Iterator;
import frysk.proc.Host;
import frysk.proc.Task;
import frysk.proc.Manager;

class AttachCommand
    implements CommandHandler
{
    private final CLI cli;
    AttachCommand(CLI cli)
    {
	this.cli = cli;
    }
    public void handle(Command cmd) throws ParseException
    {
	ArrayList params = cmd.getParameters();
	int pid = 0;
	int tid = 0;
	if (params.size() == 1 && params.get(0).equals("-help")) {
	    cli.printUsage(cmd);
	    return;
	}
	boolean cliOption = true;

	if (params.size() < 1) {
	    cli.printUsage(cmd);
	    return;
	}
 
	for (int idx = 0; idx < params.size(); idx++) {
	    if (((String)params.get(idx)).equals("-cli"))
		cliOption = true;
	    else if (((String)params.get(idx)).equals("-no-cli"))
		cliOption = false;
	    else if (((String)params.get(idx)).equals("-task")) {
		idx += 1;
		tid = Integer.parseInt(((String)params.get(idx)));
	    }
	    else if (((String)params.get(idx)).indexOf('-') == 0) {
		cli.printUsage(cmd);
		return;
	    }
	    else if (((String)params.get(idx)).matches("[0-9]+"))
		pid = Integer.parseInt((String)params.get(idx)); 
	}

	if (cliOption) {
	    cli.procSearchFinished = false;
	    Manager.host.requestFindProc(new ProcId(pid),
					 new Host.FindProc() {
		    public void procFound (ProcId procId)
		    {
			synchronized (cli) {
			    cli.proc = Manager.host.getProc(procId);
			    cli.procSearchFinished = true;
			    cli.notifyAll();
			}
		    }

		    public void procNotFound (ProcId procId, Exception e)
		    {
			synchronized (cli) {
			    cli.proc = null;
			    cli.procSearchFinished = true;
			    cli.notifyAll();
			}
		    }});
	    synchronized (cli) {
		while (!cli.procSearchFinished) {
		    try {
			cli.wait();
		    }
		    catch (InterruptedException ie) {
			cli.proc = null;
		    }
		}
	    }
	}
	if (cli.proc == null) {
	    cli.addMessage("Couldn't find process " + pid,
			   Message.TYPE_ERROR);
	    return;
	}

	if (pid == tid || tid == 0)
	    cli.task = cli.proc.getMainTask();
	else
	    for (Iterator i = cli.proc.getTasks ().iterator ();
		 i.hasNext (); ) {
		cli.task = (Task) i.next ();
		if (cli.task.getTid () == tid)
		    break;
	    }
	if (cliOption) {
	    cli.startAttach(pid, cli.proc, cli.task);
	    cli.finishAttach();
	}
	else {
	    // This can't work because the event loop isn't started in
	    // the non-cli case, so we can't find the proc.
	    // symtab = new SymTab(pid, proc, task, null); 
	}
    }
}
