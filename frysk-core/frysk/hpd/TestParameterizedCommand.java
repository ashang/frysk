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

import java.util.List;

public class TestParameterizedCommand extends TestLib {

    private ParameterizedCommand command;
    private String argument;
    private Input input;
    private boolean parsedOption;
    private boolean interpreted;
    private boolean helped;

    public void setUp() {
	super.setUp();
	argument = null;
	parsedOption = false;
	interpreted = false;
	helped = false;
	command = new ParameterizedCommand("parser", "<<description>>",
					   "<<syntax>>", "<<full>>") {
		void interpret(CLI cli, Input input, Object options) {
		    interpreted = true;
		}
		void help(CLI cli, Input input) {
		    helped = true;
		}
		int complete(CLI cli, PTSet ptset, String incomplete,
			     int base, List candidates) {
		    return -1;
		}
	    };
    }
    public void tearDown() {
	command = null;
	input = null;
	argument = null;
	super.tearDown();
    }

    private void parse(String string) {
	input = new Input(string).accept();
	command.interpret(null, input);
    }

    public void testDashDash() {
	parse("parser --");
	assertEquals("Params list should be empty", 0, input.size());
    }

    public void testRegular() {
	parse("parser argument");
	assertEquals("Param list should have one item", 1, input.size());
	assertEquals("Argument should be 'argument'", "argument",
		     input.parameter(0));
    }

    public void testRegularDashDash() {
	parse("parser argument --");
	assertEquals("Param list should have one item", 1, input.size());
	assertEquals("Argument should be 'argument'", "argument",
		     input.parameter(0));
    }

    public void testOption() {
	command.add(new CommandOption("short", "short option") {
		void parse(String argument, Object options) {
		    parsedOption = true;
		}
	    });
	parse("parser -short");
	assertEquals("Params list should be empty", 0, input.size());
	assertTrue("Option should have been handled", parsedOption);
	assertTrue("interpreted", interpreted);
	assertFalse("helped", helped);
    }

    public void testOptionAfterDashDash() {
	command.add(new CommandOption("short", "short option") {
		void parse(String argument, Object options) {
		    parsedOption = true;
		}
	    });
	parse("parser -- -short");
	assertEquals("Params list should be empty", 0, input.size());
	assertTrue("Option should have been handled", parsedOption);
    }

    public void testOptionBeforeDashDash() {
	command.add(new CommandOption("short", "short option") {
		void parse(String argument, Object options) {
		    fail("Should not have parsed option");
		}
	    });
	parse("parser -short --");
	assertEquals("Param list should have one item", 1, input.size());
	assertEquals("Argument should be '-short'", "-short",
		     input.parameter(0));
	assertFalse("Argument should not have been parsed", parsedOption);
    }

    public void testOptionWithArgs() {
	command.add(new CommandOption("short", "short option", "ARG") {
		void parse(String arg, Object options) {
		    parsedOption = true;
		    argument = arg;
		}
	    });
	parse("parser -short argument");
	assertEquals("Params list should be empty", 0, input.size());
	assertTrue("Option should have been handled", parsedOption);
	assertEquals("Option should have argument 'argument'",
		     "argument", argument);
    }

    public void testOptionWithArgsAfterDashDash() {
	command.add(new CommandOption("short", "short option", "ARG") {
		void parse(String arg, Object options) {
		    parsedOption = true;
		    argument = arg;
		}
	    });
	parse("parser -- -short argument");
	assertEquals("Params list should be empty", 0, input.size());
	assertTrue("Option should have been handled", parsedOption);
	assertEquals("Option should have argument 'argument'",
		     "argument", argument);
    }

    public void testOptionWithArgsBeforeDashDash() {
	command.add(new CommandOption("short", "short option", "ARG") {
		void parse(String arg, Object options) {
		    parsedOption = true;
		    argument = arg;
		}
	    });
	parse("parser -short argument --");
	assertEquals("Params list should have 2 elements", 2, input.size());
	assertEquals("First argument: '-short", "-short", input.parameter(0));
	assertEquals("Second argument: 'argument", "argument",
		     input.parameter(1));
	assertFalse("Option should not have been handled", parsedOption);
    }

    public void testHelp() {
	parse("parser -help");
	assertFalse("interpreted", interpreted);
	assertTrue("helped", helped);
    }
}
