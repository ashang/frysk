// This file is part of the program FRYSK.
//
// Copyright 2006, Red Hat Inc.
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


package frysk.proc;

import inua.eio.ByteBuffer;

import java.util.HashMap;

/**
 * Internal proc class that represents a Breakpoint at a certain
 * address in a Proc.
 */
public class Breakpoint
{
  // These two fields define a Breakpoint
  private final long address;
  private final Proc proc;

  // Whether the breakpoint is setup for a step instruction.
  private boolean stepping;

  // Static cache of installed break points.
  private static HashMap installed = new HashMap();

  // The original instruction at the location we replaced with
  // BREAKPOINT_INSTRUCTION.
  private Instruction origInstruction;

  /**
   * Private constructor called by create to record address and
   * proc.
   */
  private Breakpoint(long address, Proc proc)
  {
    if (proc == null)
      throw new NullPointerException("proc");

    this.address = address;
    this.proc = proc;
  }

  /**
   * Creates a Breakpoint for the Proc at the given Address but does
   * not set it yet.  Returns the appropriate Breakpoint depending on
   * host type. If a Breakpoint for this address and proc is already
   * installed that Breakpoint will be returned.
   */
  public static Breakpoint create(long address, Proc proc)
  {
    Breakpoint breakpoint = new Breakpoint(address, proc);

    // If possible return an existing installed breakpoint.
    synchronized (installed)
      {
	Breakpoint existing = (Breakpoint) installed.get(breakpoint);
	if (existing != null)
	  return existing;
      }
    return breakpoint;
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
    synchronized (installed)
      {
	Breakpoint existing = (Breakpoint) installed.get(this);
	if (existing != null)
	  throw new IllegalStateException("Already installed: " + this);

	installed.put(this, this);
    
	set(task);
	// This throws RuntimeException for the following two reasons:
	// 1st) thrown out by Task.getIsa() in set(), if the
	//      frysk works well, the exception shouldnot be thrown out. If we
	//      get one, it mean some runtime exception occurs.
	// 2nd) This method will be called in
	//      TaskState.Running.handleStoppedEvent().
	//      There's no exception hanling there. So we have to throw one
	//      RuntimeException here.
      }
  }

  /**
   * Actually sets the breakpoint.
   */
  private void set(Task task)
  {
    ByteBuffer buffer = task.getMemory();
    Isa isa = task.getIsa();
    Instruction bpInstruction = isa.getBreakpointInstruction();
    
    origInstruction = isa.getInstruction(buffer, address);

    // Put in the breakpoint.
    byte[] bs = bpInstruction.getBytes();
    buffer.position(address);
    for (int index = 0; index < bs.length; index++)
      buffer.putByte(bs[index]);
  }

  /**
   * Removes the breakpoint. Caller must make sure it is called only
   * when it is installed.
   */
  public void remove(Task task)
  {
    synchronized (installed)
      {
	if (! this.equals(installed.remove(this)))
	  throw new IllegalStateException("Not installed: " + this);

	reset(task);
      }
  }

  /**
   * Actually removes the breakpoint.
   */
  private void reset(Task task)
  {
    ByteBuffer buffer = null;
    
    buffer = task.getMemory();
    buffer.position(address);
    
    byte[] bs = origInstruction.getBytes();
    for (int index = 0; index < bs.length; index++)
      buffer.putByte(bs[index]);
  }

  // XXX Prepare step and step done are not multi-task safe.

  /**
   * Prepares the given Task for a step over the breakpoint.
   * This sets up the program counter and makes sure the next
   * instruction is the one on which the breakpoint was placed.
   * Should not be called again until <code>stepDone</code> is
   * called. The given Task should be stopped.
   */
  public void prepareStep(Task task)
  {
    if (stepping)
      throw new IllegalStateException("Already stepping");

    reset(task);
    stepping = true;
  }

  /**
   * Notifies the breakpoint that a step has just taken place
   * and that the task should be put into a state ready to continue
   * (keeping the breakpoint in place).
   */
  public void stepDone(Task task)
  {
    if (isInstalled())
      {
	if (! stepping)
	  throw new IllegalStateException("Not stepping");

	set(task);
      }

    // This throws RuntimeException for the following two reasons:
    // 1st) thrown out by Task.getIsa() in set(), if the
    //      frysk works well, the exception shouldnot be thrown out. If we
    //      get one, it mean some runtime exception occurs.
    // 2nd) setDone() will be called in
    //      TaskState.Running.handleTrappedEvent().
    //      There's no exception hanling there. So we have to throw one
    //      RuntimeException here.
    
    stepping = false;
  }

  /**
   * Returns true if break point is installed and not yet removed.
   */
  public boolean isInstalled()
  {
    synchronized(installed)
      {
	return this.equals(installed.get(this));
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

    Breakpoint other = (Breakpoint) o;
    return other.proc.equals(proc) && other.address == address;
  }

  public String toString()
  {
    return this.getClass().getName() + "[proc=" + proc
      + ", address=0x" + Long.toHexString(address) + "]";
  }
}
