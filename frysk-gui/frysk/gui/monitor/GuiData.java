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

import java.util.LinkedList;
import java.util.Observable;

import org.gnu.gtk.Widget;

import frysk.gui.common.dialogs.DialogManager;
import frysk.gui.monitor.observers.ObserverRoot;
import frysk.gui.monitor.observers.ObserverRunnable;
import frysk.gui.monitor.observers.TaskExecObserver;
import frysk.gui.monitor.observers.TaskTerminatingObserver;

/**
 * Used to store a pointer to objects in the backend, and extra data that is
 * GUI specific.
 */
public class GuiData {
	
	Widget widget;
	LinkedList observers;
	
	public GuiObservable observerAdded;
	public GuiObservable observerRemoved;
	
	public GuiData(){
		this.observerAdded = new GuiObservable();
		this.observerRemoved = new GuiObservable();
		this.observers = new LinkedList();
	}
	
	public void add(ObserverRoot observer){
		this.observers.add(observer);
		this.observerAdded.notifyObservers(observer);
	}
	
	public void add(final TaskExecObserver observer){
		this.add((ObserverRoot)observer);
		observer.addRunnable(new ObserverRunnable(){
			public void run(Observable o, Object obj) {
				DialogManager.showWarnDialog("Received TaskExec Event !");
			}
		});
	}
	
	public void add(TaskTerminatingObserver observer) {
		this.add((ObserverRoot)observer);
		observer.addRunnable(new ObserverRunnable(){
			public void run(Observable o, Object obj) {
				DialogManager.showWarnDialog("Received TaskExiting Event !");				
			}
		});
	}
	
	public void remove(ObserverRoot observer){
		this.observers.remove(observer);
		this.observerRemoved.notifyObservers(observer);	
	}
	
	public void remove(TaskExecObserver observer){
		remove((ObserverRoot)observer);
		ActionPool.theActionPool.addExecObserver.removeObservers(this);
	}
	
	public void remove(TaskTerminatingObserver observer){
		remove((ObserverRoot)observer);
		ActionPool.theActionPool.addExitingObserver.removeObservers(this);
	}
	
	public LinkedList getObservers(){
		return this.observers;
	}
	
	public void setWidget(Widget widget){
		if(this.widget != null){
			throw new RuntimeException("Trying to set widget when widget is already set.");
		}
		this.widget = widget;
	}
	
	public Widget getWidget(){
		return this.widget;
	}
	
	public boolean hasWidget(){
		return (this.widget != null);
	}
	
	
}
