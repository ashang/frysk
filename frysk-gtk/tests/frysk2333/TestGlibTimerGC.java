// This file is part of the program FRYSK.
//
// Copyright 2005, Red Hat Inc.
//
// FRYSK is free software; you can redistribute it and/or modify it
// under the terms of the GNU General Public License as published by
// the Free Software Foundation; version 2 of the License.
//
// FRYSK is distributed in the hope that it will be useful, but
// WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
// General Public License for more details.
// type filter text
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

package frysk2333;

import org.gnu.glib.Fireable;
import org.gnu.glib.Timer;
import org.gnu.gtk.Gtk;
import org.gnu.glib.CustomEvents;

public class TestGlibTimerGC {

	/**
	 * @param args
	 */

	public static void main(String[] args) {
	
		// Tests http://sourceware.org/bugzilla/show_bug.cgi?id=2333
		// on an unpatched JG on RHEL4-U3 compiled with GCC 4.0.1
		// the Timer object should cause a GC panic

		Gtk.init(new String[]{});

		// Add Timer test to custom event. When this event is processed
		// and run, and exits - it ensures we lose reference to the timer
		// and allows it to be GC'd

		 CustomEvents.addEvent(new Runnable() {
			public void run() {

				Timer timer = new Timer(1000, new Fireable() {
					public boolean fire() {                                  
						System.out.println("Testing Timer ... ");
						Gtk.mainQuit();

						return false;
					}

					protected void finalize() throws Throwable {
						System.out.println("Finalizing");
						super.finalize();
						System.out.println("Finalizing Complete");

					}

			});
			timer.start();
		  }});


		Gtk.main();

		// Event has been inserted into Custom Event queue
		// Now do a GC
		System.out.println("Garbage collecting");
		System.gc();
		System.out.println("Garbage collecting complete");

		// Normally the sleep is not needed. But we sleep 
		// to make sure the program does not exit early.
		// On an unpatched system, a system SIGSEGV should occur

		try {
			System.out.println("Sleeping");
			Thread.sleep(2000);
			System.out.println("Sleeping Complete");

		} catch (Exception e) {
			System.out.println("Interrupted");
		}
	}
}
