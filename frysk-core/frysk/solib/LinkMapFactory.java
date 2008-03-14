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

import java.util.LinkedList;
import lib.dwfl.Elf;
import lib.dwfl.ElfSection;
import lib.dwfl.ElfData;
import lib.dwfl.ElfEHeader;
import lib.dwfl.ElfPHeader;
import frysk.rsl.Log;
import frysk.proc.Auxv;
import inua.eio.ByteBuffer;
import frysk.proc.Proc;
import java.io.File;
import lib.dwfl.ElfCommand;

/**
 * Extract from a core file all the information needed to construct
 * and maintain a corefile Host, Proc, and Task.  The Elf objects used
 * to extract the information are all closed.
 */

public class LinkMapFactory {
    private static final Log fine = Log.fine(LinkMapFactory.class);

    static LinkMap[] extractLinkMaps(Proc proc) {
	File exeFile = new File(proc.getExe());
	return extractLinkMaps(new Elf(exeFile, ElfCommand.ELF_C_READ),
			       exeFile, proc.getMainTask().getMemory(),
			       proc.getAuxv());
    }

    public static LinkMap[] extractLinkMaps(Elf exeElf, File exeFile,
					    ByteBuffer memory, Auxv[] auxv) {
	fine.log("extractLinkMaps elf", exeElf, "memory", memory,
		 "auxv", auxv);

	// Find where the DynamicSegment was loaded into memory.
	DynamicSegment dynamicSegment = new DynamicSegment(auxv, exeElf);

	// From that segment address, find linkmap table.
	long linkmapAddress = getLinkMapAddress(memory, dynamicSegment);

	// No link map, hence no libraries have so far been loaded
	// (core was taken while the program was at the entry point or
	// shortly after).  This is ok, the executable is still being
	// used for things like the exe path.
	if (linkmapAddress == 0)
	    return null;

	// Edge case: Save interp name as it is not included the
	// linkmap as it is loaded by the kernel.
	String interpName = getExeInterpreterName(exeElf);

	// Edge case: Get interp address so when we traverse the
	// linkmap it can be paired with its name
	long interpAddr = getExeInterpreterAddress(exeElf); 

	// Edge case: find the map corresponding to the vdso.
	long vdsoAddr = getVdsoAddress(auxv);

	// Build the link-map table from the link-map table address
	LinkedList maps = new LinkedList();
	long linkStep = linkmapAddress;
	while (linkStep != 0) {
	    memory.position(linkStep);
	    long l_addr = memory.getUWord();
	    long s_addr = memory.getUWord();
	    long l_dyn = memory.getUWord();
	    String name = getString(s_addr, memory);
	    linkStep = memory.getUWord();
	    if (s_addr == interpAddr) {
		fine.log("Found interpretator at linkmap address", s_addr);
		name = interpName;
	    } else if ((l_dyn & -0x1000) == vdsoAddr) {
		name = "[vdso]";
	    } else if (maps.size() == 0) {
		// entry 0 is executable
		name = exeFile.getPath();
	    }
	    LinkMap map = new LinkMap(l_addr, l_dyn, s_addr, name);
	    fine.log("add map", map);
	    maps.add(map);
	}

	// Return the maps as an array.
	LinkMap[] linkMaps = new LinkMap[maps.size()];
	maps.toArray(linkMaps);
	return linkMaps;
    }

    /**
     * Build an address map covering [addressLow,addressHigh) with
     * permissions {permR, permW, permX, permP }, device devMajor
     * devMinor, inode, and the pathname's offset/length within the
     * buf.
     *
     * !shared implies private, they are mutually exclusive.
     */

    private static String getString(long startAddr, ByteBuffer buffer) {
	StringBuffer stringBuffer = new StringBuffer();
	byte in = -1;
	long currentAddr = startAddr;

	while (in != 0) {
	    // Read until end of buffer or null
	    try {
		in = buffer.getByte(currentAddr);
	    } catch (RuntimeException e) {
		break;
	    }

	    if (in == 0)
		break;

	    stringBuffer.append((char) in);
	    currentAddr++;
	}

	return stringBuffer.toString();
    }

