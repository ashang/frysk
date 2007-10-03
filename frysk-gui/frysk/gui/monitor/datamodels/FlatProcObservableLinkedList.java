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

import java.util.Hashtable;
import java.util.Observable;
import java.util.Observer;
import java.util.logging.Level;
import java.util.logging.Logger;

import frysk.gui.Gui;
import frysk.gui.monitor.GuiProc;
import frysk.gui.monitor.ObservableLinkedList;
import frysk.proc.Manager;
import frysk.proc.Proc;

public class FlatProcObservableLinkedList extends ObservableLinkedList{

	/**
	 * Comment for <code>serialVersionUID</code>
	 */
	
	private Logger errorLog = Logger.getLogger (Gui.ERROR_LOG_ID);
	private static final long serialVersionUID = 1L;
	
	private ProcCreatedObserver procCreatedObserver;
	private ProcDestroyedObserver procDestroyedObserver;

	Hashtable hashMap;
	
	public FlatProcObservableLinkedList(){
		super();
		
		this.hashMap = new Hashtable();
		
		this.procCreatedObserver = new ProcCreatedObserver();
		this.procDestroyedObserver = new ProcDestroyedObserver();
	
//		System.out.println(this + ": FlatProcObservableLinkedList.FlatProcObservableLinkedList() adding observers to backend");
		Manager.host.observableProcAddedXXX.addObserver(this.procCreatedObserver);
		Manager.host.observableProcRemovedXXX.addObserver(this.procDestroyedObserver);

	}
	
	class ProcCreatedObserver implements Observer{
	    	public void update (Observable o, Object obj){
	    		final Proc proc = (Proc) obj;
	    		org.gnu.glib.CustomEvents.addEvent(new Runnable(){
	    			public void run() {
	    				GuiProc guiProc = null;
                        
	    				try {
	    					guiProc = GuiProc.GuiProcFactory.getGuiProc(proc);
	    				} catch (Exception e) {
	    					errorLog.log(Level.WARNING, "FlatProcObservableLinkedList.ProcCreatedObserver: Cannot get proc " + 
	    							proc +" from guiFacory",e);
	    					return;
	    				}
                        
                        try {
	    					if(!guiProc.isOwned())
	    						return;
	    				} catch (Exception e)
	    				{
	    					errorLog.log(Level.WARNING, "FlatProcObservableLinkedList.ProcCreatedObserver: Cannot check  guiProc " + 
	    							"ownership",e);
	    					return;
	    				}
	    				
	    				if (guiProc == null) {
	    					return;
	    				}
                        
                        
                        if (guiProc.getNiceExecutablePath().equals(GuiProc.PATH_NOT_FOUND))
                          return;
	    				
	    				//System.out.println(this + ": ProcCreatedObserver.update() " + guiProc.getNiceExecutablePath());
	    				guiProc.setName(guiProc.getNiceExecutablePath()+" - " + proc.getPid());
	    				guiProc.setToolTip(guiProc.getNiceExecutablePath());
	    				
	    				try {
	    					add(guiProc);
	    					hashMap.put(proc, guiProc);
	    				} catch (Exception e) {
	    					errorLog.log(Level.WARNING, "FlatProcObservableLinkedList.ProcCreatedObserver: Cannot add proc " + 
	    							proc +" to DataModel",e);
	    					return;
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
	    				try {
	    					remove(hashMap.get(proc));
	    					hashMap.remove(proc);
	    				} catch (Exception e) {
	    					errorLog.log(Level.WARNING, "FlatProcObservableLinkedList.ProcDestroyedObserver: Cannot remove proc " + 
	    							proc + " from DataModel",e);
	    					return;
	    				}
	    			}
	    		});
		}
	}

}
