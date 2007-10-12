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

import java.text.ParseException;
import java.util.Iterator;
import frysk.value.Value;
import java.util.ArrayList;

public class ExamineCommand extends CLIHandler {

    public ExamineCommand(CLI cli) {
	super(cli, "examine", "examine a value", "examine VALUE\n",
		"Examine a value in more detail.");
    }

    public void handle(Command cmd) throws ParseException {
	PTSet ptset = cli.getCommandPTSet(cmd);
	ArrayList params = cmd.getParameters();
	parser.parse(params);
	if (parser.helpOnly)
	    return;

	if (params.size() == 0) {
	    cli.outWriter.println("No value to examine");
	    return;
	}
	Value value;
	Iterator taskDataIter = ptset.getTaskData();
	while (taskDataIter.hasNext()) {
	    TaskData tdata = (TaskData) taskDataIter.next();
	    try {
		// XXX: Is this right, is this the entire expresson?
		value = cli.parseValue(tdata.getTask(), (String) params.get(0));
	    } catch (RuntimeException nnfe) {
		cli.addMessage(new Message(nnfe.getMessage(),
					   Message.TYPE_ERROR));
		return;
	    }

	    // For moment, just print the bytes.
	    cli.outWriter.println("[" + tdata.getParentID() + "."
		    + tdata.getID() + "]");
	    byte[] bytes = value.getLocation().toByteArray();
	    for (int i = 0; i < bytes.length; i++) {
		cli.outWriter.println(i + ": " + bytes[i]);
	    }
	}
    }
}
