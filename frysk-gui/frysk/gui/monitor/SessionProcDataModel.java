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

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Observable;
import java.util.Observer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.gnu.gtk.DataColumn;
import org.gnu.gtk.DataColumnBoolean;
import org.gnu.gtk.DataColumnInt;
import org.gnu.gtk.DataColumnObject;
import org.gnu.gtk.DataColumnString;
import org.gnu.gtk.TreeIter;
import org.gnu.gtk.TreeModel;
import org.gnu.gtk.TreeStore;
import org.gnu.pango.Weight;

import frysk.event.TimerEvent;
import frysk.gui.Gui;
import frysk.gui.sessions.DebugProcess;
import frysk.gui.sessions.Session;
import frysk.proc.Manager;
import frysk.proc.Proc;
import frysk.proc.Task;
import frysk.proc.ProcObserver.Tasks;

public class SessionProcDataModel {
	
	private TreeStore treeStore;
	
	
	DataColumn[] columns;
	
	private DataColumnInt    pidDC;
	private DataColumnString commandDC;
	private DataColumnString colorDC;
	private DataColumnObject procDataDC;
	private DataColumnInt weightDC;
	private DataColumnInt threadParentDC;
	private DataColumnBoolean isThreadDC;
	private DataColumnBoolean sensitiveDC; 
	
	private HashMap iterHash;
	
	private TimerEvent refreshTimer;
	
	private Logger errorLog = Logger.getLogger (Gui.ERROR_LOG_ID);
	
	/**{
	 * Local Observers
	 * */
//	private ProcCreatedObserver procCreatedObserver;
//	private ProcDestroyedObserver procDestroyedObserver;
//	private TaskCreatedObserver taskCreatedObserver;
//	private TaskDestroyedObserver taskDestroyedObserver;
	/** }*/

	private Session currentSession;
	
	public SessionProcDataModel() throws IOException{

		this.pidDC     = new DataColumnInt();
		this.commandDC = new DataColumnString();
		this.colorDC   = new DataColumnString();
		this.procDataDC= new DataColumnObject();
		this.weightDC = new DataColumnInt();
		this.isThreadDC = new DataColumnBoolean();
		this.threadParentDC = new DataColumnInt();
		this.sensitiveDC = new DataColumnBoolean();
		
		this.treeStore = new TreeStore(new DataColumn[]{pidDC,
				commandDC,
				colorDC,
				procDataDC,
				weightDC,
				threadParentDC,
				isThreadDC,
				sensitiveDC});
		
		// Change to HashMap from HashTable
		this.iterHash = new HashMap();
	
		this.refreshTimer = new TimerEvent(0, 5000){
			public void execute() {
				Manager.host.requestRefreshXXX (true);
			}
		};
		
		Manager.eventLoop.add (this.refreshTimer);
		
//		this.procCreatedObserver = new ProcCreatedObserver();
//		this.procDestroyedObserver = new ProcDestroyedObserver();
//		this.taskCreatedObserver = new TaskCreatedObserver();
//		this.taskDestroyedObserver = new TaskDestroyedObserver ();
		
//		Manager.host.observableProcAddedXXX.addObserver(this.procCreatedObserver);
//		Manager.host.observableProcRemovedXXX.addObserver(this.procDestroyedObserver);
//		Manager.host.observableTaskAddedXXX.addObserver (taskCreatedObserver);
//		Manager.host.observableTaskRemovedXXX.addObserver (taskDestroyedObserver);
	}
	
	public void setSession(Session session){
		this.currentSession = session;
		Iterator iterator = this.currentSession.getProcesses().iterator();
		System.out
				.println(this
						+ ": SessionProcDataModel.setSession() " + this.currentSession.getProcesses().size());
		while(iterator.hasNext()){
		    //System.out.println(this + ": SessionProcDataModel.setSession()");
			this.addProc(((DebugProcess)iterator.next()).getProc());
		}
	}
	
	public void addProc(Proc proc){
		TreeIter iter = treeStore.appendRow(null);

		iterHash.put(proc.getId(), iter);
		
		treeStore.setValue(iter, commandDC, proc.getCommand());
		treeStore.setValue(iter, pidDC, proc.getPid());
		treeStore.setValue(iter, procDataDC, (new ProcData(proc)));
		treeStore.setValue(iter, weightDC, Weight.NORMAL.getValue());
		treeStore.setValue(iter, isThreadDC, false);
			
		treeStore.setValue(iter,threadParentDC, 0);
		treeStore.setValue(iter,sensitiveDC, true);		
	
		proc.requestAddTasksObserver(new Tasks() {
		
			public void deletedFrom(Object observable) {
			}
		
			public void addFailed(Object observable, Throwable w) {
				throw new RuntimeException("Failed to add tasks observer");
			}
		
			public void addedTo(Object observable) {
			}
		
			public void existingTask(Task task) {
				addTask(task);
			}
		
			public void taskRemoved(Task task) {
				removeTask(task);
			}
		
			public void taskAdded(Task task) {
				addTask(task);
			}
		});
	}
	
