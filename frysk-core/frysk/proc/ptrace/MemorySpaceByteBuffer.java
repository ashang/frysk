// This file is part of the program FRYSK.
// 
// Copyright 2008 Oracle Corporation.
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

package frysk.proc.ptrace;

import java.io.File;
import inua.eio.ByteBuffer;
import frysk.sys.Ptrace.AddressSpace;
import frysk.sys.Errno;
//import java.io.IOException;
import frysk.sys.StatelessFile;
import frysk.event.Request;
import frysk.proc.Manager;

public class MemorySpaceByteBuffer
    extends ByteBuffer
{
    private final AddressSpace addressSpace;
    private final int pid;

    private MemorySpaceByteBuffer (int pid, AddressSpace addressSpace,
				   long lowerExtreem, long upperExtreem)
    {
	super (lowerExtreem, upperExtreem);
	this.pid = pid;
	this.addressSpace = addressSpace;
	peekRequest = new PeekRequest();
	pokeRequest = new PokeRequest();
	peeksRequest = new PeeksRequest();
    }
    public MemorySpaceByteBuffer (int pid, AddressSpace addressSpace)
    {
	this (pid, addressSpace, 0, addressSpace.length ());
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
    protected int peek (long index)
    {
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
    protected void poke (long index, int value)
    {
	pokeRequest.request (index, value);
    }

    private class PeeksRequest
	extends Request
    {
	private long index;
	private long length;
	private long offset;
	private byte[] bytes;
	PeeksRequest()
	{
	    super(Manager.eventLoop);
	}
	public void execute ()
	{
	    length = addressSpace.peek(pid, index, length, bytes, offset);
	}
	public long request (long index, byte[] bytes,
			     long offset, long length)
	{
	    long rc;
	    if (isEventLoopThread()) {
		File fn = new File ("/proc/" + pid + "/mem");
		StatelessFile sf = new StatelessFile (fn);
		try {
		    rc = sf.pread (index, bytes, offset, length);
		} catch (Errno ioe) {
		    rc = addressSpace.peek(pid, index, length, bytes, offset);
		}
	    }
	    else synchronized (this) {
		this.index = index;
		this.bytes = bytes;
		this.offset = offset;
		this.length = length;
		request();
		rc = length;
	    }
	    return rc;
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
	return new MemorySpaceByteBuffer (up.pid, up.addressSpace,
					  lowerExtreem, upperExtreem);
    }
}
