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

package frysk.gui.monitor;

import java.util.Hashtable;
import java.util.Observable;
import java.util.Observer;

import org.gnu.gtk.DataColumn;
import org.gnu.gtk.DataColumnObject;
import org.gnu.gtk.DataColumnString;
import org.gnu.gtk.TreeIter;
import org.gnu.gtk.TreeStore;

import frysk.event.TimerEvent;
import frysk.proc.Manager;
import frysk.proc.Proc;

/**
 * @author swagiaal
 *
 * A data model that groups PID's by executable
 * name
 */
public class ProcWiseDataModel {

	private TreeStore treeStore;

	private DataColumnString nameDC;
	private DataColumnObject objectDC;
	
	private ProcCreatedObserver procCreatedObserver;
	private ProcDestroyedObserver procDestroyedObserver;

	private TimerEvent refreshTimer;
	
	private Hashtable iterHash;
	
	ProcWiseDataModel(){
		this.iterHash = new Hashtable();
		
		this.nameDC = new DataColumnString();
		this.objectDC = new DataColumnObject();
		
		this.treeStore = new TreeStore(new DataColumn[] {this.nameDC, this.objectDC});

		this.refreshTimer = new TimerEvent(0, 5000){
			public void execute() {
				Manager.host.requestRefreshXXX (true);
			}
		};
		
		Manager.eventLoop.add (this.refreshTimer);
		
		this.procCreatedObserver = new ProcCreatedObserver();
		this.procDestroyedObserver = new ProcDestroyedObserver();
		
		Manager.host.observableProcAddedXXX.addObserver(this.procCreatedObserver);
		Manager.host.observableProcRemovedXXX.addObserver(this.procDestroyedObserver);
	}

	private void setRow(TreeIter row, String name, ProcData data){
		treeStore.setValue(row, nameDC, name);
		treeStore.setValue(row, objectDC, data);
	}

	public DataColumnString getNameDC() {
		return nameDC;
	}

	public DataColumnObject getPathDC() {
		return objectDC;
	}
	
	class ProcCreatedObserver implements Observer{
    	public void update (Observable o, Object obj){
    		final Proc proc = (Proc) obj;
    		org.gnu.glib.CustomEvents.addEvent(new Runnable(){
				 public void run() {
					 // get an iterator pointing to the parent
					TreeIter parent = (TreeIter) iterHash.get(proc.getCommand());
					System.out.println("ProcCreatedObserver.update() adding " + proc.getCommand() + " " + proc.getPid());
					
					if(parent == null){
						System.out.println(" ProcCreatedObserver.update() first element");
						parent = treeStore.appendRow(null);
						iterHash.put(proc.getCommand(), parent);
						setRow(parent, proc.getCommand() + "\t" + proc.getPid(), new ProcData(proc));
					}else{
						TreeIter iter = treeStore.appendRow(parent);
						if(((ProcData)treeStore.getValue(parent, objectDC)).getProc() != null){
							System.out.println(" ProcCreatedObserver.update() second element");
							Proc oldProc = ((ProcData)treeStore.getValue(parent, objectDC)).getProc();
    						setRow(parent, proc.getCommand(), new ProcData(null));
    						setRow(iter, ""+oldProc.getPid(), new ProcData(oldProc));
    						iter = treeStore.appendRow(parent);
    					}
						//setRow(iter, "", ""+proc.getPid(), proc.getExe());
						setRow(iter, ""+proc.getPid(), new ProcData(proc));
					}
				 }
			});
        }
    }
    

	class ProcDestroyedObserver implements Observer{
		public void update(Observable o, Object obj) {
			final Proc proc = (Proc)obj;

			org.gnu.glib.CustomEvents.addEvent(new Runnable(){
				public void run() {
					TreeIter parent = (TreeIter) iterHash.get(proc.getCommand());
					System.out.println("ProcDestroyedObserver.update() trying to remove " + proc.getCommand() + " " + proc.getPid());
					System.out.println("ProcDestroyedObserver.update() parent " + parent);
					
					try{
						if(parent == null){
							throw new NullPointerException("proc " + proc + "Not found in TreeIter HasTable. Cannot be removed"); //$NON-NLS-1$ //$NON-NLS-2$
						}
						int n = parent.getChildCount();

						if(n == 0){
							treeStore.removeRow(parent);
							iterHash.remove(proc.getCommand());
							return;
						}

						if(n > 1){
							for(int i = 0; i < n; i++){
								TreeIter iter = parent.getChild(i);
								if(((ProcData)treeStore.getValue(iter, objectDC)).getProc().getPid() == proc.getPid()){
									treeStore.removeRow(iter);
									break;
								}
							}
						}
						
						n = parent.getChildCount();
						if(n == 1){
							TreeIter iter = parent.getChild(0);
							Proc oldProc = ((ProcData)treeStore.getValue(iter, objectDC)).getProc();
    						setRow(parent, oldProc.getCommand() + "\t" + oldProc.getPid(), new ProcData(oldProc));
    						
							treeStore.removeRow(iter);
						}
						
					}catch (NullPointerException e) {
	//					errorLog.log(Level.WARNING,"proc " + proc + "Not found in TreeIter HasTable. Cannot be removed",e); //$NON-NLS-1$ //$NON-NLS-2$
						e.printStackTrace();
					}
				}
			});
		}
	}


	public TreeStore getModel() {
		return this.treeStore;
	}
	
}
