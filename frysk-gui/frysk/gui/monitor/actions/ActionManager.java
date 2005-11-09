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

import java.util.LinkedList;
import java.util.Observable;

/**
 * Only once instance.
 * Keeps a list of available actions.
 * Provides an interface for instanciating those actions.
 * */
public class ActionManager extends Observable {
	
	public static ActionManager theManager = new ActionManager();
	
	private LinkedList procActions;
	private LinkedList taskActions;
	
	public ActionManager(){
		this.procActions = new LinkedList();
		this.taskActions = new LinkedList();
		this.initActionList();
	}
	
	/**
	 * Instantiates each one of the static task observers
	 * and adds it to the list.
	 * */
	private void initActionList() {
//		this.addProcActionPrototype(new Attach());
//		this.addProcActionPrototype(new Detach());
		this.addProcActionPrototype(new Stop());
		this.addProcActionPrototype(new Resume());
		this.addProcActionPrototype(new PrintProc());
		
		this.addTaskActionPrototype(new ShowSourceWin());
		this.addTaskActionPrototype(new PrintTask());
	}

	/**
	 * Returns a copy of the prototype given.
	 * A list of available prototypes can be 
	 * @param prototype a prototype of the observer to be
	 * instantiated.
	 * */
	public Action getObserver(Action prototype){
		//XXX: Not implemented.
		throw new RuntimeException("Not implemented");
		//return prototype.getCopy();
	}
	
	public LinkedList getProcActions(){
		return this.procActions;
	}

	public LinkedList getTaskActions(){
		return this.taskActions;
	}

	/**
	 * add an observer to the list of available observers.
	 * */
	public void addProcActionPrototype(Action observer){
		this.procActions.add(observer);
		this.hasChanged();
		this.notifyObservers();
	}
	
	public void addTaskActionPrototype(TaskAction observer){
		this.taskActions.add(observer);
		this.hasChanged();
		this.notifyObservers();
	}
	
}
