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

import inua.PrintWriter;

public class PrintDebugFrame
    implements inua.Print
{
    DebugFrame[] debugFrameSections;
    public PrintDebugFrame (Dwarf dwarf)
    {
	debugFrameSections = dwarf.getDebugFrames ();
    }

    private class Builder
	extends DebugFrame.Builder
    {
	PrintWriter o;
	DebugFrame section;
	Builder (PrintWriter o, DebugFrame section)
	{
	    this.o = o;
	    this.section = section;
	}
	PrintWriter printAugmentation (long position)
	{
	    o.print ('"');
	    while (true) {
		byte b = section.getSBYTE (position++);
		if (b == 0)
		    break;
		o.print ((char) b);
	    }
	    o.print ('"');
	    return o;
	}

	void print ()
	{
	    o.print ("The section ");
	    o.print (section.name ());
	    o.print (" contains:");
	    o.println ();
	    section.construct (this);
	    o.println ();
	}

	Action buildTerminalEntry (long position)
	{
	    o.println ();
	    o.printx (8,'0',position);
	    o.print (" ZERO terminator");
	    o.println ();
	    return Action.BREAK;
	}
    }
    
    class FramesBuilder
	extends Builder
    {
	boolean emptyAugmentationNewLine;
	FramesBuilder (PrintWriter o, DebugFrame section)
	{
	    super (o, section);
	}
	Action buildCommonInformationEntry (long position,
					    long length,
					    long limit,
					    long cieID,
					    short version,
					    long augmentation,
					    long codeAlignmentFactor,
					    long dataAlignmentFactor,
					    long returnAddressRegister,
					    long initialInstructions)
	{
	    o.println ();
	    o.printx (8,'0',position);
	    o.print (' ');
	    o.printx (8,'0',length);
	    o.print (' ');
	    o.printx (8,'0',cieID);
	    o.print (" CIE");
	    o.println ();
	    o.print ("  Version:               ");
	    o.print (version);
	    o.println ();
	    o.print ("  Augmentation:          ");
	    printAugmentation (augmentation);
	    o.println ();
	    o.print ("  Code alignment factor: ");
	    o.print (codeAlignmentFactor);
	    o.println ();
	    o.print ("  Data alignment factor: ");
	    o.print (dataAlignmentFactor);
	    o.println ();
	    o.print ("  Return address column: ");
	    o.print (returnAddressRegister);
	    o.println ();
	    emptyAugmentationNewLine = true;
	    return Action.PARSE;
	}
	Action buildFrameDescriptionEntry (long position,
					   long length,
					   long limit,
					   long ciePointer,
					   long ciePosition,
					   long initialLocation,
					   long addressRange,
					   long instructions)
	{
	    o.println ();
	    o.printx (8, '0',position);
	    o.print (' ');
	    o.printx (8, '0',length);
	    o.print (' ');
	    o.printx (8, '0',ciePointer);
	    o.print (" FDE cie=");
	    o.printx (8, '0',ciePosition);
	    o.print (" pc=");
	    o.printx (8, '0',initialLocation);
	    o.print ("..");
	    o.printx (8, '0',initialLocation + addressRange);
	    o.println ();
	    emptyAugmentationNewLine = false;
	    return Action.PARSE;
	}
	Action buildAugmentationData (long augmentationDataPosition,
				      long augmentationDataLength)
	{
	    if (augmentationDataLength > 0) {
		o.print ("  Augmentation data:    ");
		for (long p = 0; p < augmentationDataLength; p++) {
		    int b = section.getUBYTE (augmentationDataPosition + p);
		    o.print (' ');
		    o.printx (2, '0',b);
		}
		o.println ();
		o.println ();
	    }
	    else if (emptyAugmentationNewLine)
		o.println ();
	    return Action.PARSE;
	}
	Action buildCallFrameInstruction (int opcode, long[] operand,
					  DebugFrame.FrameUnwindTable table)
	{
	    o.print ("  ");
	    switch (opcode) {
	    case DW.CFA.def_cfa_register:
		// compat with readelf
		o.print ("DW_CFA_def_cfa_reg");
		break;
		default:
		    o.print (DW.CFA.toString(opcode));
	    }
	    switch (opcode) {
	    case DW.CFA.nop:
		break;
	    case DW.CFA.advance_loc:
	    case DW.CFA.advance_loc1:
		o.print (": ");
		o.print (operand[0]);
		o.print (" to ");
		o.printx (table.finalSet.location);
		break;
	    case DW.CFA.offset:
	    case DW.CFA.offset_extended:
	    case DW.CFA.offset_extended_sf:
		o.print (": r");
		o.print (operand[0]);
		o.print (" at cfa");
		o.printp (operand[1]);
		break;
	    case DW.CFA.def_cfa:
		o.print (": r");
		o.print (operand[0]);
		o.print (" ofs ");
		o.print (operand[1]);
		break;
	    case DW.CFA.def_cfa_offset:
		o.print (": ");
		o.print (operand[0]);
		break;
	    case DW.CFA.def_cfa_register:
		o.print (": r");
		o.print (operand[0]);
		break;
	    case DW.CFA.register:
		o.print (": r");
		o.print (operand[0]);
		o.print (" in r");
		o.print (operand[1]);
		break;
	    default:
		o.print (": unimplemented");
	    }
	    o.println ();
	    return Action.PARSE;
	}
    }

    class FramesInterpBuilder
	extends Builder
    {
	FramesInterpBuilder (PrintWriter o, DebugFrame section)
	{
	    super (o, section);
	}
	Action buildCommonInformationEntry (long position,
					    long length,
					    long limit,
					    long cieID,
					    short version,
					    long augmentation,
					    long codeAlignmentFactor,
					    long dataAlignmentFactor,
					    long returnAddressRegister,
					    long initialInstructions)
	{
	    o.println ();
	    o.printx (8, '0',position);
	    o.print (' ');
	    o.printx (8, '0',length);
	    o.print (' ');
	    o.printx (8, '0',cieID);
	    o.print (" CIE ");
	    printAugmentation (augmentation);
	    o.print (" cf=");
	    o.print (codeAlignmentFactor);
	    o.print (" df=");
	    o.print (dataAlignmentFactor);
	    o.print (" ra=");
	    o.print (returnAddressRegister);
	    o.println ();
	    return Action.PARSE;
	}
	Action buildFrameDescriptionEntry (long position,
					   long length,
					   long limit,
					   long ciePointer,
					   long ciePosition,
					   long initialLocation,
					   long addressRange,
					   long instructions)
	{
	    o.println ();
	    o.printx (8, '0',position);
	    o.print (' ');
	    o.printx (8, '0',length);
	    o.print (' ');
	    o.printx (8, '0',ciePointer);
	    o.print (" FDE cie=");
	    o.printx (8, '0',ciePosition);
	    o.print (" pc=");
	    o.printx (8, '0',initialLocation);
	    o.print ("..");
	    o.printx (8, '0',initialLocation + addressRange);
	    o.println ();
	    return Action.PARSE;
	}
	Action buildFrameUnwindTable (DebugFrame.FrameUnwindTable table)
	{
	    // Headings
	    {
		o.print ("   LOC   CFA      ");
		DebugFrame.RegisterRuleSet.SetIterator columns
		    = table.finalSet.setIterator ();
		while (columns.hasNext ()) {
		    DebugFrame.RegisterRule rule = columns.next ();
		    if (rule.register == table.returnAddressRegister)
			o.print (-5,"ra");
		    else {
			o.print ("r");
			o.print (-4, rule.register);
		    }
		}
		o.println ();
	    }
	    // Rows
	    DebugFrame.FrameUnwindTable.RowIterator rows = table.rowIterator ();
	    while (rows.hasNext ()) {
		DebugFrame.RegisterRuleSet row = rows.next ();
		o.printx (8, '0',row.location);
		o.print (-10," r" + row.cfaRegister + "+" + row.cfaOffset);
		// Iterate over the final column as that contains the
		// final set of registers.
		DebugFrame.RegisterRuleSet.SetIterator finalColumns
		    = table.finalSet.setIterator ();
		while (finalColumns.hasNext ()) {
		    DebugFrame.RegisterRule finalRule = finalColumns.next ();
		    DebugFrame.RegisterRule rule = row.get (finalRule);
		    if (rule != null)
			switch (rule.rule) {
			case DebugFrame.RegisterRule.UNDEFINED:
			    o.print (-5, 'u');
			    break;
			case DebugFrame.RegisterRule.OFFSET:
			    o.print ('c');
			    o.printp (-4, rule.operand);
			    break;
			case DebugFrame.RegisterRule.REGISTER:
			    o.print ('r');
			    o.print (-4, rule.operand);
			    break;
			default:
			    o.print ("Unknown rule ");
			    o.print (rule.rule);
			    break;
			}
		    else {
			o.print (-5, 'u');
		    }
		}
		o.println ();
	    }
	    return Action.PARSE;
	}
    }
    public void print (PrintWriter o)
    {
	for (int i = 0; i < debugFrameSections.length; i++) {
	    new FramesBuilder (o, debugFrameSections[i]).print ();
	}
    }
    public void printInterp (PrintWriter o)
    {
	for (int i = 0; i < debugFrameSections.length; i++) {
	    new FramesInterpBuilder (o, debugFrameSections[i]).print ();
	}
    }
}
