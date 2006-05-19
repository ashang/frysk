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

package frysk.gui.monitor.datamodels;

import java.util.HashMap;
import java.util.Observable;
import java.util.Observer;

import frysk.event.TimerEvent;
import frysk.gui.monitor.ObservableLinkedList;
import frysk.gui.monitor.GuiProc;
import frysk.proc.Manager;
import frysk.proc.Proc;

public class FlatProcObservableLinkedList extends ObservableLinkedList{

	/**
	 * Comment for <code>serialVersionUID</code>
	 */
	private static final long serialVersionUID = 1L;
	
	private ProcCreatedObserver procCreatedObserver;
	private ProcDestroyedObserver procDestroyedObserver;

	HashMap hashMap;
	
	public FlatProcObservableLinkedList(){
		super();
		
		this.hashMap = new HashMap();
		
		this.procCreatedObserver = new ProcCreatedObserver();
		this.procDestroyedObserver = new ProcDestroyedObserver();
	
		Manager.host.observableProcAddedXXX.addObserver(this.procCreatedObserver);
		Manager.host.observableProcRemovedXXX.addObserver(this.procDestroyedObserver);

		TimerEvent refreshTimer = new TimerEvent(0, 5000){
			public void execute() {
				Manager.host.requestRefreshXXX (true);
			}
		};
		
		Manager.eventLoop.add (refreshTimer);
	}
	
	class ProcCreatedObserver implements Observer{
	    	public void update (Observable o, Object obj){
	    		final Proc proc = (Proc) obj;
	    		org.gnu.glib.CustomEvents.addEvent(new Runnable(){
	    			public void run() {
	    				if(proc.getUID() != Manager.host.getSelf().getUID()){
	    					return;
	    				}
	    				
	    				String execPath = proc.getCommand() + " *path could not be retrieved*";
	    				try{
	    					execPath = proc.getExe();
	    				}catch (Exception e) {
	    					
	    				}
	    				
	    				GuiProc procData = GuiProc.GuiProcFactory.getGuiProc(proc);
	    				procData.setName(proc.getPid() + " " + execPath);
	    				procData.setToolTip(execPath);
	    				
	    				add(procData);
	    				hashMap.put(proc, procData);
	    			}
	    		});
	        }
	    	
	    }
	    

	class ProcDestroyedObserver implements Observer{
		public void update(Observable o, Object obj) {
			final Proc proc = (Proc)obj;
			org.gnu.glib.CustomEvents.addEvent(new Runnable(){
	    			public void run() {
	    				remove(hashMap.get(proc));
	    				hashMap.remove(proc);
	    			}
	    		});
		}
	}

}
