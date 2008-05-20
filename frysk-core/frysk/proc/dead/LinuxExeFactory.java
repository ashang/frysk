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

package frysk.proc.dead;

import java.io.File;
import java.util.List;
import java.util.LinkedList;
import lib.dwfl.Elf;
import lib.dwfl.ElfException;
import lib.dwfl.ElfFileException;
import lib.dwfl.ElfCommand;
import lib.dwfl.ElfEHeader;
import frysk.proc.MemoryMap;
import frysk.rsl.Log;
import frysk.solib.SOLibMapBuilder;
import frysk.sysroot.SysRoot;
import frysk.sysroot.SysRootCache;
import frysk.sysroot.SysRootFile;

public class LinuxExeFactory {
    private static final Log fine = Log.fine(LinuxExeFactory.class);

    /**
     * Attempt to create an elf proc, throw an exception if it fails.
     */
    private static DeadProc createElfProc(final File exeFile, String[] args) {
	Elf exeElf = null;
	final SysRootFile sysRootFile = new SysRootFile(SysRootCache.getSysRoot(exeFile.getName()), 
		exeFile);
	try {
	    exeElf = new Elf(sysRootFile.getSysRootedFile(), ElfCommand.ELF_C_READ);
	    ElfEHeader eHeader = exeElf.getEHeader();
	    class BuildExeMaps extends SOLibMapBuilder {
		private final List metaData = new LinkedList();
		public void buildMap(long addrLow, long addrHigh, boolean permRead,
				     boolean permWrite, boolean permExecute,
				     long offset, String name, long align) {
		    metaData.add(new MemoryMap(addrLow, addrHigh, permRead,
					       permWrite, permExecute, false,
					       offset, -1, -1, -1,
					       sysRootFile.getSysRootedPath()));
		}
		MemoryMap[] getMemoryMaps() {
		    MemoryMap[] memoryMaps = new MemoryMap[metaData.size()];
		    metaData.toArray(memoryMaps);
		    return memoryMaps;
		}
	    }
	    BuildExeMaps SOMaps = new BuildExeMaps();
	    // Add in case for executables maps.
	    SOMaps.construct(sysRootFile.getSysRootedFile(), 0);
	    
	    LinuxExeHost host
		= new LinuxExeHost(sysRootFile, eHeader, SOMaps.getMemoryMaps(), args);
	    return host.getProc();
	} catch (ElfFileException e) {
	    // File I/O is just bad; re-throw (need to catch it as
	    // ElfFileException is a sub-class of ElfException).
	    throw e;
	} catch (ElfException e) {
	    // Bad elf is ok; anything else is not.
	    return null;
	} finally {
	    if (exeElf != null)
		exeElf.close();
	}
    }

    /**
     * Attempt to create an interpreter proc (for instance for a script).
     */
    private static DeadProc createInterpreterProc(File exeFile, String[] args) {
	String[] interpreterArgs = InterpreterFactory.parse(exeFile, args);
	if (interpreterArgs == null)
	    return null;
	fine.log("createInterpProc", interpreterArgs);
	// FIXME: This is bogus; needs to find the interpreter file
	// within the context of the sysroot; that means passing in
	// the SysRoot as a parameter.
	return createElfProc(new File(interpreterArgs[0]), interpreterArgs);
    }

    public static DeadProc createProc(File exeFile, String[] args) {
	fine.log("createProc exe", exeFile, "args", args);
	DeadProc proc;
	proc = createElfProc(exeFile, args);
	if (proc != null)
	    return proc;
	proc = createInterpreterProc(exeFile, args);
	if (proc != null)
	    return proc;
	throw new RuntimeException("Not an executable: " + exeFile);
    }

    public static DeadProc createProc(String[] args) {
	fine.log("createProc args", args);
	SysRoot sysRoot = new SysRoot(SysRootCache.getSysRoot(args[0]));
	File exe = sysRoot.getPathViaSysRoot(args[0]).getFile();
	return createProc(exe, args);
    }
    
    public static DeadProc createProc(String[] args, String sysroot) {
	fine.log("createProc args", args, "sysroot", sysroot);
	SysRoot sysRoot = new SysRoot(sysroot);
	File exe = sysRoot.getPathViaSysRoot(args[0]).getFile();
	return createProc(exe, args);
    }
}
