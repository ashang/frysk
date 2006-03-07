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

package frysk.gui.monitor;

import org.gnu.glade.LibGlade;
import org.gnu.gtk.Button;
import org.gnu.gtk.ResponseType;
import org.gnu.gtk.event.ButtonEvent;
import org.gnu.gtk.event.ButtonListener;
import org.gnu.gtk.event.TreeSelectionEvent;
import org.gnu.gtk.event.TreeSelectionListener;

import frysk.gui.common.dialogs.Dialog;
import frysk.gui.monitor.observers.ObserverManager;
import frysk.gui.monitor.observers.ObserverRoot;

public class ObserversDialog extends Dialog {
	
	ListView observersListView;
	
	Button newObserverButton;
	Button editObserverButton;
	Button deleteObserverButton;
	Button duplicateObserverButton;
	
	private ObservableLinkedList scratchList;
	
	ObserversDialog(LibGlade glade){
		super(glade.getWidget("observersDialog").getHandle());
	
		this.scratchList = new ObservableLinkedList();
		
		this.observersListView = new ListView(glade.getWidget("observersTreeView").getHandle());
		this.observersListView.watchLinkedList(scratchList);
		this.observersListView.getSelection().addListener(new TreeSelectionListener() {
			public void selectionChangedEvent(TreeSelectionEvent event) {
				updateEnabled();
			}
		});
		
		
		this.newObserverButton = (Button) glade.getWidget("createObserverButton");
		this.newObserverButton.addListener(new ButtonListener() {
			public void buttonEvent(ButtonEvent event) {
				if (event.isOfType(ButtonEvent.Type.CLICK)) {
					WindowManager.theManager.editObserverDialog.editNewObserver();
					int response = showEditObserverDialog();
					if(response == ResponseType.OK.getValue()){
						ObserverRoot newObserver = WindowManager.theManager.editObserverDialog.getObserver();
						scratchList.add(newObserver);
						observersListView.setSelectedObject(newObserver);
					}

				}
			}
		});
		
		this.editObserverButton = (Button) glade.getWidget("editCustomObserverButton");
		this.editObserverButton.addListener(new ButtonListener() {
			public void buttonEvent(ButtonEvent event) {
				if (event.isOfType(ButtonEvent.Type.CLICK)) {
					ObserverRoot selected = (ObserverRoot)observersListView.getSelectedObject();
					WindowManager.theManager.editObserverDialog.editObserver(selected);
					showEditObserverDialog();
				
					ObserverRoot newObserver = WindowManager.theManager.editObserverDialog.getObserver();
					scratchList.swap(selected,newObserver);
					observersListView.setSelectedObject(newObserver);
				}
			}
		});
		
		this.deleteObserverButton = (Button) glade.getWidget("removeObserverButton");
		this.deleteObserverButton.addListener(new ButtonListener() {
			public void buttonEvent(ButtonEvent event) {
				if (event.isOfType(ButtonEvent.Type.CLICK)) {
					ObserverRoot selected = (ObserverRoot) observersListView.getSelectedObject();
					if(selected != null){
						scratchList.remove(selected);
						if(scratchList.size() > 0){
							observersListView.setSelectedObject((GuiObject) scratchList.getLast());
						}
					}
				}
			}
		});
		
		duplicateObserverButton = (Button) glade.getWidget("observerDuplicateButton");
		duplicateObserverButton.addListener(new ButtonListener() {
			public void buttonEvent(ButtonEvent event) {
				if (event.isOfType(ButtonEvent.Type.CLICK)) {
					ObserverRoot selected = (ObserverRoot)observersListView.getSelectedObject();
					ObserverRoot copy = ObserverManager.theManager.getObserverCopy(selected);
					copy.setName("CopyOf" + selected.getName());
					scratchList.add(scratchList.indexOf(selected), event);
				}
			}
		});

		this.updateEnabled();
	}
	
	protected void updateEnabled() {
		boolean enable = (observersListView.getSelectedObject() != null); 
		editObserverButton.setSensitive(enable);
		duplicateObserverButton.setSensitive(enable);
	}

	private int showEditObserverDialog(){
		WindowManager.theManager.editObserverDialog.showAll();
		return WindowManager.theManager.editObserverDialog.run();
	}
	
}
