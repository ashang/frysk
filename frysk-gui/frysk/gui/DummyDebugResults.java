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

package frysk.gui;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.gnu.glade.GladeXMLException;
import org.gnu.glade.LibGlade;
import org.gnu.gtk.CheckButton;
import org.gnu.gtk.ColorButton;
import org.gnu.gtk.Gtk;
import org.gnu.gtk.HBox;
import org.gnu.gtk.HScale;
import org.gnu.gtk.SizeGroup;
import org.gnu.gtk.SizeGroupMode;
import org.gnu.gtk.VBox;
import org.gnu.gtk.Viewport;
import org.gnu.gtk.event.RangeEvent;
import org.gnu.gtk.event.RangeListener;

import frysk.gui.common.IconManager;
import frysk.gui.common.Messages;

public class DummyDebugResults {

	private static final String BASE_PATH = "frysk/gui/";
    private static final String GLADE_PKG_PATH = "glade/";
	
    private LibGlade glade;
    
	public DummyDebugResults(String[] args, String[] gladep, String[] imaged,
			String[] messaged, String[] testfiled){
		
		Gtk.init(args);
		
		IconManager.setImageDir(imaged);
		IconManager.loadIcons();
		IconManager.useSmallIcons();
			
		Messages.setBundlePaths(messaged);
			
		for(int i = 0; i < gladep.length; i++){
			try {
				System.out.println("Trying " + gladep[i]);
				glade = new LibGlade(gladep[i] + "/debugresultswindow.glade", this);
			} catch (GladeXMLException e) {
				continue;
			} catch (FileNotFoundException e) {
				continue;
			} catch (IOException e) {
				continue;
			}
			
			if(glade != null)
				break;
		}
		
		if(glade == null)
			System.exit(1);		
		
		Viewport vp = (Viewport) this.glade.getWidget("observerSelectViewport");
		
		SizeGroup checkSize = new SizeGroup(SizeGroupMode.HORIZONTAL);
		
		VBox box = new VBox(true, 6);
		for(int i = 0; i < 10; i++){
			HBox box2 = new HBox(false, 12);
			CheckButton button = new CheckButton("Observer "+ i, true);
			checkSize.addWidget(button);
			box2.packStart(button, false, false, 0);
			box2.packStart(new ColorButton(), false, false, 0);
			box.packStart(box2);
		}
		box.showAll();
		vp.add(box);
		
		vp = (Viewport) this.glade.getWidget("timelineViewport");
		final DebugHistory hist = new DebugHistory(0);
		vp.add(hist);
		vp.showAll();
		
		final HScale scale = (HScale) this.glade.getWidget("thresholdBar");
		scale.addListener(new RangeListener() {
			public void rangeEvent(RangeEvent arg0) {
				hist.setThreshold((int)scale.getValue());
			}
		});
		
		Gtk.main();
	}
	
	public static void main(String[] args) {
		DummyDebugResults res = new DummyDebugResults(args, new String[] {
			     GLADE_PKG_PATH,
			     BASE_PATH + GLADE_PKG_PATH,
			     // Check both relative ...
			     Build.SRCDIR + "/" + BASE_PATH + GLADE_PKG_PATH,
			     // ... and absolute.
			     Build.ABS_SRCDIR + "/" + BASE_PATH + GLADE_PKG_PATH,
			 },
			 new String[] {
			     Build.ABS_SRCDIR + "/" + BASE_PATH + "images/"
			 }, 
			 new String[] {
			     "./common", Build.SRCDIR + "/" + BASE_PATH + "common/",
			     Build.ABS_SRCDIR + "/" + BASE_PATH + "common/"
			 },
			 new String[] {
			     "./srcwin/testfiles", Build.SRCDIR + "/" + BASE_PATH + "srcwin/testfiles",
			     Build.ABS_SRCDIR + "/" + BASE_PATH + "srcwin/testfiles"
			 });
		res.toString();
	}
}
