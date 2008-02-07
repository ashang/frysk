// This file is part of the program FRYSK.
//
// Copyright 2007, 2008 Red Hat Inc.
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

import frysk.sys.ProcessIdentifier;
import java.util.Iterator;
import inua.eio.ByteBuffer;
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
  // The breakpoints associated with the process address space.
  private final BreakpointAddresses breakpoints;

    /**
     * Private constructor used by subBuffer()
     */
    private LogicalMemoryBuffer(ProcessIdentifier tid,
				AddressSpace addressSpace,
				BreakpointAddresses breakpoints,
				long lower, long upper) {
	super(tid, addressSpace, lower, upper);
	this.breakpoints = breakpoints;
    }
  
    /**
     * Package local contructor used by LinuxTask to create a logical
     * memory space for a task when requested.
     */
    LogicalMemoryBuffer(ProcessIdentifier tid,
			AddressSpace addressSpace,
			BreakpointAddresses breakpoints) {
	super(tid, addressSpace);
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
  
  protected int peek(long index, byte[] bytes, int offset, int length)
  {
    synchronized (breakpoints)
      {
	Iterator it;
	it = breakpoints.getBreakpoints(index, index + length);
	int r = 0;
	while (it.hasNext())
	  {
	    Breakpoint breakpoint = (Breakpoint) it.next();
	    // address - index falls inside the byte[] so will be at most
	    // a positive int apart.
	    int l = (int) (breakpoint.getAddress() - index) - r;
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
	    bytes[offset + r] = b;
	    r++;
	  }
	return super.peek(index + r, bytes, offset + r, length - r) + r;
      }
  }

  /**
   * Pokes the value at the given index. Unless a breakpoint is set at
   * that location (FIXME: this limitation should be lifted).
   */
  protected void poke (long index, int value)
  {
    Breakpoint breakpoint = breakpoints.getBreakpoint(index);
    if (breakpoint != null)
      throw new UnsupportedOperationException("breakpoint set at: " + index);

    super.poke(index, value);
  }
  
  /**
   * Pokes the given bytes from offset at the index plus the given
   * lenght. Unless a breakpoint is set in that range (FIXME: this
   * limitation should be lifted).
   */
  protected int poke(long index, byte[] bytes, int offset, int length)
  {
    synchronized (breakpoints)
      {
	Iterator it;
	it = breakpoints.getBreakpoints(index, index + length);
	if (it.hasNext())
	  throw new UnsupportedOperationException("breakpoint set between "
						  + index + " and "
						  + index + length);
      }

    return super.poke(index, bytes, offset, length);
  }

  protected ByteBuffer subBuffer(ByteBuffer parent,
				 long lower, long upper)
  {
    LogicalMemoryBuffer sub = (LogicalMemoryBuffer) parent;
    return new LogicalMemoryBuffer (sub.pid, sub.addressSpace,
				    sub.breakpoints,
				    lower, upper);
  }
}
