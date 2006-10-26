// This file is part of the program FRYSK.
//
// Copyright 2006, Red Hat Inc.
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

package frysk3116;

import org.gnu.gtk.Gtk;
import org.gnu.gtk.ScrolledWindow;
import org.gnu.gtk.TextView;
import org.gnu.gtk.VBox;
import org.gnu.gdk.Window;
import org.gnu.gdk.Drawable;
import org.gnu.gdk.GC;

/**
 * Creates a testcase for BZ 3116. 
 * 
 * Use of Regions and GC (Graphics Context) 
 * can cause pointer smash.
 * 
 *
 */

public class RegionAndGCFailure
{

    public static void main (String[] args)
    {
	Gtk.init (new String[] {});
	
	// Top level window.
	org.gnu.gtk.Window parent = new org.gnu.gtk.Window();
	VBox box = new VBox(false,0);
	parent.add(box);
	
	// Scrolled window to hold our TextView.
	ScrolledWindow sw = new ScrolledWindow();
	box.add(sw);
    
	// TextView.
	TextView tv = new TextView();
	sw.addWithViewport(tv);
    
	// Have to do this show() so GC will work.
	parent.showAll();
    
	// Get our org.gnu.gdk.Window from the textview.
	// This is simply the easiest way to get a workable
	// instance of a GDK window, which is critical to the test.
	Window drawingArea = tv.getWindow();
    
	// Get Graphical Context.
	GC myContext = new GC((Drawable)drawingArea);

	// Get the y coordinates for the top and bottom of the
	// window. This creates an anonymous Region object, that will
	// be scheduled for GC after these operations complete. It
	// should be the final piece to get the smash.
   	drawingArea.getClipRegion();
   	drawingArea.getClipRegion();

        new Thread (new Runnable() 
	    {
		public void run() 
		{			  
		    // Create an idle situation.
		    try {
			Thread.sleep(1000);
		    } catch (InterruptedException e) {
			throw new RuntimeException (e);
		    }
		    Gtk.mainQuit();
		}
	    }).start ();

   	// GC stuff; should finalize things.
   	System.gc();

	// Free up corresponding GTK objects.
        Gtk.main();
   	
    }
}
