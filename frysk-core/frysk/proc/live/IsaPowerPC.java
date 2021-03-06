// This file is part of the program FRYSK.
//
// Copyright 2006 IBM Corp.
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

import java.util.LinkedList;
import java.util.List;
import inua.eio.ByteBuffer;
import frysk.isa.registers.Register;
import frysk.proc.Task;
import frysk.proc.Proc;
import frysk.proc.Auxv;

abstract class IsaPowerPC implements Isa {
    protected IsaPowerPC(Register PC) {
    }

   // the illegal instruction for powerpc: 0x7d821008.
   // the default order is BIG_ENDIAN
   protected static final Instruction ppcBreakpoint = 
   	new Instruction(new byte[] { (byte)0x7d, (byte)0x82, 
				     (byte)0x10, (byte)0x08 }, false);

  /**
   * Get the breakpoint instruction of the PowerPC platform.
   */
  public final Instruction getBreakpointInstruction()
  {
    return ppcBreakpoint;
  }

  public List getOutOfLineAddresses(Proc proc)
  {
    LinkedList addrs = new LinkedList();
    Auxv[] auxv = proc.getAuxv ();
    // Find the Auxv ENTRY data
    for (int i = 0; i < auxv.length; i++)
      {
	if (auxv[i].type == inua.elf.AT.ENTRY)
	addrs.add(Long.valueOf(auxv[i].val));
      }
    return addrs;
  }

  /**
   * Returns the instruction at the given location in the memory
   * buffer, or null if there is no valid instruction at the given
   * location. FIXME - needs a real InstructionParser!
   */
  public Instruction getInstruction(ByteBuffer bb, long addr)
  {
    // XXX assume all instructions are 4 bytes.
    bb.position(addr);
    byte[] bs = new byte[4];
    for (int i = 0; i < 4; i++)
      bs[i] = bb.getByte();
    return new Instruction(bs, false);
  }

  
  /**
   * Get the true breakpoint address according to PC register after hitting 
   * one breakpoint set in task. In PowerPC, the PC register's value will 
   * remain unchanged. 
   */
  public final long getBreakpointAddress(Task task) {
    long pcValue = 0;
    pcValue = task.getPC();
    return pcValue;
  }

  /**
   * FIXME. Not yet implemented for PowerPC platform.
   */
  public boolean isTaskStepped(Task task)
  {
    return false;
  }

  /**
   * FIXME. Not checked whether or not spurious trap events are
   * generated on PowerPC.
   */
  public boolean hasExecutedSpuriousTrap(Task task)
  {
    return false;
  }

  /**
   * Returns true if the given Task is at an instruction that will invoke
   * the sig return system call.
   *
   * FIXME On powerpc this method is not yet implemented and always
   * return false.
   */
  public boolean isAtSyscallSigReturn(Task task)
  {
    return false;
  }

}
