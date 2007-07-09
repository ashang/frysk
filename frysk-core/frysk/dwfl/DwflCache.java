// This file is part of the program FRYSK.
//
// Copyright 2007, Red Hat Inc.
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

package frysk.dwfl;

import frysk.proc.Proc;
import frysk.proc.Task;
import java.util.Iterator;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import lib.dw.Dwfl;

/**
 * Cache of most-recently opened Dwfl-s.  This class ensures that code
 * requiring dwfl always accesses the most recently opened version.
 * If a process changes, and the Dwfl needs to be updated, this class
 * will re-open the dwfl returning a new object.
 */
public class DwflCache
{
    private static Logger logger = Logger.getLogger("frysk");

    /**
     * Cache of per-proc Dwfl objects.
     */
    private static WeakHashMap currentDwfls = new WeakHashMap();
  
    /**
     * Table of each task's last modified counter.
     */
    private static WeakHashMap taskMod = new WeakHashMap();

    /**
     * Cache of all Dwfl objects.
     */
    private static WeakHashMap allDwfls = new WeakHashMap();

    /**
     * Return a Dwfl for a {@link frysk.proc.Proc}.  If there is no
     * Dwfl, create a new one.
     * 
     * @param proc the given {@link frysk.proc.Proc}.
     * @return a Dwfl created with proc's maps.
     */
    public static Dwfl getDwfl(Proc proc) {
	logger.log(Level.FINE, "entering createDwfl, proc: {0}\n", proc);
	Dwfl dwfl = (Dwfl) currentDwfls.get(proc); 
	if (dwfl != null) {
	    return dwfl;
	}
	// Create a new Dwfl.
	dwfl = DwflFactory.createDwfl(proc);
	// Save it in both the Proc and all cache.
	currentDwfls.put(proc, dwfl);
	allDwfls.put(proc, dwfl);
	taskMod.clear();
	for (Iterator i = proc.getTasks().iterator(); i.hasNext(); ) {
           Task task = (Task) i.next();
           taskMod.put(task, new Integer(task.getMod()));
	}
	return dwfl;
    }

    /**
     * return a Dwfl for a {@link frysk.proc.Task}.
     * 
     * @param task the given {@link frysk.proc.Task}.
     * @return a Dwfl created using the tasks maps.
     */
    public static Dwfl getDwfl(Task task) {
	logger.log(Level.FINE, "entering createDwfl, task: {0}\n", task);
	// Check if this task has changed since (if) a dwfl was last
	// created.  If it hasn't changed returned the cached dwfl.
	// If it has changed recreate the dwfl and update the maps.
	Integer mod = (Integer)taskMod.get(task);
	if (mod != null) {
	    logger.log(Level.FINEST, "taskMod contains task, taskMod {0}\n",
		       taskMod);
	    if (mod.intValue() == task.getMod()) {
		logger.log(Level.FINEST, "returning dwfl\n");
		return getDwfl(task.getProc());
	    }
	}
    
	logger.log(Level.FINEST, "taskMap doesn't contain task\n", taskMod);
   
	// Remove the existing dwfl, creating a new one.
	taskMod.put(task, new Integer(task.getMod()));
	// XXX: Should close the removed Dwfl, but at present that
	// will make a right mess.
	Dwfl dwfl = (Dwfl)currentDwfls.remove(task.getProc());
	// XXX: Should close the removed Dwfl, but at present that
	// will make a right mess.
	dwfl = getDwfl(task.getProc());
	return dwfl;
    }

    /**
     * Remove a Dwfl created for a {@link frysk.proc.Proc}. (Example:
     * after an exec.)
     * 
     * @param proc the given {@link frysk.proc.Proc}.
     */
    public static void removeDwfl(Proc proc) {
	Dwfl d = (Dwfl) currentDwfls.remove(proc);
	if (d != null) {
	    d.close();
	}
    }

    /**
     * Clear a Dwfl created for a {@link frysk.proc.Task}. (Example:
     * after an exec.)
     * 
     * @param task the given {@link frysk.proc.Task}.
     */
    public static void removeDwfl(Task task) {
	removeDwfl(task.getProc());
    }

    public static void clear() {
	currentDwfls.clear();
	for (Iterator i = allDwfls.values().iterator(); i.hasNext();) {
	    Dwfl d = (Dwfl) i.next();
	    d.close();
	    i.remove();
	}
    }
}
