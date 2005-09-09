// This file is part of FRYSK.
//
// Copyright 2005, Red Hat Inc.
//
// FRYSK is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
//
// FRYSK is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with FRYSK; if not, write to the Free Software
// Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
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
