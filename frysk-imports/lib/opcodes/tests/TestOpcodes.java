// This file is part of the program FRYSK.
//
// Copyright 2005, 2006, 2007 Red Hat Inc.
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


package lib.opcodes.tests;

import inua.eio.ByteBuffer;

import java.util.LinkedList;

import frysk.Config;
import frysk.junit.TestCase;
import lib.opcodes.Disassembler;
import lib.opcodes.Instruction;
import lib.opcodes.OpcodesException;

public class TestOpcodes
  extends TestCase
{
  public void testDisassembler ()
  {

    ByteBuffer buffer = new DummyByteBuffer();
    final int numInstructions = 16;

    Disassembler disAsm = new Disassembler(buffer);
    LinkedList list = null;
    try
      {
        list = disAsm.disassembleInstructions(0, numInstructions);
      }
    catch (OpcodesException e)
      {
        e.printStackTrace(System.err);
        fail("Exception thrown during disassembly");
      }

    assertNotNull(list);
    assertEquals(list.size(), numInstructions);

    String[] insts = new String[0];

    // for powerpc
    if (Config.getTargetCpuXXX ().indexOf("powerpc") != - 1)
      {
	insts = new String[]
	  { 
	    "bctr",
	    "sc",
	    "blr",
	    "nop",
	    "li      r0,2",
	    "addi    r12,r12,872",
	    "addi    r1,r1,112",
	    "mtctr   r11",
	    "rldicr  r5,r5,36,27"
	  };
      }
    // for X86_64
    else if (Config.getTargetCpuXXX ().indexOf("_64") != - 1)
      {

        insts = new String[] { "add    %al,(%rcx)",
                               "add    (%rbx),%al",
                               "add    $0x5,%al",
                               "(bad)",
                               "(bad)"};

      }
    // for x86
    else
      {
	insts = new String[] { "add    %al,(%ecx)",
                               "add    (%ebx),%al", 
                               "add    $0x5,%al", 
                               "push   %es",
                               "pop    %es" 
			       };
      }

    assertNotNull(insts);


    // Address for DummyByteBuffer is started at 0x00.
    int address = 0;
    for (int i = 0; i < insts.length; i++)
      {
        Instruction inst = (Instruction) list.get(i);
        assertNotNull(inst);

        assertEquals(address, inst.address);
	//Remve tailing whiespace before compare.
        assertEquals(insts[i], inst.instruction.trim());

	address += inst.length;
      }
  }
}
