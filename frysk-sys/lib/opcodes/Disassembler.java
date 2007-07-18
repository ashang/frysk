// This file is part of the program FRYSK.
//
// Copyright 2005, Red Hat Inc.
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

import java.util.LinkedList;
import java.util.List;
import inua.eio.ByteBuffer;

public class Disassembler
{
    private List isnList = new LinkedList();
    private StringBuffer instruction = new StringBuffer();
    private final ByteBuffer memory;
    public Disassembler (ByteBuffer memory)
    {
	this.memory = memory;
    }
    
    public List disassembleInstructions (long address, long count) {
	isnList.clear();
	// This function will call the below java methods to update
	// the linked list.
	disassemble(address, count);
	return isnList;
    }
    
    public List disassembleInstructionsStartEnd (long startAddress, 
	    long endAddress) {
	isnList.clear();
	// This function will call the below java methods to update
	// the linked list.
	disassembleStartEnd(startAddress, endAddress);
	return isnList;
    }
    
    void startInstruction() {
	instruction.setLength(0);
    }
    void endInstruction(long address, int length) {
	isnList.add(new Instruction(address, length, instruction.toString()));
    }
    /**
     * See opcodes fprintf_func.
     */
    void printInstruction(String text) {
	instruction.append(text);
    }
    /**
     * See opcodes print_address_func.
     */
    void printAddress(long address) {
	instruction.append("0x").append(Long.toHexString(address));
    }

    /**
     * See opcodes print_address_func.
     */
    byte readMemory(long address) {
	return memory.getByte(address);
    }

    private native void disassemble (long address, long count);
    
    private native void disassembleStartEnd (long startAddress, long endAddress);
}
