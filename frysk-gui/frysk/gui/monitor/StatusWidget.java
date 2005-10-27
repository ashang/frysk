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
 * Created on Sep 12, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package frysk.gui.monitor;

import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Observable;
import java.util.Observer;

import org.gnu.gtk.CellRendererText;
import org.gnu.gtk.DataColumn;
import org.gnu.gtk.DataColumnObject;
import org.gnu.gtk.DataColumnString;
import org.gnu.gtk.Frame;
import org.gnu.gtk.HBox;
import org.gnu.gtk.Label;
import org.gnu.gtk.ListStore;
import org.gnu.gtk.Menu;
import org.gnu.gtk.MenuItem;
import org.gnu.gtk.PolicyType;
import org.gnu.gtk.ScrolledWindow;
import org.gnu.gtk.ShadowType;
import org.gnu.gtk.TextView;
import org.gnu.gtk.TreeIter;
import org.gnu.gtk.TreePath;
import org.gnu.gtk.TreeView;
import org.gnu.gtk.TreeViewColumn;
import org.gnu.gtk.VBox;
import org.gnu.gtk.VSeparator;
import org.gnu.gtk.event.MenuItemEvent;
import org.gnu.gtk.event.MenuItemListener;
import org.gnu.gtk.event.MouseEvent;
import org.gnu.gtk.event.MouseListener;

import frysk.gui.monitor.observers.ObserverRoot;
import frysk.gui.monitor.observers.ObserverRunnable;

public class StatusWidget extends VBox{

	Label nameLabel;
	private GuiData data;
	private TextView logTextView;
	private Frame frame;
	
	public  Observable notifyUser;
	
	public StatusWidget(GuiData data){
		super(false,0);
		//FontDescription font = new FontDescription();
		this.notifyUser = new Observable();
		this.data = data;
		
		HBox mainVbox = new HBox(false, 0);
		
		//========================================
		frame = new Frame("");
		frame.add(mainVbox);
		this.add(frame);
		//========================================
		
		//========================================
		initLogTextView();
		ScrolledWindow logScrolledWindow = new ScrolledWindow();
		logScrolledWindow.addWithViewport(logTextView);
		logScrolledWindow.setShadowType(ShadowType.IN);
		logScrolledWindow.setPolicy(PolicyType.AUTOMATIC, PolicyType.AUTOMATIC);
		mainVbox.packStart(logScrolledWindow, true, true, 0);
		//========================================
		
		//========================================
		VSeparator seperator = new VSeparator();
		mainVbox.packStart(seperator, false, true, 5);
		//========================================
		
		//========================================
		HBox hbox = new HBox(false, 0);
		VBox vbox = new VBox(false, 0);
		hbox.packStart(new Label("Attached Observers: "), false, false, 0);
		hbox.packStart(new Label(""), true, false, 0);
		vbox.packStart(hbox, false, false, 0);
		ScrolledWindow scrolledWindow = new ScrolledWindow();
		scrolledWindow.addWithViewport(initAttacheObserversTreeView());
		scrolledWindow.setShadowType(ShadowType.IN);
		scrolledWindow.setPolicy(PolicyType.AUTOMATIC, PolicyType.AUTOMATIC);
		vbox.packStart(scrolledWindow, true, true, 0);
		mainVbox.packStart(vbox, false, true, 0);
		//========================================

		this.showAll();
	}
	
	private TreeView initAttacheObserversTreeView(){
		final TreeView treeView = new TreeView();
		final DataColumnString nameDC = new DataColumnString();
		final DataColumnObject observersDC = new DataColumnObject();
		DataColumn[] columns = new DataColumn[2];
		columns[0] = nameDC;
		columns[1] = observersDC;
		final ListStore listStore = new ListStore(columns);
		treeView.setHeadersVisible(false);
		
		// handle add evets
		this.data.observerAdded.addObserver(new Observer(){
			public void update(Observable observable, Object obj) {
				ObserverRoot observer = (ObserverRoot) obj;
				TreeIter iter = listStore.appendRow();
				listStore.setValue(iter, nameDC, observer.getName());
				listStore.setValue(iter, observersDC, observer);
			}
		});
		
		// handle remove evets
		this.data.observerRemoved.addObserver(new Observer(){
			public void update(Observable o, Object obj) {
				TreeIter iter = listStore.getFirstIter();
				ObserverRoot observer = (ObserverRoot)obj;
				ObserverRoot myObserver;
				while(iter != null){
					myObserver = (ObserverRoot) listStore.getValue(iter, observersDC);
					if(myObserver == observer){
						listStore.removeRow(iter);
						break;
					}
					iter = iter.getNextIter();
				}
			}
		});
		
		treeView.setModel(listStore);
		CellRendererText cellRendererText = new CellRendererText();
		TreeViewColumn observersCol = new TreeViewColumn();
		observersCol.packStart(cellRendererText, false);
		observersCol.addAttributeMapping(cellRendererText, CellRendererText.Attribute.TEXT , nameDC);
		treeView.appendColumn(observersCol);
		
		final Menu menu = new Menu();
		MenuItem item = new MenuItem("Remove", false);
		item.addListener(new MenuItemListener() {
			public void menuItemEvent(MenuItemEvent event) {
				TreePath path = (treeView.getSelection().getSelectedRows())[0];
				data.remove((ObserverRoot) listStore.getValue(listStore.getIter(path), observersDC));
			}
		});
		menu.add(item);
		menu.showAll();
		
		treeView.addListener(new MouseListener(){

			public boolean mouseEvent(MouseEvent event) {
				if(event.getType() == MouseEvent.Type.BUTTON_PRESS 
						& event.getButtonPressed() == MouseEvent.BUTTON3){
					if((treeView.getSelection().getSelectedRows()).length > 0){
						menu.popup();						
					}
                    return true;
				}
				return false;
			}
		});
		
		
		return treeView;
	}


	private void initLogTextView(){
		this.logTextView = new TextView();
		LinkedList observers = this.data.getObservers();
		ListIterator iter = observers.listIterator();
		while(iter.hasNext()){
			final ObserverRoot observer = (ObserverRoot) iter.next();
			observer.addRunnable(new ObserverRunnable(){
				public void run(Observable o, Object obj) {
					logTextView.getBuffer().insertText("Event: " + observer.getName() + "\n");					
				}
			});
		}
		
		this.data.observerAdded.addObserver(new Observer(){

			public void update(Observable arg0, Object obj) {
				final ObserverRoot observer = (ObserverRoot)obj;
				logTextView.getBuffer().insertText("Event: " + observer.getName() + " added\n");
				observer.addRunnable(new ObserverRunnable(){
					public void run(Observable o, Object obj) {
						logTextView.getBuffer().insertText("Event: " + observer.getName() + "\n");
					}
				});
			}
			
		});
		
		this.data.observerRemoved.addObserver(new Observer(){

			public void update(Observable arg0, Object obj) {
				ObserverRoot observer = (ObserverRoot)obj;
				logTextView.getBuffer().insertText("Event: " + observer.getName() + " removed\n");
			}
			
		});
		
	}
	
	public void setName(String name){
		this.frame.setLabel(name);
	}
}
