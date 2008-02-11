// This file is part of the program FRYSK.
//
// Copyright 2007, 2008, Red Hat Inc.
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

package frysk.sys.termios;

import frysk.rsl.Log;
import java.lang.reflect.Field;
import java.util.Iterator;

/**
 * Manipulate the speed setting of a terminal.
 */
public class TestSpeed extends TestLib {
    private static final Log fine = Log.fine(TestSpeed.class);

    /**
     * Change the test PTY's speed.  Verify the results.
     */
    private void verifySpeed(Speed speed) {
	fine.log(this, "verifySpeed", speed);
	termios.set(speed);
	assertEquals("input speed set", speed, termios.getInputSpeed());
	assertEquals("output speed set", speed, termios.getOutputSpeed());
	setPseudoTerminal(termios);
	assertEquals("input speed same", speed, termios.getInputSpeed());
	assertEquals("output speed same", speed, termios.getOutputSpeed());
	getPseudoTerminal(termios);
	assertEquals("input speed get", speed, termios.getInputSpeed());
	assertEquals("output speed get", speed, termios.getOutputSpeed());
	verifySttyOutputContains("speed " + speed);
    }

    public void testSpeeds ()
    {
	Speed[] speeds = Speed.getSpeeds ();
	// Check that at least one known speed is present.
	boolean foundBAUD0 = false;
	for (int i = 0; i < speeds.length; i++) {
	    if (speeds[i].equals (Speed.BAUD_0)) {
		foundBAUD0 = true;
		break;
	    }
	}
	assertTrue ("found BAUD 0", foundBAUD0);
	// Check each speed is correctly wired.
	for (int i = 0; i < speeds.length; i++) {
	    Speed speed = speeds[i];
	    verifySpeed (speed);
	}

    }

    /**
     * Check that the Speed names are correct.
     */
    public void testSpeedNames() throws IllegalAccessException {
	for (Iterator i = Mode.getStaticFields(Speed.class).iterator();
	     i.hasNext(); ) {
	    Field field = (Field) i.next();
	    Speed speed = (Speed) field.get(null);
	    fine.log(this, "testSpeedField", field.getName());
	    assertEquals("speed field name", "BAUD_" + speed,
			 field.getName());
	}
    }
}
