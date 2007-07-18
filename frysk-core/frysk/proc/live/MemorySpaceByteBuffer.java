// This file is part of the program FRYSK.
// 
// Copyright 2007 Oracle Corporation.
// Copyright 2007, Red Hat Inc.
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
import frysk.sys.Ptrace.AddressSpace;
import frysk.sys.Errno;
import frysk.sys.proc.Mem;
import frysk.event.Request;
import frysk.proc.Manager;

public class MemorySpaceByteBuffer
    extends ByteBuffer
{
    private MemorySpaceByteBuffer (long lowerExtreem,
				   long upperExtreem,
				   PeekRequest peekRequest,
				   PokeRequest pokeRequest,
				   PeeksRequest peeksRequest)
    {
	super (lowerExtreem, upperExtreem);
	this.peekRequest = peekRequest;
	this.pokeRequest = pokeRequest;
	this.peeksRequest = peeksRequest;
    }
    public MemorySpaceByteBuffer (int pid)
    {
	this (0, AddressSpace.TEXT.length (), new PeekRequest(pid),
	      new PokeRequest(pid), new PeeksRequest(pid));
    }

    private static class PeekRequest
	extends Request
    {
	private long index;
	private int value;
	private final int pid;
	PeekRequest(int pid)
	{
	    super(Manager.eventLoop);
	    this.pid = pid;
	}
	public void execute ()
	{
	    value = AddressSpace.TEXT.peek(pid, index);
	}
	public int request (long index)
	{
	    if (isEventLoopThread())
		return AddressSpace.TEXT.peek(pid, index);
	    else synchronized (this) {
		this.index = index;
		request();
		return value;
	    }
	}
    }
    private final PeekRequest peekRequest;
    protected int peek (long index)
    {
	return peekRequest.request (index);
    }

    private static class PokeRequest
	extends Request
    {
	private long index;
	private int value;
	private final int pid;
	PokeRequest(int pid)
	{
	    super(Manager.eventLoop);
	    this.pid = pid;
	}
	public void execute ()
	{
	    AddressSpace.TEXT.poke(pid, index, value);
	}
	public void request (long index, int value)
	{
	    if (isEventLoopThread())
		AddressSpace.TEXT.poke(pid, index, value);
	    else synchronized (this) {
		this.index = index;
		this.value = value;
		request();
	    }
	}
    }
    private final PokeRequest pokeRequest;
    protected void poke (long index, int value)
    {
	pokeRequest.request (index, value);
    }

    private static class PeeksRequest
	extends Request
    {
	private long index;
	private long length;
	private long offset;
	private byte[] bytes;
	private Mem mem;
	private final int pid;
	PeeksRequest(int pid) {
	    super(Manager.eventLoop);
	    mem = new Mem(pid);
	    this.pid = pid;
	}
	private long peek(long index, byte[] bytes, long offset, long length) {
	    if (mem != null) {
		try {
		    return mem.pread (index, bytes, offset, length);
		} catch (Errno ioe) {
		    mem = null;
		}
	    }
	    return AddressSpace.TEXT.peek(pid, index, length, bytes, offset);
	}
	public void execute ()
	{
	    length = peek(index, bytes, offset, length);
	}
	public long request (long index, byte[] bytes,
			     long offset, long length)
	{
	    if (isEventLoopThread()) {
		return peek(index, bytes, offset, length);
	    }
	    else synchronized (this) {
		this.index = index;
		this.bytes = bytes;
		this.offset = offset;
		this.length = length;
		request();
		return length;
	    }
	}
    }
    private final PeeksRequest peeksRequest;
    protected long peek (long index, byte[] bytes, long offset, long length)
    {
	return peeksRequest.request(index, bytes, offset, length);
    }

    protected ByteBuffer subBuffer (ByteBuffer parent, long lowerExtreem,
				    long upperExtreem)
    {
	MemorySpaceByteBuffer up = (MemorySpaceByteBuffer)parent;
	return new MemorySpaceByteBuffer (lowerExtreem, upperExtreem,
					  up.peekRequest,
					  up.pokeRequest,
					  up.peeksRequest);
    }
}
