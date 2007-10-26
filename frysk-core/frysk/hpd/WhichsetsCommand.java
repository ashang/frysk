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
import java.util.Iterator;
import java.util.ArrayList;

/*
 * Set commands
 */

class WhichsetsCommand extends Command {

    private static final String full = "The whichsets command displays sets to "
	    + "which a particular thread(s)\n"
	    + "belongs. When no argument is used, the membership of each "
	    + "thread in the\n"
	    + "target p/t set is displayed. If a thread(s) is specified as "
	    + "the\n"
	    + "argument, only its membership information will be displayed. ";

    WhichsetsCommand() {
	super("whichsets",
	      "List all user-defined set to which a proc/task belongs.",
	      "whichsets [p/t-set]", full);
    }

    public void parse(CLI cli, Input cmd) throws ParseException {
	ArrayList params = cmd.getParameters();
	if (params.size() == 1 && params.get(0).equals("-help")) {
	    cli.printUsage(cmd);
	    return;
	}
	PTSet searchset = null;
	PTSet tempset = null;
	TaskData temptd = null;
	String setname = null;

	// ??? check builtin sets
	if (params.size() <= 1) {
	    if (params.size() == 0)
		searchset = cli.targetset;
	    else if (params.size() == 1)
		searchset = cli.createSet((String) params.get(0));

	    // start iterating through available sets
	    for (Iterator it = searchset.getTaskData(); it.hasNext();) {
		temptd = (TaskData) it.next();
		cli.addMessage("Task " + temptd.getParentID() + "."
			+ temptd.getID() + " is in sets: \n",
			Message.TYPE_NORMAL);
		for (Iterator iter = cli.namedPTSets.keySet().iterator(); iter
			.hasNext();) {
		    setname = (String) iter.next();
		    tempset = (PTSet) (cli.namedPTSets).get(setname);

		    if (tempset.containsTask(temptd.getParentID(), temptd
			    .getID()))
			cli.addMessage("\t" + setname + "\n",
				Message.TYPE_NORMAL);
		}
		cli.addMessage("\n", Message.TYPE_NORMAL);
	    }
	} else {
	    cli.printUsage(cmd);
	}
    }
}
