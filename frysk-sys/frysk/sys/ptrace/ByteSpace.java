// This file is part of the program FRYSK.
// 
// Copyright 2005, 2006, 2007, 2008, Red Hat Inc.
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

package frysk.sys.ptrace;

import frysk.rsl.Log;
import frysk.sys.ProcessIdentifier;

/**
 * A ptrace space, that can be efficiently peeked and poked a "word"
 * or even "byte" at a time.
 */
public class ByteSpace {
    private static final Log fine = Log.fine(ByteSpace.class);

    private final long length;
    private final String name;
    private final int ptPeek;
    private final int ptPoke;

    ByteSpace(long length, String name, int ptPeek, int ptPoke) {
	this.name = super.toString() + ":" + name;
	this.length = length;
	this.ptPeek = ptPeek;
	this.ptPoke = ptPoke;
    }

    public String toString() {
	return name;
    }
    public long length() {
	return length;
    }

    /**
     * Fetch a byte at ADDR of process PID.
     */
    public int peek(ProcessIdentifier pid, long addr) {
	fine.log(this, "peek", pid, "addr", addr, "...");
	int ret = peek(pid.intValue(), addr);
	fine.log("... peek", pid, "returns", ret);
	return ret;
    }
    private native int peek (int pid, long addr);

    /**
     * Store the byte at ADDR of process PID.
     */
    public void poke(ProcessIdentifier pid, long addr, int data) {
	fine.log(this, "poke", pid, "addr", addr, "data", (long) data);
	poke(pid.intValue(), addr, data);
    }
    private native void poke(int pid, long addr, int data);

    /**
     * Transfer data between the local BYTES array and process PID.
     * Locally the data starts at OFFSET and goes for LENGTH bytes.
     *
     * This is a host oriented transfer; hence LENGTH, which must fall
     * within the bounds of BYTES, is an int.
     */
    public void transfer(ProcessIdentifier pid, long addr,
			 byte[] bytes, int offset, int length,
			 boolean write) {
	fine.log(this, "transfer", pid, "addr", addr,
		 "offset", offset, "length", length,
		 write ? "write ..." : "read ...");
	transfer(write ? ptPoke : ptPeek, pid.intValue(), addr,
		 bytes, offset, length);
    }

    /**
     * Transfer data between the local BYTES array and process PID.
     * Up to LENGTH bytes are copied, starting at OFFSET in the BYTES
     * array.  The number of bytes actually transfered is returned.
     *
     * This is a target oriented transfer; hence LENGTH, as an address
     * sized quantity, is a long and can be larger than the bounds of
     * BYTES.
     */
    public int transfer(ProcessIdentifier pid, long addr, long length,
			byte[] bytes, int offset, boolean write) {
	fine.log(this, "transfer", pid, "addr", addr, "length", length,
		 "offset", offset, write ? "write ..." : "read ...");
	int size;
	if (offset >= 0 && length >= 0) {
	    if (offset + length > bytes.length)
		size = bytes.length - offset;
	    else
		size = (int) length;
	} else {
	    size = -1; // triggers exception
	}
	transfer(write ? ptPoke : ptPeek, pid.intValue(), addr,
		 bytes, offset, size);
	return size;
    }

    private native final void transfer(int op, int pid, long addr,
				       byte[] bytes, int offset, int length);

    private static native ByteSpace text();
    private static native ByteSpace data();
    private static native ByteSpace usr();

    public static final ByteSpace TEXT = text();
    public static final ByteSpace DATA = data();
    public static final ByteSpace USR = usr();
}
