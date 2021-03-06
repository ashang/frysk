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

import frysk.proc.Manager;
import frysk.proc.Proc;

/**
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class GuiProc extends GuiCoreObjectWrapper{
	
	public static final String PATH_NOT_FOUND = "*Could not retrieve path*";

	private Proc proc;
	
	private String executableName;
	private String executablePath;
	private String niceExecutablePath;
	
	public GuiObservable executablePathChanged;
	
    public GuiProc(Proc proc){
      
      if(proc == null){
	throw new IllegalArgumentException("proc cannot be null");
      }
      this.proc = proc;
	
      this.executablePathChanged = new GuiObservable();
      
      this.executableName = "";

      this.setExecutablePath();
      this.setNiceExecutablePath();
    }

    public Proc getProc() {
      return proc;
    }
	
    private void setNiceExecutablePath(){
		
      this.niceExecutablePath = this.getFullExecutablePath();

      if(niceExecutablePath.indexOf('\0') != -1){
	niceExecutablePath = niceExecutablePath.substring(0,niceExecutablePath.indexOf("\0"));
      }

      if(niceExecutablePath.endsWith(" (deleted)")){
	niceExecutablePath = niceExecutablePath.substring(0,niceExecutablePath.indexOf(" (deleted)"));
      }
		
      if(niceExecutablePath.indexOf(".#prelink#") != -1){
	niceExecutablePath = niceExecutablePath.substring(0,niceExecutablePath.indexOf(".#prelink#"));
      }
		
      if(this.executablePath == PATH_NOT_FOUND){
	this.executableName = proc.getCommand();
      }else{
	File file = new File(niceExecutablePath);		
	this.executableName = file.getName();
      }
    }

    /**
     * The executable path of a proc might be mangled with things like '(deleted)'
     * or "#prelink#" if the executable has been deleted or the link changed. This
     * function cleans this out and returns the clean path.
     * @return
     */
    public String getNiceExecutablePath(){
      this.setNiceExecutablePath();
      return this.niceExecutablePath;
    }
	
    private void setExecutablePath(){
      String newPath = "";
      
      try{
	newPath = proc.getExeFile().getSysRootedPath();
			
      }catch (Exception e) {
	try {
	  if (proc.getCmdLine().length > 0)
	    newPath = proc.getCmdLine()[0];
	  else
	    newPath = PATH_NOT_FOUND;
					
	} catch (Exception e2) {
	  newPath = PATH_NOT_FOUND;
	  return;
	}
      }
      
      if(!newPath.equals(this.executablePath)){
	this.executablePath = newPath;
	this.executablePathChanged.notifyObservers(this);
      }
    }
	
    /**
     * Returns wether this user owns this process or not.
     * 
     * - Checks uid and * gid.
     * - Checks if the given process is this frysk process if so returns false.
     * - Also checks that the user has acces to /pro/exe if not false is returned.
     * - Checks if this process is the init process, return false if so.
     * 
     * @return boolean; true of the user owns this process, and can debug it
     *         false otherwise;
     */
    public boolean isOwned() {
	if (this.getProc().getPid() == 1) {
	    return false;
	}

	if (this.proc.getPid() == Manager.host.getSelf().getPid()) {
	    return false;
	}

	if (Manager.host.getSelf().getUID() == 0) {
	    return true;
	}

	try {
	    proc.getExeFile().getSysRootedPath();
	} catch (Exception e) {
	    return false;
	}

	if ((this.proc.getUID() == Manager.host.getSelf().getUID() || this.proc.getGID() == Manager.host.getSelf().getGID())) {
	    return true;
	}

	return false;
    }
	
	public String getFullExecutablePath(){
	  this.setExecutablePath();
	  return this.executablePath;
	}
	
	/**
	 * Tries to call getExe() on the proc. If that 
	 * fails, then getCmmd[0] is used.
	 */
	public String getExecutableName(){
	  this.setNiceExecutablePath();
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
