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
import java.util.Iterator;
import frysk.proc.Task;

import frysk.debuginfo.DebugInfoFrame;

class WhereCommand extends CLIHandler {

    private static final String full = "The where command displays the current "
	    + "execution location(s) and the\n"
	    + "call stack(s) - or sequence of procedure calls - which led to "
	    + "that\n" + "point.";

    WhereCommand(CLI cli) {
	super(cli, "where",
		"Display the current execution location and call stack",
		"where [ {num-levels | -all} ] [-args]", full);
    }

    public void handle(Command cmd) throws ParseException {
	PTSet ptset = cli.getCommandPTSet(cmd);
	ArrayList params = cmd.getParameters();
	if (params.size() == 1 && params.get(0).equals("-help")) {
	    cli.printUsage(cmd);
	    return;
	}

	int level = 0;

	if (params.size() != 0)
	    level = Integer.parseInt((String) params.get(0));
	Iterator taskIter = ptset.getTasks();
	while (taskIter.hasNext()) {
	    Task task = (Task) taskIter.next();
	    DebugInfoFrame tmpFrame = null;
	    int l = cli.getTaskStackLevel(task);
	    int stopLevel;

	    if (level > 0)
		stopLevel = l + level;
	    else
		stopLevel = 0;

	    tmpFrame = cli.getTaskFrame(task);
	    while (tmpFrame != null) {
		cli.outWriter.print("#" + l + " ");
		tmpFrame.toPrint(cli.outWriter, false);
		cli.outWriter.println();
		tmpFrame = tmpFrame.getOuterDebugInfoFrame();
		l += 1;
		if (l == stopLevel)
		    break;
	    }
	}

    }
}
