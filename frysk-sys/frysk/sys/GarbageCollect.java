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

package frysk.sys;

/**
 * Attempt to force a garbage collect.
 */

public class GarbageCollect {
    /**
     * Variable to flag that the garbage collector; or at least
     * finalize was run.
     */
    private static class Garbage {
	static volatile boolean collected;
	static void create() {
	    new Garbage();
	}
	private Garbage() {
	    collected = false;
	}
	protected void finalize() {
	    collected = true;
	}
    }
    private static void leakMemory(int amount) {
	for (int i = 0; i < amount; i++) {
	    new Long(i);
	}
    }
    public static synchronized boolean run() {
	Garbage.create();
	int agressive = 0;
	while (!Garbage.collected) {
	    System.gc();
	    switch (agressive++) {
	    case 0:
	    case 1:
		// Simple yield; if there's garbage to collect it will
		// run.
		Thread.yield();
		break;
	    case 2:
	    case 3:
		// Create some garbage so that the GC feels a reason
		// to run.
		leakMemory(10000 << agressive);
		// Now that there's more garbage, re-nudge the gc() so
		// that it can see it (without this the gc() often
		// didn't run).
		System.gc();
		try {
		    Thread.sleep(50 << agressive);
		} catch (InterruptedException e) {
		}
		break;
	    default:
		System.err.println("garbage uncollected");
		return false;
	    }
	    // Clean out any collected garbage.
	    System.runFinalization();
	}
	return true;
    }
}
