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
import java.util.List;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Comparator;

abstract class ActionPointCommands extends ParameterizedCommand {

    static private class Options {
	boolean disabled;
	boolean enabled;
	boolean present;
	boolean barriers;
	boolean breakpoints;
	boolean displaypoints;
	boolean watchpoints;
    }
    Object options() {
	return new Options();
    }

    private boolean argsRequired;

    ActionPointCommands(boolean argsRequired, String name, String syntax,
			String description) {
	super(name, syntax, description);
	this.argsRequired = argsRequired;
	add(new CommandOption("enabled", "only enabled action-points") {
		void parse(String arg, Object options) {
		    ((Options)options).present = true;
		    ((Options)options).enabled = true;
		}
	    });
	add(new CommandOption("disabled", "only disabled action-points") {
		void parse(String arg, Object options) {
		    ((Options)options).present = true;
		    ((Options)options).disabled = true;
		}
	    });
	add(new CommandOption("watch", "only watch-points") {
		void parse(String arg, Object options) {
		    ((Options)options).present = true;
		    ((Options)options).watchpoints = true;
		}
	    });
	add(new CommandOption("break", "only break-points") {
		void parse(String arg, Object options) {
		    ((Options)options).present = true;
		    ((Options)options).breakpoints = true;
		}
	    });
	add(new CommandOption("display", "only display-points") {
		void parse(String arg, Object options) {
		    ((Options)options).present = true;
		    ((Options)options).displaypoints = true;
		}
	    });
	add(new CommandOption("barrier", "only barriers") {
		void parse(String arg, Object options) {
		    ((Options)options).present = true;
		    ((Options)options).barriers = true;
		}
	    });
	add(new CommandOption("all", "all action-points") {
		void parse(String arg, Object options) {
		    ((Options)options).present = true;
		    ((Options)options).enabled = true;
		    ((Options)options).disabled = true;
		    ((Options)options).breakpoints = true;
		    ((Options)options).watchpoints = true;
		    ((Options)options).displaypoints = true;
		    ((Options)options).barriers = true;
		}
	    });
    }

    int complete(CLI cli, PTSet ptset, String incomplete, int base,
		 List alternatives) {
	return -1;
    }

    // FIXME: Should be able to implement the bulk of this interpret()
    // method as generic code.
    final void interpret(CLI cli, Input input, Object o) {
	Options options = (Options)o;
	if (argsRequired && input.size() == 0 && !options.present)
	    throw new InvalidCommandException("missing arguments");
	if (input.size() != 0 && options.present)
	    throw new InvalidCommandException("too many arguments");
	PTSet ptset = cli.getCommandPTSet(input);
	interpret(cli, ptset, input.stringValue(), options);
    }
    abstract void interpret(CLI cli, PTSet ptset, String actionpoints,
			    Options options);

    static class Actions extends ActionPointCommands {
	Actions() {
	    super(false, "actions", "actions", "List action points");
	}
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
	private final TaskComparator taskComparator = new TaskComparator();

