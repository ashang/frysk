// This file is part of the program FRYSK.
//
// Copyright 2007, 2008, Red Hat Inc.
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

import frysk.proc.Auxv;
import frysk.proc.MemoryMap;
import frysk.proc.Proc;
import frysk.proc.Task;
import frysk.rsl.Log;
import lib.dwfl.Dwfl;
import lib.dwfl.DwflModule;

/**
 * Factory for creating Dwfl objects for Procs and Tasks.
 */

public class DwflFactory {
    private static final Log fine = Log.fine(DwflFactory.class);

    /**
     * Check whether a given {@link frysk.proc.MemoryMap} from a
     * {@link frysk.proc.Proc} refers to the vdso section.
     * 
     * @param proc the {@link frysk.proc.Proc} the map refers to.
     * @param map the Map to check.
     * @return true if map is not null and refers to the vdso section.
     */
    public static boolean isVDSO (Proc proc, MemoryMap map) {
	if (map == null) {
	    return false;
	}
	return VDSOAddressLow(proc) == map.addressLow;
    }

    /**
     * Find the low address of the vdso map section of a given {@link
     * frysk.proc.Proc}.
     * 
     * @param proc the given {@link frysk.proc.Proc}.
     * @return the low address of the vdso map section of proc.
     */
    private static long VDSOAddressLow (Proc proc) {
	Auxv[] auxv = proc.getAuxv();
	if (auxv != null) {
	    for (int i = 0; i < auxv.length; i++) {
		if (auxv[i].type == inua.elf.AT.SYSINFO_EHDR) {
		    return auxv[i].val;
		}
	    }
	}
	fine.log("Couldn't get vdso address");
	return 0;
    }

    /**
     * Check if a given {@link frysk.proc.MemoryMap} does not refer to
     * an elf image.
     * 
     * @param map the given {@link frysk.proc.MemoryMap}.
     * @return true if the map section does not refer to an elf image.
     */
    private static boolean isEmptyMap (MemoryMap map) {
	return map.name.equals("")
	    || (map.inode == 0 && map.devMinor == 0 && map.devMajor == 0);
    }
    
    /**
     * Refresh an existing dwfl. Package private, use DwflCache.getDwfl().
     */
    static Dwfl updateDwfl(Dwfl dwfl, Task task) {
	
	Proc proc = task.getProc();
	MemoryMap[] maps = proc.getMaps();
	dwfl.dwfl_report_begin();

	int count = 0;
	String name = null;
	long low = 0, high = 0, devMinor = 0, devMajor = 0;
	int inode = 0;

	// Creating Dwfl_Modules for each elf image and the vdso section.
	// Condensing elf mappings into a single Dwfl_Module per elf image.

	// Base case:
	// While the map is empty skip

	// XXX: Add an explicit if count == maps.length -1 break. What is the 
	// failure case for not finding a [vdso]?
        while (! isVDSO(proc, maps[count]) && isEmptyMap(maps[count]))
        {
            if (count == (maps.length-1))
                break;
            count++;
        }

	// If map represents the vdso section, report vdso.
	if (isVDSO(proc, maps[count])) {
	    fine.log("Found the vdso!");
	    dwfl.dwfl_report_module(maps[count].name, maps[count].addressLow,
				    maps[count].addressHigh);
	} else {
	    // If map represents an elf mapping store its data..
	    name = maps[count].name;
	    low = maps[count].addressLow;
	    high = maps[count].addressHigh;
	    inode = maps[count].inode;
	    devMinor = maps[count].devMinor;
	    devMajor = maps[count].devMajor;
	}

	// Induction Step:
	while (++count < maps.length) {
	    
	    // if vdso report old (if old), flush old, then report vdso.
	    if (isVDSO(proc, maps[count])) {
		if (name != null)
		    dwfl.dwfl_report_module(name, low, high);
		
		name = null;
		dwfl.dwfl_report_module(maps[count].name, maps[count].addressLow,
					maps[count].addressHigh);
		continue;
	    } else if (isEmptyMap(maps[count])) {
		// if empty, report old (if old), flush old.
		if (name != null)
		    dwfl.dwfl_report_module(name, low, high);
		
		name = null;
		continue;
	    } else if (maps[count].name.equals(name)
		       && maps[count].inode == inode
		       && maps[count].devMinor == devMinor
		       && maps[count].devMajor == devMajor) {
		// if old elf, increase highAddress.
		high = maps[count].addressHigh;
	    } else {
		// if new elf, report old, store new
		if (name != null) {
		    dwfl.dwfl_report_module(name, low, high);
		}
		name = maps[count].name;
		low = maps[count].addressLow;
		high = maps[count].addressHigh;
		inode = maps[count].inode;
		devMinor = maps[count].devMinor;
		devMajor = maps[count].devMajor;
	    }
	}

	// if last is elf, report elf.
	if (! isEmptyMap(maps[maps.length - 1])
	    && ! isVDSO(proc, maps[maps.length - 1])) {
	    dwfl.dwfl_report_module(name, low, high);
	}

	dwfl.dwfl_report_end();
	DwflModule module = dwfl.getModule(VDSOAddressLow(proc));

	fine.log("updateDwfl main task", proc.getMainTask(),
		 "memory", proc.getMainTask().getMemory(),
		 "dwfl module", module);
	// XXX: Should this method instead have this block of memory
	// pre-fetched and passed in?
	if (module != null) {
	    module.setUserData(task.getMemory());
	}

	return dwfl;
    }
}
