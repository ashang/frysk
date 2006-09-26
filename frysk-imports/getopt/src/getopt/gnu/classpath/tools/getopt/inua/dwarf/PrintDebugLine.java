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

import inua.util.PrintWriter;
import inua.util.Print;

public class PrintDebugLine
    implements Print
{
    DebugLine debugLineSection;
    public PrintDebugLine (DebugLine debugLineSection)
    {
	this.debugLineSection = debugLineSection;
    }
    public PrintDebugLine (Dwarf dwarf)
    {
	this (dwarf.getDebugLine ());
    }

    class PrintLineBuilder
	extends DebugLine.Builder
    {
	PrintWriter o;

	PrintLineBuilder (PrintWriter f)
	{
	    o = f;
	}

	Action buildProgramHeader (DebugLine.ProgramHeader h)
	{
	    o.println ();
	    o.print ("  Length:                      ");
	    o.print (h.unitLength);
	    o.println ();
	    o.print ("  DWARF Version:               ");
	    o.print (h.version);
	    o.println ();
	    o.print ("  Prologue Length:             ");
	    o.print (h.headerLength);
	    o.println ();
	    o.print ("  Minimum Instruction Length:  ");
	    o.print (h.minimumInstructionLength);
	    o.println ();
	    o.print ("  Initial value of 'is_stmt':  ");
	    o.print (h.defaultIsStmt ? 1 : 0);
	    o.println ();
	    o.print ("  Line Base:                   ");
	    o.print (h.lineBase);
	    o.println ();
	    o.print ("  Line Range:                  ");
	    o.print (h.lineRange);
	    o.println ();
	    o.print ("  Opcode Base:                 ");
	    o.print (h.opcodeBase);
	    o.println ();
	    o.print ("  (Pointer size:               4)");
	    o.println ();
	    o.println ();
	    o.println (" Opcodes:");
	    for (int i = 1; i < h.standardOpcodeLengths.length; i++) {
		o.print ("  Opcode ");
		o.print (i);
		o.print (" has ");
		o.print (h.standardOpcodeLengths[i]);
		o.print (" args");
		o.println ();
	    }
	    o.println ();
	    if (h.includeDirectories.length > 1) {
		o.println (" The Directory Table:");
		for (int i = 1; i < h.includeDirectories.length; i++) {
		    o.print ("  ");
		    o.print (h.includeDirectories[i]);
		    o.println ();
		}
	    }
	    else {
		o.println (" The Directory Table is empty.");
	    }
	    o.println ();
	    if (h.fileNames.length > 1) {
		o.println (" The File Name Table:");
		o.println ("  Entry	Dir	Time	Size	Name");
		for (int i = 1; i < h.fileNames.length; i++) {
		    DebugLine.File f = h.fileNames[i];
		    o.print ("  ");
		    o.print (i);
		    o.print ('\t');
		    o.print (f.includeDirectory);
		    o.print ('\t');
		    o.print (f.modificationTime);
		    o.print ('\t');
		    o.print (f.length);
		    o.print ('\t');
		    o.print (f.name);
		    o.println ();
		}
	    }
	    else {
		o.println (" The File Name Table is empty.");
	    }
	    o.println ();
	    o.println (" Line Number Statements:");
	    return Action.PARSE;
	}

	Action buildSpecialInstruction (int adjustedOpcode,
					int addressIncrement,
					int lineIncrement,
					DebugLine.StateMachine sm)
	{
	    o.print ("  Special opcode ");
	    o.print (adjustedOpcode);
	    o.print (":");
	    o.print (" advance Address by ");
	    o.print (addressIncrement);
	    o.print (" to 0x");
	    o.printx (sm.address);
	    o.print (" and Line by ");
	    o.print (lineIncrement);
	    o.print (" to ");
	    o.print (sm.line);
	    o.println ();
	    return Action.PARSE;
	}

	Action buildStandardInstruction (int opcode, long[] operand,
					 DebugLine.StateMachine sm)
	{
	    switch (opcode) {
	    case DW.LNS.copy:
		o.print ("  Copy");
		o.println ();
		break;
	    case DW.LNS.advance_pc:
		o.print ("  Advance PC by ");
		o.print (operand[0]);
		o.print (" to ");
		o.printx (sm.address);
		o.println ();
		break;
	    case DW.LNS.advance_line:
		o.print ("  Advance Line by ");
		o.print (operand[0]);
		o.print (" to ");
		o.print (sm.line);
		o.println ();
		break;
	    case DW.LNS.set_file:
		o.print ("  Set File Name to entry ");
		o.print (operand[0]);
		o.print (" in the File Name Table");
		o.println ();
		break;
	    case DW.LNS.const_add_pc:
		o.print ("  Advance PC by constant ");
		o.print (operand[0]);
		o.print (" to 0x");
		o.printx (sm.address);
		o.println ();
		break;
	    default:
		o.print ("  Standard opcode ");
		o.print (opcode);
		o.print (":");
		o.println ();
		break;
	    }
	    return Action.PARSE;
	}

	Action buildExtendedInstruction (int extendedOpcode,
					 long instruction, int numBytes,
					 DebugLine.StateMachine sm)
	{
	    o.print ("  Extended opcode ");
	    o.print (extendedOpcode);
	    o.print (":");
	    switch (extendedOpcode) {
	    case DW.LNE.end_sequence:
		o.print (" End of Sequence");
		o.println ();
		o.println ();
		break;
	    case DW.LNE.set_address:
		o.print (" set Address to 0x");
		o.printx (sm.address);
		o.println ();
		break;
	    default:
		for (int b = 1; b < numBytes; b++) {
		    o.print (' ');
		    o.print (debugLineSection.getUBYTE (instruction + b));
		}
		o.println ();
		break;
	    }
	    return Action.PARSE;
	}
    }


    public void print (PrintWriter o)
    {
	o.println ();
	o.println ("Dump of debug contents of section .debug_line:");
	debugLineSection.construct (new PrintLineBuilder (o));
	o.println ();
    }
}

