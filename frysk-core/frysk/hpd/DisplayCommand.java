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
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import frysk.proc.Task;
import frysk.rt.DisplayManager;
import frysk.rt.DisplayValueObserver;
import frysk.rt.UpdatingDisplayValue;
import frysk.stack.FrameIdentifier;
import frysk.stepping.SteppingEngine;
import frysk.value.Value;

/**
 * The display command is used to create a display on an expression. The syntax
 * for this command should be "display <expression>". Whenever the value of the
 * expression changes, the user will be notified.
 * 
 */
public class DisplayCommand extends CLIHandler {

    private List displays;

    private static final String desc = "creates a display on an expression";

    DisplayCommand(CLI cli) {
	super(cli, "display", desc, "display expr", desc);
	displays = new LinkedList();
    }


    public void handle(Command cmd) throws ParseException {
	final PrintWriter output = cli.getPrintWriter();

	PTSet ptset = cli.getCommandPTSet(cmd);
	ArrayList args = cmd.getParameters();
	if (args.size() > 1)
	    throw new ParseException("Too many arguments to display", 0);
	if (args.size() == 0)
	    throw new ParseException("Too few arguments to display", 0);
	Iterator taskIter = ptset.getTasks();
	SteppingEngine engine = cli.getSteppingEngine();
	while (taskIter.hasNext()) {
	    Task myTask = (Task) taskIter.next();
	    /*
	     * if debugInfo is null, that probably means that we're not
	     * attached to anything
	     */
	    if (cli.getTaskDebugInfo(myTask) == null) {
		output.write("Not attached to any task, nothing to display\n");
		output.flush();
		return; // XXX
	    }

	    FrameIdentifier fIdent = cli.getTaskFrame(myTask)
		    .getFrameIdentifier();

	    UpdatingDisplayValue uDisp = DisplayManager.createDisplay(myTask,
		    fIdent, engine, (String) args.get(0));

	    /*
             * We need to keep a local record of what displays have been
             * created via the HPD, so that we avoid adding observers twice
             * and duplicating the output when the display updates.
             */
	    if (!displays.contains(uDisp)) {
		displays.add(uDisp);
		uDisp.addObserver(new DisplayValueObserver() {
		    public void updateValueChanged(UpdatingDisplayValue value) {
			output.print(value.getId() + ": " + value.getName()
				+ " = " + value.getValue().toPrint());
			output.flush();
		    }

		    public void updateUnavailableOutOfScope(
			    UpdatingDisplayValue value) {
			output.println(value.getId() + ": " + value.getName()
				+ " = <unavailable>");
			output.flush();
		    }

		    public void updateUnavailbeResumedExecution(
			    UpdatingDisplayValue value) {
		    }

		    public void updateAvailableTaskStopped(
			    UpdatingDisplayValue value) {
		    }

		    public void updateDisabled(UpdatingDisplayValue value) {
		    }
		});
	    }
	    
	    Value v = uDisp.getValue();
	    if (v == null)
		output.println(uDisp.getId() + ": " + args.get(0)
			+ " = <unavailable>");
	    else
		output.println(uDisp.getId() + ": " + uDisp.getName() + " = "
			+ v.toPrint());
	    output.flush();
	}
    }
}
