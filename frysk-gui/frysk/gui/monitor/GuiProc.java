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
/**
 * Used to store a pointer to the Proc object
 * and any data that is relates to the process but is gui specific.
 * Used to pass data to ActionPool Actions.
 * Actions also manipulate data stored in here
 * to keep it up to date.
 * for example the Attach action will set proc from null
 * to point to the Proc object returned by the backend
 * attach function.
 */
package frysk.gui.monitor;

import java.io.File;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import frysk.gui.Gui;
import frysk.gui.monitor.observers.TaskObserverRoot;
import frysk.proc.Manager;
import frysk.proc.Proc;

/**
 * @author swagiaal
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class GuiProc extends GuiData{
	
	private static final String PATH_NOT_FOUND = "*Could not retrieve path*";

	private Proc proc;
	private Logger errorLog = Logger.getLogger (Gui.ERROR_LOG_ID);
	
	private String executableName;
	private String executablePath;
	private String niceExecutbalePath;
	
    private ObservableLinkedList tasks;
    
	private GuiProc(Proc proc){
		if(proc == null){
			throw new IllegalArgumentException("proc cannot be null");
		}
		this.proc = proc;
		
		this.executableName = "";

		this.setExecutablePath();
		this.setNiceExecutablePath();
	
        this.tasks = new ObservableLinkedList();
    }

    public ObservableLinkedList getTasks(){
      return this.tasks;
    }
    
	public Proc getProc() {
		return proc;
	}
	
    public void addGuiTask(GuiTask guiTask){
      this.tasks.add(guiTask);
    }
    
    public void removeGuiTask(GuiTask guiTask){
      this.tasks.remove(guiTask);
    }
    
	private void setNiceExecutablePath(){
		
		this.niceExecutbalePath = this.getFullExecutablePath();

		if(niceExecutbalePath.indexOf('\0') != -1){
			niceExecutbalePath = niceExecutbalePath.substring(0,niceExecutbalePath.indexOf("\0"));
		}

		if(niceExecutbalePath.endsWith(" (deleted)")){
			niceExecutbalePath = niceExecutbalePath.substring(0,niceExecutbalePath.indexOf(" (deleted)"));
		}
		
		if(niceExecutbalePath.indexOf(".#prelink#") != -1){
			niceExecutbalePath = niceExecutbalePath.substring(0,niceExecutbalePath.indexOf(".#prelink#"));
		}
		
		if(this.executablePath == PATH_NOT_FOUND){
			this.executableName = proc.getCommand();
		}else{
			File file = new File(this.getNiceExecutablePath());		
			this.executableName = file.getName();
		}
	}

	public String getNiceExecutablePath(){
		return this.niceExecutbalePath;
	}
	
	private void setExecutablePath(){
		try{
			this.executablePath = proc.getExe();
			
//			File file = new File(this.getNiceExecutablePath());
//			this.executableName = file.getName();

		}catch (Exception e) {
			try {
				this.executablePath = proc.getCmdLine()[0];
			} catch (Exception e2) {
				this.executablePath = PATH_NOT_FOUND;
//				this.executableName = proc.getCommand();
			}
//			File file = new File(this.executablePath);
//			this.executableName = file.getName();
		}
	}
	
	public void add(final TaskObserverRoot observer){
		
		observer.onAdded(new Runnable() { 
			public void run() {
				//XXX: this will result in the Observer being
				// added too many times this is solved by a 
				// hack right now. better model should be found
	
				//observers.add(observer);
			}
		});
		
		observer.onDeleted(new Runnable() {
			public void run() {
				observers.remove(observer);
			}
		});
		observer.apply(this.proc);
		observers.add(observer);
	}
	
	/**
	 * Returns wether this user owns this process
	 * or not.
	 * @return boolean; true of the user owns this
	 * process false otherwise;
	 */
	public boolean isOwned(){
		boolean owned = false;
		try {
			owned = (this.proc.getUID() == Manager.host.getSelf().getUID() ||
					this.proc.getGID() == Manager.host.getSelf().getGID());
		} catch (Exception e) {
			errorLog.log(Level.WARNING, "GuiProc.isOwned: Error checking host/proc ownership",e);
		}
		
		return owned;
	}
	
	public String getFullExecutablePath(){
		return this.executablePath;
	}
	
	/**
	 * Tries to call getExe() on the proc. If that 
	 * fails, then getCmmd[0] is used.
	 */
	public String getExecutableName(){
		return this.executableName;
	}
	
	
	public static class GuiProcFactory{
		static HashMap map = new HashMap();
		
		public static GuiProc getGuiProc(Proc proc){
			GuiProc guiProc;
			
			guiProc = (GuiProc)map.get(proc);
			
			if(guiProc == null){
				guiProc = new GuiProc(proc);
				map.put(proc, guiProc);
			}
			
			return guiProc;
		}
	}
	
}
