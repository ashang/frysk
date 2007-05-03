// This file is part of the program FRYSK.
//
// Copyright 2005, 2006, Red Hat Inc.
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

package frysk.sys;

import frysk.junit.TestCase;

/**
 * Test manipulation of a signal set.
 */

public class TestSignalSet
    extends TestCase
{
    /**
     * Check that fill, and empty, add remove signals from the set.
     */
    public void testFillEmpty ()
    {
	SignalSet set = new SignalSet ();
	assertFalse ("set contains Sig.HUP", set.contains (Sig.HUP));
	set.fill ();
	assertTrue ("set contains Sig.HUP", set.contains (Sig.HUP));
	set.empty ();
	assertFalse ("set contains Sig.HUP", set.contains (Sig.HUP));
    }

    /**
     * Check that adding, and removing a signal works.
     */
    public void testAddRemove ()
    {
	SignalSet set = new SignalSet ();
	assertFalse ("Sig.HUP is member", set.contains (Sig.HUP));
	assertFalse ("Sig.USR1 is member", set.contains (Sig.USR1));

	set.add (Sig.HUP);
	assertTrue ("Sig.HUP is member", set.contains (Sig.HUP));
	assertFalse ("Sig.USR1 is member", set.contains (Sig.USR1));

	set.add (Sig.USR1);
	assertTrue ("Sig.HUP is member", set.contains (Sig.HUP));
	assertTrue ("Sig.USR1 is member", set.contains (Sig.USR1));

	set.remove (Sig.USR1);
	assertTrue ("Sig.HUP is member", set.contains (Sig.HUP));
	assertFalse ("Sig.USR1 is member", set.contains (Sig.USR1));
    }

    /**
     * Check creating a SignalSet from an array.
     */
    public void testNewFromArray ()
    {
	SignalSet set = new SignalSet (new Sig[] { Sig.HUP, Sig.USR1 });
	assertTrue ("set contains Sig.HUP", set.contains (Sig.HUP));
	assertTrue ("set contains Sig.USR1", set.contains (Sig.USR1));
	assertFalse ("set contains Sig.USR2", set.contains (Sig.USR2));
    }

    /**
     * Check creating a SignalSet from a single signal.
     */
    public void testNewFromSig ()
    {
	SignalSet set = new SignalSet (Sig.HUP);
	assertTrue ("set contains Sig.HUP", set.contains (Sig.HUP));
	assertFalse ("set contains Sig.USR1", set.contains (Sig.USR1));
    }

    /**
     * Check toArray does just that.
     */
    public void testToArray()
    {
	Sig[] sigs = new Sig[] { Sig.HUP, Sig.USR1 };
	SignalSet set = new SignalSet (sigs);
	// Check right length
	assertEquals ("number of sigs", sigs.length, set.toArray().length);
	// Check all found
	for (int sigI = 0; sigI < sigs.length; sigI++) {
	    boolean found = false;
	    for (int setI = 0; setI < sigs.length; setI++) {
		if (sigs[sigI].equals (set.toArray()[setI]))
		    found = true;
	    }
	    assertTrue ("signal " + sigs[sigI] + " found", found);
	}
    }

    /**
     * Check a signal set.
     */
    public void testEmptyToString()
    {
	assertEquals ("empty set", "{}", new SignalSet().toString());
    }
    public void testSingleToString()
    {
	assertEquals ("SIGHUP set", "{SIGHUP}",
		      new SignalSet (Sig.HUP).toString());
    }
    public void testMultiToString()
    {
	// assumes a specific ordering
	if (Sig.HUP.compareTo(Sig.USR1) < 0)
	    assertEquals("SIGHUP+SIGUSR1 set", "{SIGUSR1,SIGHUP}",
			 new SignalSet(new Sig[] {Sig.HUP, Sig.USR1})
			 .toString());
	else
	    assertEquals("SIGHUP+SIGUSR1 set", "{SIGHUP,SIGUSR1}",
			 new SignalSet(new Sig[] {Sig.HUP, Sig.USR1})
			 .toString());
    }

    public void testProcMask ()
    {
	SignalSet set = new SignalSet (new Sig[] { Sig.WINCH });
	SignalSet old = new SignalSet ();
	SignalSet pending = new SignalSet ();
	set.setProcMask (old);

	// Check that a masked signal becomes pending
	pending.getPending ();
	assertFalse ("pending contains Sig.WINCH",
		     pending.contains (Sig.WINCH));
	Signal.tkill (Tid.get (), Sig.WINCH);
	pending.getPending ();
	assertTrue ("pending contains Sig.WINCH",
		    pending.contains (Sig.WINCH));

	// Calling sigsuspend, unblocking the SigWINCH signal, hangs.
	// This is because the signal is ignored.

	// Put things back
	old.setProcMask ();
    }
}
