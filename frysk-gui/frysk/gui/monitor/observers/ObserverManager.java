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

import java.io.File;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jdom.Element;

import frysk.Config;
import frysk.gui.Gui;
import frysk.gui.monitor.ObjectFactory;
import frysk.gui.monitor.ObservableLinkedList;
import frysk.gui.monitor.UniqueHashMap;
import frysk.gui.monitor.WindowManager;
import frysk.gui.monitor.actions.CaptureStackFrameAction;

/**
 * Only once instance.
 * The purpose of the ObserverManager is to make transparent
 * to the client wether it is using a custom or built in observer.
 * Keeps a list of available observers (custom/built in)
 * Manages observer creation through using prototypes.
 * For known static observers the ObserverManager is responsible
 * for instantiating and adding their prototypes. 
 * */
public class ObserverManager {

	public static ObserverManager theManager = new ObserverManager();
	
	static{
		theManager.init();
	}
	
	/**
	 * A table that hashes observer names to
	 * their prototypes. Also used to make sure
	 * observer names are unique.
	 */
	private UniqueHashMap nameHash;
	
	private final String OBSERVERS_DIR = Config.FRYSK_DIR + "Observers" + "/";
	private Logger errorLog = Logger.getLogger (Gui.ERROR_LOG_ID);
	/**
	 * a list containing a prototype of every available
	 * observer.
	 * */
	private ObservableLinkedList taskObservers;
	
	private ObservableLinkedList baseObservers;
	
    public ProgramObserver programObserver;
    
	public ObserverManager(){
		this.baseObservers = new ObservableLinkedList();
		this.taskObservers = new ObservableLinkedList();
	
        this.programObserver = new ProgramObserver();
        
		this.nameHash = new UniqueHashMap();
		
		ObjectFactory.theFactory.makeDir(OBSERVERS_DIR);
	}
	
	public void init(){
		this.initTaskObservers();
		this.loadObservers();
	}
	
	/**
	 * Instantiates each one of the static task observers
	 * and adds it to the list.
	 * */
	private void initTaskObservers() {
		
		//============================================
		ObserverRoot observer = new TaskSignaledObserver();
		observer.dontSaveObject();
		this.tryAddTaskObserverPrototype(observer);
		this.addBaseObserverPrototype((ObserverRoot) observer.getCopy());
		
		//============================================
		observer = new TaskExecObserver();
		observer.dontSaveObject();
		this.tryAddTaskObserverPrototype(observer);
		this.addBaseObserverPrototype((ObserverRoot) observer.getCopy());
		((TaskExecObserver)observer).taskActionPoint.addAction(new CaptureStackFrameAction());
		
        //============================================
		observer = new TaskForkedObserver();
		observer.dontSaveObject();
		this.tryAddTaskObserverPrototype(observer);
		this.addBaseObserverPrototype((ObserverRoot) observer.getCopy());
        ((TaskForkedObserver)observer).parentTaskActionPoint.addAction(new CaptureStackFrameAction());
        ((TaskForkedObserver)observer).offspringTaskActionPoint.addAction(new CaptureStackFrameAction());

        //============================================
		observer = new TaskTerminatingObserver();
		observer.dontSaveObject();
		this.tryAddTaskObserverPrototype(observer);
		this.addBaseObserverPrototype((ObserverRoot) observer.getCopy());
        ((TaskTerminatingObserver)observer).taskActionPoint.addAction(new CaptureStackFrameAction());
        
		//============================================
		observer = new TaskCloneObserver();
		observer.dontSaveObject();
		this.tryAddTaskObserverPrototype(observer);
		this.addBaseObserverPrototype((ObserverRoot) observer.getCopy());
        ((TaskCloneObserver)observer).parentTaskActionPoint.addAction(new CaptureStackFrameAction());
        ((TaskCloneObserver)observer).offspringTaskActionPoint.addAction(new CaptureStackFrameAction());

		//============================================
		observer = new TaskSyscallObserver();
		observer.dontSaveObject();
		this.tryAddTaskObserverPrototype(observer);
		this.addBaseObserverPrototype((ObserverRoot) observer.getCopy());
		
//		observer = new ExitNotificationObserver();
//		tryAddTaskObserverPrototype(observer);	
		//============================================
//		final TaskForkedObserver customObserver = new TaskForkedObserver();
//		customObserver.setName("Custom 'ls' Watcher");
		
		
//		final TaskForkedObserver forkedObserver = new TaskForkedObserver();
//    forkedObserver.setName("XProgramWatcherX");
//    forkedObserver.forkedTaskFilterPoint.addFilter(new TaskProcNameFilter());
//    forkedObserver.forkingTaskFilterPoint.addFilter(new TaskProcNameFilter());
//    forkedObserver.forkedTaskFilterPoint.addFilter(new TaskProcNameFilter());
//    forkedObserver.forkingTaskFilterPoint.addFilter(new TaskProcNameFilter());
//
//    forkedObserver.genericActionPoint.addAction(new LogAction());
//    forkedObserver.genericActionPoint.addAction(new LogAction());
//    forkedObserver.genericActionPoint.addAction(new LogAction());
//    forkedObserver.genericActionPoint.addAction(new LogAction());
//    forkedObserver.genericActionPoint.addAction(new LogAction());
//    forkedObserver.genericActionPoint.addAction(new LogAction());
		
		//final TaskExecObserver   execObserver = new TaskExecObserver();
		
//		AddTaskObserverAction stickyObserverAction = new AddTaskObserverAction();
//		stickyObserverAction.setObserver(forkedObserver);
//		forkedObserver.forkedTaskActionPoint.addAction(stickyObserverAction);
//		
//		//forkedObserver.apply(proc);
//		//execObserver.apply(proc);
//		forkedObserver.dontSaveObject();
//
//		tryAddTaskObserverPrototype(forkedObserver);	
	} 

