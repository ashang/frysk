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
import java.util.List;
import frysk.debuginfo.DebugInfoFrame;
import frysk.debuginfo.DebugInfoStackFactory;
import frysk.proc.Task;

abstract class StackCommands extends ParameterizedCommand {

    private static class Options {
	boolean printScopes;
    }
    Object options() {
	return new Options();
    }

    StackCommands(String description, String syntax, String full) {
	super(description, syntax, full);
	add(new CommandOption("scopes", "include scopes") {
		void parse(String arg, Object options) {
		    ((Options)options).printScopes = true;
		}
	    });
    }

    int completer(CLI cli, Input input, int cursor, List candidates) {
	return -1;
    }

    static private void printStack(CLI cli, DebugInfoFrame frame,
				   int stopLevel, Options options) {
	DebugInfoStackFactory.printStackTrace(cli.outWriter, frame,
					      stopLevel, true,
					      options.printScopes, true);
    }
    static private void printFrame(CLI cli, DebugInfoFrame frame,
				   Options options) {
	printStack(cli, frame, -1, options);
    }

    static private void select(CLI cli, PTSet ptset, Magnitude whereTo,
			       Options options) {
	for (Iterator i = ptset.getTaskData(); i.hasNext(); ) {
	    TaskData td = (TaskData)i.next();
	    Task task = td.getTask();
	    td.printHeader(cli.outWriter);
	    DebugInfoFrame currentFrame = cli.getTaskFrame(task);
	    // Where to?
	    int newLevel;
	    if (whereTo.sign > 0) {
		newLevel = currentFrame.level() + whereTo.magnitude;
	    } else if (whereTo.sign < 0) {
		newLevel = currentFrame.level() - whereTo.magnitude;
	    } else {
		newLevel = whereTo.magnitude;
	    }
	    // Get there.
	    DebugInfoFrame newFrame = currentFrame;
	    while (newFrame != null && newFrame.level() != newLevel) {
		if (newFrame.level() < newLevel) {
		    newFrame = newFrame.getOuterDebugInfoFrame();
		} else {
		    newFrame = newFrame.getInnerDebugInfoFrame();
		}
	    }
	    // Success?
	    if (newFrame == null) {
		// Reached end-of-stack
		if (currentFrame.level() > newLevel) {
		    cli.outWriter.println("Top of stack");
		} else {
		    cli.outWriter.println("Bottom of stack");
		}
	    } else if (newFrame == currentFrame) {
		// Same frame; just print it.
		printFrame(cli, newFrame, options);
	    } else {
		// New frame, change it.
		cli.setTaskFrame(task, newFrame);
		printFrame(cli, newFrame, options);
	    }
	}
    }

    static class Down extends StackCommands {
	Down() {
	    super("Move down one or more levels in the call stack",
		  "down [num-levels]",
		  "Move down towards the stack bottom or outer most frame");
	}
	void interpret(CLI cli, Input input, Object options) {
	    int count;
	    switch (input.size()) {
	    case 0:
		count = 1;
		break;
	    case 1:
		count = Integer.parseInt(input.parameter(0));
		break;
	    default:
		throw new InvalidCommandException("too many arguments");
	    }
	    select(cli, cli.getCommandPTSet(input), new Magnitude(+1, count),
		   (Options)options);
	}
    }

    static class Up extends StackCommands {
	Up() {
	    super("Move up one or more levels in the call stack",
		  "up [num-levels]",
		  "Move up towards the stack top or inner most frame");
	}
	void interpret(CLI cli, Input input, Object options) {
	    int count;
	    switch (input.size()) {
	    case 0:
		count = 1;
		break;
	    case 1:
		count = Integer.parseInt(input.parameter(0));
		break;
	    default:
		throw new InvalidCommandException("too many arguments");
	    }
	    select(cli, cli.getCommandPTSet(input), new Magnitude(-1, count),
		   (Options)options);
	}
    }

    static class Frame extends StackCommands {
	Frame() {
	    super("Move to the specified frame",
		  "frame [level]",
		  "Move to the specifed stack frame");
	}
	void interpret(CLI cli, Input input, Object options) {
	    Magnitude count;
	    switch (input.size()) {
	    case 0:
		count = new Magnitude(+1, 0); // go no where
		break;
	    case 1:
		count = new Magnitude(input.parameter(0));
		break;
	    default:
		throw new InvalidCommandException("too many arguments");
	    }
	    select(cli, cli.getCommandPTSet(input), count, (Options)options);
	}
    }

    static class Where extends StackCommands {
	Where() {
	    super("Display the current execution location and call stack",
		  "where [ {num-levels ] [ -scopes ]",
		  ("The where command displays the current execution"
		   + " location(s) and the call stack(s) - or sequence"
		   + " of procedure calls - which led to that point."));
	}

	public void interpret(CLI cli, Input input, Object o) {
	    int levels;
	    switch (input.size()) {
	    case 0:
		levels = 0;
		break;
	    case 1:
		levels = Integer.parseInt(input.parameter(0));
		break;
	    default:
		throw new InvalidCommandException("Too many arguments");
	    }
	    Options options = (Options)o;	
	    PTSet ptset = cli.getCommandPTSet(input);
	    for (Iterator i = ptset.getTaskData(); i.hasNext(); ) {
		TaskData td = (TaskData)i.next();
		Task task = td.getTask();
		DebugInfoFrame currentFrame = cli.getTaskFrame(task);
		td.printHeader(cli.outWriter);
		// XXX: How come the pt set code didn't sort this out;
		// filtering out running tasks???
		if (cli.getSteppingEngine() == null
		    || !cli.getSteppingEngine().isTaskRunning(task)) {
		    printStack(cli, currentFrame, levels, options);
		} else {
		    cli.outWriter.println("Running task?");
		}
	    }
	    cli.outWriter.flush();
	}
    }
}
