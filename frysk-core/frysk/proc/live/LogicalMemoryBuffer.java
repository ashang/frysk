// This file is part of the program FRYSK.
//
// Copyright 2007 Red Hat Inc.
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

import frysk.proc.Breakpoint;
import frysk.proc.BreakpointAddresses;
import frysk.proc.Instruction;

import java.util.Iterator;

import inua.eio.ByteBuffer;
import inua.eio.ByteOrder;

import frysk.sys.Ptrace.AddressSpace;

/**
 * MemorySpaceByteBuffer that filters out anything the frysk core
 * might have done to the underlying memory. It takes the breakpoints
 * embedded in the process and on a peek will filter those out and
 * replace the bytes with the logical bytes as the user would normally
 * see them from the process.
 */
class LogicalMemoryBuffer extends AddressSpaceByteBuffer
{
  // Byte order set on the buffer (needed for creating a subBuffer).
  private final ByteOrder order;

  // The breakpoints associated with the process address space.
  private final BreakpointAddresses breakpoints;

  // Private constructor used by subBuffer()
  private LogicalMemoryBuffer(int tid, AddressSpace addressSpace,
			      ByteOrder order,
			      BreakpointAddresses breakpoints,
			      long lower, long upper)
  {
    super(tid, addressSpace, lower, upper);
    order(order);
    this.order = order;
    this.breakpoints = breakpoints;
  }
  
  // Package local contructor used by LinuxTask to create a logical
  // memory space for a task when requested.
  LogicalMemoryBuffer(int tid,
		      AddressSpace addressSpace,
		      ByteOrder order,
		      BreakpointAddresses breakpoints)
  {
    super(tid, addressSpace);
    order(order);
    this.order = order;
    this.breakpoints = breakpoints;
  }
  
  protected int peek(long caret)
  {
    Breakpoint breakpoint = breakpoints.getBreakpoint(caret);
    if (breakpoint != null)
      {
	// This really shouldn't happen, it means the breakpoint
	// is already uninstalled.
	Instruction instruction = breakpoint.getInstruction();
	if (instruction != null)
	  {
	    byte[] ibs = instruction.getBytes();
	    return ibs[0] & 0xff;
	  }
      }
    return super.peek(caret);
  }
  
  protected long peek(long index, byte[] bytes, long offset, long length)
  {
    synchronized (breakpoints)
      {
	Iterator it;
	it = breakpoints.getBreakpoints(index, index + length);
	long r = 0;
	while (it.hasNext())
	  {
	    Breakpoint breakpoint = (Breakpoint) it.next();
	    long l = breakpoint.getAddress() - (index + r);
	    // Do we need to be worried about "short peeks"?
	    r += super.peek(index + r, bytes, offset + r, l);

	    byte b;
	    Instruction instruction = breakpoint.getInstruction();
	    // This really shouldn't happen, it means the breakpoint
	    // is already uninstalled.
	    if (instruction != null)
	      b = instruction.getBytes()[0];
	    else
	      b = (byte) super.peek(index + r);
	    // Since we are addressing a array both offset and r
	    // cannot really be bigger than an int, they could still
	    // overflow, but then we get a negative offset exception
	    // which seems fine because in that case offset + r was
	    // already larger than the array length.
	    bytes[(int) (offset + r)] = b;
	    r++;
	  }
	return super.peek(index + r, bytes, offset + r, length - r) + r;
      }
  }
  
  protected ByteBuffer subBuffer(ByteBuffer parent,
				 long lower, long upper)
  {
    LogicalMemoryBuffer sub = (LogicalMemoryBuffer) parent;
    return new LogicalMemoryBuffer (sub.pid, sub.addressSpace,
				    sub.order, sub.breakpoints,
				    lower, upper);
  }
}
