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
     * Refresh an existing dwfl.  Package private, use
     * DwflCache.getDwfl().
     */
    static Dwfl updateDwfl(Dwfl dwfl, Task task) {
	Proc proc = task.getProc();
	MemoryMap[] maps = proc.getMaps();
	dwfl.mapBegin();
	for (int i = 0; i < maps.length; i++) {
	    MemoryMap map = maps[i];
	    dwfl.mapModule(map.name, map.addressLow, map.addressHigh,
			   map.devMajor, map.devMinor, map.inode);
	}
	dwfl.mapEnd();
	fine.log("updateDwfl main task", proc.getMainTask(),
		 "memory", proc.getMainTask().getMemory());
	return dwfl;
    }
}
