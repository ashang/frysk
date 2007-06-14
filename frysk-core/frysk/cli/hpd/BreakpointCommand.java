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

package frysk.cli.hpd;

import frysk.proc.Task;
import frysk.rt.BreakpointManager;
import frysk.rt.FunctionBreakpoint;
import frysk.rt.LineBreakpoint;
import frysk.rt.SourceBreakpoint;
import frysk.rt.SourceBreakpointObserver;
import java.io.PrintWriter;
import java.text.ParseException;
import java.util.ArrayList;
import javax.naming.NameNotFoundException;
import lib.dw.DwarfDie;

class BreakpointCommand
    extends CLIHandler
{
    private static final String descr = "set source breakpoint";

    private BreakpointCommand(String name, CLI cli)
    {
	super(name, cli, new CommandHelp(name, descr, "break @file@lineno", descr));
    }

    BreakpointCommand(CLI cli)
    {
	this("break", cli);
    }

    static private class CLIBreakpointObserver
	implements SourceBreakpointObserver
    {
	private CLI cli;
	public CLIBreakpointObserver(CLI cli) {
	    this.cli = cli;
	}
	public void addedTo (Object observable) {}
	public void addFailed (Object observable, Throwable w) {}
	public void deletedFrom (Object observable) {}
	public void updateHit(SourceBreakpoint bpt, Task task, long address) {
	    //XXX This cli.running business is a hack for other
	    //commands in CLI (where) and needs to go away when
	    //multiple processes / tasks are supported.
	    cli.running = false;
	}
    }
  
    public void handle(Command cmd) throws ParseException
    {
	cli.refreshSymtab();
	ArrayList params = cmd.getParameters();
	if (params.size() != 1) {
	    cli.printUsage(cmd);
	    return;
	}
	String breakpt = (String)params.get(0);
	String fileName;
	int lineNumber;
	SourceBreakpoint actionpoint;
	BreakpointManager bpManager = cli.getSteppingEngine().getBreakpointManager();
	Task task = cli.getTask();
	final PrintWriter outWriter = cli.getPrintWriter();
	if (breakpt.charAt(0) == '@') {
	    String[] bptParams = breakpt.split("@");
	    if (bptParams.length != 3) {
		// XXX should use notion of "current" source file
		throw new ParseException("bad syntax for breakpoint:" + breakpt,
					 0);
	    }
	    fileName = bptParams[1];
	    lineNumber = Integer.parseInt((String)bptParams[2]);
	    actionpoint = bpManager.addLineBreakpoint(fileName, lineNumber, 0);
	    actionpoint.addObserver(new CLIBreakpointObserver(cli) {
		    public void updateHit(SourceBreakpoint bpt, Task task,
					  long address) {
			super.updateHit(bpt, task, address);
			LineBreakpoint lbpt = (LineBreakpoint)bpt;
			outWriter.print("Breakpoint ");
			outWriter.print(lbpt.getId());
			outWriter.print(" @");
			outWriter.print(lbpt.getFileName());
			outWriter.print("@");
			outWriter.println(lbpt.getLineNumber());
		    }
		});
	}
	else {
	    DwarfDie die = null;
	    try {
		die = cli.debugInfo.getSymbolDie(breakpt);
	    }
	    catch (NameNotFoundException e) {
		// cli.getPrintWriter().println(e.getMessage());
		// return;
	    }

	    actionpoint = bpManager.addFunctionBreakpoint(breakpt, die);
	    actionpoint.addObserver(new CLIBreakpointObserver(cli)
		{
		    public void updateHit(SourceBreakpoint bpt, Task task,
					  long address)
		    {
			super.updateHit(bpt, task, address);
			FunctionBreakpoint fbpt = (FunctionBreakpoint)bpt;
			outWriter.print("Breakpoint ");
			outWriter.print(fbpt.getId());
			outWriter.print(" ");
			outWriter.println(fbpt.getName());
		    }
		});
	}

	bpManager.enableBreakpoint(actionpoint, task);
	cli.getPrintWriter().println("breakpoint " + actionpoint.getId());
    }
}
