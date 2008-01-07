// This file is part of the program FRYSK.
//
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


package frysk.proc;

import inua.eio.ByteBuffer;

/**
 * An architecture independent way of representing an assembly level
 * instruction which is used for stepping and breakpoint insertion.
 * Possibly knows about displaced instruction execution and/or
 * emultation. Constructed by archtecture Isas given a memory buffer
 * and address of the instruction. Instruction objects are immutable.
 * 
 * @see Isa.getInstruction(Buffer, long)
 */
public class Instruction
{
  // Opcode name
  private final String name;

  // The instruction bytes.
  private final byte[] instr;

  // Whether the instruction can be done out-of-line.
  private final boolean executeOutOfLine;

  // Whether the instruction can be simulated.
  private final boolean simulate;

  /**
   * Package private constructor called by the Isa ito create an inmutable
   * Instruction.
   */
  Instruction(String name, byte[] instr,
	      boolean executeOutOfLine, boolean simulate)
  {
    this.name = name;
    this.instr = (byte[]) instr.clone();
    this.executeOutOfLine = executeOutOfLine;
    this.simulate = simulate;
  }

  Instruction(byte[] instr)
  {
    this(instr, false);
  }

  Instruction(String name, byte[] instr)
  {
    this(name, instr, false);
  }

  Instruction(byte[] instr, boolean executeOutOfLine)
  {
    this("UNKNOWN", instr, executeOutOfLine, false);
  }

  Instruction(byte[] instr, boolean executeOutOfLine, boolean simulate)
  {
    this("UNKNOWN", instr, executeOutOfLine, simulate);
  }

  Instruction(String name, byte[] instr, boolean executeOutOfLine)
  {
    this(name, instr, executeOutOfLine, false);
  }

  /**
   * Returns a human readable string representation of the instruction.
   * Often just the opcode name, but can include any instruction arguments.
   */
  public String getName()
  {
    return name;
  }

  /**
   * Returns the raw instruction bytes.
   */
  public byte[] getBytes()
  {
    return (byte[]) instr.clone();
  }

  /**
   * Whether or not this instruction can be executed out of line.
   */
  public boolean canExecuteOutOfLine()
  {
    return executeOutOfLine;
  }

  /**
   * Prepares the given Task for executing this instruction at the
   * given address. Pair with fixupExecuteOutOfLine after a Task step
   * over the out of line instruction to setup the Task as if the Task
   * did a step over the instruction at the given pc.
   * <p>
   * The default implementation puts the bytes of the instruction at
   * address and sets the pc of the Task to that address. Override when
   * the instruction needs anything more.
   */
  public void setupExecuteOutOfLine(Task task, long pc, long address)
  {
    ByteBuffer buffer = task.getRawMemory();
    buffer.position(address);
    buffer.put(instr);
    task.setPC(address);
  }

  /**
   * After the instruction has been executed out of line fixes up the
   * given Task as if the instruction was actually executed at the
   * given pc instead of the given address.
   * <p>
   * The default implementation just sets the pc at the given pc plus
   * the length of this instruction. Override when the instruction needs
   * to do anything else.
   */
  public void fixupExecuteOutOfLine(Task task, long pc, long address) {
      task.setPC(pc + instr.length);
  }

  /**
   * Whether or not this instruction can be emulated.
   */
  public boolean canSimulate()
  {
    return simulate;
  }

  /**
   * The default implementation just does a sanity check and throws
   * an exception if simulation was requested on a instruction that
   * cannot be emulated.
   */
  public void simulate(Task task)
  {
    if (! simulate)
      throw new IllegalStateException("Cannot simulate instruction");
    else
      throw new IllegalStateException("Can simulate instruction,"
				      + " but Instruction doesn't override"
				      + " simultate() method: "
				      + this);
  }

  /**
   * A human readable representation of the Instruction including the
   * class name, the opcode name and the instruction bytes.
   */
  public String toString()
  {
    StringBuilder sb = new StringBuilder(this.getClass().getName());
    sb.append("[");
    sb.append(getName());
    sb.append(", 0x");
    for (int i = 0; i < instr.length; i++)
      sb.append(Integer.toHexString(instr[i] & 0xff));
    sb.append("]");
    return sb.toString();
  }
}