	/*
         * Print out the specified actionpoints. These will be
         * filtered as per the possible arguments in the hpd. We have
         * also added the --display option, which will output the
         * current displays (non-Javadoc)
         * 
         * @see frysk.hpd.CLIHandler#handle(frysk.hpd.Command)
         */
	void interpret(CLI cli, PTSet ptset, String actionpoints,
		       Options options) {
	    int[] ids = null;
	    // generate a list of actionpoints to display
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
	    if (!options.present)
		options.enabled
		    = options.disabled
		    = options.displaypoints
		    = options.barriers
		    = options.breakpoints
		    = options.watchpoints
		    = true;
	    // Print out the breakpoints
	    if (options.breakpoints || options.enabled || options.disabled) {
		BreakpointManager bpManager = cli.getSteppingEngine()
		    .getBreakpointManager();
		Iterator iterator = bpManager.getBreakpointTableIterator();
		if (iterator.hasNext())
		    cli.outWriter.println("BREAKPOINTS");
		while (iterator.hasNext()) {
		    SourceBreakpoint bpt = (SourceBreakpoint) iterator.next();
		    // Only display enabled/disblaed breakpoints if
		    // the appropriate flags are set. We include the
		    // showEnabled || showDisabled so that we only
		    // care about this if at least one of the two
		    // flags is set.
		    if (((bpt.getUserState() == SourceBreakpoint.ENABLED
			  && !options.enabled)
			 || (bpt.getUserState() == SourceBreakpoint.DISABLED
			     && !options.disabled))
			&& (options.enabled || options.disabled))
			continue;
		    // If we were given a list of points, only output
		    // breakpoints if they match one of the points
		    if (ids != null
			&& Arrays.binarySearch(ids, bpt.getId()) < 0)
			continue;
		    cli.outWriter.print(bpt.getId());
		    cli.outWriter.print(" ");
		    if (bpt.getUserState() == SourceBreakpoint.ENABLED) {
			cli.outWriter.print(" y ");
		    } else {
			cli.outWriter.print(" n ");
		    }
		    bpt.output(cli.outWriter);
		    cli.outWriter.print(" ");
		    // Print tasks in which breakpoint is enabled
		    Set taskEntrySet = bpt.getTaskStateMap().entrySet();
		    Map.Entry[] taskEntries
			= new Map.Entry[taskEntrySet.size()];
		    taskEntrySet.toArray(taskEntries);
		    Arrays.sort(taskEntries, taskComparator);
		    for (int i = 0; i < taskEntries.length; i++) {
			int id = ((Task) taskEntries[i].getKey()).getTaskId()
			    .intValue();
			SourceBreakpoint.State state = (SourceBreakpoint.State) taskEntries[i]
			    .getValue();
			if (state == SourceBreakpoint.ENABLED) {
			    cli.outWriter.print(id);
			    cli.outWriter.print(" ");
			}
		    }
		    cli.outWriter.println();
		}
		cli.outWriter.println();
	    }
	    // Print out the displays
	    if (options.displaypoints || options.disabled || options.enabled) {
		Iterator iterator = DisplayManager.getDisplayIterator();
		if (iterator.hasNext())
		    cli.outWriter.println("DISPLAYS");
		while (iterator.hasNext()) {
		    UpdatingDisplayValue uDisp
			= (UpdatingDisplayValue) iterator.next();
		    // Similar to the breakpoint section, if one of
		    // the enabled / disabled flags is set, only
		    // display displays of that type
		    if (((uDisp.isEnabled() && !options.enabled)
			 || (!uDisp.isEnabled() && !options.disabled))
			&& (options.enabled || options.disabled))
			continue;
		    // If we are given a list of actionpoints to
		    // display, only display those points
		    if (ids != null
			&& Arrays.binarySearch(ids, uDisp.getId()) < 0)
			continue;
		    cli.outWriter.print(uDisp.getId());
		    cli.outWriter.print(" ");
		    if (uDisp.isEnabled())
			cli.outWriter.print(" y ");
		    else
			cli.outWriter.print(" n ");
		    cli.outWriter.print("\"");
		    cli.outWriter.print(uDisp.getName());
		    cli.outWriter.print("\" ");
		    cli.outWriter.print(uDisp.getTask().getTaskId().intValue());
		    cli.outWriter.println();
		}
		cli.outWriter.println();
	    }
	}
    }