	public void addTask(Task task){
		// get an iterator pointing to the parent
		TreeIter parent;
		parent = (TreeIter) iterHash.get(task.getProc().getId());
		
		
		TreeIter iter = treeStore.appendRow(parent);
		
		iterHash.put(task.getTaskId(), iter);
		
		treeStore.setValue(iter, commandDC, Long.toHexString(task.getEntryPointAddress()));
		treeStore.setValue(iter, pidDC, task.getTid());
		treeStore.setValue(iter, weightDC, Weight.NORMAL.getValue());
		treeStore.setValue(iter, threadParentDC, task.getProc().getPid());
		treeStore.setValue(iter, isThreadDC, true);
			
		treeStore.setValue(iter, procDataDC, (new TaskData(task)));
		treeStore.setValue(iter,sensitiveDC, true);
	}
	
	public void removeTask(Task task){
		TreeIter iter = (TreeIter) iterHash.get(task.getTaskId());
		if(iter == null){
			throw new NullPointerException("task " + task + "Not found in TreeIter HasTable. Cannot be removed"); //$NON-NLS-1$ //$NON-NLS-2$
		}
			
		treeStore.removeRow(iter);
		iterHash.remove(task.getTaskId());
	}
	
	public void removeProc(Proc proc){
		TreeIter iter = (TreeIter) iterHash.get(proc.getId());
		if(iter == null){
			throw new RuntimeException("proc " + proc + "Not found in TreeIter HasTable. Cannot be removed"); //$NON-NLS-1$ //$NON-NLS-2$
		}
			
		treeStore.removeRow(iter);
		iterHash.remove(proc.getId());
	}
	
	public void stopRefreshing(){
		Manager.eventLoop.remove(refreshTimer);
	}
	
	public void setRefreshTime(int sec){
		Manager.eventLoop.remove(refreshTimer);
		this.refreshTimer = new TimerEvent(0, sec*1000){
			public void execute() { 
				Manager.host.requestRefreshXXX (true); 
			}
		};
		Manager.eventLoop.add (refreshTimer);
	}
	
	public void refresh() throws IOException{
		Manager.host.requestRefreshXXX (true);
	}
	
	
	
	
	public DataColumnInt getPidDC() {
		return this.pidDC;
	}

	public DataColumnString getCommandDC() {
		return this.commandDC;
	}

	public DataColumnString getColorDC() {
		return this.colorDC;
	}

	public DataColumnObject getProcDataDC() {
		return this.procDataDC;
	}
	
	public DataColumnInt getWeightDC() {
		return this.weightDC;
	}

	public DataColumnInt getThreadParentDC() {
		return this.threadParentDC;
	}
	
	public DataColumnBoolean getIsThreadDC() {
		return this.isThreadDC;
	}
	
	public DataColumnBoolean getHasParentDC() {
		return this.isThreadDC;
	}

	public DataColumnBoolean getSensitiveDC() {
		return this.sensitiveDC;
	}

	public TreeModel getModel() {
		return this.treeStore;
	}
	
	
	class TaskDestroyedObserver implements Observer{
		public void update (Observable o, final Object obj){
			org.gnu.glib.CustomEvents.addEvent(new Runnable(){
				public void run() {
					final Task task = (Task) obj;
					TreeIter iter = (TreeIter) iterHash.get(task.getTaskId());
//					System.out.println(" TaskDestroyedObserver.update() trying to remove Task " + task.getTid()+ " " + iter );
					try{
						if(iter == null){
							throw new NullPointerException("task " + task + "Not found in TreeIter HasTable. Cannot be removed"); //$NON-NLS-1$ //$NON-NLS-2$
						}
						
						// get an iterator pointing to the parent
						TreeIter parent;
						if(task.getProc() == null){
							parent = null;
						}else{
							parent = (TreeIter) iterHash.get(task.getProc().getId());
						}
					
						if(getThreadCount(parent) == 1 ){
//							treeStore.setValue(parent, sensitiveDC, true);
							treeStore.setValue(parent, sensitiveDC, false);
						}else{
//							treeStore.setValue(parent, sensitiveDC, false);
							treeStore.setValue(parent, sensitiveDC, true);
						}
						
						treeStore.removeRow(iter);
						iterHash.remove(task.getTaskId());
				
					}catch (NullPointerException e) {
						errorLog.log(Level.WARNING,"trying to remove task " + task + "before it is added",e); //$NON-NLS-1$ //$NON-NLS-2$
					}
				}
			});
		}
    }
    
	private int getThreadCount(TreeIter iter){
	    int n = iter.getChildCount();
	    int threadCount = 0;
	    //		System.out.println("ProcDestroyedObserver.update() iter not null checking children n: " + n );
	    for (int i = 0; i < n; i++) {
		    if(treeStore.getValue(iter.getChild(i), isThreadDC) == true){
			    threadCount++;
		    }
	    }
	    // System.out.println(this + ": ProcDataModel.getThreadCount() " + treeStore.getValue(iter, commandDC) + " " + treeStore.getValue(iter, pidDC) + " "+ threadCount); 
	    return threadCount;
	    
    }
    
}
