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

abstract class DbgVariableCommands extends ParameterizedCommand {

    DbgVariableCommands(String description, String syntax, String full) {
	super(description, syntax, full);
    }

    int completer(CLI cli, Input input, int cursor, List completions) {
	Input.Token incomplete = input.incompleteToken(cursor);
	int newOffset = cli.dbgvars.complete(incomplete.value,
					     incomplete.end - incomplete.start,
					     completions);
	return incomplete.absolute(newOffset);
    }

    static class Set extends DbgVariableCommands {
	Set() {
	    super("Change or view a debugger variable.",
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
	}
	public void interpret(CLI cli, Input input, Object options) {
	    switch (input.size()) {
	    default:
		throw new InvalidCommandException
		    ("wrong number of parameters");
	    case 0:
		cli.outWriter.println(cli.dbgvars.toString());
		break;
	    case 1:
		String var = input.parameter(0);
		if (!cli.dbgvars.variableIsValid(var))
		    throw new InvalidCommandException("Debug variable \""
						      + var
						      + "\" is invalid");
		cli.outWriter.print(var);
		cli.outWriter.print(" = ");
		cli.outWriter.print(cli.dbgvars.getValue(var));
		cli.outWriter.println();
		break;
	    case 2:
	    case 3:
		if (input.size() == 3 && !input.parameter(1).equals("="))
		    throw new InvalidCommandException("missing \"=\"");
		String variable = input.parameter(0);
		if (!cli.dbgvars.variableIsValid(variable))
		    throw new InvalidCommandException("Debugger variable \""
						      + variable
						      + "\" is invalid");
		String value = input.parameter(input.size() - 1);
		if (!cli.dbgvars.valueIsValid(variable, value))
		    throw new InvalidCommandException("Debug variable value \""
						      + value
						      + "\" is invalid");
		cli.dbgvars.setVariable(variable, value);
		break;
	    }
	}
    }

    static class Unset extends DbgVariableCommands {
	Unset() {
	    super("Revert variable value to default.",
		  "unset [ <debugger-var> | -all ]",
		  ("The unset command reverses the effects of any previous"
		   + " set operations, restoring the debugger state"
		   + " variable(s) to their default settings.  When the"
		   + " argument -all is specified, the command affects all"
		   + " debugger state variables, restoring them to the"
		   + " original settings that were in effect when the"
		   + " debugging session began.  When just a single"
		   + " argument is included, only that variable is"
		   + " affected."));
	    add(new CommandOption("all", "Revert all variables") {
		    void parse(String arg, Object option) {
			((Options)option).all = true;
		    }
		});
	}

	private class Options {
	    boolean all = false;
	}
	Object options() {
	    return new Options();
	}

	public void interpret(CLI cli, Input input, Object o) {
	    Options options = (Options)o;
	    if (options.all) {
		if (input.size() != 0)
		    throw new InvalidCommandException
			("Too many arguments for -all");
		cli.dbgvars.unsetAll();
		cli.outWriter.println("All debug variables reset");
		return;
	    }

	    switch (input.size()) {
	    case 0:
		throw new InvalidCommandException("Missing argument");
	    case 1:
		String temp = input.parameter(0);
		if (!cli.dbgvars.variableIsValid(temp))
		    throw new InvalidCommandException
			("\"" + temp + "\" is not a valid debugger variable");
		cli.dbgvars.unsetVariable(temp);
		break;
	    default:
		throw new InvalidCommandException("Too many arguments");
	    }
	}
    }
}
