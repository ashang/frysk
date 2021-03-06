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
	command = new ParameterizedCommand("<<description>>",
					   "<<syntax>>", "<<full>>") {
		void interpret(CLI cli, Input input, Object options) {
		    interpreted = true;
		}
		void help(CLI cli, Input input) {
		    helped = true;
		}
		int completer(CLI cli, Input input, int cursor,
			      List candidates) {
		    return -1;
		}
	    };
	command.add(new CommandOption("arg", "long parameterized option",
				      "ARG") {
		void parse(String arg, Object options) {
		    parsedOption = true;
		    argument = arg;
		    assertNotNull("-arg's argument", argument);
		}
	    });
	// make -a ambigious
	command.add(new CommandOption("aRG", "long parameterized option",
				      "ARG") {
		void parse(String arg, Object options) {
		    fail("should never specify -aRG");
		}
	    });
	command.add(new CommandOption("opt", "long option") {
		void parse(String arg, Object options) {
		    parsedOption = true;
		    argument = arg;
		    assertNull("-opt's argument", argument);
		}
	    });
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

    private void check(String string, String stringValue,
		       String[] parameters,
		       boolean parsedOption, String argument) {
	parse(string);
	assertEquals("input size", parameters.length, input.size());
	assertEquals("stringValue", stringValue, input.stringValue());
	assertEquals("parsedOption", this.parsedOption, parsedOption);
	assertEquals("argument", this.argument, argument);
	for (int i = 0; i < parameters.length; i++) {
	    assertEquals("parameter " + i, parameters[i], input.parameter(i));
	}
	assertTrue("interpreted", interpreted);
	assertFalse("helped", helped);
    }

    public void testDashDash() {
	check("parser --", "", new String[0], false, null);
    }

    public void testRegular() {
	check("parser argument", "argument",
	      new String[] { "argument" }, false, null);
    }

    public void testRegularDashDash() {
	check("parser argument --", "argument",
	      new String[] { "argument" }, false, null);
    }

    public void testOption() {
	check("parser -opt", "", new String[0], true, null);
    }

    public void testOptionAfterDashDash() {
	check("parser -- -opt", "", new String[0], true, null);
    }

    public void testOptionBeforeDashDash() {
	check("parser -opt --", "-opt", new String[] { "-opt" }, false, null);
    }

    public void testOptionWithArg() {
	check("parser -arg argument", "", new String[0], true, "argument");
    }

    public void testOptionWithArgAfterDashDash() {
	check("parser -- -arg argument", "", new String[0], true, "argument");
    }

    public void testOptionWithArgBeforeDashDash() {
	check("parser -arg argument --", "-arg argument",
	      new String[] { "-arg", "argument" },
	      false, null);
    }

    public void testOptionWithDashArg() {
	check("parser -arg -1", "",
	      new String[0],
	      true, "-1");
    }

    private void checkInvalid(String string) {
	RuntimeException thrown = null;
	try {
	    parse(string);
	} catch (InvalidCommandException e) {
	    thrown = e;
	}
	assertNotNull("exception thrown", thrown);
    }

    public void testMissingArg() {
	checkInvalid("parser -arg");
    }

    public void testUnknownOpt() {
	checkInvalid("parser -unknown");
    }

    public void testShortAmbigiousOption() {
	checkInvalid("parser -a");
    }
    public void testShortUnambigiousOption() {
	check("parser arg -o", "arg",
	      new String[] { "arg" },
	      true, null);
    }

    public void testHelp() {
	parse("parser -help");
	assertFalse("interpreted", interpreted);
	assertTrue("helped", helped);
    }
}
