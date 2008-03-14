// This file is part of the program FRYSK.
//
// Copyright 2007, 2008 Red Hat Inc.
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

package frysk.solib;

import lib.dwfl.Elf;
import lib.dwfl.ElfEHeader;
import lib.dwfl.ElfPHeader;
import frysk.rsl.Log;
import frysk.proc.Auxv;

/**
 * Extract from a core file all the information needed to construct
 * and maintain a corefile Host, Proc, and Task.  The Elf objects used
 * to extract the information are all closed.
 */

class DynamicSegment {
    private static final Log fine = Log.fine(DynamicSegment.class);

    final long addr;
    final long size;

    /**
     * Helper function to return the backing core file's dynamic
     * segment address and size
     */
    DynamicSegment(Auxv[] auxv, Elf exeElf) {
	fine.log("DynamicSegment auxv", auxv, "exe", exeElf);
	// If we do not have an executable, we cannot find the dynamic
	// segment in target memory.
	long auxvEntryPoint = getEntryPoint(auxv);
	long exeEntryPoint = getEntryPoint(exeElf);
	ElfPHeader exeDynamicSegment = getDynamicSegment(exeElf);

	// Calculate relocated segment address, if necessary.
	addr = exeDynamicSegment.vaddr + auxvEntryPoint - exeEntryPoint;
	size = exeDynamicSegment.filesz;
	fine.log("getDynamicSegmentAddress addr", addr, "size", size);
    }

    /**
     * Helper function to find the backing executable's dynamic
     * segment.
     **/
    private static ElfPHeader getDynamicSegment(Elf exeElf) {
    	fine.log("getExeDynamicSegmentAddress");
	ElfEHeader eHeader = exeElf.getEHeader();
	// Find dynamic segment by iterating through program segment
	// headers
	for (int headerCount = 0; headerCount < eHeader.phnum; headerCount++) {
	    ElfPHeader pHeader = exeElf.getPHeader(headerCount);
	    // Found the dynamic section
	    if (pHeader.type == ElfPHeader.PTYPE_DYNAMIC) {
		fine.log("getDynamicSegmentAddress found", pHeader);
		return pHeader;
	    }
	}
	return null;
    }

    /**
     * Helper function to locate and report the backing Executables
     * entry point
     */
    private static long getEntryPoint(Elf exeElf) {
	fine.log("getEntryPoint", exeElf);
	ElfEHeader eHeader = exeElf.getEHeader();
	if (eHeader == null)
	    throw new RuntimeException("executable is not elf");
	fine.log("elf entry-point", eHeader.entry);
	return eHeader.entry;
    }

    /**
     * Helper function to locate and report the AUXV entry point.
     */
    private static long getEntryPoint(Auxv[] auxv) {
	fine.log("getEntryPoint", auxv);
	// Need auxv data
	long entryPoint = 0;
	// Find the Auxv ENTRY data
	for (int i = 0; i < auxv.length; i++) {
	    if (auxv[i].type == inua.elf.AT.ENTRY) {
		entryPoint = auxv[i].val;
		break;
	    }
	}
	fine.log("auxv entry-point", entryPoint);
	return entryPoint;
    }
}
