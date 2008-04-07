// This file is part of the program FRYSK.
//
// Copyright 2008, Red Hat Inc.
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

package frysk.isa.watchpoints;


import java.util.ArrayList;
import java.util.List;
import frysk.proc.Task;

public abstract class WatchpointFunctions  {

    // Architecture Watchpoint Count. Number of usable
    // Address-Breakpoint Registers 
    protected int noOfWatchpoints = 0;

   /**
    * Builds and sets a hardware watchpoint on a task.
    *
    * @param task - task to set a watchpoint on.
    * @param index - watchpoint number to write. Architecture
    * dependent.
    * @param addr - linear virtual address to watch.
    * @param range - length of range to watch. Normally
    * 1,2 or 4 bytes. 8 on 64 bit systems. Architecture dependent.
    * @param writeOnly - When true, only trigger when address is
    * written. False, trigger when address is read or written to.
    */
    public abstract void setWatchpoint(Task task, int index, 
				       long addr, int range,
				       boolean writeOnly); 

    /**
     * Reads a watchpoint. Takes a task, and an index.
     *
     * @param task - task to read a watchpoint from.
     * @param index - watchpoint number to read.
     *
     * @return long - value of register for watchpoint.
     */
    public abstract Watchpoint readWatchpoint(Task task, int index);

    /**
     * Deletes a watchpoint. Takes a task, and an index.
     *
     * @param task - task on which to delete a watchpoint.
     * @param index - watchpoint number to delete.
     *
     * @return long - value of register for wp
     */
    public abstract void deleteWatchpoint(Task task, int index);

    
    /**
     * Returns all the watchpoints know in the 
     * debug control registers
     *
     * @param task - task on which to delete a watchpoint.
     *
     * @return List- List of watchpoints
     *
     **/
    public List getAllWatchpoints(Task task) {
	List listOfWP = new ArrayList();
	for (int i=0; i<getWatchpointCount(); i++) {
	    listOfWP.add(readWatchpoint(task,i));
	}
	return listOfWP;   
    }

    /**
     * Reads the Debug control register.
     *
     * @param task - task to read the debug control
     * register from.
     */
    protected abstract long readControlRegister(Task task);

    /**
     * Reads the Debug status register.
     *
     * @param task - task to read the debug status
     * register from.
     */
    protected abstract long readStatusRegister(Task task);

    /**
     * Reads the Debug Status Register and checks if 
     * the breakpoint specified has fired.
     *
     * @param task - task to read the debug control
     * register from.
     * @param index - Debug register to check

     */
    public abstract boolean hasWatchpointTriggered(Task task, int index);

    /**
     * Resets the appropriate bit in the debug status register
     * after a watchpoint has triggered, thereby reseting it.
     *
     * @param task - task to read the debug control
     * register from.
     * @param index - Debug register to reset.
     */
    public abstract void resetWatchpoint(Task task, int index);

    /**
     * Returns number of watchpoints for this architecture
     *
     * @return int number of usable watchpoints.
     */
    public final int getWatchpointCount() {
	return noOfWatchpoints;
    }
}
