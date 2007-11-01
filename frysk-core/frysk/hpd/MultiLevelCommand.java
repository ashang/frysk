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

import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Iterator;
import java.util.List;

/**
 * A handler class for the CLI that supplies its own help messages.
 */

public abstract class MultiLevelCommand extends Command {
    private final SortedMap subCommands = new TreeMap();

    MultiLevelCommand(String name, String description, String syntax,
		      String full) {
	super(name, description, syntax, full);
    }
    /**
     * Add the specified command.
     */
    protected MultiLevelCommand add(Command command) {
	subCommands.put(command.getName(), command);
	return this;
    }

    private void help(CLI cli) {
	for (Iterator i = subCommands.values().iterator(); i.hasNext(); ) {
	    Command c = (Command)(i.next());
	    cli.outWriter.print(c.getName());
	    cli.outWriter.print(": ");
	    cli.outWriter.print(c.getHelp().getDescription());
	}
    }

    public void interpret(CLI cli, Input input) {
	String subAction = input.parameter(0);
	if (subAction == null) {
	    help(cli);
	    return;
	}
	Command subCommand = (Command)(subCommands.get(subAction));
	if (subCommand == null) {
	    String subName = null;
	    for (Iterator i = subCommands.keySet().iterator(); i.hasNext(); ) {
		String key = (String)(i.next());
		if (key.startsWith(subAction)) {
		    if (subName == null) {
			subName = key;
		    } else {
			throw new InvalidCommandException("Ambigious command: "
							  + subAction);
		    }
		}
	    }
	    if (subCommand == null) {
		throw new InvalidCommandException("Unknown command: "
						  + subAction);
	    }
	    // This must work!
	    subCommand = (Command)(subCommands.get(subName));
	}
	subCommand.interpret(cli, input.accept());
    }

    int complete(CLI cli, Input input, int cursor, List candidates) {
	// Get the next token.
	Input.Token token;
	if (input.size() > 0) {
	    token = input.token(0);
	} else {
	    token = null;
	}
	// If the cursor is past this token then a sub-command of this
	// command is being completed.  Find that sub-command and call
	// it.
	if (token != null && cursor > token.end) {
	    Command subCommand = (Command)subCommands.get(token.value);
	    if (subCommand == null)
		throw new InvalidCommandException("unknown command: "
						  + token.value);
	    return subCommand.complete(cli, input.accept(), cursor,
				       candidates);
	}
	// Create a completion list for this command.
	String incomplete;
	if (token == null) {
	    incomplete = "";
	} else {
	    incomplete = token.value;
	}
	for (Iterator i = subCommands.keySet().iterator(); i.hasNext(); ) {
	    String subCommand = (String)i.next();
	    if (subCommand.startsWith(incomplete))
		candidates.add(subCommand);
	}
	// If there's only one token, append a trailing blank so that
	// things are ready for the next token.
	if (candidates.size() == 1) {
	    candidates.set(0, ((String)candidates.get(0)) + " ");
	}
	// Return the start of the tokens (if there is one)
	int start = (token == null) ? cursor : token.start;
	// XXX: I think this is one out; yet the +1 is needed to get
	// all cases to work.
	return 1 + start;
    }
}
