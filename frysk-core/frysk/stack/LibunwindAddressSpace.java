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

package frysk.stack;

import frysk.isa.registers.Register;
import lib.unwind.Unwind;
import lib.unwind.UnwindX8664;
import lib.unwind.UnwindX86;
import lib.unwind.UnwindPPC32;
import lib.unwind.UnwindPPC64;
import frysk.dwfl.DwflFactory;
import frysk.isa.ISA;
import frysk.proc.MemoryMap;
import frysk.proc.Task;
import frysk.rsl.Log;
import lib.unwind.AddressSpace;
import lib.unwind.ByteOrder;
import lib.unwind.ProcInfo;
import frysk.isa.registers.RegisterMap;

class LibunwindAddressSpace extends AddressSpace {
    private static final Log fine = Log.fine(LibunwindAddressSpace.class);

    private final Task task;
    private final ISA isa;
    private final RegisterMap registerMap;

    static private Unwind unwinder(ISA isa) {
	// FIXME: Call UnwindFactory.singleton(task.getISA())!
	if (isa == ISA.IA32)
	    return new UnwindX86();
	else if (isa == ISA.X8664)
	    return new UnwindX8664();
	else if (isa == ISA.PPC32BE) 
	    return new UnwindPPC32();
	else if (isa == ISA.PPC64BE)
	    return new UnwindPPC64();
	else
	    throw new RuntimeException("unhandled ISA: " + isa);
    }

    LibunwindAddressSpace(Task task, ByteOrder byteOrder) {
	super(unwinder(task.getISA()),
	      // FIXME: Do something useful with the byteOrder!
	      lib.unwind.ByteOrder.DEFAULT);
	this.task = task;
	this.isa = task.getISA();
	this.registerMap = LibunwindRegisterMapFactory.getRegisterMap(isa);
    }

    public int accessMem (long addr, byte[] valp, boolean write) {
	fine.log(this, "accessMem address", addr, "length", valp.length);
	task.getMemory().get(addr, valp, 0, valp.length);
	return 0;
    }

    public int accessReg (Number regnum, byte[] fpvalp, boolean write) {
	Register register = registerMap.getRegister(regnum);
	int length;
	// Truncate transfer to size-of-register.
	if (fpvalp.length > register.getType().getSize())
	    length = register.getType().getSize();
	else
	    length = fpvalp.length;
	fine.log(this, "accessReg", regnum, "register", register,
		 "length", length);
	task.access(register, 0, length, fpvalp, 0, write);
	return 0;
    }

    public long getReg(Number regnum) {
	Register register = registerMap.getRegister(regnum);
	fine.log(this, "getReg", regnum, "register", register);
	long val = task.getRegister(register);
	fine.log(this, "read value", val);
	return val;
    }

    public void setReg(Number regnum, long regval) {
	Register register = registerMap.getRegister(regnum);
	fine.log(this, "setReg", regnum, "register", register, "value", regval);
	task.setRegister(register, regval);
    }

    public int findProcInfo(long ip, boolean needUnwindInfo,
			    ProcInfo procInfo) {
	fine.log(this, "findProcInfo ip", ip, "needUnwindInfo", needUnwindInfo);
	MemoryMap map = task.getProc().getMap(ip);
	int ret;
	if (map == null) {
	    fine.log(this, "Couldn't find memory map");
	    return procInfo.fillNotAvailable();
	} else if (DwflFactory.isVDSO(task.getProc(), map)) {
	    fine.log(this, "Filling from VDSO " +
		     "low", map.addressLow,
		     "high", map.addressHigh,
		     "offset", map.offset);
	    ret = procInfo.fillFromVDSO(this, map.addressLow,
					map.addressHigh, map.offset,
					ip, needUnwindInfo);
	} else {
	    fine.log(this, "Filling from file " +
		     "name", map.name,
		     "low", map.addressLow,
		     "high", map.addressHigh,
		     "offset", map.offset);
	    ret = procInfo.fillFromElfImage(this, map.name,
					    map.addressLow,
					    map.addressHigh, map.offset,
					    ip, needUnwindInfo);
	}
	return ret;
    }

    public void putUnwindInfo (final ProcInfo procInfo) {
    }
}
