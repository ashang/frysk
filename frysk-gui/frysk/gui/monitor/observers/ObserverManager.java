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

package frysk.gui.monitor.observers;

import java.util.Observable;
import java.util.Observer;

import frysk.gui.common.dialogs.WarnDialog;
import frysk.gui.monitor.ObservableLinkedList;
import frysk.gui.monitor.actions.Action;
import frysk.gui.monitor.actions.ForkedAction;
import frysk.gui.monitor.filters.TaskProcNameFilter;
import frysk.proc.Proc;
import frysk.proc.Task;

/**
 * Only once instance.
 * The purpose of the ObserverManager is to make transparent
 * to the client wether it is using a custom or built in observer.
 * Keeps a list of available observers (custom/built in)
 * Manages observer creation through using prototypes.
 * For known static observers the ObserverManager is responsible
 * for instantiating and adding their prototypes. 
 * */
public class ObserverManager extends Observable {

	public static ObserverManager theManager = new ObserverManager();
	
	/**
	 * a list containing a prototype of every available
	 * observer;
	 * */
	private ObservableLinkedList taskObservers;
	
	public ObserverManager(){
		this.taskObservers = new ObservableLinkedList();
		this.initTaskObservers();
	}
	
	/**
	 * Instantiates each one of the static task observers
	 * and adds it to the list.
	 * */
	private void initTaskObservers() {
		this.addTaskObserverPrototype(new TaskExecObserver());
		this.addTaskObserverPrototype(new TaskTerminatingObserver());
		this.addTaskObserverPrototype(new TaskForkedObserver());
		this.addTaskObserverPrototype(new TaskCloneObserver());
		this.addTaskObserverPrototype(new SyscallObserver());
		

		
		final TaskForkedObserver customObserver = new TaskForkedObserver();
		customObserver.setName("Custom 'ls' Watcher");
		customObserver.procFilterPoint.addFilter(new TaskProcNameFilter("xxxxxx"));
		final TaskExecObserver   lsObserver = new TaskExecObserver();
		lsObserver.taskFilterPoint.addFilter(new TaskProcNameFilter("ls"));
		customObserver.addAction(new Action("Dialog shower", ""){
			public void execute() {
				System.out.println("Exec Observer ");
				WarnDialog dialog = new WarnDialog("ls is about to happen");
				dialog.showAll();
				dialog.run();
			}
		});
		
		customObserver.addForkedAction(new ForkedAction(){
			public void execute(Task task, Proc child) {
//				System.out.println(".execute() " + child.getPid());
//				Iterator iter = child.getTasks().iterator();
//				while(iter.hasNext()){
//					Task myTask = (Task) iter.next();
//					myTask.requestAddForkedObserver(customObserver);
//					myTask.requestAddExecedObserver(lsObserver);
//				}
				
				child.observableTaskAdded.addObserver(new Observer(){
					public void update(Observable arg0, Object task) {
						Task myTask = (Task) task;
						myTask.requestAddForkedObserver(customObserver);					
					}
				});
			}
		});
		
		this.addTaskObserverPrototype(customObserver);
	}

	/**
	 * Returns a copy of the prototype given.
	 * A list of available prototypes can be 
	 * @param prototype a prototype of the observer to be
	 * instantiate.
	 * */
	public TaskObserverRoot getTaskObserver(TaskObserverRoot prototype){
		return prototype.getCopy();
	}
	
	public ObservableLinkedList getObservers(){
		return this.taskObservers;
	}

	/**
	 * add the given prototype to the list of available observers.
	 * @param observer the observer prototype to be added.
	 * */
	public void addTaskObserverPrototype(ObserverRoot observer){
		this.taskObservers.add(observer);
//      not needed linked lists are now observable
//		this.hasChanged();
//		this.notifyObservers();
	}
	
	/**
	 * Remove the given prototype to the list of available observers.
	 * @param observer the observer prototype to be removed.
	 * */
	public void removeTaskObserverPrototype(ObserverRoot observer){
		this.taskObservers.remove(observer);System.out.println("ObserverManager.removeTaskObserverPrototype()");
//      not needed linked lists are now observable
//		this.hasChanged();
//		this.notifyObservers();
	}
	
	
}
