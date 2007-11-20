// This file is part of the program FRYSK.
//
// Copyright 2006, 2007, Red Hat Inc.
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

package frysk.proc;

import inua.eio.ByteOrder;
import frysk.isa.IA32Registers;
import frysk.isa.X8664Registers;
import frysk.isa.X87Registers;

/**
 * Factory to create either IA32 or X8664 RegisterBanks.
 */

public class X86BankRegisters {

    public static final BankRegisterMap IA32 = new BankRegisterMap()
	.add(new BankRegister (0, 24, 4,IA32Registers.EAX))
	.add(new BankRegister (0, 0, 4, IA32Registers.EBX))
	.add(new BankRegister (0, 4, 4, IA32Registers.ECX))
	.add(new BankRegister (0, 8, 4, IA32Registers.EDX))
	.add(new BankRegister (0, 12, 4, IA32Registers.ESI))
	.add(new BankRegister (0, 16, 4, IA32Registers.EDI))
	.add(new BankRegister (0, 20, 4, IA32Registers.EBP))
	.add(new BankRegister (0, 52, 4, IA32Registers.CS))
	.add(new BankRegister (0, 28, 4, IA32Registers.DS))
	.add(new BankRegister (0, 32, 4, IA32Registers.ES))
	.add(new BankRegister (0, 36, 4, IA32Registers.FS))
	.add(new BankRegister (0, 40, 4, IA32Registers.GS))
	.add(new BankRegister (0, 64, 4, IA32Registers.SS))
	.add(new BankRegister (0, 44, 4, IA32Registers.ORIG_EAX))
	.add(new BankRegister (0, 48, 4, IA32Registers.EIP))
	.add(new BankRegister (0, 56, 4, IA32Registers.EFLAGS))
	.add(new BankRegister (0, 60, 4, IA32Registers.ESP))
	.add(new BankRegister (1, 0, 4, "cwd"))
	.add(new BankRegister (1, 4, 4, "swd"))
	.add(new BankRegister (1, 8, 4, "twd"))
	.add(new BankRegister (1, 12, 4, IA32Registers.FIP))
	.add(new BankRegister (1, 16, 4, IA32Registers.FCS))
	.add(new BankRegister (1, 20, 4, "foo"))
	.add(new BankRegister (1, 24, 4, "fos"))
	.add(new BankRegister (1, 28, 10, X87Registers.ST0))
	.add(new BankRegister (1, 38, 10, X87Registers.ST1))
	.add(new BankRegister (1, 48, 10, X87Registers.ST2))
	.add(new BankRegister (1, 58, 10, X87Registers.ST3))
	.add(new BankRegister (1, 68, 10, X87Registers.ST4))
	.add(new BankRegister (1, 78, 10, X87Registers.ST5))
	.add(new BankRegister (1, 88, 10, X87Registers.ST6))
	.add(new BankRegister (1, 98, 10, X87Registers.ST7))
	.add(new BankRegister (2, 160, 16, IA32Registers.XMM0))
	.add(new BankRegister (2, 176, 16, IA32Registers.XMM1))
	.add(new BankRegister (2, 192, 16, IA32Registers.XMM2))
	.add(new BankRegister (2, 208, 16, IA32Registers.XMM3))
	.add(new BankRegister (2, 224, 16, IA32Registers.XMM4))
	.add(new BankRegister (2, 240, 16, IA32Registers.XMM5))
	.add(new BankRegister (2, 256, 16, IA32Registers.XMM6))
	.add(new BankRegister (2, 272, 16, IA32Registers.XMM7))
	.add(new BankRegister (3, 252, 4, IA32Registers.D0))
	.add(new BankRegister (3, 256, 4, IA32Registers.D1))
	.add(new BankRegister (3, 260, 4, IA32Registers.D2))
	.add(new BankRegister (3, 264, 4, IA32Registers.D3))
	.add(new BankRegister (3, 268, 4, IA32Registers.D4))
	.add(new BankRegister (3, 272, 4, IA32Registers.D5))
	.add(new BankRegister (3, 276, 4, IA32Registers.D6))
	.add(new BankRegister (3, 280, 4, IA32Registers.D7))
	;

