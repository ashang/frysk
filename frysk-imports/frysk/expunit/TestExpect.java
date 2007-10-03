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

package frysk.expunit;

import frysk.junit.TestCase;

/**
 * Test ExpectUnit framework.
 */

public class TestExpect
    extends TestCase
{
    Expect e;
    public void tearDown ()
    {
	if (e != null)
	    e.close ();
    }

    /**
     * Check that short hand EOF works.
     */
    public void testAssertEOF ()
    {
	e = new Expect (new String[] { "/bin/true" });
	e.assertEOF ();
    }

    /**
     * Try a timeout, that passes.
     */
    public void testTimeout ()
    {
	e = new Expect (new String[] { "/bin/cat" });
	long oldTime = System.currentTimeMillis ();
	e.assertExpect (100, new Timeout ()
	    {
		public void timeout ()
		{
		    // ignore
		}
	    }, null, null);
	long newTime = System.currentTimeMillis ();
	assertTrue ("time passed", newTime > oldTime);
    }

    /**
     * Try to match a sequence of simple strings.
     */
    public void testEquals ()
    {
	e = new Expect (new String[] { "/bin/echo", "catdog" });
	e.assertExpect (new Equals ("cat"));
	e.assertExpect (new Equals ("dog"));
    }

    /**
     * Try to match a regular expression, confirm it was correctly
     * removed by following it with an anchored match.
     */
    public void testRegex ()
    {
	e = new Expect ("tee");
	e.send ("catchthebi");
	// Skip "catch", match "the", leaving "bi".
	e.assertExpect ("the");
	// Append "rd", making "bird"
	e.send ("rd");
	// Match the "bird".
	e.assertExpect ("bird");
    }

    /**
     * Try a command that is invoked via bash, bash will cause
     * globbing et.al. to occure.  Expands the shell PID.
     */
    public void testUnderBash ()
    {
	e = new Expect ("echo x $$ y");
	e.assertExpect ("x " + e.getPid () + " y");
    }

    /**
     * Try interacting with bash, check here that the pseudo-terminal
     * is correctly configured - that no double echo is occuring for
     * instance.
     */
    public void testBash ()
    {
	// Create the shell, as sh
	e = new Expect (new String[] { "/bin/sh" });
	// Match the initial prompt.  This ensures that things are
	// blocked until the shell is fully running.  As a side effect
	// it also discards all output so far.
	e.assertExpect ("\\$ ");
	// Send a command to simplify the prompt, and then match it.
	// Notice how the matching pattern includes the
	// carrage-return, the new-line, and the promt.  That ensures
	// that it doesn't accidently match the command.
	e.send ("PS1='\\$ '\r");
	e.assertExpect ("\r\n\\$ ");
	// Check out the terminal, again be careful to match the
	// prompt.
	e.send ("stty -a\r");
	final StringBuffer g = new StringBuffer ();
	e.assertExpect (new Match[]
	    {
		new Regex ("\\s(-brkint)\\s.*\\$ ")
		{
		    public void execute ()
		    {
			g.append (group (1));
		    }
		},
		new Regex ("\\s(brkint)\\s.*\\$ ")
		{
		    public void execute ()
		    {
			g.append (group (1));
		    }
		}
	    });
	assertEquals ("brk mode", "-brkint", g.toString ());
    }
}