    static class Delete extends ActionPointCommands {
	Delete() {
	    super(true, "delete", "delete actionpointID",
		  "delete a source breakpoint");
	}
	void interpret(CLI cli, PTSet ptset, String actionpoints,
		       Options options) {
	    // generate a list of actionpoints to delete
	    if (!actionpoints.equals("")) {
		String[] points = actionpoints.split(",");
		int[] ids = new int[points.length];
		for (int i = 0; i < points.length; i++)
		    try {
			ids[i] = Integer.parseInt(points[i]);
		    } catch (NumberFormatException e) {
			throw new InvalidCommandException
			    ("Invalid actionpoint id " + points[i]);
		    }
		Arrays.sort(ids);
		for (int i = 0; i < ids.length; i++) {
		    BreakpointManager bpManager = cli.getSteppingEngine()
			.getBreakpointManager();
		    SourceBreakpoint bpt = bpManager.getBreakpoint(ids[i]);
		    if (bpt != null) {
			for (Iterator taskIter = ptset.getTasks();
			     taskIter.hasNext(); ) {
			    Task task = (Task)taskIter.next();
			    bpManager.disableBreakpoint(bpt, task);
			}
			cli.outWriter.print("breakpoint ");
			cli.outWriter.print(bpt.getId());
			cli.outWriter.println(" deleted");
		    } else if (DisplayManager.deleteDisplay(ids[i])) {
			// Failed to get a breakpoint, try to get a
			// display instead
			cli.outWriter.print("display ");
			cli.outWriter.print(ids[i]);
			cli.outWriter.println(" deleted");
		    } else {
			cli.outWriter.println("no such actionpoint");
		    }
		}
		return;
	    }
	    // Delete breakpoints. We need to consider all of -break,
	    // -enabled, and -disabled and delete breakpoints
	    // accordingly
	    if (options.breakpoints || options.enabled || options.disabled) {
		BreakpointManager bpManager = cli.getSteppingEngine()
		    .getBreakpointManager();
		for (Iterator iter = bpManager.getBreakpointTableIterator();
		     iter.hasNext(); ) {
		    SourceBreakpoint bpt = (SourceBreakpoint)iter.next();
		    if (options.breakpoints
			|| (options.enabled
			    && (bpt.getUserState()
				== SourceBreakpoint.ENABLED))
			|| (options.disabled
			    && (bpt.getUserState()
				== SourceBreakpoint.DISABLED))) {
			for (Iterator taskIter = ptset.getTasks();
			     taskIter.hasNext(); ) {
			    Task task = (Task)taskIter.next();
			    bpManager.disableBreakpoint(bpt, task);
			}
			cli.outWriter.print("breakpoint ");
			cli.outWriter.print(bpt.getId());
			cli.outWriter.println(" deleted");
		    }
		}
	    }
	    // Delete Displays. Again, we need to pay attention to
	    // whether -enabled and -disabled are set
	    if (options.displaypoints || options.enabled || options.disabled) {
		for (Iterator iter = DisplayManager.getDisplayIterator();
		     iter.hasNext(); ) {
		    UpdatingDisplayValue disp
			= (UpdatingDisplayValue) iter.next();
		    if (options.displaypoints
			|| (options.enabled && disp.isEnabled())
			|| (options.disabled && !disp.isEnabled())) {
			DisplayManager.deleteDisplay(disp);
			cli.outWriter.print("display ");
			cli.outWriter.print(disp.getId());
			cli.outWriter.println(" deleted");
		    }
		}
	    }
	}
    }

    static class Disable extends ActionPointCommands {
	Disable() {
	    super(true, "disable", "disable actionpointID",
		  "disable a source breakpoint");
	}
	public void interpret(CLI cli, PTSet ptset, String actionpoints,
			      Options options) {
	    // generate a list of actionpoints to disable
	    if (!actionpoints.equals("")) {
		String[] points = actionpoints.split(",");
		int[] ids = new int[points.length];
		for (int i = 0; i < points.length; i++)
		    try {
			ids[i] = Integer.parseInt(points[i]);
		    } catch (NumberFormatException e) {
			throw new InvalidCommandException
			    ("Invalid actionpoint id " + points[i]);
		    }
		Arrays.sort(ids);
		for (int i = 0; i < ids.length; i++) {
		    BreakpointManager bpManager = cli.getSteppingEngine()
			.getBreakpointManager();
		    SourceBreakpoint bpt = bpManager.getBreakpoint(ids[i]);
		    if (bpt != null) {
			for (Iterator taskIter = ptset.getTasks();
			     taskIter.hasNext(); ) {
			    Task task = (Task)taskIter.next();
			    bpManager.disableBreakpoint(bpt, task);
			}
			cli.outWriter.print("breakpoint ");
			cli.outWriter.print(bpt.getId());
			cli.outWriter.println(" disabled");
		    } else if (DisplayManager.disableDisplay(ids[i])) {
			// Failed to get a breakpoint, try to get a
			// display instead
			cli.outWriter.print("display ");
			cli.outWriter.print(ids[i]);
			cli.outWriter.println(" disabled");
		    } else {
			cli.outWriter.println("no such actionpoint");
		    }
		}
		return;
	    }
	    // Disable breakpoints For our purposes -break and
	    // -enabled are equivalent here, as disabling all
	    // breakpoints is identical to disabling all enabled
	    // breakpoints.
	    if (options.breakpoints || options.enabled) {
		BreakpointManager bpManager = cli.getSteppingEngine()
		    .getBreakpointManager();
		for (Iterator iter = bpManager.getBreakpointTableIterator();
		     iter.hasNext(); ) {
		    SourceBreakpoint bpt = (SourceBreakpoint) iter.next();
		    if (bpt.getUserState() == SourceBreakpoint.ENABLED) {
			for (Iterator taskIter = ptset.getTasks();
			     taskIter.hasNext(); ) {
			    Task task = (Task)taskIter.next();
			    bpManager.disableBreakpoint(bpt, task);
			}
			cli.outWriter.print("breakpoint ");
			cli.outWriter.print(bpt.getId());
			cli.outWriter.println(" disabled");
		    }
		}
	    }
	    // Disable displays Similar to breakpoints, -enable also
	    // means we disable all the displays.
	    if (options.displaypoints || options.enabled) {
		for (Iterator iter = DisplayManager.getDisplayIterator();
		     iter.hasNext(); ) {
		    UpdatingDisplayValue uDisp
			= (UpdatingDisplayValue) iter.next();
		    if (uDisp.isEnabled()) {
			uDisp.disable();
			cli.outWriter.print("display ");
			cli.outWriter.print(uDisp.getId());
			cli.outWriter.println(" disabled");
		    }
		}
	    }
	}
    }

