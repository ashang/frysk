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

import frysk.proc.Task;
import frysk.rt.BreakpointManager;
import frysk.rt.DisplayManager;
import frysk.rt.SourceBreakpoint;
import frysk.rt.UpdatingDisplayValue;

import java.io.PrintWriter;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

class DisableCommand extends Command {
    private static final String descr = "disable a source breakpoint";

    DisableCommand(CLI cli) {
	super(cli, "disable", descr, "disable actionpointID", descr);
    }

    public void parse(Input cmd) throws ParseException {
	PTSet ptset = cli.getCommandPTSet(cmd);
	String actionpoints = "";
	boolean disEnabled = false/* , disDisabled = false */, disBreak = false;
	boolean disDisplay = false, disWatch = false, disBarrier = false;
	ArrayList args = cmd.getParameters();
	int[] ids = null;

	PrintWriter outWriter = cli.getPrintWriter();

	/*
         * Parse the command line arguments. There should be at most one, and it
         * should either be a comma-delimited list of actionpoint ids or one of
         * the "-" options specified in the hpd. We also allow a "-display"
         * option to disable only displays
         */
	if (args.size() == 0)
	    throw new ParseException("Too few arguments to disable", 0);
	if (args.size() > 1)
	    throw new ParseException("Too many arguments to disable", 0);

	String param = (String) args.get(0);
	// doesn't start with a dash, must be the list of actionpoints
	if (param.indexOf("-") != 0)
	    actionpoints = param;
	// starts with a '-', must be an argument
	else if (param.equals("-enabled"))
	    disEnabled = true;
	// TODO: 'disable -disabled' seems like a no-op
	// else if (param.equals("-disabled"))
	// disDisabled = true;
	else if (param.equals("-break"))
	    disBreak = true;
	else if (param.equals("-display"))
	    disDisplay = true;
	else if (param.equals("-watch"))
	    disWatch = true;
	else if (param.equals("-barrier"))
	    disBarrier = true;
	else if (param.equals("-help")) {
	    cli.printUsage(cmd);
	    return;
	} else
	    throw new ParseException("Unknown argument " + param
		    + " to disable", 0);

	// generate a list of actionpoints to disable
	if (!actionpoints.equals("")) {
	    String[] points = actionpoints.split(",");
	    ids = new int[points.length];
	    for (int i = 0; i < points.length; i++)
		try {
		    ids[i] = Integer.parseInt(points[i]);
		} catch (NumberFormatException e) {
		    throw new ParseException("Invalid actionpoint id "
			    + points[i], 0);
		}
	    Arrays.sort(ids);
	}

	// If a list of actionpoints were supplied, disable them and exit
	if (ids != null) {
	    for (int i = 0; i < ids.length; i++) {
		BreakpointManager bpManager = cli.getSteppingEngine()
			.getBreakpointManager();
		SourceBreakpoint bpt = bpManager.getBreakpoint(ids[i]);
		if (bpt != null) {
		    Iterator taskIter = ptset.getTasks();
		    while (taskIter.hasNext()) {
			bpManager
				.disableBreakpoint(bpt, (Task) taskIter.next());
		    }
		    outWriter
			    .println("breakpoint " + bpt.getId() + " disabled");
		}
		// Failed to get a breakpoint, try to get a display instead
		else if (DisplayManager.disableDisplay(ids[i])) {
		    outWriter.println("display " + ids[i] + " disabled");
		} else {
		    outWriter.println("no such actionpoint");
		}
	    }

	    return;
	}

	/*
         * Disable breakpoints For our purposes -break and -enabled are
         * equivalent here, as disabling all breakpoints is identical to
         * disabling all enabled breakpoints.
         */
	if (disEnabled || disBreak) {
	    BreakpointManager bpManager = cli.getSteppingEngine()
		    .getBreakpointManager();
	    Iterator iter = bpManager.getBreakpointTableIterator();
	    while (iter.hasNext()) {
		SourceBreakpoint bpt = (SourceBreakpoint) iter.next();
		if (bpt.getUserState() == SourceBreakpoint.ENABLED) {
		    Iterator taskIter = ptset.getTasks();
		    while (taskIter.hasNext()) {
			bpManager
				.disableBreakpoint(bpt, (Task) taskIter.next());
		    }
		    outWriter
			    .println("breakpoint " + bpt.getId() + " disabled");
		}
	    }
	}

	/*
         * Disable displays Similar to breakpoints, -enable also means we
         * disable all the displays.
         */
	if (disEnabled || disDisplay) {
	    Iterator iter = DisplayManager.getDisplayIterator();
	    while (iter.hasNext()) {
		UpdatingDisplayValue uDisp = (UpdatingDisplayValue) iter.next();
		if (uDisp.isEnabled()) {
		    uDisp.disable();
		    outWriter.println("display " + uDisp.getId() + " disabled");
		}
	    }
	}

	/*
         * Disable Watchpoints
         */
	if (disEnabled || disWatch) {

	}

	/*
         * Disable Barriers
         */
	if (disEnabled || disBarrier) {

	}
    }
}
