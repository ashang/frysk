// This file is part of the program FRYSK.
//
// Copyright 2005, 2006 Red Hat Inc.
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

import java.util.HashMap;
import java.util.Iterator;
import inua.eio.ByteOrder;
import inua.eio.ByteBuffer;

/**
 * Instruction Set Architecture.
 *
 */

public interface Isa
{
  /**
   * Return an iterator that will iterate over every user-visible
   * register in the <code>Isa</code>.
   *
   * @return the iterator
   */
  Iterator RegisterIterator();

  String getRegisterNameByUnwindRegnum(long regnum);

  /**
   * Get a <code>Register</code> object by name.
   *
   * @param name the register name
   * @return a register object
   */
  Register getRegisterByName(String name);

  /**
   * Get the value of the program counter in a task.
   * @param task the task
   * @return program counter, which might be negative!
   */
  long pc(Task task);
  
  /**
   * Get the word size.
   *
   * @return  word size in bytes.
   */
  int getWordSize();

  /**
   * Get <code>ByteOrder</code> for this <code>Isa</code>.
   *
   * @return the byte order.
   */
  ByteOrder getByteOrder();
  
  /**
   * Get the breakpoint instruction.
   * 
   * @return bytes[] the instruction of the ISA.
   */
  byte[] getBreakpointInstruction();
 
  /**
   * Get the true breakpoint address according to PC register after hitting 
   * one breakpoint set in task. Different arch will take different action
   * when it hit one breakpoint. In X86/X86-64, the length of the breakpoint
   * instruction will be added to the PC register.However in PPC64, the PC 
   * register's value will remain unchanged. 
   * 
   * The function will take different actions according to task's ISA.
   * 
   */
  long getBreakpointAddress(Task task);

  /**
   * Reports whether or not the given Task just did a step of an
   * instruction.
   */
  boolean isTaskStepped(Task task);

  /**
   * Returns true if the last instruction executed by the given Task was
   * a trapping instruction. This method should distinquish instructions
   * that are handled by the kernel (like syscall enter instructions) and
   * those that generate a trap signal. True is returned only when the
   * instruction should generate a signal. Called from the state machine
   * when a trap event has been detected that cannot be attributed to
   * entering a signal handler or a normal step instruction notification.
   */
  boolean hasExecutedSpuriousTrap(Task task);

  /**
   * Return an array of ByteBuffers for accessing the register
   * banks. It's possible for different elements of the array to be
   * shared.
   *
   * @returns array of ByteBuffer objects.
   */
  ByteBuffer[] getRegisterBankBuffers(int pid);
  
  // int addressSize;
  // InstructionSet;
  // FloatingPointFormat;
  // Breakpoint;
  // howToDoWatchpoints;
  // howToComputePcAfterTrap;
  // howToStepOutOfRange;

  /**
   * Return the System Call Decoder.
   *
   * XXX: This ISA object should be re-named; it is really the
   * OS+ISA object.
   */
  //SyscallEventInfo getSyscallEventInfo ()
  //{
  //throw new RuntimeException ("not implemented");
  //}

  /** @return Syscall[] return system call list for this Linux<ISA>. */
  Syscall[] getSyscallList ();
  /** @return HashMap return a HashMap for unknown system calls. */
  HashMap getUnknownSyscalls ();

  /** @return Syscall return system call object if the name could be 
   * found in syscallList, otherwise return null. */
  Syscall syscallByName (String Name);

  /** @return Elf machine type */
  int getElfMachineType();
}
