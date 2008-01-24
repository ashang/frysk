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
import java.util.logging.Level;
import java.util.logging.Logger;
import lib.unwind.AddressSpace;
import lib.unwind.ByteOrder;
import lib.unwind.ElfImage;
import lib.unwind.ProcInfo;
import frysk.isa.registers.RegisterMap;

class LibunwindAddressSpace extends AddressSpace {

    private static final Logger logger = Logger.getLogger("frysk");
    private final Task task;
    private final ISA isa;
    private final RegisterMap registerMap;

    // procInfo is a wrapper for a RawDataManaged object, keep a reference
    // to it for as long as needed.
    ProcInfo procInfo;


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
	logger.log(Level.FINEST, "accessMem address {0} length: {1}\n",
		   new Object[] {
		       Long.toHexString(addr),
		       new Integer(valp.length)
		   });
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
	logger.log(Level.FINE, "{0}: accessReg {1} ({2}), {3} bytes\n",
		   new Object[] { this, regnum, register,
				  new Integer(length) });
	task.access(register, 0, length, fpvalp, 0, write);
	return 0;
    }

    public long getReg(Number regnum) {
	Register register = registerMap.getRegister(regnum);
	logger.log(Level.FINE, "{0}: getReg {1} ({2})",
		   new Object[] { this, regnum, register });
	long val = task.getRegister(register);
	logger.log(Level.FINE, "read value: 0x{0}\n",
		   Long.toHexString(val));
	return val;
    }

    public void setReg(Number regnum, long regval) {
	Register register = registerMap.getRegister(regnum);
	logger.log(Level.FINE, "{0}: setReg {1} ({2}), val {3}",
		   new Object[] { this, regnum, register, new Long(regval) });
	task.setRegister(register, regval);
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

    public void putUnwindInfo (final ProcInfo procInfo) {
	// No longer need to hold procInfo.
	this.procInfo = null;
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
