// This file is part of the program FRYSK.
//
// Copyright 2006, 2007, 2008 Red Hat Inc.
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

import java.util.HashMap;

import frysk.proc.Task;

/**
 * Internal proc class that represents a Breakpoint at a certain
 * address in a Proc. Some attempts are made to have synchronize
 * different Breakpoint instances at the same address in the same
 * Proc, but currently this isn't a full singleton.
 */
public class Watchpoint implements Comparable
{
  // These two fields define a Breakpoint
  private final long address;
  private final int length;
  private final Task task;


  // Static cache of installed watchpoints.
  private static HashMap installedWatchpoints = new HashMap();



  /**
   * Private constructor called by create to record address and
   * proc.
   */
  private Watchpoint(long address, int length, Task task)
  {
    if (task == null)
      throw new NullPointerException("Cannot place a watchpoint when task == null.");

    this.address = address;
    this.task = task;
    this.length = length;

    if (this.length <= 0)
	throw new RuntimeException("Watchpoint length has to be > 0");
  }

  /**
   * Creates a Breakpoint for the Proc at the given Address but does
   * not set it yet.  Returns the appropriate Breakpoint depending on
   * host type. If a Breakpoint for this address and proc is already
   * installed that Breakpoint will be returned.
   */
  public static Watchpoint create(long address, int length, Task task)
  {
    Watchpoint watchpoint = new Watchpoint(address, length, task);

    // If possible return an existing installed breakpoint.
    synchronized (installedWatchpoints)
      {
	Watchpoint existing = (Watchpoint) installedWatchpoints.get(watchpoint);
	if (existing != null)
	  return existing;
      }
    return watchpoint;
  }

  public long getAddress()
  {
    return address;
  }


  /**
   * Installs breakpoint. Caller must make sure there is no breakpoint set
   * at that address yet and that install() is not called again till remove()
   * is called on it.
   */
  public void install(Task task)
  {
    synchronized (installedWatchpoints)
      {
	Watchpoint existing = (Watchpoint) installedWatchpoints.get(this);
	if (existing != null)
	  throw new IllegalStateException("Watchpoint Already installed: " + this);

	installedWatchpoints.put(this, this);
    
	set(task);
      }
  }

  /**
   * Actually sets the breakpoint.
   */
  private void set(Task task)
  {
//      ByteBuffer buffer = ((LinuxPtraceTask)task).getRawMemory();
//    Isa isa = ((LinuxPtraceTask)task).getIsaFIXME();
//    Instruction bpInstruction = isa.getBreakpointInstruction();
//    
//    origInstruction = isa.getInstruction(buffer, address);
//
//    // Put in the breakpoint.
//    byte[] bs = bpInstruction.getBytes();
//    buffer.position(address);
//    buffer.put(bs);
  }

  /**
   * Removes the breakpoint. Caller must make sure it is called only
   * when it is installed and not in the middle of a step.
   */
  public void remove(Task task)
  {
    synchronized (installedWatchpoints)
      {
	if (! this.equals(installedWatchpoints.remove(this)))
	  throw new IllegalStateException("Not installed: " + this);

	reset(task);
      }
  }

  /**
   * Actually removes the breakpoint.
   */
  private void reset(Task task)
  {
//      ByteBuffer buffer = ((LinuxPtraceTask)task).getRawMemory();
//    buffer.position(address);
//    
//    Isa isa = ((LinuxPtraceTask)task).getIsaFIXME();
//    Instruction bpInstruction = isa.getBreakpointInstruction();
//    byte[] bp = bpInstruction.getBytes();
//
//    byte[] bs = origInstruction.getBytes();
//
//    // Only need to put back the part of the original instruction
//    // covered by the breakpoint instruction bytes.
//    buffer.put(bs, 0, bp.length);
  }


  /**
   * Returns the Proc to which this breakpoint belongs.
   */
  public Task getTask()
  {
    return this.task;
  }

  /**
   * Returns true if break point is installed and not yet removed.
   */
  public boolean isInstalled()
  {
    synchronized(installedWatchpoints)
      {
	return this.equals(installedWatchpoints.get(this));
      }
  }

  // Utility methods for keeping the map of breakpoints.

  public int hashCode()
  {
    return (int) (address ^ (address >>> 32));
  }

  public boolean equals(Object o)
  {
    if (o == null || o.getClass() != this.getClass())
      return false;

    Watchpoint other = (Watchpoint) o;
    return other.task.equals(task) && other.address == address;
  }

  /**
   * Uses natural ordering on address.
   */
  public int compareTo(Object o) 
  {
    Watchpoint other = (Watchpoint) o;
    return (int) (this.address - other.address);
  }

  public String toString()
  {
    return this.getClass().getName() + "[task=" + task
      + ", address=0x" + Long.toHexString(address) + "]";
  }
}
