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

package frysk.gui.monitor.actions;

import java.util.Observable;

import frysk.gui.monitor.ObservableLinkedList;

/**
 * Only once instance.
 * Keeps a list of available actions.
 * Provides an interface for instantiating those actions.
 * */
public class ActionManager extends Observable {
	
	public static ActionManager theManager = new ActionManager();
	
	private ObservableLinkedList procActions;
	private ObservableLinkedList taskActions;
	private ObservableLinkedList genericActions;
	
	public ActionManager(){
		this.procActions = new ObservableLinkedList();
		this.taskActions = new ObservableLinkedList();
		this.genericActions = new ObservableLinkedList();
		this.initActionList();
	}
	
	/**
	 * Instantiates each one of the static task observers
	 * and adds it to the list.
	 * */
	private void initActionList() {
		this.addGenericActionPrototype(new Stop());
		this.addGenericActionPrototype(new Resume());
		this.addGenericActionPrototype(new LogAction());
		
		this.addTaskActionPrototype(new ShowSourceWin());
		this.addTaskActionPrototype(new PrintTask());
		this.addTaskActionPrototype(new AddTaskObserverAction());

		this.addProcActionPrototype(new PrintProc());
	}

	/**
	 * Returns a copy of the prototype given.
	 * A list of available prototypes can be 
	 * @param prototype a prototype of the observer to be
	 * instantiated.
	 * */
	public Action getObserver(Action prototype){
		//XXX: Not implemented.
		throw new RuntimeException("Not implemented"); //$NON-NLS-1$
		//return prototype.getCopy();
	}
	
	public ObservableLinkedList getProcActions(){
		return this.procActions;
	}

	public ObservableLinkedList getTaskActions(){
		return this.taskActions;
	}

	public ObservableLinkedList getGenericActions(){
		return this.genericActions;
	}

	/**
	 * Add a ProcAction to the list of available ProcAction
	 * prototypes.
	 * @param prototype the action to be added.
	 * */
	public void addProcActionPrototype(ProcAction prototype){
		this.procActions.add(prototype);
	}
	
	/**
	 * Add a TaskAction to the list of available TaskAction
	 * prototypes.
	 * @param prototype the action to be added.
	 * */
	public void addTaskActionPrototype(TaskAction prototype){
		this.taskActions.add(prototype);
	}

	/**
	 * Add a generic Action to the list of available Action
	 * prototypes.
	 * @param prototype the action to be added.
	 * */
	public void addGenericActionPrototype(Action prototype) {
		this.genericActions.add(prototype);
	}
	
}