    static class Enable extends ActionPointCommands {
	Enable() {
	    super(true, "enable", "enable actionpointID",
		  "enable a source breakpoint");
	}
	void interpret(CLI cli, PTSet ptset, String actionpoints,
		       Options options) {
	    // generate a list of actionpoints to enable
	    if (!actionpoints.equals("")) {
		String[] points = actionpoints.split(",");
		int[] ids = new int[points.length];
		for (int i = 0; i < points.length; i++)
		    try {
			ids[i] = Integer.parseInt(points[i]);
		    } catch (NumberFormatException e) {
			throw new InvalidCommandException
			    ("Invalid actionpoint id " + points[i]);
		    }
		Arrays.sort(ids);
		for (int i = 0; i < ids.length; i++) {
		    BreakpointManager bpManager = cli.getSteppingEngine()
			.getBreakpointManager();
		    SourceBreakpoint bpt = bpManager.getBreakpoint(ids[i]);
		    if (bpt != null) {
			for (Iterator taskIter = ptset.getTasks();
			     taskIter.hasNext(); ) {
			    Task task = (Task)taskIter.next();
			    bpManager.enableBreakpoint(bpt, task);
			}
			cli.outWriter.print("breakpoint ");
			cli.outWriter.print(bpt.getId());
			cli.outWriter.println(" enabled");
		    } else if (DisplayManager.enableDisplay(ids[i])) {
			// Failed to get a breakpoint, try to get a
			// display instead
			cli.outWriter.print("display ");
			cli.outWriter.print(ids[i]);
			cli.outWriter.println(" enabled");
		    } else {
			cli.outWriter.println("no such actionpoint");
		    }
		}
		return;
	    }
	    // Enable breakpoints For our purposes -break and
	    // -disabled are equivalent here, as enabling all
	    // breakpoints is identical to enabling all disabled
	    // breakpoints.
	    if (options.disabled || options.breakpoints) {
		BreakpointManager bpManager = cli.getSteppingEngine()
		    .getBreakpointManager();
		for (Iterator iter = bpManager.getBreakpointTableIterator();
		     iter.hasNext(); ) {
		    SourceBreakpoint bpt = (SourceBreakpoint) iter.next();
		    if (bpt.getUserState() == SourceBreakpoint.DISABLED) {
			for (Iterator taskIter = ptset.getTasks();
			     taskIter.hasNext(); ) {
			    Task task = (Task) taskIter.next();
			    bpManager.enableBreakpoint(bpt, task);
			}
			cli.outWriter.print("breakpoint ");
			cli.outWriter.print(bpt.getId());
			cli.outWriter.println(" enabled");
		    }
		}
	    }
	    // Enable displays Similar to breakpoints, -disabled also
	    // means we enable all the displays.
	    if (options.disabled || options.displaypoints) {
		for (Iterator iter = DisplayManager.getDisplayIterator();
		     iter.hasNext(); ) {
		    UpdatingDisplayValue uDisp
			= (UpdatingDisplayValue) iter.next();
		    if (!uDisp.isEnabled()) {
			uDisp.enable();
			cli.outWriter.print("display ");
			cli.outWriter.print(uDisp.getId());
			cli.outWriter.println(" enabled");
		    }
		}
	    }
	}
    }
}
