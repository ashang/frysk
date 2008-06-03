// This file is part of the program FRYSK.
//
// Copyright 2005, 2007, 2008, Red Hat Inc.
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

package lib.dwfl;

import java.util.LinkedList;
import frysk.rsl.Log;

public class Dwfl {
    private static final Log fine = Log.fine(Dwfl.class);

    private long pointer;

    private DwflModule[] modules;

    protected final DwarfDieFactory factory = DwarfDieFactory.getFactory();
  
    public Dwfl(String sysroot) {
	pointer = dwflBegin(sysroot);
    }

    /**
     * Get all the DwflModule objects associated with this Dwfl. Use a
     * cached array if possible.
     *
     * @return an array of DwflModule.
     */
    public DwflModule[] getModules() {
	if (modules == null) {
	    dwfl_getmodules();
	}
	return modules;
    }

    /**
     * Get new all the DwflModule objects associated with this
     * Dwfl. This requests a new array from libdwfl and stores it as
     * the cached list for return by getModules.
     *
     * @return an array of DwflModule.
     */
    public DwflModule[] getModulesForce()    {
	dwfl_getmodules();
	return modules;
    }
  
    /**
     * Get the DwflModule associated with an address.
     *
     * @return The module
     */
    public native DwflModule getModule(long addr);
    
    public DwflModule getCompliationUnitModule(DwarfDie cuDie){
	return new DwflModule(dwfl_cumodule(cuDie.getPointer()), this);
    }
    
    private native long dwfl_cumodule(long cuDie);
    
    public DwflLine getSourceLine (long addr) {
	long val = 0;
	try {
	    val = dwfl_getsrc(addr);
	} catch (NullPointerException npe) {
	    System.out.println(npe.getMessage());
	    val = 0;
	}
	if (val == 0)
	    return null;
	return new DwflLine(val, this);
    }

    public DwflDieBias getCompilationUnit (long addr) {
	return dwfl_addrdie(addr);
    }

    long getPointer () {
	return pointer;
    }

    protected void finalize () {
	close();
    }

    /**
     * Get all the DwflLine objects associated with a line in a source file.
     */
    public LinkedList getLineAddresses(String fileName, int lineNo,
				       int column) {
	DwflModule[] modules = getModules();
	if (modules == null) {
	    return null;
	}
	LinkedList list = new LinkedList();
	for (int i = 0; i < modules.length; i++) {
	    DwflModule mod = modules[i];
	    DwflLine[] lines = mod.getLines(fileName, lineNo, column);
	    
	    if (lines != null) {
		for (int j = 0; j < lines.length; j++) {
		    list.add(lines[j]);
		}
	    }
	}
	return list;
    }
  
    /**
     * Test to see if the requested line number is executable.
     */
    public boolean isLineExecutable (String fileName, int lineNo,
				     int column) {
	DwflModule[] modules = getModules();
	if (modules == null)
	    return false;
    
	for (int i = 0; i < modules.length; i++) {
	    DwflModule mod = modules[i];
	    DwflLine[] lines = mod.getLines(fileName, lineNo, column);
	    if (lines != null) {
		return true;
	    }
	}
	return false;
    }

    public DwarfDieFactory getFactory() {
	return factory;
    }
  
    public void close() {
	if (this.pointer != 0) {
	    dwfl_end();
	    this.pointer = 0;
	}
    }
  
    private static native long dwflBegin (String s);
  
    /**
     * Start a refresh of the address map.
     */
    public void reportBegin() {
	fine.log(this, "reportBegin");
	reportBegin(pointer);
    }
    private static native void reportBegin(long pointer);
    /**
     * Finish a refresh of the address map.
     */
    public void reportEnd() {
	fine.log(this, "reportEnd");
	reportEnd(pointer);
    }
    private static native void reportEnd(long pointer);
    /**
     * Report a mapped component.
     */
    public void reportModule(String moduleName, long low, long high) {
	fine.log(this, "reportModule", moduleName, "low", low, "high", high);
	reportModule(pointer, moduleName, low, high);
    }
    private static native void reportModule(long pointer, String moduleName,
					    long low, long high);


    private String name;
    private long low;
    private long high;
    private int inode;
    private int devMajor;
    private int devMinor;
    private long vdso;
    /**
     * Start refreshing the address map using raw information
     * extracted from /proc/pid/maps.
     */
    public void mapBegin(long vdso) {
	reportBegin();
	this.vdso = vdso;
	name = null;
    }
    /**
     * Report a single raw line from /proc/pid/maps.
     */
    public void mapModule(String name, long low, long high,
			  int inode, int devMajor, int devMinor) {
	if (this.name != null && this.name.equals(name)
	    && this.inode == inode
	    && this.devMajor == devMajor
	    && this.devMinor == devMinor) {
	    // A repeat of a previous map (but with more addresses)
	    // extend the address range.
	    this.high = high;
	} else {
	    if (this.name != null) {
		// There's a previous map, report and flush it.
		reportModule(this.name, this.low, this.high);
		this.name = null;
	    }
	    if (name.equals("")
		|| (inode == 0 && devMajor == 0 && devMinor == 0)) {
		// An empty map, do nothing.
	    } else if (this.vdso == low) {
		// A vdso, report it immediatly.
		reportModule(name, low, high);
	    } else {
		// A new map, save it.
		this.name = name;
		this.low = low;
		this.high = high;
		this.inode = inode;
		this.devMajor = devMajor;
		this.devMinor = devMinor;
	    }
	}
    }
    /**
     * Finish reporting a raw address map.
     */
    public void mapEnd() {
	if (this.name != null) {
	    // Report any dangling old map.
	    reportModule(this.name, this.low, this.high);
	    this.name = null;
	}
	reportEnd();
    }

    protected native void dwfl_end ();

    // protected native long[] dwfl_get_modules();
    // protected native long[] dwfl_getdwarf();
    protected native long dwfl_getsrc (long addr);
  
    protected native DwflDieBias dwfl_addrdie (long addr);
  
    protected native long dwfl_addrmodule (long addr);

    protected native void dwfl_getmodules();
}