    public static final BankRegisterMap X8664 = new BankRegisterMap()
	.add(new BankRegister(0, 80, 8, X8664Registers.RAX))
	.add(new BankRegister(0, 40, 8, X8664Registers.RBX))
	.add(new BankRegister(0, 88, 8, X8664Registers.RCX))
	.add(new BankRegister(0, 96, 8, X8664Registers.RDX))
	.add(new BankRegister(0, 104, 8, X8664Registers.RSI))
	.add(new BankRegister(0, 112, 8, X8664Registers.RDI))
	.add(new BankRegister(0, 32, 8, X8664Registers.RBP))
	.add(new BankRegister(0, 152, 8, X8664Registers.RSP))
	.add(new BankRegister(0, 72, 8, X8664Registers.R8))
	.add(new BankRegister(0, 64, 8, X8664Registers.R9))
	.add(new BankRegister(0, 56, 8, X8664Registers.R10))
	.add(new BankRegister(0, 48, 8, X8664Registers.R11))
	.add(new BankRegister(0, 24, 8, X8664Registers.R12))
	.add(new BankRegister(0, 16, 8, X8664Registers.R13))
	.add(new BankRegister(0, 8, 8, X8664Registers.R14))
	.add(new BankRegister(0, 0, 8, X8664Registers.R15))
	.add(new BankRegister(0, 128, 8, X8664Registers.RIP))
	.add(new BankRegister(0, 144, 8, X8664Registers.RFLAGS))
	.add(new BankRegister(0, 136, 8, "cs"))
	.add(new BankRegister(0, 160, 8, "ss"))
	.add(new BankRegister(0, 184, 8, "ds"))
	.add(new BankRegister(0, 192, 8, "es"))
	.add(new BankRegister(0, 200, 8, "fs"))
	.add(new BankRegister(0, 208, 8, "gs"))
	.add(new BankRegister(0, 120, 8, X8664Registers.ORIG_RAX))
	.add(new BankRegister(0, 168, 8, X8664Registers.FS_BASE))
	.add(new BankRegister(0, 176, 8, X8664Registers.GS_BASE))
	.add(new BankRegister(1, 0, 2, "cwd"))
	.add(new BankRegister(1, 2, 2, "swd"))
	.add(new BankRegister(1, 4, 2, "ftw"))
	.add(new BankRegister(1, 6, 2, "fop"))
	.add(new BankRegister(1, 8, 8, "fprip"))
	.add(new BankRegister(1, 16, 8, "rdp"))
	.add(new BankRegister(1, 24, 4, "mxcsr"))
	.add(new BankRegister(1, 28, 4, "mxcsr_mask"))
	.add(new BankRegister(1, 32, 10, X87Registers.ST0))
	.add(new BankRegister(1, 48, 10, X87Registers.ST1))
	.add(new BankRegister(1, 64, 10, X87Registers.ST2))
	.add(new BankRegister(1, 80, 10, X87Registers.ST3))
	.add(new BankRegister(1, 96, 10, X87Registers.ST4))
	.add(new BankRegister(1, 112, 10, X87Registers.ST5))
	.add(new BankRegister(1, 128, 10, X87Registers.ST6))
	.add(new BankRegister(1, 144, 10, X87Registers.ST7))
	.add(new BankRegister(1, 160, 16, X8664Registers.XMM0))
	.add(new BankRegister(1, 176, 16, X8664Registers.XMM1))
	.add(new BankRegister(1, 192, 16, X8664Registers.XMM2))
	.add(new BankRegister(1, 208, 16, X8664Registers.XMM3))
	.add(new BankRegister(1, 224, 16, X8664Registers.XMM4))
	.add(new BankRegister(1, 240, 16, X8664Registers.XMM5))
	.add(new BankRegister(1, 256, 16, X8664Registers.XMM6))
	.add(new BankRegister(1, 272, 16, X8664Registers.XMM7))
	.add(new BankRegister(1, 288, 16, X8664Registers.XMM8))
	.add(new BankRegister(1, 304, 16, X8664Registers.XMM9))
	.add(new BankRegister(1, 320, 16, X8664Registers.XMM10))
	.add(new BankRegister(1, 336, 16, X8664Registers.XMM11))
	.add(new BankRegister(1, 352, 16, X8664Registers.XMM12))
	.add(new BankRegister(1, 368, 16, X8664Registers.XMM13))
	.add(new BankRegister(1, 384, 16, X8664Registers.XMM14))
	.add(new BankRegister(1, 400, 16, X8664Registers.XMM15))
	.add(new BankRegister(2, 848, 8, "d0"))
	.add(new BankRegister(2, 856, 8, "d1"))
	.add(new BankRegister(2, 864, 8, "d2"))
	.add(new BankRegister(2, 872, 8, "d3"))
	.add(new BankRegister(2, 880, 8, "d4"))
	.add(new BankRegister(2, 888, 8, "d5"))
	.add(new BankRegister(2, 896, 8, "d6"))
	.add(new BankRegister(2, 904, 8, "d7"))
	;

    public static BankRegisterMap IA32_ON_X8664
	= new IndirectBankRegisterMap(ByteOrder.LITTLE_ENDIAN, IA32, X8664)
	.add("eax", "rax")
	.add("ebx", "rbx")
	.add("ecx", "rcx")
	.add("edx", "rdx")
	.add("esi", "rsi")
	.add("edi", "rdi")
	.add("ebp", "rbp")
	.add("cs", "cs")
	.add("ds", "ds")
	.add("es", "es")
	.add("fs", "fs")
	.add("gs", "gs")
	.add("ss", "gs")
	.add("orig_eax", "orig_rax")
	.add("eip", "rip")
	.add("eflags","rflags")
	.add("esp", "rsp")
	.add("cwd", "cwd")
	.add("swd", "swd")
	.add("twd", "ftw")
	.add("fip", "fprip")
	.add("fcs", 0)
	.add("foo", "rdp")
	.add("fos", 0)
	.add("st0")
	.add("st1")
	.add("st2")
	.add("st3")
	.add("st4")
	.add("st5")
	.add("st6")
	.add("st7")
	.add("xmm0")
	.add("xmm1")
	.add("xmm2")
	.add("xmm3")
	.add("xmm4")
	.add("xmm5")
	.add("xmm6")
	.add("xmm7")
	.add("d0", "d0")
	.add("d1", "d1")
	.add("d2", "d2")
	.add("d3", "d3")
	.add("d4", "d4")
	.add("d5", "d5")
	.add("d6", "d6")
	.add("d7", "d7")
	;
}
