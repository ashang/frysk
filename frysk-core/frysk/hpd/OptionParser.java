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

import gnu.classpath.tools.getopt.Option;
import gnu.classpath.tools.getopt.OptionException;
import gnu.classpath.tools.getopt.OptionGroup;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.LinkedList;
import java.util.Iterator;
import java.util.List;
import java.io.PrintWriter;

class OptionParser {

    private final List options = new LinkedList();
    private final OptionGroup defaultGroup;
    private final OptionGroup finalGroup;
    private final String header;
    private final String footer;
    private final String programName;

    OptionParser(String programName, String header, String footer) {
	this.programName = programName;
	this.header = header;
	this.footer = footer;
	defaultGroup = new OptionGroup(programName + " Options");
	finalGroup = new OptionGroup("Standard Options");
	Option help = new Help();
	finalGroup.add(help);
	options.add(help);
    }

    void add(Option option) {	
	defaultGroup.add(option);
	options.add(option);
    }

    private static class HelpException extends RuntimeException {
	static final long serialVersionUID = 1;
    }

    private class Help extends Option {
	Help() {
	    super("help", "print this help");
	}
	public void parsed(String argument) throws OptionException {
	    throw new HelpException();
	}
    }

    void printHelp(PrintWriter out) {
	ByteArrayOutputStream scratchStream = new ByteArrayOutputStream();
	PrintStream writer = new PrintStream(scratchStream);
	if (header != null) {
	    writer.println(header);
	}
	if (defaultGroup != null) {
	    defaultGroup.printHelp(writer, true);
	    writer.println();
	}
	finalGroup.printHelp(writer, true);
	if (footer != null) {
	    writer.println(footer);
	}
	out.print(scratchStream.toString());
    }

    /**
     * Given a list of arguments, parse and remove options as they are
     * found.
     */
    boolean parse(Input input) {
	try {
	    for (int currentIndex = input.size() - 1;
		 currentIndex > -1;
		 --currentIndex) {
		String string = input.parameter(currentIndex);
		if (string.equals("--")) {
		    input.remove(currentIndex);
		    break;
		}
		if (string.charAt(0) != '-')
		    continue;
		handleLongOption(input, string, currentIndex + 1);
		input.remove(currentIndex);
	    }
	    // See if something went wrong.
	    validate();
	} catch (HelpException h) {
	    return false;
	} catch (OptionException err) {
	    throw new InvalidCommandException
		(programName + ": " + err.getMessage() + "\n"
		 + programName + ": Try '" + programName
		 + " -help for more information");
	}
	return true;
    }

    private void handleLongOption(Input input, String real, int index)
	throws OptionException {
	String option = real.substring(real.lastIndexOf('-') + 1);
	String justName = option;
	int eq = option.indexOf('=');
	if (eq != -1)
	    justName = option.substring(0, eq);
	boolean isPlainShort = justName.length() == 1;
	char shortName = justName.charAt(0);
	Option found = null;
	for (Iterator i = options.iterator(); i.hasNext(); ) {
	    Option opt = (Option)(i.next());
	    if (justName.equals(opt.getLongName())) {
		found = opt;
		break;
	    }
	    if ((isPlainShort || opt.isJoined())
		&& opt.getShortName() == shortName) {
		if (!isPlainShort) {
		    // The rest of the option string is the argument.
		    eq = 0;
		}
		found = opt;
		break;
	    }
	}

	if (found == null) {
	    String msg = "unrecognized option '" + real + "'";
	    throw new OptionException(msg);
	}
	String argument = null;
	if (found.getTakesArgument()) {
	    if (eq == -1) {
		argument = input.parameter(index);
		input.remove(index);
	    }
	    else
		argument = option.substring(eq + 1);
	} else if (eq != -1) {
	    String msg = "option " + found.getLongName()
		+ " doesn't allow an argument";
	    throw new OptionException(msg);
	}
	found.parsed(argument);

    }

    protected void validate() throws OptionException {
	// Base implementation does nothing.
    }
}
