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
 * Created on Oct 13, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package frysk.gui.monitor;

import java.util.Iterator;
import java.util.Observable;
import java.util.Observer;

import org.gnu.gdk.KeyValue;
import org.gnu.glade.LibGlade;
import org.gnu.gtk.Button;
import org.gnu.gtk.Entry;
import org.gnu.gtk.Label;
import org.gnu.gtk.TreeView;
import org.gnu.gtk.VBox;
import org.gnu.gtk.Window;
import org.gnu.gtk.event.ButtonEvent;
import org.gnu.gtk.event.ButtonListener;
import org.gnu.gtk.event.EntryEvent;
import org.gnu.gtk.event.EntryListener;
import org.gnu.gtk.event.FocusEvent;
import org.gnu.gtk.event.FocusListener;
import org.gnu.gtk.event.KeyEvent;
import org.gnu.gtk.event.KeyListener;
import org.gnu.gtk.event.TreeSelectionEvent;
import org.gnu.gtk.event.TreeSelectionListener;

import frysk.gui.monitor.observers.ObserverManager;
import frysk.gui.monitor.observers.ObserverRoot;
import frysk.gui.monitor.observers.TaskObserverRoot;

public class CustomeObserverWindow extends Window implements Observer {
	
	private DetailedObserverTreeView observerTreeView;
	
	private Entry customObserverNameEntry;

	private ListView baseObserverTreeView;

	private FilterWidget filterWidget;
	private ActionsWidget actionsWidget;
	
	private Label nameSummaryLabel;
	private Label baseObserverSummaryLabel;
	private Label filtersSummaryLabel;
	private Label actionsSummaryLabel;
	
	ObserverRoot selectedObserver;
	
	//private Vector observerBackup;
	
	private ObservableLinkedList scratchList;
	
	public CustomeObserverWindow(LibGlade glade){
		super(((Window)glade.getWidget("customeObserverWindow")).getHandle()); //$NON-NLS-1$
		
		this.scratchList = new ObservableLinkedList(ObserverManager.theManager.getTaskObservers());
		
		//=========================================
		Button button = (Button) glade.getWidget("customObserverOkButton"); //$NON-NLS-1$
		button.addListener(new ButtonListener(){
			public void buttonEvent(ButtonEvent event) {
				if(event.getType() == ButtonEvent.Type.CLICK){
					WindowManager.theManager.customeObserverWindow.hideAll();
				}
			}
		});
		//=========================================
		
		//=========================================
		button = (Button) glade.getWidget("customObserverCancelButton"); //$NON-NLS-1$
		button.addListener(new ButtonListener(){
			public void buttonEvent(ButtonEvent event) {
				if(event.getType() == ButtonEvent.Type.CLICK){
					WindowManager.theManager.customeObserverWindow.hideAll();
				}
			}
		});
		//=========================================
		
		//=========================================
		this.customObserverNameEntry = (Entry) glade.getWidget("customObserverNameEntry"); //$NON-NLS-1$
		this.customObserverNameEntry.addListener(new EntryListener() {
			public void entryEvent(EntryEvent event) {
				if(event.isOfType(EntryEvent.Type.CHANGED)){
					setObserverName(customObserverNameEntry.getText());
				}
			}
		});
		
		this.customObserverNameEntry.addListener(new KeyListener() {
		
			public boolean keyEvent(KeyEvent event) {
				if(event.getKeyval() == KeyValue.Enter3270 || event.getKeyval() == 65293 || event.getKeyval() == 65421){
					
				}
				return false;
			}
		});
		
		this.customObserverNameEntry.addListener(new FocusListener() {
			public boolean focusEvent(FocusEvent event) {
				if(event.isOfType(FocusEvent.Type.FOCUS_OUT)){
					setObserverName(customObserverNameEntry.getText());
				}
				return false;
			}
		});
		//=========================================
		
		//=========================================
		this.baseObserverTreeView = new ListView(((TreeView)glade.getWidget("baseObserversTreeView")).getHandle()); //$NON-NLS-1$
		this.baseObserverTreeView.watchLinkedList(scratchList);
//		ObserverManager.theManager.addObserver(new Observer(){
//			public void update(Observable observable, Object obj) {
//				populateObserverTreeView();
//			}
//		});

	
		baseObserverTreeView.getSelection().addListener(new TreeSelectionListener() {
			public void selectionChangedEvent(TreeSelectionEvent event) {
				updateBaseObserverSummary(baseObserverTreeView.getSelectedObject().getName());
				remove();
				add();
			}
		});
//		this.populateObserverTreeView();
		//=========================================
		
		//=========================================
		this.observerTreeView = new DetailedObserverTreeView(((TreeView)glade.getWidget("simplexListView")).getHandle(), this.scratchList); //$NON-NLS-1$
		this.observerTreeView.getSelection().addListener(new TreeSelectionListener() {
			public void selectionChangedEvent(TreeSelectionEvent arg0) {
				setSelectedObserver(observerTreeView.getSelectedObserver());
			}
		});
		//=========================================
		
		//=========================================
//		this.sourceActionsTreeView = new ListView(((TreeView)glade.getWidget("sourceActionsTreeView")).getHandle());
//		this.populateSourceActionsTreeView();
//		
//		ActionManager.theManager.addObserver(new Observer(){
//			public void update(Observable observable, Object obj) {
//				populateSourceActionsTreeView();
//			}
//		});
		//=========================================
		
		//=========================================
		this.filterWidget = new FilterWidget(((VBox)glade.getWidget("filtersWidget")).getHandle()); //$NON-NLS-1$
		//=========================================

		//=========================================
		this.actionsWidget = new ActionsWidget(glade);
		//=========================================

		//=========================================
		button = (Button)glade.getWidget("deleteObserverButton"); //$NON-NLS-1$
		button.addListener(new ButtonListener() {
			public void buttonEvent(ButtonEvent event) {
				if(event.isOfType(ButtonEvent.Type.CLICK)){
					remove();
				}
			}
		});
		//=========================================
		
		//=========================================
		button = (Button)glade.getWidget("newObserverButton"); //$NON-NLS-1$
		button.addListener(new ButtonListener() {
			public void buttonEvent(ButtonEvent event) {
				if(event.isOfType(ButtonEvent.Type.CLICK)){
					createNewObserver();
				}
			}
		});
		//=========================================
		
		this.nameSummaryLabel         = (Label) glade.getWidget("nameSummaryLabel"); //$NON-NLS-1$
		
		this.baseObserverSummaryLabel = (Label) glade.getWidget("baseObserverSummaryLabel"); //$NON-NLS-1$
	
		this.filtersSummaryLabel      = (Label) glade.getWidget("filtersSummaryLabel"); //$NON-NLS-1$
		this.updateFiltersSummary(""); //$NON-NLS-1$
		
		this.actionsSummaryLabel      = (Label) glade.getWidget("actionsSummaryLabel"); //$NON-NLS-1$
		this.updateActionsSummary(""); //$NON-NLS-1$
	
		this.setSelectedObserver(this.observerTreeView.getSelectedObserver());

		button = (Button) glade.getWidget("customObserverCancelButton");
		button.addListener(new ButtonListener() {
			public void buttonEvent(ButtonEvent event) {
				if(event.isOfType(ButtonEvent.Type.CLICK)){
					// Cancel... dump all editing refresh list.
					CustomeObserverWindow.this.scratchList.clear();
					CustomeObserverWindow.this.scratchList.copyFromList(ObserverManager.theManager.getTaskObservers());
				}
			}
		});
		
		button = (Button) glade.getWidget("customObserverOkButton");
		button.addListener(new ButtonListener() {
			public void buttonEvent(ButtonEvent event) {
				if(event.isOfType(ButtonEvent.Type.CLICK)){
					ObserverManager.theManager.getTaskObservers().clear();
					Iterator iterator = CustomeObserverWindow.this.scratchList.iterator();
					while (iterator.hasNext()) {
						ObserverRoot observer = (ObserverRoot) iterator.next();
						ObserverManager.theManager.addTaskObserverPrototype(observer);
					}
//					CustomeObserverWindow.this.scratchList.clear();
//					CustomeObserverWindow.this.scratchList.copyFromList(ObserverManager.theManager.getTaskObservers());
				}
			}
		});
	
		
	}
	
//	private void populateObserverTreeView() {
//		Iterator iter = ObserverManager.theManager.getTaskObservers().iterator();
//		while(iter.hasNext()){
//			this.baseObserverTreeView.add((ObserverRoot) iter.next());
//		}
//	}
	
