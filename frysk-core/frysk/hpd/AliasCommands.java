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
import java.util.Map;
import java.util.Iterator;

abstract class AliasCommands extends ParameterizedCommand {

    int completer(CLI cli, Input input, int cursor, List completions) {
	Input.Token incomplete = input.incompleteToken(cursor);
	for (Iterator i = cli.aliases.keySet().iterator(); i.hasNext(); ) {
	    String key = (String)i.next();
	    if (key.startsWith(incomplete.value))
		completions.add(key);
	}
	CompletionFactory.padSingleCandidate(completions);
	return incomplete.absolute(0);
    }

    AliasCommands(String name, String description, String syntax,
		  String full) {
	super(name, description, syntax, full);
    }

    static class Alias extends AliasCommands {
	Alias() {
	    super("alias", "create or view user-defined commands",
		  ("alias <command-name> <command-body> -- define an alias\n"
		   + "alias [ <command-name> ] -- view an alias"),
		  ("The alias command associates a "
		   + "user-defined name with a list of one or more debugger"
		   + " commands. After definition, the user-defined command"
		   + " can be used in the same way as a debugger-defined"
		   + " command, including as part of the definition of new"
		   + " user-defined commands."));
	}

	void interpret(CLI cli, Input cmd, Object options) {
	    switch (cmd.size()) {
	    default:
		throw new InvalidCommandException("Too many parameters");
	    case 2:
		cli.aliases.put(cmd.parameter(0), cmd.parameter(1));
		break;
	    case 1:
		String temp = cmd.parameter(0);
		if (!cli.aliases.containsKey(temp))
		    throw new InvalidCommandException("Alias \"" + temp
						      + "\" not defined.");
		cli.outWriter.print(temp);
		cli.outWriter.print(" = ");
		cli.outWriter.print(cli.aliases.get(temp));
		cli.outWriter.println();
		break;
	    case 0:
		if (cli.aliases.size() == 0) {
		    cli.outWriter.println("No aliases.");
		} else {
		    for (Iterator i = cli.aliases.entrySet().iterator();
			 i.hasNext(); ) {
			Map.Entry entry = (Map.Entry)i.next();
			cli.outWriter.print(entry.getKey());
			cli.outWriter.print(" = ");
			cli.outWriter.print(entry.getValue());
			cli.outWriter.println();
		    }
		}
		break;
	    }
	}
    }

    static class Unalias extends AliasCommands {
	private static class Options {
	    boolean deleteAll;
	}
	Object options() {
	    return new Options();
	}
	Unalias() {
	    super("unalias", "Create or view user-define commands.",
		  "unalias [ command-name | -all -",
		  ("The unalias command removes the alias that was"
		   + " previously established for the specified"
		   + " user-defined command name."));
	    add(new CommandOption("all", "delete all use-defined aliases") {
		    void parse(String argument, Object options) {
			((Options)options).deleteAll = true;
		    }
		});
	}

	public void interpret(CLI cli, Input input, Object o) {
	    Options options = (Options)o;
	    if (options.deleteAll) {
		if (input.size() != 0)
		    throw new InvalidCommandException("Extra parameters");
		cli.outWriter.println("Removing all aliases.");
		cli.aliases.clear();
	    } else {
		if (input.size() == 0)
		    throw new InvalidCommandException("Missing alias");
		for (int i = 0; i < input.size(); i++) {
		    String temp = input.parameter(i);
		    if (cli.aliases.containsKey(temp)) {
			cli.outWriter.print("Removed alias \"");
			cli.outWriter.print(temp);
			cli.outWriter.println("\"");
			cli.aliases.remove(temp);
		    } else {
			cli.outWriter.print("Alias \"");
			cli.outWriter.print(temp);
			cli.outWriter.println("\" not defined.");
		    }
		}
	    }
	}
    }
}
