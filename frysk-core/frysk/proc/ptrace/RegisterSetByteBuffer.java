// This file is part of the program FRYSK.
//
// Copyright 2006, 2007, Red Hat Inc.
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

import frysk.sys.Ptrace.RegisterSet;
import frysk.event.Request;
import frysk.proc.Manager;
import inua.eio.ByteBuffer;

/*
 * A ByteBuffer interface to structures returned by ptrace which must
 * be read or written all at once e.g., the registers or floating
 * point registers.
 */
public class RegisterSetByteBuffer
    extends ByteBuffer
{
    private final int pid;
    private final RegisterSet registerSet;
    private final byte[] bytes;
  
    private RegisterSetByteBuffer (int pid, RegisterSet registerSet,
				   long lowerExtreem, long upperExtreem)
    {
	super(lowerExtreem, upperExtreem);
	this.pid = pid;
	this.registerSet = registerSet;
	bytes = new byte[registerSet.length ()];
	getRegs = new GetRegs();
	setRegs = new SetRegs();
    }
    public RegisterSetByteBuffer(int pid, RegisterSet registerSet) 
    {
	this (pid, registerSet, 0, registerSet.length());
    }
  
    private class GetRegs
	extends Request
    {
	GetRegs()
	{
	    super(Manager.eventLoop);
	}
	public final void execute()
	{
	    registerSet.get(pid, bytes);
	}
	public void request ()
	{
	    if (isEventLoopThread())
		execute();
	    else synchronized (this) {
		super.request();
	    }
	}
    }
    private final GetRegs getRegs;
    private void getRegs()
    {
	getRegs.request();
    }

    private class SetRegs
	extends Request
    {
	SetRegs()
	{
	    super(Manager.eventLoop);
	}
	public void execute()
	{
	    registerSet.set(pid, bytes);
	}
	public void request ()
	{
	    if (isEventLoopThread())
		// Skip the event-loop
		execute ();
	    else synchronized (this) {
		super.request();
	    }
	}
    }
    private final SetRegs setRegs;
    private void setRegs()
    {
	setRegs.request();
    }

    protected int peek (long index) 
    {
	getRegs();
	return bytes[(int)index];
    }
  
    protected void poke (long index, int value)
    {
	getRegs();
	bytes[(int)index] = (byte)value;
	setRegs();
    }
  
    protected long peek (long index, byte[] bytes, long off, long len) 
    {
	getRegs();
	for (int i = 0; i < len; i++) {
	    bytes[(int)off + i] = this.bytes[(int)index + i];
	}
	return len;
    }
  
    protected ByteBuffer subBuffer (ByteBuffer parent, long lowerExtreem,
				    long upperExtreem)
    {
	RegisterSetByteBuffer up = (RegisterSetByteBuffer)parent;
	return new RegisterSetByteBuffer (up.pid, up.registerSet,
					  lowerExtreem, upperExtreem);
    }
}
