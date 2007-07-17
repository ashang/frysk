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

public class TestRegex
    extends TestCase
{
    Expect e;
    public void setUp ()
    {
	e = null;
    }
    public void tearDown ()
    {
	if (e != null)
	    e.close ();
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
	e.expect ("the");
	// Append "rd", making "bird"
	e.send ("rd");
	// Match the "bird".
	e.expect ("bird");
    }

    /**
     * Try to match several grouped items.
     */
    public void testGroups ()
    {
	e = new Expect ("tee");
	e.send ("zzaabbccaa");
	e.expect (new Regex ("(a+)([bc]+)a+")
	    {
		public void execute ()
		{
		    // The goal here is to check that Regex is
		    // correctly wired up to Java's Matcher, and not
		    // that Matcher is working.  For instance:
		    // CLASSPATH Matcher might return -1 instead of
		    // throwing IndexOutOfBoundsException when the
		    // group isn't valid, that isn't tested here.
		    assertEquals ("number groups", 2, groupCount ());
		    assertEquals ("group", "aabbccaa", group ());
		    assertEquals ("start", 2, start ());
		    assertEquals ("end", 10, end ());

		    assertEquals ("group 0", "aabbccaa", group (0));
		    assertEquals ("start 0", 2, start (0));
		    assertEquals ("end 0", 10, end (0));

		    assertEquals ("group 1", "aa", group (1)); 
		    assertEquals ("start 1", 2, start (1));
		    assertEquals ("end 1", 4, end (1));

		    assertEquals ("group 2", "bbcc", group (2)); 
		    assertEquals ("start 2", 4, start (2));
		    assertEquals ("end 2", 8, end (2));
		}
	    });
    }
}
