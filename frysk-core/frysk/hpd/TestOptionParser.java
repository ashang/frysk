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

public class TestOptionParser extends TestLib {

    private OptionParser parser;
    private boolean ok;
    private String argument;
    private Input input;
    private boolean parsedOption;

    public void setUp() {
	super.setUp();
	parser = new OptionParser("parser", "<<header>>", "<<footer>>");
	ok = false;
	argument = null;
	parsedOption = false;
    }
    public void tearDown() {
	parser = null;
	input = null;
	argument = null;
	super.tearDown();
    }

    private void parse(String string) {
	input = new Input(string).accept();
	ok = parser.parse(input);
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
	parser.add(new Option("short", "short option") {
		public void parsed(String argument) throws OptionException {
		    parsedOption = true;
		}
	    });
	parse("parser -short");
	assertEquals("Params list should be empty", 0, input.size());
	assertTrue("Option should have been handled", parsedOption);
    }

    public void testOptionAfterDashDash() {
	parser.add(new Option("short", "short option") {
		public void parsed(String argument) throws OptionException {
		    parsedOption = true;
		}
	    });
	parse("parser -- -short");
	assertEquals("Params list should be empty", 0, input.size());
	assertTrue("Option should have been handled", parsedOption);
    }

    public void testOptionBeforeDashDash() {
	parser.add(new Option("short", "short option") {
		public void parsed(String argument) throws OptionException {
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
	parser.add(new Option("short", "short option", "ARG") {
		public void parsed(String arg) throws OptionException {
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
	parser.add(new Option("short", "short option", "ARG") {
		public void parsed(String arg) throws OptionException {
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
	parser.add(new Option("short", "short option", "ARG") {
		public void parsed(String arg) throws OptionException {
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
	assertTrue("Help only should be activated", !ok);
    }
}
