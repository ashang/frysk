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

public class PrintDebugInfo
    implements Print
{
    DebugStr debugStrSection;
    DebugInfo debugInfoSection;
    DebugRanges debugRangesSection;

    public PrintDebugInfo (DebugInfo debugInfoSection,
			   DebugRanges debugRangesSection,
			   DebugStr debugStrSection)
    {
	this.debugInfoSection = debugInfoSection;
	this.debugRangesSection = debugRangesSection;
	this.debugStrSection = debugStrSection;
    }
    public PrintDebugInfo (Dwarf dwarf)
    {
	this (dwarf.getDebugInfo (), dwarf.getDebugRanges (),
	      dwarf.getDebugStr ());
    }

    private class PrintInfoBuilder
	extends DebugInfo.Builder
    {
	long baseAddress;
	PrintWriter o;
	int level;
	PrintInfoBuilder (PrintWriter f)
	{
	    o = f;
	}
	Action buildCompilationUnitHeader (Format format,
					   long position,
					   long unitLength, int version,
					   long debugAbbrevOffset,
					   short addressSize)
	{
	    o.print ("  Compilation Unit ");
	    o.println ();
	    o.print ("   Length:        ");
	    o.print (unitLength);
	    o.println ();
	    o.print ("   Version:       ");
	    o.print (version);
	    o.println ();
	    o.print ("   Abbrev Offset: ");
	    o.print (debugAbbrevOffset);
	    o.println ();
	    o.print ("   Pointer Size:  ");
	    o.print (addressSize);
	    o.println ();
	    level = 0;
	    return Action.PARSE;
	}
	Action buildDebuggingInformationEntry (long position,
					       long abbreviationCode,
					       DebugInfo.CachedAbbreviation abbrev)
	    
	{
	    o.print (" <");
	    o.print (level);
	    o.print ("><");
	    o.printx (position);
	    o.print (">: Abbrev Number: ");
	    o.print (abbreviationCode);
	    o.print (" (");
	    o.print (DW.TAG.toString (abbrev.tag));
	    o.print (")");
	    o.println ();
	    if (abbrev.hasChildren)
		level++;
	    return Action.PARSE;
	}
	Action buildDebuggingInformationEnd ()
	{
	    level--;
	    return Action.PARSE;
	}
	Action buildAttributeValue (Format format,
				    int name, int form,
				    DebugSection slice, long value)
	{
	    StringBuffer string = new StringBuffer ();
	    o.print ("     ");
	    o.print (-18, DW.AT.toString (name));
	    o.print (": ");
	    switch (form) {
	    case DW.FORM.addr:
		o.print ("0x");
		o.printx (value);
		break;
	    case DW.FORM.strp:
		o.print ("(indirect string, offset: 0x");
		o.printx (value);
		o.print ("): ");
		debugStrSection.get (value, string);
		o.print (string);
		break;
	    case DW.FORM.ref4:
		o.print ('<');
		o.printx (value);
		o.print (">");
		break;
	    case DW.FORM.block1:
		o.print (value);
		o.print (" byte block: ");
		for (int i = 0; i < value; i++) {
		    o.printx (slice.getUBYTE (slice.position () + i));
		    o.print (' ');
		}
		break;
	    case DW.FORM.string:
		slice.get (string);
		o.print (string);
		break;
	    default:
		o.print (value);
	    }
	    o.print ('\t');
	    switch (name) {
	    case DW.AT.frame_base:
	    case DW.AT.location:
	    case DW.AT.data_member_location:
	    case DW.AT.vtable_elem_location:
		o.print ("(");
		if (form == DW.FORM.block1)
		    PrintDebugLoc.print (o, slice, value);
		else
		    o.print ("location list");
		o.print (')');
		break;
	    case DW.AT.language:
		o.print ('(');
		o.print (DW.LANG.toPrintString (value));
		o.print (')');
		break;
	    case DW.AT.encoding:
		o.print ('(');
		o.print (DW.ATE.toPrintString (value));
		o.print (')');
		break;
	    case DW.AT.accessibility:
		o.print ('(');
		o.print (DW.ACCESS.toPrintString (value));
		o.print (')');
		break;
	    case DW.AT.virtuality:
		o.print ('(');
		o.print (DW.VIRTUALITY.toPrintString (value));
		o.print (')');
		break;
	    case DW.AT.inline:
		o.print ('(');
		o.print (DW.INL.toPrintString (value));
		o.print (')');
		break;
	    case DW.AT.low_pc:
		// 3.2.1 Normal and Partial Compilation Unit
		// Entries: The base address of a compilaton unit
		// is defined as the value of the DW_AT_low_pc
		// attribute, if present; otherwize, it is
		// undefined.
		baseAddress = value;
		break;
	    case DW.AT.ranges:
		class PrintRangesBuilder
		    extends DebugRanges.Builder
		{
		    PrintWriter o;
		    String sep = "";
		    long baseAddress; 
		    PrintRangesBuilder (PrintWriter o, long baseAddress)
		    {
			this.o = o;
			this.baseAddress = baseAddress;
		    }
		    void buildRangeListBase (long newBase)
		    {
			baseAddress = newBase;
		    }
		    void buildRangeListEntry (long begin, long end)
		    {
			o.print (sep);
			o.print ("0x");
			o.printx (baseAddress + begin);
			o.print (" - 0x");
			o.printx (baseAddress + end);
			sep = ", ";
		    }
		}
		// 3.2.1 Normal and Partial Compilation Unit
		// Entries: The base address of a compilaton unit
		// is defined as the value of the DW_AT_low_pc
		// attribute, if present; otherwize, it is
		// undefined.
		o.print ('(');
		debugRangesSection.construct
		    (new PrintRangesBuilder (o, baseAddress), format, value);
		o.print (')');
		break;
	    default:
		// o.print ("<Name ");
		// o.print (DW.AT.toString (name));
		// o.print (" unhandled>");
		break;
	    }
	    o.println ();
	    return Action.PARSE;
	}
    }
    public void print (PrintWriter o)
    {
	o.print ("The section .debug_info contains:");
	o.println ();
	o.println ();
	debugInfoSection.construct (new PrintInfoBuilder (o));
	o.println ();
    }
}
