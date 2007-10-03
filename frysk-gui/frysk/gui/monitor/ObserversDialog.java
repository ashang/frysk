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

import java.util.Iterator;
import java.util.LinkedList;

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
	
	Button okButton;
	Button applyButton;
	Button cancelButton;
	
	/**
	 * the old and new scratch lists keep
	 * a list of what changes the user made so that
	 * these can be committed to ObserverManager when
	 * the ok is clicked
	 * this is what is store in the lists
	 *                                  old    |  new
	 *                                -------------------
	 * user deletes an observer:   oldObserver | null  
	 * user edits an observer  :   oldObserver | newObserver
	 * user adds an observer   :   null        | newObserver
	 {*/
	LinkedList scratchOld;
	LinkedList scratchNew;
	/**
	 * }
	 */
	
	private ObservableLinkedList scratchList;
	
	ObserversDialog(LibGlade glade){
		super(glade.getWidget("observersDialog").getHandle());
	
		scratchOld = new LinkedList();
		scratchNew = new LinkedList();
		
	//	this.scratchList = new ObservableLinkedList();
		this.scratchList = ObserverManager.theManager.getTaskObservers();
		//this.retrieveList();
		
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
						newObserver.doSaveObject();
						scratchList.add(newObserver);
						observersListView.setSelectedObject(newObserver);

						scratchOld.add(null);
						scratchNew.add(newObserver);
						
					}

				}
			}
		});
		
		
		this.editObserverButton = (Button) glade.getWidget("editCustomObserverButton");
		this.editObserverButton.addListener(new ButtonListener() {
			public void buttonEvent(ButtonEvent event) {
				if (event.isOfType(ButtonEvent.Type.CLICK)) {
					ObserverRoot selected = (ObserverRoot)observersListView.getSelectedObject();
					ObserverRoot scratchCopy = selected.getCopy();
					WindowManager.theManager.editObserverDialog.editObserver(scratchCopy);
					int response = showEditObserverDialog();
					if(response == ResponseType.OK.getValue()){
						ObserverRoot newObserver = WindowManager.theManager.editObserverDialog.getObserver();
						newObserver.doSaveObject();
						scratchList.swap(selected,newObserver);
						observersListView.setSelectedObject(newObserver);
						System.out.println(this + ": .buttonEvent() swapped");

						scratchOld.add(selected);
						scratchNew.add(newObserver);
					}
				}
			}
		});
		
		
		this.deleteObserverButton = (Button) glade.getWidget("removeObserverButton");
		this.deleteObserverButton.addListener(new ButtonListener() {
			public void buttonEvent(ButtonEvent event) {
				if (event.isOfType(ButtonEvent.Type.CLICK)) {
					ObserverRoot selected = (ObserverRoot) observersListView.getSelectedObject();
					if(selected != null){
						int index = scratchList.indexOf(selected);
						scratchList.remove(selected);
						if(scratchList.size() == index){
							observersListView.setSelectedObject((GuiObject) scratchList.get(index-1));
						}
						
						scratchOld.add(selected);
						scratchNew.add(null);	
					}
				}
			}
		});
	
		
		duplicateObserverButton = (Button) glade.getWidget("observerDuplicateButton");
		duplicateObserverButton.addListener(new ButtonListener() {
			public void buttonEvent(ButtonEvent event) {
				if (event.isOfType(ButtonEvent.Type.CLICK)) {
					ObserverRoot selected = (ObserverRoot)observersListView.getSelectedObject();
					ObserverRoot newObserver = ObserverManager.theManager.getObserverCopy(selected);
					newObserver.setName("CopyOf_" + selected.getName());
					scratchList.add(scratchList.indexOf(selected)+1, newObserver);
					scratchList.add(newObserver);
					
					scratchOld.add(null);
					scratchNew.add(newObserver);
				}
			}
		});
		
		
		this.okButton = (Button) glade.getWidget("observersOkButton");
		okButton.addListener(new ButtonListener() {
			public void buttonEvent(ButtonEvent event) {
				if (event.isOfType(ButtonEvent.Type.CLICK)) {
					ObserversDialog.this.hideAll();
					commitChanges();
				}
			}
		});
		
		
		this.cancelButton = (Button) glade.getWidget("observersCancelButton");
		cancelButton.addListener(new ButtonListener() {
			public void buttonEvent(ButtonEvent event) {
				if (event.isOfType(ButtonEvent.Type.CLICK)) {
					ObserversDialog.this.hideAll();
					undoChanges();
				}
			}
		});
		
		
		this.applyButton = (Button) glade.getWidget("observersApplyButton");
		applyButton.addListener(new ButtonListener() {
			public void buttonEvent(ButtonEvent event) {
				if (event.isOfType(ButtonEvent.Type.CLICK)) {
					commitChanges();
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
	
	
//	private void retrieveList(){
//		scratchList.clear();
//		scratchList.copyFromList(ObserverManager.theManager.getTaskObservers());
//	}
	
	private void commitChanges(){
//		ObserverManager.theManager.getTaskObservers().clear();
//		Iterator iterator = scratchList.iterator();
//		while (iterator.hasNext()) {
//			ObserverRoot observer = (ObserverRoot) iterator.next();
//			ObserverManager.theManager.addTaskObserverPrototype(observer);
//		}
		

		scratchOld.clear();
		scratchNew.clear();
		
		ObserverManager.theManager.save();
	}
	
	private void undoChanges(){
		System.out.println(this + ": ObserversDialog.undoChanges()");
//		scratchList.clear();
//		scratchList.copyFromList(ObserverManager.theManager.getTaskObservers());
//		
//		this.scratchNew.clear();
//		this.scratchOld.clear();
//		ObserverManager.theManager.getTaskObservers().clear();
//		Iterator iterator = scratchList.iterator();
//		while (iterator.hasNext()) {
//			ObserverRoot observer = (ObserverRoot) iterator.next();
//			ObserverManager.theManager.addTaskObserverPrototype(observer);
//		}
		
		Iterator a = scratchOld.iterator();
		Iterator b = scratchNew.iterator();
		
		ObserverRoot oldObserver;
		ObserverRoot newObserver;
		
		while(a.hasNext()){
			oldObserver = (ObserverRoot) a.next();
			newObserver = (ObserverRoot) b.next();

			if(oldObserver != null && newObserver == null){
				//Observer was removed
//				ObserverManager.theManager.removeTaskObserverPrototype(oldObserver);
				ObserverManager.theManager.addTaskObserverPrototype(oldObserver);
				continue;
			}
			
			if(oldObserver == null && newObserver != null){
				//Observer was added
//				ObserverManager.theManager.addTaskObserverPrototype(newObserver);
				ObserverManager.theManager.removeTaskObserverPrototype(newObserver);
				continue;
			}
			
			if(oldObserver != null && newObserver != null){
				//Observer was edited
//				ObserverManager.theManager.swapTaskObserverPrototype(oldObserver, newObserver);
				ObserverManager.theManager.swapTaskObserverPrototype(newObserver, oldObserver);
				continue;
			}
			
			throw new RuntimeException("invalid combination\noldObserver = " + oldObserver + "\nnewObserver = " + newObserver);
		}
				
		scratchOld.clear();
		scratchNew.clear();

	}

//	public int run(){
//		this.retrieveList();
//		return super.run();
//	}

}
