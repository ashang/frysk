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
import frysk.proc.Task;
import java.util.List;
import frysk.debuginfo.DebugInfoFrame;
import frysk.debuginfo.DebugInfoStackFactory;

class WhereCommand extends ParameterizedCommand {

    private static class Options {
	boolean printScopes;
    }
    Object options() {
	return new Options();
    }

    WhereCommand() {
	super("where",
	      "Display the current execution location and call stack",
	      "where [ {num-levels | -all} ] [-scopes]",
	      ("The where command displays the current execution"
	       + " location(s) and the call stack(s) - or sequence"
	       + " of procedure calls - which led to that point."));
	add(new CommandOption("scopes", "include scopes") {
		void parse(String arg, Object options) {
		    ((Options)options).printScopes = true;
		}
	    });
    }

    int complete(CLI cli, PTSet ptset, String incomplete, int base,
		 List candidates) {
	return -1;
    }

    public void interpret(CLI cli, Input input, Object o) {
	if (input.size() > 1)
	    throw new InvalidCommandException("Too many arguments");
	Options options = (Options)o;
	int level = 0;
	if (input.size() == 1)
	    level = Integer.parseInt(input.parameter(0));
	
	PTSet ptset = cli.getCommandPTSet(input);
	Iterator taskIter = ptset.getTaskData();
        boolean moreThanOneTask = false;
	while (taskIter.hasNext()) {
            TaskData td = (TaskData)taskIter.next();
            if (!moreThanOneTask && taskIter.hasNext())
                moreThanOneTask = true;
            Task task = (Task)td.getTask();
	    DebugInfoFrame tmpFrame = cli.getTaskFrame(task);
	    int l = tmpFrame.level();
	    int stopLevel;

	    if (level > 0)
		stopLevel = l + level;
	    else
		stopLevel = 0;

            if (moreThanOneTask) {
                td.toPrint(cli.outWriter, true);
                cli.outWriter.println();
            }
            if (cli.getSteppingEngine() == null
                || !cli.getSteppingEngine().isTaskRunning(task)) {
                DebugInfoStackFactory.printStackTrace(cli.outWriter, tmpFrame,
                                                      stopLevel, true,
						      options.printScopes,
                                                      true);
            }
	}
        cli.outWriter.flush();
    }
}
