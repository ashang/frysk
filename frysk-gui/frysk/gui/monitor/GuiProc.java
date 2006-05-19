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

import java.util.HashMap;

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
	private Proc proc;
	
	private GuiProc(Proc proc){
		this.proc = proc;
	}

	public void setProc(Proc proc) {
		this.proc = proc;
	}

	public Proc getProc() {
		return proc;
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
		return (this.proc.getUID() == Manager.host.getSelf().getUID() ||
			this.proc.getGID() == Manager.host.getSelf().getGID() );
	}
	
	public String getFullExecutablePath(){
		String execPath = proc.getCommand() + " * path could not be retrieved *";
		try{
			execPath = proc.getExe();
		}catch (Exception e) {}

		return execPath;
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
