// This file is part of the program FRYSK.
// 
// Copyright 2008, Red Hat Inc.
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

package frysk.rsl;

import inua.util.PrintWriter;
import java.io.StringWriter;

/**
 * Class for constructing a backtrace string.
 */
public class TestCallers extends TestLib {
    private Printer oldPrinter;
    public void tearDown() {
	if (oldPrinter != null) {
	    Log.set(oldPrinter);
	    oldPrinter = null;
	}
    }

    public void testCALLER() {
	StringWriter buf = new StringWriter();
	// XXX: Restored during tearDown
	oldPrinter = Log.set(new PrintWriter(buf));
	Log log = get("").set(Level.FINE).get(Level.FINE);
	log.log("caller", Log.CALLER);
	assertTrue("log", buf.toString().contains(" caller "));
	assertTrue("log", buf.toString().contains("testCALLER"));
    }

    public void testNonLoggedCallers() {
	String[] callers = Callers.callers(get("", Level.FINE), 2);
	assertEquals("callers.length", 1, callers.length);
	assertEquals("callers[0]", "<unknown>", callers[0]);
    }

    public void testLoggedCallers() {
	Log log = get("").set(Level.FINE).get(Level.FINE);
	String[] callers = Callers.callers(log, 2);
	assertEquals("callers.length", 2, callers.length);
	assertTrue("callers[0]", callers[0].contains("testLoggedCallers"));
	assertEquals("callers[1]", "...", callers[1]);
    }

    public void testLoggedAllCallers() {
	Log log = get("").set(Level.FINE).get(Level.FINE);
	String[] callers = Callers.callers(log, Integer.MAX_VALUE);
	assertTrue("callers[0]", callers[0].contains("testLoggedAllCallers"));
	assertTrue("callers[N]", !callers[callers.length - 1].equals("..."));
    }
}
