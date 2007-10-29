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

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import frysk.proc.Task;
import frysk.rt.BreakpointManager;
import frysk.rt.DisplayManager;
import frysk.rt.SourceBreakpoint;
import frysk.rt.UpdatingDisplayValue;

class ActionsCommand extends Command {
    private static final String descr = "List action points";

    ActionsCommand() {
	super("actionpoints", descr, "actionpoints", descr);
    }


    private static final Map.Entry[] dummy = new Map.Entry[] {};

    private static class TaskComparator implements Comparator {
	public int compare(Object o1, Object o2) {
	    Map.Entry me1 = (Map.Entry) o1;
	    Map.Entry me2 = (Map.Entry) o2;
	    int id1 = ((Task) me1.getKey()).getTaskId().intValue();
	    int id2 = ((Task) me2.getKey()).getTaskId().intValue();
	    if (id1 < id2)
		return -1;
	    else if (id1 > id2)
		return 1;
	    else
		return 0;
	}
    }

    private static final TaskComparator taskComparator = new TaskComparator();

    /*
         * Print out the specified actionpoints. These will be filtered as per
         * the possible arguments in the hpd. We have also added the --display
         * option, which will output the current displays (non-Javadoc)
         * 
         * @see frysk.hpd.CLIHandler#handle(frysk.hpd.Command)
         */
    public void parse(CLI cli, Input cmd) {
	String actionpoints = "";
	boolean showEnabled = false, showDisabled = false, showBreak = false, showDisplay = false, showWatch = false, showBarrier = false;
	int[] ids = null;

	PrintWriter outWriter = cli.getPrintWriter();

	/*
         * Parse the command line arguments.  There should be at most
         * one, and it should either be a comma-delimited list of
         * actionpoint ids or one of the "-" options specified in the
         * hpd. We also allow a "-display" option to show only
         * displays
         */
	if (cmd.size() > 0) {
	    if (cmd.size() > 1)
		throw new InvalidCommandException
		    ("Too many arguments to actionpoints");

	    String param = cmd.parameter(0);
	    // doesn't start with a dash, must be the list of actionpoints
	    if (param.indexOf("-") != 0)
		actionpoints = param;
	    // starts with a '-', must be an argument
	    else if (param.equals("-enabled"))
		showEnabled = true;
	    else if (param.equals("-disabled"))
		showDisabled = true;
	    else if (param.equals("-break"))
		showBreak = true;
	    else if (param.equals("-display"))
		showDisplay = true;
	    else if (param.equals("-watch"))
		showWatch = true;
	    else if (param.equals("-barrier"))
		showBarrier = true;
	    else
		throw new InvalidCommandException
		    ("Unknown argument " + param + " to actionpoints");
	}

	// generate a list of actionpoints to display
	/*
         * TODO: We should probably look for a more efficient way of printing
         * out only specific action points, as the current method of iterating
         * through everything and only printing out the points specified could
         * become costly
         */
	if (!actionpoints.equals("")) {
	    String[] points = actionpoints.split(",");
	    ids = new int[points.length];
	    for (int i = 0; i < points.length; i++)
		try {
		    ids[i] = Integer.parseInt(points[i]);
		} catch (NumberFormatException e) {
		    throw new InvalidCommandException
			("Invalid actionpoint id " + points[i]);
		}
	    Arrays.sort(ids);
	}

	// If none of the flags were set, we display everything: set them all
	if (!showEnabled && !showDisabled && !showBarrier && !showBreak
		&& !showDisplay && !showWatch)
	    showEnabled = showDisabled = showDisplay = showBarrier = showBreak = showWatch = true;

	// Print out the breakpoints
	if (showBreak || showEnabled || showDisabled) {
	    BreakpointManager bpManager = cli.getSteppingEngine()
		    .getBreakpointManager();
	    Iterator iterator = bpManager.getBreakpointTableIterator();
	    if (iterator.hasNext())
		outWriter.println("BREAKPOINTS");
	    while (iterator.hasNext()) {
		SourceBreakpoint bpt = (SourceBreakpoint) iterator.next();

		/*
                 * Only display enabled/disblaed breakpoints if the appropriate
                 * flags are set. We include the showEnabled || showDisabled so
                 * that we only care about this if at least one of the two flags
                 * is set.
                 */
		if (((bpt.getUserState() == SourceBreakpoint.ENABLED && !showEnabled) || (bpt
			.getUserState() == SourceBreakpoint.DISABLED && !showDisabled))
			&& (showEnabled || showDisabled))
		    continue;

		/*
                 * If we were given a list of points, only output breakpoints if
                 * they match one of the points
                 */
		if (ids != null && Arrays.binarySearch(ids, bpt.getId()) < 0)
		    continue;

		outWriter.print(bpt.getId() + " ");
		if (bpt.getUserState() == SourceBreakpoint.ENABLED) {
		    outWriter.print(" y ");
		} else {
		    outWriter.print(" n ");
		}
		bpt.output(outWriter);
		outWriter.print(" ");
		// Print tasks in which breakpoint is enabled
		Set taskEntrySet = bpt.getTaskStateMap().entrySet();
		Map.Entry[] taskEntries = (Map.Entry[]) taskEntrySet
			.toArray(dummy);
		Arrays.sort(taskEntries, taskComparator);
		for (int i = 0; i < taskEntries.length; i++) {
		    int id = ((Task) taskEntries[i].getKey()).getTaskId()
			    .intValue();
		    SourceBreakpoint.State state = (SourceBreakpoint.State) taskEntries[i]
			    .getValue();
		    if (state == SourceBreakpoint.ENABLED) {
			outWriter.print(id);
			outWriter.print(" ");
		    }
		}
		outWriter.println();
	    }
	    outWriter.println();
	}

	// Print out the displays
	if (showDisplay || showDisabled || showEnabled) {
	    Iterator iterator = DisplayManager.getDisplayIterator();
	    if (iterator.hasNext())
		outWriter.println("DISPLAYS");
	    while (iterator.hasNext()) {
		UpdatingDisplayValue uDisp = (UpdatingDisplayValue) iterator
			.next();

		/*
                 * Similar to the breakpoint section, if one of the enabled /
                 * disabled flags is set, only display displays of that type
                 */
		if (((uDisp.isEnabled() && !showEnabled) || (!uDisp.isEnabled() && !showDisabled))
			&& (showEnabled || showDisabled))
		    continue;

		/*
                 * If we are given a list of actionpoints to display, only
                 * display those points
                 */
		if (ids != null && Arrays.binarySearch(ids, uDisp.getId()) < 0)
		    continue;

		outWriter.print(uDisp.getId() + " ");
		if (uDisp.isEnabled())
		    outWriter.print(" y ");
		else
		    outWriter.print(" n ");
		outWriter.print("\"" + uDisp.getName() + "\" ");
		outWriter.print(uDisp.getTask().getTaskId().intValue());
		outWriter.println();
	    }
	    outWriter.println();
	}
    }
}
