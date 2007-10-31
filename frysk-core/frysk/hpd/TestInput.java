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

/**
 * Check that the Input tokenizer breaks up and advances commands
 * correctly.
 */
public class TestInput extends TestLib {
    private void check(Input input, String set,
		       String[] results,
		       int[] starts,
		       int[] ends) {
	assertEquals("size", input.size(), results.length);
	for (int i = 0; i < results.length; i++) {
	    assertEquals("parameter " + i, results[i], input.parameter(i));
	    assertEquals("start " + i, starts[i], input.token(i).start);
	    assertEquals("end " + i, ends[i], input.token(i).end);
	}
    }

    private void check(Input input, String set, String[] results) {
	int[] starts = new int[results.length];
	int[] ends = new int[results.length];
	for (int i = 0; i < results.length; i++) {
	    starts[i] = input.getFullCommand().indexOf(results[i]);
	    ends[i] = starts[i] + results[i].length();
	}
	check(input, set, results, starts, ends);
    }

    private void check(String input, String[] results) {
	check(new Input(input), null, results);
    }

    public void testAccept() {
	Input input = new Input("action p0 p1");
	assertNull("getAction", input.getAction());
	check(input, null, new String[] { "action", "p0", "p1" });
	input = input.accept();
	assertEquals("getAction at action", "action", input.getAction());
	check(input, null, new String[] { "p0", "p1" });
	input = input.accept();
	assertEquals("getAction at p0", "p0", input.getAction());
	check(input, null, new String[] { "p1" });
	input = input.accept();
	assertEquals("getAction at p1", "p1", input.getAction());
	check(input, null, new String[0]);
    }

    public void testEmpty() {
	check("", new String[0]);
    }

    public void testOneToken() {
	check("token", new String[] { "token" });
    }

    public void testOneSpacedToken() {
	check("  token  ", new String[] { "token" });
    }
    
    public void testSeveralTokens() {
	check(" 1  2  3   4", new String[] { "1", "2", "3", "4" });
    }

    public void testDoubleQuote() {
	// Remember, \" is one character.
	check(new Input("1 \" 2 \" 3"), null,
	      new String[] { "1", " 2 ", "3" },
	      new int[] { 0, 2, 8 },
	      new int[] { 1, 7, 9 });
    }

    public void testDoubleQuoteInToken() {
	// Remember, \" is one character.
	check(new Input(" a\" \"b "), null,
	      new String[] { "a b" },
	      new int[] { 1 },
	      new int[] { 6 });
    }

    public void testEmptyQuote() {
	// Remember, \" is one character.
	check(new Input("\"\""), null, new String[] { "" },
	      new int[] { 0 },
	      new int[] { 2 });
    }

    public void testEmptyQuoteBetweenParameters() {
	// Remember, \" is one character.
	check(new Input("1 \"\" 3"), null,
	      new String[] {"1", "", "3"},
	      new int[] { 0, 2, 5 },
	      new int[] { 1, 4, 6 });
    }

    public void testEscapedQuote() {
	// Remember, \" is one character.
	check(new Input("\\\""), null, new String[] { "\"" },
	      new int[] { 0 },
	      new int[] { 2 });
    }

    public void testSet() {
	check(new Input(" [1.2] "), "[1.2]", new String[0]);
    }

    public void testSetAndParameters() {
	check(new Input(" [1.2] a b"), "[1.2]", new String[] { "a", "b" });
    }

    private void checkInvalidCommandException(String input) {
	boolean caught = false;
	try {
	    new Input(input);
	} catch (InvalidCommandException e) {
	    caught = true;
	}
	assertTrue("caught InvalidCommandException", caught);
    }

    public void testMissingQuote() {
	checkInvalidCommandException("  \" ");
    }

    public void testMissingRightBracket() {
	checkInvalidCommandException("  [ ");
    }

    public void testMissingEscapee() {
	checkInvalidCommandException("\\");
    }
}
