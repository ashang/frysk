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
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;

public class TestOptionParser extends TestLib {

    private static class DummyParseCommand extends Command {
	boolean parsedOption = false;
	boolean ok;
	String argument = null;
	List params;

	public DummyParseCommand() {
	    super("parser", "parse some stuff",
		  "parse ARGUMENTS [OPTIONS]", "test the parser");
	}

	public void parse(CLI cli, Input cmd) {
	    params = cmd.getParameters();
	    ok = parser.parse(cmd);
	}

    }

    private CLI cli;
    private DummyParseCommand parser;
    public void setUp() {
	super.setUp();
	cli = new CLI("(fhpd) ", new PrintStream(new ByteArrayOutputStream()));
	parser = new DummyParseCommand();
	cli.addHandler(parser);
    }
    public void tearDown() {
	cli = null;
	parser = null;
	super.tearDown();
    }

    public void testDashDash() {
	cli.execCommand("parser --");
	assertTrue("Params list should be empty", parser.params.isEmpty());
    }

    public void testRegular() {
	cli.execCommand("parser argument");
	assertEquals("Param list should have one item", parser.params.size(), 1);
	assertEquals("Argument should be 'argument'", parser.params.get(0),
		"argument");

    }

    public void testRegularDashDash() {
	cli.execCommand("parser argument --");
	assertEquals("Param list should have one item", parser.params.size(), 1);
	assertEquals("Argument should be 'argument'", parser.params.get(0),
		"argument");
    }

    public void testOption() {
	parser.parser.add(new Option("short", "short option") {

	    public void parsed(String argument) throws OptionException {
		parser.parsedOption = true;
	    }
	});
	cli.execCommand("parser -short");
	assertTrue("Params list should be empty", parser.params.isEmpty());
	assertTrue("Option should have been handled", parser.parsedOption);
    }

    public void testOptionAfterDashDash() {
	parser.parser.add(new Option("short", "short option") {

	    public void parsed(String argument) throws OptionException {
		parser.parsedOption = true;
	    }
	});
	cli.execCommand("parser -- -short");
	assertTrue("Params list should be empty", parser.params.isEmpty());
	assertTrue("Option should have been handled", parser.parsedOption);
    }

    public void testOptionBeforeDashDash() {
	parser.parser.add(new Option("short", "short option") {

	    public void parsed(String argument) throws OptionException {
		fail("Should not have parsed option");
	    }
	});
	cli.execCommand("parser -short --");
	assertEquals("Param list should have one item", parser.params.size(), 1);
	assertEquals("Argument should be '-short'", parser.params.get(0),
		"-short");
	assertFalse("Argument should not have been parsed", parser.parsedOption);
    }

    public void testOptionWithArgs() {
	parser.parser.add(new Option("short", "short option", "ARG") {

	    public void parsed(String argument) throws OptionException {
		parser.parsedOption = true;
		parser.argument = argument;
	    }

	});
	cli.execCommand("parser -short argument");
	assertTrue("Params list should be empty", parser.params.isEmpty());
	assertTrue("Option should have been handled", parser.parsedOption);
	assertEquals("Option should have argument 'argument'", parser.argument,
		"argument");
    }

    public void testOptionWithArgsAfterDashDash() {
	parser.parser.add(new Option("short", "short option", "ARG") {

	    public void parsed(String argument) throws OptionException {
		parser.parsedOption = true;
		parser.argument = argument;
	    }

	});
	cli.execCommand("parser -- -short argument");
	assertTrue("Params list should be empty", parser.params.isEmpty());
	assertTrue("Option should have been handled", parser.parsedOption);
	assertEquals("Option should have argument 'argument'", parser.argument,
		"argument");
    }

    public void testOptionWithArgsBeforeDashDash() {
	parser.parser.add(new Option("short", "short option", "ARG") {
	    public void parsed(String argument) throws OptionException {
		parser.parsedOption = true;
		parser.argument = argument;
	    }

	});
	cli.execCommand("parser -short argument --");
	assertEquals("Params list should have 2 elements",
		parser.params.size(), 2);
	assertEquals("First argument: '-short", parser.params.get(0), "-short");
	assertEquals("Second argument: 'argument", parser.params.get(1),
		"argument");
	assertFalse("Option should not have been handled", parser.parsedOption);

    }

    public void testHelp() {
	cli.execCommand("parser -help");
	assertTrue("Help only should be activated", !parser.ok);
    }
}
