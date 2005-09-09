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

class X8664Linux
{
    static class Isa
	extends frysk.proc.Isa
    {
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
