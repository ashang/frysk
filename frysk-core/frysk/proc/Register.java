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

import util.eio.*;

public class Register
{
    // Type type;
    // boolean readWrite;
    int bank;
    int offset;
    int length;
    String name;

    Register (Isa isa, int bank, int offset, int length, String name)
    {
	this.bank = bank;
	this.offset = offset;
	this.length = length;
	this.name = name;
	isa.registers.add (this);
    }

    public long get (frysk.proc.Task task)
    {
	ByteBuffer b = task.registerBank[bank];
	long val = 0;
	if (b.order () == util.eio.ByteOrder.LITTLE_ENDIAN) {
	    for (int i = offset + length - 1; i >= offset; i--) {
	        val = val << 8 | (b.get (i) & 0xff);
	    }
	}
	else {
	    for (int i = offset; i < offset + length; i++) {
	        val = val << 8 | (b.get (i) & 0xff);
	    }
	}
	return val;
    }

    public void put (frysk.proc.Task task, long val)
    {
	ByteBuffer b = task.registerBank[bank];
	if (b.order () == util.eio.ByteOrder.LITTLE_ENDIAN) {
	    for (int i = offset; i < offset + length; i++) {
	        b.putByte (i, (byte)(val & 0xff));
	        val = val >> 8;
	    }
	}
	else {
	    for (int i = offset + length - 1; i >= offset; i--) {
	        b.putByte (i, (byte)(val & 0xff));
	        val = val >> 8;
	    }
	}
    }

    public String getName ()
    {
	return name;
    }

    // void get (proc.Task task, byte[] bytes, int off, int len);
    // void get (Task task, byte[] array);
}
