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

class SetCommand extends Command {
    private static final String full = "The set command supports the viewing of "
	    + "debugger state variables and the\n"
	    + "assignment of new values to them.  When no arguments are "
	    + "specified, the\n"
	    + "names and current values for all debugger state variables are\n"
	    + "displayed. When just a single argument is included, the "
	    + "debugger echoes\n"
	    + "the variable name and displays its current value.  The second "
	    + "argument\n"
	    + "defines the value that should replace any previous value for "
	    + "that\n"
	    + "variable.  It must be enclosed in quotes if it contains "
	    + "multiple\n" + "words. ";

    private final DbgVariables dbgvars;

    SetCommand(CLI cli, DbgVariables dbgvars) {
	super(cli, "set", "Change or view a debugger variable.",
		"set debugger-var = value\nset [debugger-var]", full);
	this.dbgvars = dbgvars;
    }

    public void parse(Input cmd) throws ParseException {
	ArrayList params = cmd.getParameters();
	if (params.size() == 1 && params.get(0).equals("-help")) {
	    cli.printUsage(cmd);
	    return;
	}
	String temp;
	if (params.size() == 3 && ((String) params.get(1)).equals("=")) {
	    temp = (String) params.get(0);
	    if (dbgvars.variableIsValid(temp)) {
		if (dbgvars.valueIsValid(temp, (String) params.get(2))) {
		    dbgvars.setVariable(temp, (String) params.get(2));
		} else
		    cli.addMessage("Illegal variable value.",
			    Message.TYPE_ERROR);
	    } else
		cli.addMessage(new Message("Illegal debugger variable \""
			+ (String) params.get(0) + "\"", Message.TYPE_ERROR));
	} else if (params.size() == 1) {
	    temp = (String) params.get(0);
	    if (dbgvars.variableIsValid(temp)) {
		cli.addMessage(
			temp + " = " + dbgvars.getValue(temp).toString(),
			Message.TYPE_NORMAL);
	    } else
		cli.addMessage(new Message("Illegal debugger variable \""
			+ (String) params.get(0) + "\"", Message.TYPE_ERROR));
	} else if (params.size() == 0) {
	    cli.addMessage(dbgvars.toString(), Message.TYPE_NORMAL);
	} else {
	    cli.printUsage(cmd);
	}
    }
}