	private void setObserverName(String name){
		this.selectedObserver.setName(name);
	}
	
	private void updateNameSummary(String summary){
		this.nameSummaryLabel.setText("("+summary+")"); //$NON-NLS-1$ //$NON-NLS-2$
		this.customObserverNameEntry.setText(summary);
	}

	private void updateBaseObserverSummary(String summary){
		this.baseObserverSummaryLabel.setText("("+summary+")"); //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	private void updateFiltersSummary(String summary){
		this.filtersSummaryLabel.setText(summary);
	}
	
	private void updateActionsSummary(String summary){
		this.actionsSummaryLabel.setText(summary);		
	}
	
	public void setSelectedObserver(ObserverRoot selectedObserver){
		if(selectedObserver == null) return;
		if(this.selectedObserver != null){
			this.selectedObserver.deleteObserver(this);
		}
		
		this.selectedObserver = selectedObserver;
		
		this.selectedObserver.addObserver(this);
		
		this.updateBaseObserverSummary(this.selectedObserver.getBaseName());
		
		this.filterWidget.setObserver(selectedObserver);

		this.actionsWidget.setObserver(selectedObserver);

		this.update(null, null);
	}

	public void update(Observable observable, Object obj) {
		this.updateNameSummary(this.selectedObserver.getName());
	}
	
	/**
	 * Create new observer as a template for a custom observer.
	 * */
	public void createNewObserver(){
		ObserverRoot newObserver = new ObserverRoot("New Observer",""); //$NON-NLS-1$ //$NON-NLS-2$
		this.scratchList.add(newObserver);
		//ObserverManager.theManager.addTaskObserverPrototype(newObserver);
		this.observerTreeView.setSelected(newObserver); //XXX
	}

	/**
	 * Remove the observer represented by current settings (selected
	 * observer).
	 * */
	public void remove(){
		//ObserverManager.theManager.removeTaskObserverPrototype(this.selectedObserver);
		this.scratchList.remove(this.selectedObserver);
	}

	/**
	 * Add the observer represented by given settings (Name, base observer,
	 * filters and Actions)
	 * */
	public void add(){
		ObserverRoot newObserver = ObserverManager.theManager.getTaskObserverCopy((TaskObserverRoot)this.baseObserverTreeView.getSelectedObject());
		newObserver.setName(this.customObserverNameEntry.getText());
		//ObserverManager.theManager.addTaskObserverPrototype(newObserver);
		this.scratchList.add(newObserver);
		this.observerTreeView.setSelected(newObserver);//XXX
	}
	
}
