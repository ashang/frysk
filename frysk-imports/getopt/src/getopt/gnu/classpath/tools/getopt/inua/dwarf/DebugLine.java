// This file is part of INUA.  Copyright 2004, 2005, Andrew Cagney
//
// INUA is free software; you can redistribute it and/or modify it
// under the terms of the GNU General Public License as published by
// the Free Software Foundation; version 2 of the License.
//
// INUA is distributed in the hope that it will be useful, but
// WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
// General Public License for more details.
// 
// You should have received a copy of the GNU General Public License
// along with INUA; if not, write to the Free Software Foundation,
// Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
// 
// In addition, as a special exception, Andrew Cagney. gives You the
// additional right to link the code of INUA with code not covered
// under the GNU General Public License ("Non-GPL Code") and to
// distribute linked combinations including the two, subject to the
// limitations in this paragraph. Non-GPL Code permitted under this
// exception must only link to the code of INUA through those well
// defined interfaces identified in the file named EXCEPTION found in
// the source code files (the "Approved Interfaces"). The files of
// Non-GPL Code may instantiate templates or use macros or inline
// functions from the Approved Interfaces without causing the
// resulting work to be covered by the GNU General Public
// License. Only Andrew Cagney may make changes or additions to the
// list of Approved Interfaces. You must obey the GNU General Public
// License in all respects for all of the INUA code and other code
// used in conjunction with INUA except the Non-GPL Code covered by
// this exception. If you modify this file, you may extend this
// exception to your version of the file, but you are not obligated to
// do so. If you do not wish to provide this exception without
// modification, you must delete this exception statement from your
// version and license this file solely under the GPL without
// exception.
package inua.dwarf;

import inua.eio.ByteBuffer;

