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

import frysk.value.Format;

/**
 * A command option; optionally parameterized.
 */

abstract class CommandOption {
    final String longName;
    final char shortName;
    final String description;
    final String parameter;
    CommandOption(String longName, char shortName, String description,
		  String parameter) {
	this.longName = longName;
	this.shortName = shortName;
	this.description = description;
	this.parameter = parameter;
    }
    CommandOption(String name, String description, String parameter) {
	this(name, '\0', description, parameter);
    }
    /**
     * An option that doesn't have an argument.
     */
    CommandOption(String name, String description) {
	this(name, '\0', description, null);
    }

    /**
     * Utility, parse a boolean parameter.
     */
    boolean parseBoolean(String argument) {
	argument = argument.toLowerCase();
	if (argument.equals("yes")
	    || argument.equals("y"))
	    return true;
	else if (argument.equals("no")
		 || argument.equals("n"))
	    return false;
	
	else
	    throw new InvalidCommandException
		("option -" + longName + " requires yes or no parameter");
    }

    /**
     * Parse sign/magnitude integer.
     */
    static class Magnitude {
	final int sign;
	final int magnitude;
	Magnitude(String argument) {
	    if (argument.charAt(0) == '+') {
		sign = 1;
		magnitude = Integer.parseInt(argument.substring(1));
	    } else if (argument.charAt(0) == '-') {
		sign = -1;
		magnitude = Integer.parseInt(argument.substring(1));
	    } else {
		sign = 0;
		magnitude = Integer.parseInt(argument);
	    }
	}
    }

    /**
     * Template option; parse a format.
     */
    static abstract class FormatOption extends CommandOption {
	FormatOption() {
	    super("format", "print format", "d|o|x|t");
	}
	void parse(String argument, Object options) {
	    Format format;
	    if (argument.compareTo("d") == 0) 
		format = Format.DECIMAL;
	    else if (argument.compareTo("o") == 0)
		format = Format.OCTAL;
	    else if (argument.compareTo("x") == 0) 
		format = Format.HEXADECIMAL;
	    else if (argument.compareTo("t") == 0)
		format = Format.BINARY;
	    else
		throw new InvalidCommandException("unrecognized format: "
						  + argument);
	    set(options, format);
	}
	abstract void set(Object options, Format format);
    }

    abstract void parse(String argument, Object options);
}
