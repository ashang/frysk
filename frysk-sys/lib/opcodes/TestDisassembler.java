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

package lib.opcodes;

import frysk.Config;
import frysk.junit.TestCase;
import inua.eio.ArrayByteBuffer;
import inua.eio.ByteBuffer;
import java.util.List;

public class TestDisassembler
    extends TestCase
{
    private static class Assembler {
	/**
	 * The disassembled instructions.
	 */
	final String[] instructions;
	/**
	 * The byte opcodes.
	 */
	final byte[] opcodes;
	Assembler(String[] instructions, byte[] opcodes) {
	    this.instructions = instructions;
	    this.opcodes = opcodes;
	}
    }

    private static final Assembler powerPc = new Assembler
	(new String[] { 
	    "bctr",
	    "sc",
	    "blr",
	    "nop",
	    "li      r0,2",
	}, new byte[] {
	     0x4e, (byte)0x80, 0x04, 0x20, // bctr
	     0x44, 0x00, 0x00, 0x02,       // sc
	     0x4e, (byte)0x80, 0x00, 0x20, // blr
	     0x60, 0x00, 0x00, 0x00,       // nop
	     0x38, 0x00, 0x00, 0x02,       // li      r0,2
	 });
    private static final Assembler x8664 = new Assembler
	(new String[] {
	    "add    %al,(%rcx)",
	    "add    (%rbx),%al",
	    "add    $0x5,%al",
	    "(bad)",
	    "(bad)"
	}, new byte[] {
	     0, 1, // add    %al,(%rcx) 
	     2, 3, // add    (%rbx),%al
	     4, 5, // add    $0x5,%al
	     6,    // (bad)
	     7     // (bad)
	 });
    private static final Assembler i386 = new Assembler
	(new String[] {
	    "add    %al,(%ecx)",
	    "add    (%ebx),%al", 
	    "add    $0x5,%al", 
	    "push   %es",
	    "pop    %es" 
	}, new byte[] {
	     0, 1, // add    %al,(%ecx)
	     2, 3, // add    (%ebx),$al
	     4, 5, // add    $0x5,%al
	     6,    // push   %es
	     7     // pop    %es
	 });
    private Assembler getAssembler() {
	if (Config.getTargetCpuXXX ().indexOf("powerpc") != - 1) {
	    // for powerpc
	    return powerPc;
	} else if (Config.getTargetCpuXXX ().indexOf("_64") != - 1) {
	    // for X86_64
	    return x8664;
	} else {
	    // for x86
	    return i386;
	}
    }
    
    public void testDisassembler() {
	if (unsupported("disassembler", !Disassembler.available()))
	    return;

	final int numInstructions = 5;
	Assembler assembler = getAssembler();
	assertEquals("assembler.instructions.length", numInstructions,
		     assembler.instructions.length);

	ByteBuffer buffer = new ArrayByteBuffer(assembler.opcodes);
	
	Disassembler disAsm = new Disassembler(buffer);
	List list = disAsm.disassembleInstructions(0, numInstructions);
	assertNotNull("list", list);
	assertEquals("list.size", numInstructions, list.size());
	
	// Address for ByteBuffer is started at 0x00.
	int address = 0;
	for (int i = 0; i < numInstructions; i++) {
	    Instruction inst = (Instruction) list.get(i);
	    assertNotNull(inst);
	    
	    assertEquals(address, inst.address);
	    //Remve tailing whiespace before compare.
	    assertEquals("assembler.instructions[" + i + "]",
			 assembler.instructions[i],
			 inst.instruction.trim());
	    
	    address += inst.length;
	}
    }
    
    public void testDisassembleStartEnd() {
	if (unsupported("disassembler", !Disassembler.available()))
	    return;
	
	final int numInstructions = 5;
	Assembler assembler = getAssembler();
	assertEquals("assembler.instructions.length", numInstructions,
		     assembler.instructions.length);

	ByteBuffer buffer = new ArrayByteBuffer(assembler.opcodes);
	
	Disassembler disAsm = new Disassembler(buffer);
	List list = disAsm.disassembleInstructionsStartEnd(0, 8);
	assertNotNull("list", list);
	assertEquals("list.size", numInstructions, list.size());
	
	// Address for ByteBuffer is started at 0x00.
	int address = 0;
	for (int i = 0; i < numInstructions; i++) {
	    Instruction inst = (Instruction) list.get(i);
	    assertNotNull(inst);
	    
	    assertEquals(address, inst.address);
	    //Remve tailing whiespace before compare.
	    assertEquals("assembler.instructions[" + i + "]",
			 assembler.instructions[i],
			 inst.instruction.trim());
	    
	    address += inst.length;
	}
    }

    public void testOutOfBounds() {
	if (unsupported("disassembler", !Disassembler.available()))
	    return;
	ByteBuffer buffer = new ArrayByteBuffer(new byte[0]);
	Disassembler disAsm = new Disassembler(buffer);
	boolean exceptionThrown = false;
	try {
	    disAsm.disassembleInstructions(0, 1);
	} catch (Exception e) {
	    exceptionThrown = true;
	}
	assertTrue("exceptionThrown", exceptionThrown);
    }
}