class DebugLine
    extends DebugSection
{

    public DebugLine (ByteBuffer section)
    {
	super (section);
    }

    public static class Builder
	extends DebugBuilder
    {
	Action buildProgramHeader (ProgramHeader header)
	{
	    return Action.PARSE;
	}
	Action buildSpecialInstruction (int adjustedOpcode,
					int addressIncrement,
					int lineIncrement,
					StateMachine sm)
	{
	    return Action.PARSE;
	}
	Action buildStandardInstruction (int opcode, long[] operands,
					 StateMachine sm)
	{
	    return Action.PARSE;
	}
	Action buildExtendedInstruction (int extendedOpcode,
					 long instruction, int numBytes,
					 StateMachine sm)
	{
	    return Action.PARSE;
	}
	Action buildProgramFooter ()
	{
	    return Action.PARSE;
	}
    }

    public void construct (Builder builder)
    {
	while (hasRemaining ()) {
	    parseLineNumberProgram (builder);
	}
    }

    private final void parseLineNumberProgram (Builder builder)
    {
	ProgramHeader header = new ProgramHeader ();
	if (builder.buildProgramHeader (header)
	    == DebugBuilder.Action.PARSE)
	    parseLineNumberProgramBody (builder, header);
	position (header.programLimit);
	builder.buildProgramFooter ();
    }

    public class File
    {
	public String name;
	public int includeDirectory;
	public long modificationTime;
	public long length;
    }

    public class ProgramHeader
    {
    	Format format;
	long programLimit;
	public long unitLength;
	public int version;
	public long headerLength;
	public short minimumInstructionLength;
	public boolean defaultIsStmt;
	public byte lineBase;
	public short lineRange;
	public short opcodeBase;
	public byte[] standardOpcodeLengths;
	public String[] includeDirectories;
	public File[] fileNames;

	// Suplementary structures constructed using information
	// ained from the header.
	protected long[] standardOperand;
	protected int constantAddPcOperand;
	
	ProgramHeader ()
	{
	    java.util.ArrayList list = new java.util.ArrayList ();
	    StringBuffer string = new StringBuffer ();

	    format = getFormat ();
	    unitLength = getInitialLength (format);
	    programLimit = position () + unitLength;

	    version = getUHALF ();
	    headerLength = getUWORD (format);
	    minimumInstructionLength = getUBYTE ();
	    defaultIsStmt = getUBYTE () != 0;
	    lineBase = getSBYTE ();
	    lineRange = getUBYTE ();
	    opcodeBase = getUBYTE ();

	    // Hack, shouldn't be allocating a byte array
	    standardOpcodeLengths = new byte[opcodeBase];
	    get (standardOpcodeLengths, 1, opcodeBase - 1);
	    
	    // Hack, shouldn't be reading the strings
	    list.clear ();
	    list.add(".");
	    while (hasRemaining ()) {
		get (string);
		if (string.length () == 0)
		    break;
		list.add (string.toString ());
	    }
	    includeDirectories = (String[]) list.toArray (new String[0]);
	
	    // Hack shouldn't be reading any of this
	    list.clear ();
	    list.add (null);
	    while (hasRemaining ()) {
		File file = new File ();
		get (string);
		if (string.length () == 0)
		    break;
		file.name = string.toString ();
		file.includeDirectory = (int) getUnsignedLEB128 ();
		file.modificationTime = getUnsignedLEB128 ();
		file.length = getUnsignedLEB128 ();
		list.add (file);
	    }
	    fileNames = (File[]) list.toArray (new File[0]);

	    // Implied operand for DW.LNS.const_add_pc.
	    constantAddPcOperand = (((255 - opcodeBase) / lineRange)
				    * minimumInstructionLength);

	    // Buffer to contain all the standard operands.
	    int maxOpcodeLength = 0;
	    for (int i = 1; i < standardOpcodeLengths.length; i++) {
		if (maxOpcodeLength < standardOpcodeLengths[i])
		    maxOpcodeLength = standardOpcodeLengths[i];
	    }
	    standardOperand = new long[maxOpcodeLength];
	}
    }

    private final void parseLineNumberProgramBody (Builder builder,
						   ProgramHeader header)
    {
	while (position () < header.programLimit) {
	    parseLineNumberSequence (builder, header);
	}
    }

    private final void parseLineNumberSequence (Builder builder,
						ProgramHeader header)
    {
	stateMachine.init (header);
	while (position () < header.programLimit) {
	    int opcode = getUBYTE ();
	    if (opcode >= header.opcodeBase) {
		parseSpecialInstruction (builder, header, opcode);
	    }
	    else if (opcode > 0) {
		parseStandardInstruction (builder, header, opcode);
	    }
	    else {
		opcode = parseExtendedInstruction (builder, header);
		if (opcode == DW.LNE.end_sequence)
		    break;
	    }
	}
    }

    private final void parseSpecialInstruction (Builder builder,
						ProgramHeader header,
						int opcode)
    {
	int adjustedOpcode;
	int addressIncrement;
	int lineIncrement;

	adjustedOpcode = opcode - header.opcodeBase;
	addressIncrement = ((adjustedOpcode / header.lineRange)
			    * header.minimumInstructionLength);
	lineIncrement = (header.lineBase
			 + (adjustedOpcode % header.lineRange));
	stateMachine.line += lineIncrement;
	stateMachine.address += addressIncrement;
	stateMachine.basicBlock = false;
	stateMachine.prologueEnd = false;
	stateMachine.epilogueBegin = false;

	builder.buildSpecialInstruction (adjustedOpcode, addressIncrement,
					 lineIncrement, stateMachine);
    }

    private final void parseStandardInstruction (Builder builder,
						 ProgramHeader header,
						 int opcode)
    {
	switch (opcode) {
	case DW.LNS.copy:
	    stateMachine.basicBlock = false;
	    stateMachine.prologueEnd = false;
	    stateMachine.epilogueBegin = false;
	    break;
	case DW.LNS.advance_pc:
	    header.standardOperand[0]
		= (getUnsignedLEB128 () * header.minimumInstructionLength);
	    stateMachine.address += header.standardOperand[0];
	    break;
	case DW.LNS.advance_line:
	    header.standardOperand[0] = getSignedLEB128 ();
	    stateMachine.line += header.standardOperand[0];
	    break;
	case DW.LNS.set_file:
	    header.standardOperand[0] = getUnsignedLEB128 ();
	    stateMachine.file = (int) header.standardOperand[0];
	    break;
	case DW.LNS.const_add_pc:
	    header.standardOperand[0] = header.constantAddPcOperand;
	    stateMachine.address += header.standardOperand[0];
	    break;
	default:
	    int numOperands = header.standardOpcodeLengths[opcode];
	    System.err.print(".debug_line: ignoring standard opcode "
			     + DW.LNS.toPrintString (opcode)
			     + ":");
	    for (int i = 0; i < numOperands; i++) {
		header.standardOperand[i] = getSignedLEB128 ();
		System.err.print (" " + header.standardOperand[i]);
	    }
	    System.err.println ();
	}
	builder.buildStandardInstruction (opcode, header.standardOperand,
					  stateMachine);
    }

    private final int parseExtendedInstruction (Builder builder,
						ProgramHeader header)
    {
	int numBytes = (int) getSignedLEB128 ();
	long instruction = position ();
	int extendedOpcode = getUBYTE ();
	switch (extendedOpcode) {
	case DW.LNE.end_sequence:
	    break;
	case DW.LNE.set_address:
	    stateMachine.address = getU (instruction + 1, numBytes - 1);
	    break;
	default:
	    System.err.println(".debug_line: ignoring extended opcode "
			       + DW.LNE.toPrintString (extendedOpcode));
	}
	builder.buildExtendedInstruction (extendedOpcode, instruction,
					  numBytes, stateMachine);
	position (instruction + numBytes);
	return extendedOpcode;
    }

    StateMachine stateMachine = new StateMachine ();
    public class StateMachine
    {
	public long address;
	int file;
	public int line;
	int column;
	boolean isStmt;
	boolean basicBlock;
	boolean endSequence;
	boolean prologueEnd;
	boolean epilogueBegin;
	int isa;
	void init (ProgramHeader header)
	{
	    address = 0;
	    file = 1;
	    line = 1;
	    column = 0;
	    isStmt = header.defaultIsStmt;
	    basicBlock = false;
	    endSequence = false;
	    prologueEnd = false;
	    epilogueBegin = false;
	    isa = 0;
	}
    }
}

