// This file is part of the program FRYSK.
//
// Copyright 2005, Red Hat Inc.
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

class PowerPcLinux
{
    static class Isa
	extends frysk.proc.Isa
    {
	static private Register[] gprs (Isa isa)
	{
	    Register[] gprs = new Register[32];
	    for (int i = 0; i < gprs.length; i++) {
		gprs[i] = new Register (isa, 0, i*4, 4, "r" + i);
	    }
	    return gprs;
	}
	Register[] gpr = gprs (this);
	Register nip = new Register (this, 0, 32*4, 4, "nip");
	Register msr = new Register (this, 0, 33*4, 4, "msr");
	Register ctr = new Register (this, 0, 35*4, 4, "ctr");
	Register link = new Register (this, 0, 36*4, 4, "link");
	Register xer = new Register (this, 0, 37*4, 4, "xer");
	Register ccr = new Register (this, 0, 38*4, 4, "ccr");
	Register trap = new Register (this, 0, 40*4, 4, "trap");
	Register dar = new Register (this, 0, 41*4, 4, "dar");
	Register dsisr = new Register (this, 0, 42*4, 4, "dsisr");
	Register result = new Register (this, 0, 43*4, 4, "result");
	static private Register[] fprs (Isa isa)
	{
	    Register[] fprs = new Register[32];
	    for (int i = 0; i < fprs.length; i++) {
		fprs[i] = new Register (isa, 0, 48*4 + i*8, 8, "fpr" + i);
	    }
	    return fprs;
	}
	Register[] fpr = fprs (this);
	Register fpsr = new Register (this, 0, (48+2*32+1)*4, 4, "");

	long pc (Task task)
	{
	    return nip.get (task);
	}
    }
    private static Isa isa;
    static Isa isaSingleton ()
    {
	if (isa == null)
	    isa = new Isa ();
	return isa;
    }


    static class SyscallEventInfo
	extends frysk.proc.SyscallEventInfo
    {
	int number (Task task)
	{
	    throw new RuntimeException ("not implemented");
	}
	long returnCode (Task task)
	{
	    throw new RuntimeException ("not implemented");
	}
	long arg (Task task, int n)
	{
	    throw new RuntimeException ("not implemented");
	}
    }
}
