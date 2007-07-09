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

import java.io.PrintWriter;
import java.text.ParseException;
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

class ActionsCommand
    extends CLIHandler
{
    private static final String descr = "List action points";

    private ActionsCommand(String name, CLI cli)
    {
	super(name, cli, new CommandHelp(name, descr, "actionpoints",
					 descr));
    }

    ActionsCommand(CLI cli)
    {
	this("actionpoints", cli);
    }

    private static final Map.Entry[] dummy = new Map.Entry[] {};

    private static class TaskComparator implements Comparator {
	public int compare(Object o1, Object o2) {
	    Map.Entry me1 = (Map.Entry)o1;
	    Map.Entry me2 = (Map.Entry)o2;
	    int id1 = ((Task)me1.getKey()).getTaskId().intValue();
	    int id2 = ((Task)me2.getKey()).getTaskId().intValue();
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
     * option, which will output the current displays
     * (non-Javadoc)
     * @see frysk.cli.hpd.CLIHandler#handle(frysk.cli.hpd.Command)
     */
    public void handle(Command cmd) throws ParseException 
    {
	/*
	 * TODO: According to the HPD spec, this command outputs a number
	 * of different types of actionpoints. How do we segment these 
	 * when displaying them to the user so that they remain readable?
	 */
	/*
	 * TODO: Parse the command arguments and change the output accordingly
	 */
	
	// Print out the breakpoints
	BreakpointManager bpManager = cli.getSteppingEngine().getBreakpointManager();
	Iterator iterator = bpManager.getBreakpointTableIterator();
	PrintWriter outWriter = cli.getPrintWriter();
	outWriter.println("BREAKPOINTS");
	while (iterator.hasNext()) {
	    SourceBreakpoint bpt = (SourceBreakpoint)iterator.next();
	    outWriter.print(bpt.getId() + " ");
	    if (bpt.getUserState() == SourceBreakpoint.ENABLED) {
		outWriter.print(" y ");
	    }
	    else {
		outWriter.print(" n ");
	    }
	    bpt.output(outWriter);
	    outWriter.print(" ");
	    // Print tasks in which breakpoint is enabled
	    Set taskEntrySet = bpt.getTaskStateMap().entrySet();
	    Map.Entry[] taskEntries = (Map.Entry[])taskEntrySet.toArray(dummy);
	    Arrays.sort(taskEntries, taskComparator);
	    for (int i = 0; i < taskEntries.length; i++) {
		int id
		    = ((Task)taskEntries[i].getKey()).getTaskId().intValue();
		SourceBreakpoint.State state
		    = (SourceBreakpoint.State)taskEntries[i].getValue();
		if (state == SourceBreakpoint.ENABLED) {
		    outWriter.print(id);
		    outWriter.print(" ");
		}
	    }
	    outWriter.println();
	}
	
	// Print out the displays
	outWriter.println("\nDISPLAYS");
	iterator = DisplayManager.getDisplayIterator();
	while(iterator.hasNext())
	{
	    UpdatingDisplayValue uDisp = (UpdatingDisplayValue) iterator.next();
	    outWriter.print(uDisp.getId() + " ");
	    if(uDisp.isEnabled())
		outWriter.print(" y ");
	    else
		outWriter.print(" n ");
	    outWriter.print(uDisp.getName() + " ");
	    outWriter.print(uDisp.getTask().getTaskId().intValue());
	    outWriter.println();
	}
    }
}
