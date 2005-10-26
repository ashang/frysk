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
/*
 * Created on Oct 18, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package frysk.gui.monitor;

import java.util.Hashtable;

import org.gnu.gdk.DragAction;
import org.gnu.gdk.ModifierType;
import org.gnu.glib.Handle;
import org.gnu.gtk.DestDefaults;
import org.gnu.gtk.Label;
import org.gnu.gtk.Notebook;
import org.gnu.gtk.TargetEntry;
import org.gnu.gtk.TargetFlags;
import org.gnu.gtk.Widget;
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


public class TearOffNotebook extends Notebook {

	/**
	 * If the window containing this Nonebook was created as 
	 * a result of a drag and drop this will store a reference 
	 * to that window, null otherwise;
	 * */
	private Window window;
	
	/**
	 * A hash table of all the notebooks that were created as a
	 * result of a drop even from this table. uses gdk window as
	 * a hash key.
	 * */
	private static Hashtable children = new Hashtable();
	
	public TearOffNotebook(Handle handle){
		super(handle);
		this.window = null;
		children.put(getRootWindow(), this);
		setupDranAndDrop();
	}
	
	public TearOffNotebook(Window window){
		super();
		this.window = window;
		setupDranAndDrop();
	}
	
	public Window getNotebookWindow() {
		return window;
	}

	private void setupDranAndDrop(){
		final TargetEntry[] entries = new TargetEntry[1];
		entries[0] = new TargetEntry("tap", TargetFlags.NO_RESTRICTION, 0);
		
		this.setDragSource(ModifierType.BUTTON1_MASK, entries , DragAction.COPY);
		this.setDragDestination(DestDefaults.ALL, entries, DragAction.COPY);
		
		this.addListener(new DragOriginListener(){

			public void dragStarted(StartDragEvent event) {
//				System.out.println("ORIGIN: dragStarted");
			}

			public void dragEnded(EndDragEvent event) {
//				System.out.println();
//				System.out.println("ORIGIN: dropped");
//				System.out.println("ORIGIN: source " + event.getDragContext().getSourceWindow());
//				System.out.println("ORIGIN: dest   " + event.getDragContext().getDestinationWindow());
//				System.out.println("ORIGIN: dest*  " + event.getSource());
//				System.out.println("ORIGIN: this   " + getRootWindow());
//				System.out.println();
				
				if(event.getDragContext().getDestination() == null){
					Window window = new Window();
					TearOffNotebook newNotebook = new TearOffNotebook(window);

					final Widget widget = getPage(getCurrentPage());
//					final Label  label  = new Label(getTabLabelText(widget));
					final Label  label  = new Label("new");
					removePage(getCurrentPage());
					newNotebook.appendPage(widget, label);
			
					window.add(newNotebook);
					System.out.println("newNotebook window: " + window.getRootWindow());
					children.put(window.getRootWindow(), newNotebook);
					
					window.resize(200, 300);
					window.realize();
					window.showAll();
					
					window.setTitle(newNotebook.toString());
				}
			}

			public void dataRequested(RequestDragDataEvent arg0) {
			}

			public void dataDeleted(DeleteDragDataEvent arg0) {
			}
			
		});
		
		this.addListener(new DragTargetListener(){

			public void destinationLeft(LeaveDragDestinationEvent arg0) {
			}

			public boolean dropped(DropDragEvent event) {
//				System.out.println();
//				System.out.println("TARGET: dropped");
//				System.out.println("TARGET: source " + event.getDragContext().getSource());
//				System.out.println("TARGET: dest   " + event.getDragContext().getDestination());
//				System.out.println("TARGET: dest*  " + event.getSource());
//				System.out.println("TARGET: this   " + getRootWindow());
//				System.out.println();
//				
//				TearOffNotebook source = (TearOffNotebook) event.getSource();
//
//				final Widget widget = source.getPage(source.getCurrentPage());
//				//final Label  label  = new Label(source.getTabLabEclipse Java Compiler v_579_R31x, 3.1.1elText(widget));
//				final Label  label  = new Label("*****");
//				source.removePage(source.getCurrentPage());
//				appendPage(widget, label);
//
//				Widget topWidget = getToplevel();
//				if(getNumPages() == 0){
//					topWidget.destroy();
//				}
				
				return false;
			}
			
			public void dataReceived(ReceiveDragDataEvent arg0) {
			}

			public boolean motionOcurred(DragMotionEvent arg0) {
				return false;
			}
			
		});
	}
	
	
}
