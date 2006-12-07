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


/**
 * Singleton; only one action pool. Flyweight; instanciate Action
 * objects here, then just call execute() from anywhere in the GUI.
 * ActionPool Provies a place for extendors to add their
 * actions. Provies a place for menus to grab their dynamically
 * extendable contents. Avoids rewriting of code, and copying of
 * objects (Ex, a MenuItem, ToolBar button, maybe even CLI, all call
 * execute on the same Action object)
 */
public class ActionPool {

        public static ActionPool theActionPool = new ActionPool();

        /**
         * Extention Points: Add your actions here, and retrieve them from here for
         * initializing GUI components {
         */

        /** Actions that can be perfomred on a process should be appened here*/
        public ObservableLinkedList processActions;
        
        /** Actions that can be perfomred on a thread should be appened here*/
        public ObservableLinkedList threadActions;
        
        /** Observers that can be added to a process */
        public ObservableLinkedList processObservers;
       
        /** Observers that can be added to a thread */
        public ObservableLinkedList threadObservers;
       
        /** } */

       // private EventLogger eventLog =  new EventLogger();

        public ActionPool() {
                this.processActions   = new ObservableLinkedList();
                this.threadActions    = new ObservableLinkedList();
                this.processObservers = new ObservableLinkedList();
                this.threadObservers  = new ObservableLinkedList();
                
                this.initActions();
        }

        		


	/**
	 * Initializes all the public actions and adds them to the apporpriet list.
	 * When adding a new action instantiate it publicly and initialized here
	 * and add it to its list.
	 */
	private void initActions() {
//		this.attach = new Attach();
//		this.processActions.add(this.attach);
//
//		this.detach = new Detach();
//		this.processActions.add(this.detach);
//
//		this.stop = new Stop();
//		this.processActions.add(this.stop);
//		this.threadActions.add (this.stop);
//		
//		this.resume = new Resume();
//		this.processActions.add(this.resume);
//		this.threadActions.add (this.resume);
//		
//		this.addCloneObserver = new AddCloneObserver();
//		this.threadObservers.add(this.addCloneObserver);
//
//		this.printState = new PrintState();
//		this.processActions.add(this.printState);
//		this.threadActions.add (this.printState);
	}

}
