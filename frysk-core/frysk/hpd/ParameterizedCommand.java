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

import java.util.TreeMap;
import java.util.Iterator;
import java.util.SortedMap;
import java.util.List;

abstract class ParameterizedCommand extends Command {
    private final SortedMap longOptions = new TreeMap();
    private final SortedMap shortOptions = new TreeMap();
    private final String syntax;
    private final String full;

    ParameterizedCommand(String name, String description,
			 String syntax, String full) {
	super(name, description, syntax, full);
	this.syntax = syntax;
	this.full = full;
    }
    ParameterizedCommand(String name, String syntax, String description) {
	this(name, description, syntax, description);
    }

    void add(CommandOption option) {	
	if (option.longName != null)
	    longOptions.put(option.longName, option);
	if (option.shortName != '\0')
	    shortOptions.put("" + option.shortName, option);
    }

    private void handleOption(Input input, String option, int index,
			      Object options) {
	// Strip any leading "-"'s
	String name = option.substring(1);
	while (name.length() > 0 && name.charAt(0) == '-')
	    name = name.substring(1);
	// Strip off =... in -option=...
	int eq = name.indexOf('=');
	if (eq != -1)
	    name = option.substring(0, eq);
	CommandOption commandOption = (CommandOption)longOptions.get(name);
	if (commandOption == null)
	    commandOption = (CommandOption)shortOptions.get(name);
	if (commandOption == null) {
	    throw new InvalidCommandException("unrecognized option '-"
					      + name + "'");
	}
	String argument = null;
	if (commandOption.parameter != null) {
	    // Require a single parameter.
	    if ((eq >= 0 && index != input.size())
		|| (eq == -1 && index != input.size() - 1))
		throw new InvalidCommandException
		    ("option -" + commandOption.longName
		     + " expects a single parameter "
		     + commandOption.parameter);
	    if (eq == -1) {
		argument = input.parameter(index);
		input.removeLast();
	    } else {
		argument = option.substring(eq + 1);
	    }
	} else {
	    // Reject a parameter.
	    if (eq != -1 || index != input.size())
		throw new InvalidCommandException
		    ("option -" + commandOption.longName
		     + " doesn't allow an argument");
	}
	commandOption.parse(argument, options);
    }

    /**
     * Given a list of arguments, parse and remove options as they are
     * found.
     */
    public final void interpret(CLI cli, Input input) {
	Object options = options();
	for (int currentIndex = input.size() - 1; currentIndex > -1;
	     --currentIndex) {
	    String string = input.parameter(currentIndex);
	    if (string.equals("--")) {
		if (currentIndex != input.size() - 1)
		    throw new InvalidCommandException
			("Invalid option "
			 + input.parameter(currentIndex + 1));
		input.removeLast();
		break;
	    }
	    if (string.equals("-help")) {
		help(cli, input);
		return;
	    }
	    if (string.charAt(0) != '-')
		continue;
	    handleOption(input, string, currentIndex + 1, options);
	    input.removeLast();
	}
	interpret(cli, input, options);
    }

    void help(CLI cli, Input input) {
	cli.outWriter.print(syntax);
	if (longOptions.size() > 1) {
	    cli.outWriter.print(" -option ...; where options are:");
	    for (Iterator i = longOptions.values().iterator();
		 i.hasNext(); ) {
		CommandOption option = (CommandOption)i.next();
		cli.outWriter.print("  -");
		cli.outWriter.print(option.longName);
		cli.outWriter.print("\t");
		cli.outWriter.print(option.description);
		cli.outWriter.println();
	    }
	} else {
	    cli.outWriter.println();
	}
	cli.outWriter.println(full);
    }

    /**
     * Return the options object (or null) which will be passed to
     * each command option parser.
     */
    Object options() {
	return this;
    }

    /**
     * Interpret command, using options.
     */
    abstract void interpret(CLI cli, Input input, Object options);

    /**
     * Complete the input.
     */
    final int complete(CLI cli, Input input, int cursor, List candidates) {
	int start;
	if (input.size() == 0)
	    start = cursor;
	else
	    start = input.token(0).start;
	int pos = complete(cli, cli.getCommandPTSet(input),
			   input.stringValue(), cursor - start, candidates);
	if (pos >= 0) {
	    return pos + start;
	} else {
	    return -1;
	}
    }

    /**
     * Complete the string.
     */
    abstract int complete(CLI cli, PTSet ptset, String incomplete,
			  int base, List candidates);
}
