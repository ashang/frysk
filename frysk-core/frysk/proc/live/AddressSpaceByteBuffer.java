// This file is part of the program FRYSK.
// 
// Copyright 2007, 2008, Red Hat Inc.
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
import frysk.sys.Errno;
import frysk.sys.ProcessIdentifier;
import frysk.sys.ptrace.AddressSpace;
import frysk.sys.proc.Mem;
import frysk.event.Request;
import frysk.proc.Manager;

public class AddressSpaceByteBuffer extends ByteBuffer {
    protected final AddressSpace addressSpace;
    protected final ProcessIdentifier pid;

    // Direct files access if possible, or null otherwise.
    private Mem mem;

    protected AddressSpaceByteBuffer (ProcessIdentifier pid,
				      AddressSpace addressSpace,
				      long lowerExtreem, long upperExtreem) {
	super (lowerExtreem, upperExtreem);
	this.pid = pid;
	this.addressSpace = addressSpace;
	peekRequest = new PeekRequest();
	pokeRequest = new PokeRequest();
	if (addressSpace == AddressSpace.TEXT
	    || addressSpace == AddressSpace.DATA)
	    // Try to use /proc; but if any error occures clear it and
	    // revert back to ptrace.
	    mem = new Mem(pid);
    }
    public AddressSpaceByteBuffer(ProcessIdentifier pid,
				  AddressSpace addressSpace) {
	this(pid, addressSpace, 0, addressSpace.length());
    }


    private class PeekRequest
	extends Request
    {
	private long index;
	private int value;
	PeekRequest()
	{
	    super(Manager.eventLoop);
	}
	public void execute ()
	{
	    value = addressSpace.peek(pid, index);
	}
	public int request (long index)
	{
	    if (isEventLoopThread())
		return addressSpace.peek(pid, index);
	    else synchronized (this) {
		this.index = index;
		request();
		return value;
	    }
	}
    }
    private final PeekRequest peekRequest;
    protected int peek(long index) {
	return peekRequest.request (index);
    }

    private class PokeRequest
	extends Request
    {
	private long index;
	private int value;
	PokeRequest()
	{
	    super(Manager.eventLoop);
	}
	public void execute ()
	{
	    addressSpace.poke(pid, index, value);
	}
	public void request (long index, int value)
	{
	    if (isEventLoopThread())
		addressSpace.poke(pid, index, value);
	    else synchronized (this) {
		this.index = index;
		this.value = value;
		request();
	    }
	}
    }
    private final PokeRequest pokeRequest;
    protected void poke(long index, int value) {
	pokeRequest.request (index, value);
    }

    private class TransferRequest extends Request {
	private long index;
	private byte[] bytes;
	private int offset;
	private int length;
	private boolean write;
	TransferRequest() {
	    super(Manager.eventLoop);
	}
        private void transfer(long index, byte[] bytes, int offset, int length,
			      boolean write) {
	    if (mem != null && !write) {
		try {
		    mem.pread(index, bytes, offset, length);
		    return;
		} catch (Errno e) {
		    // Give up on mem; and fall back to ptrace.  This
		    // can happen when /proc isn't mounted, or when a
		    // process is terminating and the kernel scrubs
		    // the /proc entry before its time.
		    mem = null;
		}
	    }
	    addressSpace.transfer(pid, index, bytes, offset, length,
				  write);
	}

	public void execute() {
	    transfer(index, bytes, offset, length, write);
	}

	public void request(long index, byte[] bytes, int offset, int length,
			    boolean write) {
	    if (isEventLoopThread())
		transfer(index, bytes, offset, length, write);
	    else synchronized (this) {
		this.index = index;
		this.bytes = bytes;
		this.offset = offset;
		this.length = length;
		this.write = write;
		super.request();
	    }
	}
    }
    private final TransferRequest transfer = new TransferRequest();

    protected int peek(long index, byte[] bytes, int offset, int length) {
	transfer.request(index, bytes, offset, length, false); // read
	return length;
    }
    protected int poke(long index, byte[] bytes, int offset, int length) {
	transfer.request(index, bytes, offset, length, true); // write
	return length;
    }

    protected ByteBuffer subBuffer (ByteBuffer parent, long lowerExtreem,
				    long upperExtreem)
    {
	AddressSpaceByteBuffer up = (AddressSpaceByteBuffer)parent;
	return new AddressSpaceByteBuffer (up.pid, up.addressSpace,
					   lowerExtreem, upperExtreem);
    }
}