	/**
	 * Returns a copy of the prototype given.
	 * A list of available prototypes can be 
	 * @param prototype a prototype of the observer to be
	 * instantiate.
	 * */
	public ObserverRoot getObserverCopy(ObserverRoot prototype){
		return (ObserverRoot) prototype.getCopy();
	}
	
	/**
	 * Returns the list of taskObservers available to the @link ObserverManager
	 * @return an @link ObservableLinkedList of TaskObservers
	 * */
	public ObservableLinkedList getTaskObservers(){
		return this.taskObservers;
	}

	public ObservableLinkedList getBaseObservers(){
		return this.baseObservers;
	}
	
	/**
	 * Replace the given ObserverRoot with the other ObserverRoot.
	 * @param toBeRemoved the observer that will be removed and replaced.
	 * @param toBeAdded the observer that will be added in place
	 * of the removed observer
	 * */
	public void swapTaskObserverPrototype(ObserverRoot toBeRemoved, ObserverRoot toBeAdded){

		int index = this.taskObservers.indexOf(toBeRemoved);
		if(index < 0){
			throw new IllegalArgumentException("The passes toBeRemoved Observer ["+ toBeRemoved+"] is not a member of taskObservers");
		}
		
		this.removeTaskObserverPrototype(toBeRemoved);

		nameHash.add(toBeAdded);
		this.taskObservers.add(index, toBeAdded);
	}
	
	public TaskObserverRoot getObserverByName(String argument) {
		return (TaskObserverRoot) this.nameHash.get(argument);
	}
	
	/**
	 * add the given prototype to the list of available observers.
	 * @param observer the observer prototype to be added.
	 * */
	public void addTaskObserverPrototype(ObserverRoot observer){
		this.nameHash.add(observer);
		this.taskObservers.add(observer);
	}
	
	/**
	 * Tries to add an observer. If there is another version
	 * with the same name it assumes the other version is
	 * more up to date and doesnt add.
	 * @param observer
	 */
	public void tryAddTaskObserverPrototype(ObserverRoot observer){
		if(!this.nameHash.nameIsUsed(observer.getName())){
			this.addTaskObserverPrototype(observer);
		}
	}
	
    /**
     * Checks of the name of the observer is already in use
     * @param name name 
     * @return
     */
    public boolean nameIsUsed(String name){
      return this.nameHash.nameIsUsed(name);
    }
    
	public void addBaseObserverPrototype(ObserverRoot observer){
		this.baseObservers.add(observer);
	}
	
	/**
	 * Remove the given prototype to the list of available observers.
	 * @param observer the observer prototype to be removed.
	 * */
	public void removeTaskObserverPrototype(ObserverRoot observer){
		this.taskObservers.remove(observer);
		if(!ObjectFactory.theFactory.deleteNode( OBSERVERS_DIR + observer.getName())){
			//throw new RuntimeException("ObserverManager.removeTaskObserverPrototype() Failed to delete " + observer.getName());
		}
		this.nameHash.remove(observer);
	}
	
	private void loadObservers(){
		WindowManager.logger.log(Level.FINE, "{0} loadObservers\n", this);
		Element node = new Element("Observer");
		File observerDir = new File(this.OBSERVERS_DIR);
		
		String[] array = observerDir.list();
		ObserverRoot loadedObserver = null;
		for (int i = 0; i < array.length; i++) {
			if(array[i].startsWith(".")){
				continue;
			}
			
			try{
				node = ObjectFactory.theFactory.importNode(OBSERVERS_DIR+array[i]);
				loadedObserver = (ObserverRoot)ObjectFactory.theFactory.loadObject(node);
				ObserverRoot existingObserver = this.getObserverByName(loadedObserver.getName());
				if(existingObserver != null){
					// if there is an observer with the same name already
					// assume the one being loaded is more up to date.
					this.removeTaskObserverPrototype(existingObserver);
				}
				WindowManager.logger.log(Level.FINER, "{0} loadObservers loaded {1}\n", new Object[]{this, loadedObserver.getName()});
			}catch (Exception e) {
				errorLog.log(Level.WARNING, "Observer  could not be loaded ",e);
				continue;
			}

			this.addTaskObserverPrototype(loadedObserver);
		}
		//System.out.println(this + ": ObserverManager.loadObservers() DONE");
	}
	
	public void save(){
		Iterator iterator = this.getTaskObservers().iterator();
		while (iterator.hasNext()) {
			ObserverRoot observer = (ObserverRoot) iterator.next();
			if(observer.shouldSaveObject()){
				Element node = new Element("Observer");
				ObjectFactory.theFactory.saveObject(observer, node);
				ObjectFactory.theFactory.exportNode( OBSERVERS_DIR + observer.getName(), node);
				WindowManager.logger.log(Level.FINER, "{0} save saved {1}\n", new Object[]{this, observer.getName()});
			}else{
				WindowManager.logger.log(Level.FINER, "{0} save did not save {1}\n", new Object[]{this, observer.getName()});
			}
		}
		
	}
	
}
