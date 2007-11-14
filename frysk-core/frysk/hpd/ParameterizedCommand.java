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

    private CommandOption lookupOption(String name) {
	CommandOption commandOption = (CommandOption)longOptions.get(name);
	if (commandOption == null)
	    commandOption = (CommandOption)shortOptions.get(name);
	return commandOption;
    }

    private String optionName(String name) {
	if (name.charAt(0) != '-')
	    return null;
	do {
	    name = name.substring(1);
	} while (name.length() > 0 && name.charAt(0) == '-');
	return name;
    }

    /**
     * Given a list of arguments, parse and remove options as they are
     * found.
     */
    public final void interpret(CLI cli, Input input) {
	Object options = options();
	while (input.size() > 0) {
	    int index = input.size() - 1;
	    String string = input.parameter(index);
	    if (string.equals("--")) {
		input.removeLast();
		break;
	    }
	    // Check for <<-option ARG>>; so that <<-option -1>> is
	    // prefered over <<-1>> (which isn't valid).
	    if (input.size() > 1) {
		String name = optionName(input.parameter(index - 1));
		if (name != null) {
		    CommandOption option = lookupOption(name);
		    if (option != null && option.parameter != null) {
			option.parse(string, options);
			input.removeLast(); // arg
			input.removeLast(); // -opt
			continue;
		    }
		}
	    }
	    if (string.equals("-help")) {
		help(cli, input);
		return;
	    }
	    // Check for <<-option>>; if nothing going give up.
	    String name = optionName(string);
	    if (name == null)
		break;
	    CommandOption commandOption = lookupOption(name);
	    if (commandOption == null) {
		throw new InvalidCommandException("unrecognized option '-"
						  + name + "'");
	    }
	    if (commandOption.parameter != null) {
		throw new InvalidCommandException
		    ("option -" + commandOption.longName
		     + " expects a single parameter "
		     + commandOption.parameter);
	    }
	    commandOption.parse(null, options);
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
}
