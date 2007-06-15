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

/**
 * An architecture independent way of representing an assembly level
 * instruction which is used for stepping and breakpoint insertion.
 * Possibly knows about displaced instruction execution and/or
 * emultation. Constructed by archtecture Isas given a memory buffer
 * and address of the instruction.
 * 
 * @see Isa.getInstruction(Buffer, long)
 */
public class Instruction
{
  // The instruction bytes.
  private final byte[] instr;

  // Whether the instruction can be done out-of-line.
  private final boolean executeOutOfLine;

  /**
   * Package private constructor called by the Isa create an inmutable
   * Instruction.
   */
  Instruction(byte[] instr, boolean executeOutOfLine)
  {
    this.instr = (byte[]) instr.clone();
    this.executeOutOfLine = executeOutOfLine;
  }

  public byte[] getBytes()
  {
    return (byte[]) instr.clone();
  }

  public boolean canExecuteOutOfLine()
  {
    return executeOutOfLine;
  }

  // Currently this only models simple out of line execution
  // and no emulation. For more complicated instructions some
  // register fixup() will be necessary.
}
