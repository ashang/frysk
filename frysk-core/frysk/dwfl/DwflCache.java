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

import frysk.hpd.DbgVariables;
import frysk.proc.Task;
import java.util.Iterator;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import lib.dwfl.Dwfl;

/**
 * Cache of most-recently opened Dwfl-s.  This class ensures that code
 * requiring dwfl always accesses the most recently opened version.
 * If a process changes, and the Dwfl needs to be updated, this class
 * will re-open the dwfl returning a new object.
 */
public class DwflCache
{
    private static Logger logger = Logger.getLogger("frysk");

    static private class Mod {
	final Dwfl dwfl;
	int count;
	Mod(Dwfl dwfl, int count) {
	    this.dwfl = dwfl;
	    this.count = count;
	}
    }

    /**
     * Map from a Task to its most recent Dwfl object.
     */
    private static WeakHashMap modMap = new WeakHashMap();

    /**
     * Cache of all Dwfl objects.
     */
    private static WeakHashMap allDwfls = new WeakHashMap();

/**
 * Given: a mock generated /sys/root/dir/usr/bin/program 
 *			   /sys/root/dir/usr/lib/debug/usr/bin/program.debug
 * Elfutils can be given an absolute or relative path to look for program.debug.
 * It will look in: 
 * 1. /sys/root/dir/usr/bin/program.debug 
 * 2. /sys/root/dir/usr/bin/a/relative/path/program.debug 
 * 3. /an/absolute/path/sys/root/dir/usr/bin/program.debug
 * It would be helpful if it also looked in /an/absolute/path/program.debug 
 * so it could be given /sys/root/dir/usr/lib/debug/usr/bin.  Lacking that we
 * need to generate a relative path that has the same effect.
 * XXX: Change to use java.io.File?
 *
 * @param pathname of executable
 * @return a path where elfutils can find program.debug for separate debuginfo.
 */

    private static String getSysRoot(String execPathParm) {
        String sysRoot = DbgVariables.getStringValue("SYSROOT");
	if (sysRoot.length() == 0)
	  return "/usr/lib/debug";
	StringBuffer execPath = new StringBuffer(execPathParm);
	StringBuffer relSysRoot = new StringBuffer("/..");
	execPath.replace(0, sysRoot.length(), "");
	int slashidx = execPath.lastIndexOf("/");
	execPath.replace(slashidx, execPath.length(),"");
	String nonSysRootPath = new String(execPath.toString());
	slashidx = execPath.lastIndexOf("/");
	while (slashidx >= 0) {
	    while (execPath.substring(slashidx).compareTo("/..") == 0) {
		execPath.replace(slashidx, execPath.length(), "");
		slashidx = execPath.lastIndexOf("/");
		execPath.replace(slashidx, execPath.length(), "");
		slashidx = execPath.lastIndexOf("/");
	    }
	    execPath.replace(slashidx, execPath.length(), "");
	    slashidx = execPath.lastIndexOf("/");
	    if (slashidx != 0)
		relSysRoot.append("/..");
	}
	relSysRoot.deleteCharAt(0);
	relSysRoot.append("/usr/lib/debug/");
	relSysRoot.append(nonSysRootPath);
	return relSysRoot.toString();
    }
    
    /**
     * return a Dwfl for a {@link frysk.proc.Task}.
     * 
     * @param task the given {@link frysk.proc.Task}.
     * @return a Dwfl created using the tasks maps.
     */
    public static Dwfl getDwfl(Task task) {
	logger.log(Level.FINE, "entering createDwfl, task: {0}\n", task);

	// If there is no dwfl for this task create one.
	String relativeSysRoot = getSysRoot(task.getProc().getExe());
	if (!modMap.containsKey(task)) {
	    logger.log(Level.FINEST, "creating new dwfl for task {0}\n", task);
	    Dwfl dwfl = new Dwfl(relativeSysRoot);
	    DwflFactory.updateDwfl(dwfl, task);
	    Mod mod = new Mod(dwfl, task.getMod());
	    modMap.put(task, mod);

	    // For cleanup, also save dwfl using Mod as a key (just need a
	    // unique key).
	    allDwfls.put(mod, dwfl);
	}

	Mod mod = (Mod) modMap.get(task);

	// If a dwfl doesn't match the Tasks mod count, update it.
	if (mod.count != task.getMod()) {
	    logger.log(Level.FINEST, "existing dwfl out-of-date\n");
	    DwflFactory.updateDwfl(mod.dwfl, task);
	    mod.count = task.getMod();
	}

	logger.log(Level.FINER, "returning existing dwfl {0}\n", mod.dwfl);
	return mod.dwfl;
    }

    public static void clear() {
	modMap.clear();
	for (Iterator i = allDwfls.values().iterator(); i.hasNext();) {
	    Dwfl d = (Dwfl) i.next();
	    d.close();
	    i.remove();
	}
    }
}
