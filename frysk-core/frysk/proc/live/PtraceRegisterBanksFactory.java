// This file is part of the program FRYSK.
//
// Copyright 2006, 2007, Red Hat Inc.
// Copyright 2006, IBM Corp.
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

package frysk.proc.live;

import inua.eio.ByteBuffer;
import inua.eio.ByteOrder;
import frysk.isa.ISA;
import frysk.sys.Ptrace.RegisterSet;
import frysk.sys.Ptrace.AddressSpace;
import frysk.proc.RegisterBanks;
import frysk.proc.X86BankRegisters;
import frysk.proc.PPCBankRegisters;
import frysk.Config;

/**
 * The target has registers scattered across one or more register
 * banks.  Map register requests onto the corresponding bank-register.
 */

class PtraceRegisterBanksFactory {

    private static ByteBuffer[] x8664Banks(int pid) {
	ByteBuffer[] bankBuffers = new ByteBuffer[] {
	    new RegisterSetByteBuffer(pid, RegisterSet.REGS),
	    new RegisterSetByteBuffer(pid, RegisterSet.FPREGS),
	    new AddressSpaceByteBuffer(pid, AddressSpace.USR)
	};
	for (int i = 0; i < bankBuffers.length; i++) {
	    bankBuffers[i].order(ByteOrder.LITTLE_ENDIAN);
	}
	return bankBuffers;
    }

    private static ByteBuffer[] ia32Banks(int pid) {
	ByteBuffer[] bankBuffers = new ByteBuffer[] {
	    new RegisterSetByteBuffer(pid, RegisterSet.REGS),
	    new RegisterSetByteBuffer(pid, RegisterSet.FPREGS),
	    new RegisterSetByteBuffer(pid, RegisterSet.FPXREGS),
	    new AddressSpaceByteBuffer(pid, AddressSpace.USR)
	};
	for (int i = 0; i < bankBuffers.length; i++) {
	    bankBuffers[i].order(ByteOrder.LITTLE_ENDIAN);
	}
	return bankBuffers;
    }

    private static ByteBuffer[] ppcBanksBE(int pid) {
	ByteBuffer[] bankBuffers = new ByteBuffer[] {
            new AddressSpaceByteBuffer(pid, AddressSpace.USR)
        };

	for (int i = 0; i < bankBuffers.length; i++) {
            bankBuffers[i].order(ByteOrder.BIG_ENDIAN);
        }	
	return bankBuffers;
    }

    static RegisterBanks create(ISA isa, int pid) {
	if (isa == ISA.X8664) {
	    return new RegisterBanks(X86BankRegisters.X8664,
				     x8664Banks(pid));
	} else if (isa == ISA.IA32) {
	    if (Config.getTargetCpuXXX().equals ("x86_64")) 
		return new RegisterBanks(X86BankRegisters.IA32_ON_X8664,
					 x8664Banks(pid));
	    else
		return new RegisterBanks(X86BankRegisters.IA32,
					 ia32Banks(pid));
	} else if (isa == ISA.PPC64BE) {
	    return new RegisterBanks(PPCBankRegisters.PPC64BE,
				     ppcBanksBE(pid));
	} else if (isa == ISA.PPC32BE) {
	    if (Config.getTargetCpuXXX().equals("powerpc64"))
		return new RegisterBanks(PPCBankRegisters.PPC32BE_ON_PPC64BE,
					 ppcBanksBE(pid));
	    else
		return new RegisterBanks(PPCBankRegisters.PPC32BE,
					 ppcBanksBE(pid));
	} else {
	    throw new RuntimeException("unhandled isa: " + isa);
	}
    }
    
}
