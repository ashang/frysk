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
package frysk.gui.monitor;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import org.gnu.gdk.DragAction;
import org.gnu.gdk.ModifierType;
import org.gnu.glade.LibGlade;
import org.gnu.gtk.DestDefaults;
import org.gnu.gtk.Notebook;
import org.gnu.gtk.TargetEntry;
import org.gnu.gtk.TargetFlags;
import org.gnu.gtk.Window;
import org.gnu.gtk.event.DeleteDragDataEvent;
import org.gnu.gtk.event.DragMotionEvent;
import org.gnu.gtk.event.DragOriginListener;
import org.gnu.gtk.event.DragTargetListener;
import org.gnu.gtk.event.DropDragEvent;
import org.gnu.gtk.event.EndDragEvent;
import org.gnu.gtk.event.LeaveDragDestinationEvent;
import org.gnu.gtk.event.ReceiveDragDataEvent;
import org.gnu.gtk.event.RequestDragDataEvent;
import org.gnu.gtk.event.StartDragEvent;

import frysk.gui.FryskGui;

public class MainWindow extends Window implements Saveable{
		
	private Notebook noteBook;
	
	private AllProcWidget allProcWidget;
	
	private Logger errorLog = Logger.getLogger(FryskGui.ERROR_LOG_ID);
	public MainWindow(LibGlade glade) throws IOException {
		super(((Window)glade.getWidget("procpopWindow")).getHandle());
		
		try {
			this.allProcWidget = new AllProcWidget(glade);
		} catch (IOException e)
		{
			errorLog.log(Level.SEVERE,"IOException from Proc Widget",e);
		}
		
		this.noteBook = (Notebook) glade.getWidget("noteBook");
				
		TargetEntry[] entries = new TargetEntry[1];
		entries[0] = new TargetEntry("tap", TargetFlags.NO_RESTRICTION, 0);
		
		this.noteBook.setDragSource(ModifierType.BUTTON1_MASK, entries , DragAction.COPY);
		this.noteBook.setDragDestination(DestDefaults.ALL, entries, DragAction.COPY);
		
		this.noteBook.addListener(new DragOriginListener(){

			public void dragStarted(StartDragEvent event) {
				System.out.println("dragStarted");
			}

			public void dragEnded(EndDragEvent event) {
				System.out.println("dragEnded");
			}

			public void dataRequested(RequestDragDataEvent arg0) {
				System.out.println("dataRequested");				
			}

			public void dataDeleted(DeleteDragDataEvent arg0) {
				System.out.println("dataDeleted");				
			}
			
		});
		
		this.noteBook.addListener(new DragTargetListener(){

			public void destinationLeft(LeaveDragDestinationEvent arg0) {
				System.out.println("----destinationLeft---");
			}

			public boolean dropped(DropDragEvent arg0) {
				System.out.println("----dropped---");
				return false;
			}

			public void dataReceived(ReceiveDragDataEvent arg0) {
				 System.out.println("----dataReceived---");				
			}

			public boolean motionOcurred(DragMotionEvent arg0) {
				System.out.println("motionOcurred");
				return false;
			}
			
		});

		this.showAll();
	}

	public void save(Preferences prefs) {
		prefs.putInt("position.x", this.getPosition().getX());
		prefs.putInt("position.y", this.getPosition().getY());
		
		prefs.putInt("size.height", this.getSize().getHeight());
		prefs.putInt("size.width", this.getSize().getWidth());
		
		allProcWidget.save(Preferences.userRoot().node(prefs.absolutePath() + "/allProcWidget"));
	}

	public void load(Preferences prefs) {
		int x = prefs.getInt("position.x", this.getPosition().getX());
		int y = prefs.getInt("position.y", this.getPosition().getY());
		this.move(x,y);
		
		int width  = prefs.getInt("size.width", this.getSize().getWidth());
		int height = prefs.getInt("size.height", this.getSize().getHeight());
		this.resize(width, height);
		
		allProcWidget.load(Preferences.userRoot().node(prefs.absolutePath() + "/allProcWidget"));
	}
	
}

