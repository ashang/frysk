// This file is part of the program FRYSK.
//
// Copyright 2006, 2007, 2008, Red Hat Inc.
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

import frysk.sys.ptrace.BlockSpace;
import frysk.event.Request;
import frysk.proc.Manager;
import inua.eio.ByteBuffer;
import frysk.sys.ProcessIdentifier;

/*
 * A ByteBuffer interface to structures returned by ptrace which must
 * be read or written all at once e.g., the registers or floating
 * point registers.
 */
public class BlockSpaceByteBuffer
    extends ByteBuffer
{
    private final ProcessIdentifier pid;
    private final BlockSpace registerSet;
    private final byte[] bytes;
  
    private BlockSpaceByteBuffer(ProcessIdentifier pid,
				 BlockSpace registerSet,
				 long lowerExtreem, long upperExtreem) {
	super(lowerExtreem, upperExtreem);
	this.pid = pid;
	this.registerSet = registerSet;
	bytes = new byte[registerSet.length()];
    }
    public BlockSpaceByteBuffer(ProcessIdentifier pid,
				BlockSpace registerSet) {
	this(pid, registerSet, 0, registerSet.length());
    }
  
    private class TransferRequest extends Request {
	TransferRequest() {
	    super(Manager.eventLoop);
	}
	private boolean write;
	public final void execute() {
	    registerSet.transfer(pid, bytes, write);
	}
	private void request(boolean write) {
	    if (isEventLoopThread()) {
		registerSet.transfer(pid, bytes, write);
	    } else {
		synchronized (this) {
		    this.write = write;
		    super.request();
		}
	    }
	}
	void getRegs() {
	    request(false); // read
	}
	void setRegs() {
	    request(true); // write
	}
    }
    private final TransferRequest transfer = new TransferRequest();

    protected int peek(long index) {
	transfer.getRegs();
	return bytes[(int)index];
    }
  
    protected void poke(long index, int value) {
	transfer.getRegs();
	bytes[(int)index] = (byte)value;
	transfer.setRegs();
    }
  
    protected int peek(long index, byte[] bytes, int off, int len) {
	transfer.getRegs();
	System.arraycopy(this.bytes, (int) index, bytes, off, len);
	return len;
    }

    protected int poke(long index, byte[] bytes, int off, int len) {
	transfer.getRegs();
	System.arraycopy(bytes, off, this.bytes, (int) index, len);
	transfer.setRegs();
	return len;
    }
  
    protected ByteBuffer subBuffer(ByteBuffer parent, long lowerExtreem,
				   long upperExtreem) {
	BlockSpaceByteBuffer up = (BlockSpaceByteBuffer)parent;
	return new BlockSpaceByteBuffer(up.pid, up.registerSet,
					lowerExtreem, upperExtreem);
    }
}
