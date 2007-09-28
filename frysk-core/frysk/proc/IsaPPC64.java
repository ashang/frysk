// This file is part of the program FRYSK.
//
// Copyright 2006 IBM Corp.
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

import lib.dwfl.ElfEMachine;

public class IsaPPC64 extends IsaPowerPC {
    IsaPPC64() {
	// FIXME: Should use frysk.isa.PPC64Registers.GPR[0] et.al.
	add(new BankRegister(0, 0, 8, "gpr0"));
	add(new BankRegister(0, 8, 8, "gpr1"));
	add(new BankRegister(0, 16, 8, "gpr2"));
	add(new BankRegister(0, 24, 8, "gpr3"));
	add(new BankRegister(0, 32, 8, "gpr4"));
	add(new BankRegister(0, 40, 8, "gpr5"));
	add(new BankRegister(0, 48, 8, "gpr6"));
	add(new BankRegister(0, 56, 8, "gpr7"));
	add(new BankRegister(0, 64, 8, "gpr8"));
	add(new BankRegister(0, 72, 8, "gpr9"));
	add(new BankRegister(0, 80, 8, "gpr10"));
	add(new BankRegister(0, 88, 8, "gpr11"));
	add(new BankRegister(0, 96, 8, "gpr12"));
	add(new BankRegister(0, 104, 8, "gpr13"));
	add(new BankRegister(0, 112, 8, "gpr14"));
	add(new BankRegister(0, 120, 8, "gpr15"));
	add(new BankRegister(0, 128, 8, "gpr16"));
	add(new BankRegister(0, 136, 8, "gpr17"));
	add(new BankRegister(0, 144, 8, "gpr18"));
	add(new BankRegister(0, 152, 8, "gpr19"));
	add(new BankRegister(0, 160, 8, "gpr20"));
	add(new BankRegister(0, 168, 8, "gpr21"));
	add(new BankRegister(0, 176, 8, "gpr22"));
	add(new BankRegister(0, 184, 8, "gpr23"));
	add(new BankRegister(0, 192, 8, "gpr24"));
	add(new BankRegister(0, 200, 8, "gpr25"));
	add(new BankRegister(0, 208, 8, "gpr26"));
	add(new BankRegister(0, 216, 8, "gpr27"));
	add(new BankRegister(0, 224, 8, "gpr28"));
	add(new BankRegister(0, 232, 8, "gpr29"));
	add(new BankRegister(0, 240, 8, "gpr30"));
	add(new BankRegister(0, 248, 8, "gpr31"));
	add(new BankRegister(0, 256, 8, "nip"));
	add(new BankRegister(0, 264, 8, "msr"));
	add(new BankRegister(0, 272, 8, "orig_r3"));
	add(new BankRegister(0, 280, 8, "ctr"));
	add(new BankRegister(0, 288, 8, "lnk"));
	add(new BankRegister(0, 296, 8, "xer"));
	add(new BankRegister(0, 304, 8, "ccr"));
	add(new BankRegister(0, 312, 8, "softe"));
	add(new BankRegister(0, 320, 8, "trap"));
	add(new BankRegister(0, 328, 8, "dar"));
	add(new BankRegister(0, 336, 8, "dsisr"));
	add(new BankRegister(0, 344, 8, "result"));
	add(new BankRegister(0, 384, 8, "fpr0"));
	add(new BankRegister(0, 392, 8, "fpr1"));
	add(new BankRegister(0, 400, 8, "fpr2"));
	add(new BankRegister(0, 408, 8, "fpr3"));
	add(new BankRegister(0, 416, 8, "fpr4"));
	add(new BankRegister(0, 424, 8, "fpr5"));
	add(new BankRegister(0, 432, 8, "fpr6"));
	add(new BankRegister(0, 440, 8, "fpr7"));
	add(new BankRegister(0, 448, 8, "fpr8"));
	add(new BankRegister(0, 456, 8, "fpr9"));
	add(new BankRegister(0, 464, 8, "fpr10"));
	add(new BankRegister(0, 472, 8, "fpr11"));
	add(new BankRegister(0, 480, 8, "fpr12"));
	add(new BankRegister(0, 488, 8, "fpr13"));
	add(new BankRegister(0, 496, 8, "fpr14"));
	add(new BankRegister(0, 504, 8, "fpr15"));
	add(new BankRegister(0, 512, 8, "fpr16"));
	add(new BankRegister(0, 520, 8, "fpr17"));
	add(new BankRegister(0, 528, 8, "fpr18"));
	add(new BankRegister(0, 536, 8, "fpr19"));
	add(new BankRegister(0, 544, 8, "fpr20"));
	add(new BankRegister(0, 552, 8, "fpr21"));
	add(new BankRegister(0, 560, 8, "fpr22"));
	add(new BankRegister(0, 568, 8, "fpr23"));
	add(new BankRegister(0, 576, 8, "fpr24"));
	add(new BankRegister(0, 584, 8, "fpr25"));
	add(new BankRegister(0, 592, 8, "fpr26"));
	add(new BankRegister(0, 600, 8, "fpr27"));
	add(new BankRegister(0, 608, 8, "fpr28"));
	add(new BankRegister(0, 616, 8, "fpr29"));
	add(new BankRegister(0, 624, 8, "fpr30"));
	add(new BankRegister(0, 632, 8, "fpr31"));
    }
  
  public int getWordSize ()
  {
    return 8;
  }

  public int getElfMachineType()
  {
    return ElfEMachine.EM_PPC64;
  }
}
