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

public class IsaPPC
  extends IsaPowerPC
{
    IsaPPC () {
	add(new BankRegister(0, 0, 4, "gpr0"));
	add(new BankRegister(0, 4, 4, "gpr1"));
	add(new BankRegister(0, 8, 4, "gpr2"));
	add(new BankRegister(0, 12, 4, "gpr3"));
	add(new BankRegister(0, 16, 4, "gpr4"));
	add(new BankRegister(0, 20, 4, "gpr5"));
	add(new BankRegister(0, 24, 4, "gpr6"));
	add(new BankRegister(0, 28, 4, "gpr7"));
	add(new BankRegister(0, 32, 4, "gpr8"));
	add(new BankRegister(0, 36, 4, "gpr9"));
	add(new BankRegister(0, 40, 4, "gpr10"));
	add(new BankRegister(0, 44, 4, "gpr11"));
	add(new BankRegister(0, 48, 4, "gpr12"));
	add(new BankRegister(0, 52, 4, "gpr13"));
	add(new BankRegister(0, 56, 4, "gpr14"));
	add(new BankRegister(0, 60, 4, "gpr15"));
	add(new BankRegister(0, 64, 4, "gpr16"));
	add(new BankRegister(0, 68, 4, "gpr17"));
	add(new BankRegister(0, 72, 4, "gpr18"));
	add(new BankRegister(0, 76, 4, "gpr19"));
	add(new BankRegister(0, 80, 4, "gpr20"));
	add(new BankRegister(0, 84, 4, "gpr21"));
	add(new BankRegister(0, 88, 4, "gpr22"));
	add(new BankRegister(0, 92, 4, "gpr23"));
	add(new BankRegister(0, 96, 4, "gpr24"));
	add(new BankRegister(0, 100, 4, "gpr25"));
	add(new BankRegister(0, 104, 4, "gpr26"));
	add(new BankRegister(0, 108, 4, "gpr27"));
	add(new BankRegister(0, 112, 4, "gpr28"));
	add(new BankRegister(0, 116, 4, "gpr29"));
	add(new BankRegister(0, 120, 4, "gpr30"));
	add(new BankRegister(0, 124, 4, "gpr31"));
	add(new BankRegister(0, 128, 4, "nip"));
	add(new BankRegister(0, 132, 4, "msr"));
	add(new BankRegister(0, 136, 4, "orig_r3"));
	add(new BankRegister(0, 140, 4, "ctr"));
	add(new BankRegister(0, 144, 4, "lnk"));
	add(new BankRegister(0, 148, 4, "xer"));
	add(new BankRegister(0, 152, 4, "ccr"));
	add(new BankRegister(0, 156, 4, "mq"));
	add(new BankRegister(0, 160, 4, "trap"));
	add(new BankRegister(0, 164, 4, "dar"));
	add(new BankRegister(0, 168, 4, "dsisr"));
	add(new BankRegister(0, 172, 4, "result"));
	add(new BankRegister(0, 192, 8, "fpr0"));
	add(new BankRegister(0, 200, 8, "fpr1"));
	add(new BankRegister(0, 208, 8, "fpr2"));
	add(new BankRegister(0, 216, 8, "fpr3"));
	add(new BankRegister(0, 224, 8, "fpr4"));
	add(new BankRegister(0, 232, 8, "fpr5"));
	add(new BankRegister(0, 240, 8, "fpr6"));
	add(new BankRegister(0, 248, 8, "fpr7"));
	add(new BankRegister(0, 256, 8, "fpr8"));
	add(new BankRegister(0, 264, 8, "fpr9"));
	add(new BankRegister(0, 272, 8, "fpr10"));
	add(new BankRegister(0, 280, 8, "fpr11"));
	add(new BankRegister(0, 288, 8, "fpr12"));
	add(new BankRegister(0, 296, 8, "fpr13"));
	add(new BankRegister(0, 304, 8, "fpr14"));
	add(new BankRegister(0, 312, 8, "fpr15"));
	add(new BankRegister(0, 320, 8, "fpr16"));
	add(new BankRegister(0, 328, 8, "fpr17"));
	add(new BankRegister(0, 336, 8, "fpr18"));
	add(new BankRegister(0, 344, 8, "fpr19"));
	add(new BankRegister(0, 352, 8, "fpr20"));
	add(new BankRegister(0, 360, 8, "fpr21"));
	add(new BankRegister(0, 368, 8, "fpr22"));
	add(new BankRegister(0, 376, 8, "fpr23"));
	add(new BankRegister(0, 384, 8, "fpr24"));
	add(new BankRegister(0, 392, 8, "fpr25"));
	add(new BankRegister(0, 400, 8, "fpr26"));
	add(new BankRegister(0, 408, 8, "fpr27"));
	add(new BankRegister(0, 416, 8, "fpr28"));
	add(new BankRegister(0, 424, 8, "fpr29"));
	add(new BankRegister(0, 432, 8, "fpr30"));
	add(new BankRegister(0, 440, 8, "fpr31"));
    }
    
  public int getWordSize ()
  {
    return 4;
  }

  public int getElfMachineType()
  {
    return ElfEMachine.EM_PPC64;
  }
}