    /**
     * Helper function to locate and report the backing corefile's
     * VDSO address
     */
    private static long getVdsoAddress(Auxv[] auxv) {
	fine.log("getVdsoAddress", auxv);
	long vdsoEntryPoint = 0;
	// Find the SYSINFO_EHDR data
	for (int i = 0; i < auxv.length; i++) {
	    if (auxv[i].type == inua.elf.AT.SYSINFO_EHDR) {
		vdsoEntryPoint = auxv[i].val;
		break;
	    }
	}
	fine.log("VDSO Address", vdsoEntryPoint);
	return vdsoEntryPoint;
    }

    /**
     * Helper function to locate the link map table in the 
     * core file. This is located in the dynamic segment table
     * at the address specified by the DT_DEBUG field.
     */
    private static long getLinkMapAddress(ByteBuffer memory,
					  DynamicSegment dynamicSegment) {
	fine.log("getLinkMapAddress");
	final int DT_DEBUG = 21;
	long dynSegmentEndAddress = dynamicSegment.addr + dynamicSegment.size;
	long dtDebugAddress = 0;
	long actualAddress = 0;
	long dtTest;

	// Position memory at the dynamic segment.
	memory.position(dynamicSegment.addr);

	// Find DT_DEBUG field in table. The table is two words.  One
	// is the DT_ tag and the other is the address of that DT_ tag
	// table type.
	while (memory.position() < dynSegmentEndAddress) {
	    // Get tag and test if it is DT_DEBUG
	    dtTest = memory.getUWord();
	    if (dtTest == DT_DEBUG) {
		// If it is record the address in the 
		// next word.
		dtDebugAddress = memory.getUWord();
		break;
	    }
	    // Otherwise, move on.
	    memory.getUWord();
	}

	if (dtDebugAddress != 0) {
	    // Go to address that DT_DEBUG tag 
	    // specified.
	    memory.position(dtDebugAddress);

	    // discard first word at that address;
	    memory.getInt();
	    long pos = memory.position();
	    int wordSize = memory.wordSize();
	    if (pos % wordSize > 0)
		pos = (pos - (pos % wordSize))+wordSize;
	
	    memory.position(pos);
	    actualAddress = memory.getUWord();
	}

	fine.log("Linkmap address is", actualAddress);
	return actualAddress;
    }

    /**
     * Helper function to locate and report the backing Executables
     * interpeters address.
     */
    private static long getExeInterpreterAddress(Elf exeElf) {
	fine.log("getExeInterpreterAddress");
	long interpreterAddress = 0;
	ElfEHeader eHeader = exeElf.getEHeader();
	// Find .interp segment by passing through progream segment
	// header
	for (int headerCount = 0; headerCount < eHeader.phnum; headerCount++) {
	    ElfPHeader pHeader = exeElf.getPHeader(headerCount);
	    if (pHeader.type == ElfPHeader.PTYPE_INTERP) {
		interpreterAddress = pHeader.vaddr;
		break;
	    }
	}
	fine.log("Interpreter Addr", interpreterAddress);
	return interpreterAddress;
    }

    /**
     * Helper function to locate and report the backing Executables
     * interpeters name
     */
    private static String getExeInterpreterName(Elf exeElf) {
	fine.log("getExeInterpreterName");
	String interpName = "";
	ElfEHeader eHeader = exeElf.getEHeader();
	// Find .interp segment by passing through progream segment
	// header
	for (int headerCount = 0; headerCount < eHeader.phnum; headerCount++) {
	    ElfPHeader pHeader = exeElf.getPHeader(headerCount);
	    if (pHeader.type == ElfPHeader.PTYPE_INTERP) {
		ElfSection interpSection = exeElf.getSection((long)headerCount);
		ElfData data = interpSection.getData();
		interpName = new String(data.getBytes());
		interpName = interpName.trim();
		break;
	    }
	}
	fine.log("Interpreter name", interpName);
	return interpName;
    }
}
