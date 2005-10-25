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

import frysk.gui.monitor.observers.SyscallObserver;
import frysk.gui.monitor.observers.TaskCloneObserver;
import frysk.gui.monitor.observers.TaskExecObserver;
import frysk.gui.monitor.observers.TaskForkedObserver;
import frysk.gui.monitor.observers.TaskTerminatingObserver;
// XXX: import frysk.proc.TaskObserver;

/**
 * @author Sami Wagiaalla
 * Singleton; only one action pool. Flyweight;
 * instanciate Action objects here, then just call execute() from
 * anywhere in the GUI.
 * ActionPool Provies a place for extendors to add their
 * actions. Provies a place for menus to grab their dynamically
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
       
        /** Observers that can be added to a thread */
        public LinkedList threadObservers;
       
        /** } */

        private EventLogger eventLog =  new EventLogger();

        public ActionPool() {
                this.processActions   = new LinkedList();
                this.threadActions    = new LinkedList();
                this.processObservers = new LinkedList();
                this.threadObservers  = new LinkedList();
                
                this.initActions();
        }

        public abstract class Action {
                protected String toolTip;

                protected String name;
                
                class UnimplementedFunctionException extends RuntimeException{
                	/**
					 * Comment for <code>serialVersionUID</code>
					 */
					private static final long serialVersionUID = 1L;

					UnimplementedFunctionException(Class theClass){
                    	throw new RuntimeException( theClass.toString() + " has not implemented this function.");
                	}
                }
                
                public Action() {
                        this.toolTip = new String();
                }

                /**
                 * Delegates the call to the appropriet function.
                 * Either @ProcData or 
                 * */
                public void execute(GuiData data){
                	if(data instanceof ProcData){
                		this.execute((ProcData)data);
                	}
                	
                	if(data instanceof TaskData){
                		this.execute((TaskData)data);
                	}
                }
                
                public void removeObservers(GuiData data){
                	if(data instanceof ProcData){
                		this.removeObservers((ProcData)data);
                	}
                	
                	if(data instanceof TaskData){
                		this.removeObservers((TaskData)data);
                	}
                }

                public void execute(ProcData data){
                	throw new UnimplementedFunctionException( this.getClass() );
                }

                public void execute(TaskData data){
                	throw new UnimplementedFunctionException( this.getClass() );
                }

                public void removeObservers(ProcData data){
                	throw new UnimplementedFunctionException( this.getClass() );
                }

                public void removeObservers(TaskData data){
                	throw new UnimplementedFunctionException( this.getClass() );
                }
                
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
                        
                	data.getProc().observableAttachedContinue.addObserver(WindowManager.theManager.logWindow.attachedContinueObserver);
                	data.getProc().observableAttachedContinue.addObserver(eventLog.attachedContinueObserver);
                       
                	data.getProc().requestAttachedContinue();
                }

				public void removeObservers(ProcData data) {
                    data.getProc().observableAttachedContinue.deleteObserver(WindowManager.theManager.logWindow.attachedContinueObserver);
                    data.getProc().observableAttachedContinue.deleteObserver(eventLog.attachedContinueObserver);
				}

        }

        public class Detach extends Action {

			public Detach() {
				this.name = "Detach";
				this.toolTip = "Detach from an attached process";
			}
	
			public void execute(final ProcData data) {
                System.out.println("sending data.getProc().requestDetachedContinue();");
                
                data.getProc().observableDetachedContinue.addObserver(WindowManager.theManager.logWindow.detachedContinueObserver);
                data.getProc().observableDetachedContinue.addObserver(eventLog.detachedContinueObserver);
                
  				data.getProc().requestDetachedContinue();
			}
	
			/**
			 * This Action does not apply to Tasks.
			 */
			public void execute(TaskData data) {
	
			}

			public void removeObservers(ProcData data) {
				data.getProc().observableDetachedContinue.deleteObserver(WindowManager.theManager.logWindow.detachedContinueObserver);
				data.getProc().observableDetachedContinue.deleteObserver(eventLog.detachedContinueObserver);
			}

			public void removeObservers(TaskData data) {
				
			}
	}

	public class Stop extends Action {
		
		public Stop() {
			this.name = "Stop";
			this.toolTip = "Stop current process";
		}

		public void execute(final ProcData data) {
            data.getProc().observableAttachedStop.addObserver(eventLog.attachedStopObserver);
			data.getProc().observableAttachedStop.addObserver(WindowManager.theManager.logWindow.attachedStopObserver);            
			
			data.getProc().requestAttachedStop();
		}

		public void removeObservers(ProcData data) {
			data.getProc().observableAttachedStop.deleteObserver(eventLog.attachedStopObserver);
			data.getProc().observableAttachedStop.deleteObserver(WindowManager.theManager.logWindow.attachedStopObserver);            
		}
	}

	public class Resume extends Action {
		
		public Resume() {
			this.name = "Resume";
			this.toolTip = "Resume execution of the current process";
		}

		public void execute(final ProcData data) {
            data.getProc().observableAttachedContinue.addObserver(eventLog.attachedResumeObserver);
            data.getProc().observableAttachedContinue.addObserver(WindowManager.theManager.logWindow.attachedResumeObserver);            
            
			data.getProc().requestAttachedContinue();
		}

		public void removeObservers(ProcData data) {
			data.getProc().observableAttachedContinue.deleteObserver(eventLog.attachedResumeObserver);
            data.getProc().observableAttachedContinue.deleteObserver(WindowManager.theManager.logWindow.attachedResumeObserver);            
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

		public void execute(final TaskData data) {
 			final TaskExecObserver taskExecObserver = new TaskExecObserver();
 			
 			taskExecObserver.onAdded(new Runnable(){
 				public void run() {
	 				data.add(taskExecObserver);
	 	 			data.getTask().requestAddExecedObserver(eventLog.taskExecObserver);
				}
 			});
 			
 			taskExecObserver.onDeleted(new Runnable(){
 				public void run() {
	 				data.remove(taskExecObserver);
	 				data.getTask().requestDeleteExecedObserver(eventLog.taskExecObserver);
				}
 			});
 			
 			// XXX: data.getTask().requestAddObserver(taskExecObserver);
			throw new RuntimeException ("XXX: Task.addObserver");
		}
	}

	public class AddExitingObserver extends Action {

		public AddExitingObserver() {
			this.name = "Exiting Observer";
			this.toolTip = "Listen for task exit events on the selected process";
		}

		public void execute(final TaskData data) {
			final TaskTerminatingObserver taskTerminatingObserver = new TaskTerminatingObserver();
			taskTerminatingObserver.onAdded(new Runnable(){
				public void run() {
					data.add(taskTerminatingObserver);
					data.getTask().requestAddTerminatingObserver(eventLog);
				}
			});
			
			taskTerminatingObserver.onDeleted(new Runnable(){
				public void run() {
					data.remove(taskTerminatingObserver);
					data.getTask().requestDeleteTerminatingObserver(eventLog);
				}
			});
			// XXX: data.getTask().requestAddObserver(taskTerminatingObserver);
			throw new RuntimeException ("XXX: Task.addObserver");
		}
		
	}

	public class AddSyscallObserver extends Action {
		
		public AddSyscallObserver(){
			this.name = "Syscall Observer";
			this.toolTip = "Listen for system call events from the selected thread";
		}
		
		public void execute(final TaskData data) {
			final SyscallObserver syscallObserver = new SyscallObserver();
			
			syscallObserver.onAdded(new Runnable() {
				public void run() {
					data.add(syscallObserver);
					// XXX: data.getTask().requestAddSyscallObserver(eventLog);
					throw new RuntimeException ("XXX: Task.addObserver");
				}
			});
			
			syscallObserver.onDeleted(new Runnable() {
				public void run() {
					data.remove(syscallObserver);
					// XXX: data.getTask().requestAddSyscallObserver(eventLog);
					throw new RuntimeException ("XXX: Task.addObserver");
				}
			});
			
			// XXX: data.getTask().requestAddObserver(syscallObserver);
			throw new RuntimeException ("XXX: Task.addObserver");
		}

		
	}
	
	public class AddForkObserver extends Action {

		public AddForkObserver() {
			this.name = "Fork Observer";
			this.toolTip = "Listen for process fork events on the selected process";
		}

		public void execute(final TaskData data) {
			final TaskForkedObserver taskForkedObserver = new TaskForkedObserver();
			
			taskForkedObserver.onAdded(new Runnable() {
				public void run() {
				    data.getTask().requestAddForkedObserver(eventLog);
				    data.add(taskForkedObserver);
				}
			});
			
			taskForkedObserver.onDeleted(new Runnable() {
				public void run() {
				    data.getTask().requestDeleteForkedObserver(eventLog);
				    data.remove(taskForkedObserver);
				}
			});
			
		
			// XXX: data.getTask().requestAddForkedObserver(taskForkedObserver);
			throw new RuntimeException ("XXX: Task.addObserver");
		}
	}


	public class AddCloneObserver extends Action {

		public AddCloneObserver() {
			this.name = "Clone Observer";
			this.toolTip = "Listens for clone events on the selected process";
		}

		public void execute(final TaskData data) {
			final TaskCloneObserver observer = new TaskCloneObserver();
			observer.onAdded(new Runnable() {
				public void run() {
					data.add(observer);
					data.getTask().requestAddClonedObserver(eventLog);
				}
			});
			
			observer.onDeleted(new Runnable() {
				public void run() {
					data.remove(observer);
					data.getTask().requestDeleteClonedObserver (eventLog);
				}
			});
			
			data.getTask().requestAddClonedObserver (observer);
		}
	}


	/**
	 * Actions: A publicly available instance of each action.
	 * {
	 */
	public Attach attach;
	public Detach detach;
	public Stop   stop;
	public Resume resume;
	public PrintState printState;
	
	public AddExecObserver addExecObserver;
	public AddExitingObserver addExitingObserver;
	public AddForkObserver addForkObserver;
	
	public AddCloneObserver addCloneObserver;
	public AddSyscallObserver addSyscallObserver;
	/**}*/

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
		this.threadObservers.add(this.addExecObserver);

		this.addExitingObserver = new AddExitingObserver();
		this.threadObservers.add(this.addExitingObserver);

		this.addCloneObserver = new AddCloneObserver();
		this.threadObservers.add(this.addCloneObserver);

		this.addSyscallObserver = new AddSyscallObserver();
		this.threadObservers.add(this.addSyscallObserver);

		this.addForkObserver = new AddForkObserver();
		this.threadObservers.add(this.addForkObserver);

		this.printState = new PrintState();
		this.processActions.add(this.printState);
		this.threadActions.add (this.printState);
	}

}
