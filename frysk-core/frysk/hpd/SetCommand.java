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

import java.util.List;

class SetCommand extends ParameterizedCommand {

    private final DbgVariables dbgvars;

    SetCommand(DbgVariables dbgvars) {
	super("set", "Change or view a debugger variable.",
	      "set debugger-var = value\nset [debugger-var]",
	      ("The set command supports the viewing of debugger state"
	       + " variables and the assignment of new values to them.  When"
	       + " no arguments are specified, the names and current values"
	       + " for all debugger state variables are displayed.  When"
	       + " just a single argument is included, the debugger echoes"
	       + " the variable name and displays its current value.  The"
	       + " second argument defines the value that should replace"
	       + " any previous value for that variable.  It must be"
	       + " enclosed in quotes if it contains multiple words."));
	this.dbgvars = dbgvars;
    }

    public void interpret(CLI cli, Input input, Object options) {
	switch (input.size()) {
	default:
	    throw new InvalidCommandException("wrong number of parameters");
	case 3:
	    if (!input.parameter(1).equals("="))
		throw new InvalidCommandException("missing \"=\"");
	    String variable = input.parameter(0);
	    if (!dbgvars.variableIsValid(variable))
		throw new InvalidCommandException("Debugger variable \""
						  + variable
						  + "\" is invalid");
	    String value = input.parameter(2);
	    if (!dbgvars.valueIsValid(variable, value))
		throw new InvalidCommandException("Variable value \""
						  + value
						  + "\" is invalid");
	    dbgvars.setVariable(variable, value);
	    break;
	case 1:
	    String var = input.parameter(0);
	    if (!dbgvars.variableIsValid(var))
		throw new InvalidCommandException("Variable \""
						  + var
						  + "\" is invalid");
	    cli.outWriter.print(var);
	    cli.outWriter.print(" = ");
	    cli.outWriter.print(dbgvars.getValue(var));
	    cli.outWriter.println();
	    break;
	case 0:
	    cli.outWriter.println(dbgvars.toString());
	    break;
	}
    }

    int complete(CLI cli, PTSet ptset, String incomplete, int base,
		 List completions) {
	return -1;
    }
}
