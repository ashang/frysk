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

//import frysk.gui.common.dialogs.DialogManager;
import frysk.proc.Manager;

/**
 * @author Sami Wagiaalla
 * Singelton; only one action pool. Flyweight;
 * instanciate Action objects here, then just call execute() from
 * anywhere in the GUI. Provies a place for extendors to add their
 * actions. Provies a place for menues to grab their dynamically
 * extendable contents. Avoids rewriting of code, and copying of objects
 * (Ex, a MenuItem, ToolBar button, maybe even CLI, all call execute on
 * the same Action object)
 */
public class ActionPool {

        public static ActionPool theActionPool = new ActionPool();

        /**
         * Extention Points: Add your actions here, and retrieve them from here for
         * initializing GUI components {
         */

        /** Actions that can be perfomred on a process should be appened here*/
        public LinkedList processActions;
        
        /** Actions that can be perfomred on a thread should be appened here*/
        public LinkedList threadActions;
        
        /** Observers that can be added to a process */
        public LinkedList processObservers;
        private EventLogger eventLog =  new EventLogger();

        /** } */

        public ActionPool() {
                this.processActions = new LinkedList();
                this.threadActions = new LinkedList();
                this.processObservers = new LinkedList();
                this.initActions();
        }

        public abstract class Action {
                protected String toolTip;

                protected String name;

                public Action() {
                        this.toolTip = new String();
                }

                public abstract void execute(ProcData data);

                public abstract void execute(TaskData data);

                public void execute(ProcData[] data) {
                        for (int i = 0; i < data.length; i++) {
                                this.execute(data[i]);
                        }
                }

                public void execute(TaskData[] data) {
                        for (int i = 0; i < data.length; i++) {
                                this.execute(data[i]);
                        }
                }

                public String getToolTip() {
                        return toolTip;
                }

                public String getName() {
                        return name;
                }

                public void setToolTip(String toolTip) {
                        this.toolTip = toolTip;
                }

                public void setName(String name) {
                        this.name = name;
                }
        }

        public class Attach extends Action {

                public Attach() {
                        this.name = "Attach";
                        this.toolTip = "Attach to a running process";
                }

                public void execute(ProcData data) {
                        System.out.println("sending Manager.host.requestAttachProc");
                        data.getProc().observableAttachedContinue
                                        .addObserver(WindowManager.theManager.logWindow.attachedContinueObserver);
                        data.getProc().observableAttachedContinue.addObserver(eventLog.attachedContinueObserver);
                        data.getProc().requestAttachedContinue();

                }

                /**
                 * This Action does not apply to Tasks.
                 * */
                public void execute(TaskData data) {
                        
                }
        }

        public class Detach extends Action {

			public Detach() {
				this.name = "Detach";
				this.toolTip = "Dettach from an attached process";
			}
	
			public void execute(final ProcData data) {
	
				Manager.host.observableProcRemoved
						.addObserver(WindowManager.theManager.logWindow.detachedContinueObserver);
				Manager.host.observableProcRemoved
						.addObserver(eventLog.detachedContinueObserver);
				data.getProc().requestDetachedContinue();
			}
	
			/**
			 * This Action does not apply to Tasks.
			 */
			public void execute(TaskData data) {
	
			}
	}

	public class Stop extends Action {
		
		public Stop() {
			this.name = "Stop";
			this.toolTip = "Stop current process";
		}

		public void execute(final ProcData data) {
			data.getProc().requestAttachedStop();
			System.out.println("sending proc.requestAttachedStop() for " + data.getProc());
		}

		public void execute(TaskData data) {
			//XXX: data.getTask().requestStop(); is not public 
		}
	}

	public class Resume extends Action {
		
		public Resume() {
			this.name = "Resume";
			this.toolTip = "Resume execution of the current process";
		}

		public void execute(final ProcData data) {
			data.getProc().requestAttachedContinue();
			System.out.println("sending proc.requestAttachedContinue() for " + data.getProc());
		}

		public void execute(TaskData data) {
			//XXX: to be implement in the back end:	data.getTask().requestContinue();
			//data.getTask().
		}
	}

	public class PrintState extends Action {
		
		public PrintState() {
			this.name = "Print State";
			this.toolTip = "Print the state of the selected process or thread";
		}

		public void execute(final ProcData data) {
			System.out.println("Proc State : " + data.getProc());
		}

		public void execute(TaskData data) {
			System.out.println("Proc State : " + data.getTask());
		}
	}
	
	public class AddExecObserver extends Action {

		public AddExecObserver() {
			this.name = "Exec Observer";
			this.toolTip = "Listen for exec events on the selected process/thread";
		}

		public void execute(ProcData data) {
			TaskExecObserver taskExecObserver = new TaskExecObserver();

			data.getProc().taskExeced.addObserver(WindowManager.theManager.logWindow);
			data.getProc().taskExeced.addObserver(eventLog);
			data.getProc().taskExeced.addObserver(taskExecObserver);
			data.add(taskExecObserver);
		}

		public void execute(TaskData data) {
			//XXX
		}
	}

	public class AddExitingObserver extends Action {

		public AddExitingObserver() {
			this.name = "Exiting Observer";
			this.toolTip = "Listen for task exit events on the selected process";
		}

		public void execute(ProcData data) {
			TaskExitingObserver taskExitingObserver = new TaskExitingObserver();

			data.getProc().taskExeced.addObserver(WindowManager.theManager.logWindow);
			data.getProc().taskExeced.addObserver(eventLog);
			data.getProc().taskExeced.addObserver(taskExitingObserver);

			data.add(taskExitingObserver);
		}

		public void execute(TaskData data) {
			// TODO Auto-generated method stub
			
		}
	}

	/**
	 * Actions: A publicly available instance of each action this might not be
	 * nessecery... thoughts ? {
	 */
	
	public Attach attach;
	public Detach detach;
	public Stop   stop;
	public Resume resume;
	public PrintState printState;
	
	public AddExecObserver addExecObserver;
	public AddExitingObserver addExitingObserver;
	
	/** } */

	/**
	 * Initializes all the public actions and adds them to the apporpriet list.
	 * When adding a new action instantiate it publicly and initialized here,
	 * and add it to its list.
	 */
	private void initActions() {
		this.attach = new Attach();
		this.processActions.add(this.attach);

		this.detach = new Detach();
		this.processActions.add(this.detach);

		this.stop = new Stop();
		this.processActions.add(this.stop);
		this.threadActions.add (this.stop);
		
		this.resume = new Resume();
		this.processActions.add(this.resume);
		this.threadActions.add (this.resume);
		
		this.addExecObserver = new AddExecObserver();
		this.processObservers.add(this.addExecObserver);

		this.addExitingObserver = new AddExitingObserver();
		this.processObservers.add(this.addExitingObserver);
		
		this.printState = new PrintState();
		this.processActions.add(this.printState);
		this.threadActions.add (this.printState);
	}

}