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

package frysk.stack;

import frysk.isa.Register;
import frysk.proc.BankRegister;
import lib.unwind.Unwind;
import lib.unwind.UnwindX8664;
import lib.unwind.UnwindX86;
import frysk.dwfl.DwflCache;
import frysk.dwfl.DwflFactory;
import frysk.event.Event;
import frysk.proc.Isa;
import frysk.proc.Manager;
import frysk.proc.MemoryMap;
import frysk.proc.Task;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import lib.dwfl.Dwfl;
import lib.dwfl.DwflModule;
import lib.dwfl.SymbolBuilder;
import lib.unwind.AddressSpace;
import lib.unwind.ByteOrder;
import lib.unwind.Cursor;
import lib.unwind.ElfImage;
import lib.unwind.ProcInfo;
import lib.unwind.ProcName;
import frysk.isa.RegisterMap;

class LibunwindAddressSpace extends AddressSpace {

    private static final Logger logger = Logger.getLogger("frysk");
    private final Task task;
    private final Isa isa;
    private final RegisterMap registerMap;

    // procInfo is a wrapper for a RawDataManaged object, keep a reference
    // to it for as long as needed.
    ProcInfo procInfo;


    static private Unwind unwinder(Task task) {
	// FIXME: Call UnwindFactory.singleton(task.getIsa())!
	if (task.getIsa().getWordSize() == 4)
	    return new UnwindX86();
	else
	    return new UnwindX8664();
    }

    LibunwindAddressSpace(Task task, ByteOrder byteOrder) {
	super(unwinder(task),
	      // FIXME: Do something useful with the byteOrder!
	      lib.unwind.ByteOrder.DEFAULT);
	this.task = task;
	this.isa = task.getIsa();
	this.registerMap = LibunwindRegisterMapFactory.getRegisterMap(isa);
    }

    public int accessMem (long addr, byte[] valp, boolean write) {
	logger.log(Level.FINEST, "accessMem address {0} length: {1}\n",
		   new Object[] {
		       Long.toHexString(addr),
		       new Integer(valp.length)
		   });
	task.getMemory().get(addr, valp, 0, valp.length);
	return 0;
    }

    public int accessFPReg (int regnum, byte[] fpvalp, boolean write)
    {
	// XXX: TODO.
	return 0;
    }

    private BankRegister getProcRegister(int regnum) {
	Register reg = registerMap.getRegister(regnum);
	if (reg == null)
	    throw new RuntimeException("unknown libunwind register: "
				       + regnum);
	BankRegister bankReg = isa.getRegisterByName(reg.name);
	if (bankReg == null)
	    throw new RuntimeException("unknown proc register: "
				       + reg.name);
	return bankReg;
    }

    public long getReg(int regnum) {
	logger.log(Level.FINE, "reading from regnum: {1}\n",
		   new Long(regnum));
	BankRegister r = getProcRegister(regnum);
	long val = r.get(task);
	logger.log(Level.FINE, "accessReg: read value: 0x{0}\n",
		   Long.toHexString(val));
	return val;
    }

    public void setReg(int regnum, long regval) {
	logger.log(Level.FINE, "writing to regnum: {1}, val: {2}\n",
		   new Object[] {
		       new Long(regnum),
		       new Long(regval)
		   });
	BankRegister r = getProcRegister(regnum);
	r.put(task, regval);
    }

    public ProcInfo findProcInfo (long ip, boolean needUnwindInfo) {
	logger.log(Level.FINE, "Entering findProcInfo, ip: {0}\n",
		   Long.toHexString(ip));
	ElfImage elfImage = getElfImage(ip);
	logger.log(Level.FINEST, "Obtained elfImage: {0}\n", elfImage);
	procInfo = getUnwinder()
	    .createProcInfoFromElfImage(this, ip, needUnwindInfo, elfImage);
	logger.log(Level.FINE, "post procInfo {0}\n", procInfo);
	return procInfo;
    }

    public int getDynInfoListAddr (byte[] dilap) {
	//XXX: Todo.
	Arrays.fill(dilap, (byte) 0);
	return - lib.unwind.Error.UNW_ENOINFO_;
    }

    private DwflModule getModuleFromAddress (long addr) {
	logger.log(Level.FINE, "Looking for addr: 0x{0}\n",
		   Long.toHexString(addr));
	Dwfl dwfl = null;
	dwfl = DwflCache.getDwfl(task);
	logger.log(Level.FINEST, "got dwfl: {0}\n", dwfl);
	if (dwfl == null) {
	    logger.log(Level.FINE, "Dwfl was null\n");
	    return null;
	}
	return dwfl.getModule(addr);
    }

    public ProcName getProcName (long addr, int maxNameSize) {
	logger.log(Level.FINE,
		   "entering getProcName addr: {0}, maxNameSize: {1}\n",
		   new Object[] {
		       Long.toHexString(addr),
		       new Integer(maxNameSize)
		   });
	// Need to tell ptrace thread to perform the getProcName operation.
	class ExecuteGetProcName 
	    implements Event, SymbolBuilder
	{
	    ProcName procName;
	    long addr;
      
	    ExecuteGetProcName (long addr) {
		this.addr = addr;
	    }
      
	    public void symbol (String name, long value, long size, int type,
				int bind, int visibility) {
		procName = new ProcName(addr-value, name);
	    }
      
	    public void execute () {
		DwflModule dwflModule = getModuleFromAddress(addr);
		logger.log(Level.FINEST, "got dwflModule: {0}\n", dwflModule);
		if (dwflModule != null) {
		    dwflModule.getSymbol(addr, this);
		    logger.log(Level.FINE, "ProcName is: {0}\n", procName);
		}
		if (procName == null)
		    procName = new ProcName(- lib.unwind.Error.UNW_EUNSPEC_);
	    }
	}
	ExecuteGetProcName executer = new ExecuteGetProcName(addr);
	Manager.eventLoop.execute(executer);
	logger.log(Level.FINE, "exiting getProcName, returning: {0}\n",
		   executer.procName);
	return executer.procName;
    }

    public void putUnwindInfo (final ProcInfo procInfo) {
	// No longer need to hold procInfo.
	this.procInfo = null;
    }

    public int resume (final Cursor cursor) {
	//XXX: Todo.
	return - lib.unwind.Error.UNW_EUNSPEC_;   
    }

    private ElfImage getElfImage (long addr) {
	logger.log(Level.FINE, "{0} Entering getElfImage, addr: 0x{1}\n", 
		   new Object [] {this, Long.toHexString(addr)} );
	ElfImage elfImage = null;
	MemoryMap map = task.getProc().getMap(addr);
    
	if (map == null) {
	    logger.log(Level.FINEST, "Couldn't find memory map.\n");
	    return null;
	}
	if (DwflFactory.isVDSO(task.getProc(), map)) {
	    logger.log(Level.FINEST, "Handling VDSO map\n");
	    elfImage = getUnwinder()
		.createElfImageFromVDSO(this, map.addressLow, 
					map.addressHigh, map.offset);
	} else {
	    logger.log(Level.FINEST, "Handling regular map name: {0}",
		       map.name);
	    elfImage = ElfImage.mapElfImage(map.name, map.addressLow, 
					    map.addressHigh, map.offset);
	}
	logger.log(Level.FINER, "Leaving getElfImage");
	return elfImage;
    }
}
