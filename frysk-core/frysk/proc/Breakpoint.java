// This file is part of the program FRYSK.
//
// Copyright 2006, 2007 Red Hat Inc.
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

  // Different ways to setup a breakpoint for stepping.
  private static final byte NOT_STEPPING = 0;
  private static final byte OUT_OF_LINE_STEPPING = 1;
  private static final byte SIMULATE_STEPPING = 2;
  private static final byte RESET_STEPPING = 3;

  // Whether the breakpoint is setup for a step instruction.
  // And if so how according to one of the above constants.
  private byte stepping;

  // Static cache of installed break points.
  private static HashMap installed = new HashMap();

  // The original instruction at the location we replaced with
  // BREAKPOINT_INSTRUCTION.
  private Instruction origInstruction;

  // The address to execute out of line if any.
  private long oo_address;

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
   * when it is installed and not in the middle of a step.
   */
  public void remove(Task task)
  {
    if (stepping != NOT_STEPPING)
      throw new IllegalStateException("Currently stepping: " + this);

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
    ByteBuffer buffer;
    
    buffer = task.getMemory();
    buffer.position(address);
    
    byte[] bs = origInstruction.getBytes();
    for (int index = 0; index < bs.length; index++)
      buffer.putByte(bs[index]);
  }

  /**
   * Prepares the given Task for a step over the breakpoint.
   * This sets up the program counter and makes sure the next
   * instruction is the one on which the breakpoint was placed.
   * Should not be called again until <code>stepDone</code> is
   * called. The given Task should be stopped and the intention
   * is that the Task immediately tries to do a step and stepDone()
   * called immediately afterwards (since this method might
   * temporarily adjust registers for this Task that get cleaned up
   * by stepDone() afterwards).
   */
  public void prepareStep(Task task)
  {
    if (stepping != NOT_STEPPING)
      throw new IllegalStateException("Already stepping");

    // We like out of line stepping above simulating the instruction
    // and if neither is possible we reset the instruction and risk
    // other Tasks missing the breakpoint (FIXME: the right way in
    // that case would be to stop all other Tasks in the Proc first.)
    if (origInstruction.canExecuteOutOfLine())
      {
	// Proc will collect an address for our usage, our wait
	// till one if available. We need to return it to Proc
	// afterwards in stepDone().
	stepping = OUT_OF_LINE_STEPPING;
	oo_address = proc.getOutOfLineAddress();
	origInstruction.setupExecuteOutOfLine(task, address, oo_address);
      }
    else if (origInstruction.canSimulate())
      {
	// FIXME: We haven't actually abstracted this correctly.  No
	// task step is really needed here, so a task step will step
	// the next instruction after this simulation before calling
	// stepDone().  Luckily no Instructions can actually simulate
	// themselves at this time.  stepDone() will warn if it does
	// happen in the future.
	stepping = SIMULATE_STEPPING;
	origInstruction.simulate(task);
      }
    else
      {
	// WARNING, WILL ROBINSON!
	stepping = RESET_STEPPING;
	reset(task);
      }
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
	if (stepping == NOT_STEPPING)
	  throw new IllegalStateException("Not stepping");
	else if (stepping == OUT_OF_LINE_STEPPING)
	  {
	    // Fixup any registers depending on the instruction being
	    // at the original pc address. And let Proc know the address
	    // is available again.
	    origInstruction.fixupExecuteOutOfLine(task, address, oo_address);
	    proc.doneOutOfLine(oo_address);
	  }
	else if (stepping == SIMULATE_STEPPING)
	  {
	    // FIXME: See prepareStep().
	    System.err.println("Instruction simulation not finished! "
			       + "Already stepped next instruction. Sorry.");
	  }
	else if (stepping == RESET_STEPPING)
	  {
	    // Put the breakpoint instruction quickly back.
	    set(task);
	  }
	else
	  throw new IllegalStateException("Impossible stepping state: "
					  + stepping);
      }

    stepping = NOT_STEPPING;
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
