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

import java.util.Set;
import java.util.Iterator;
import java.util.TreeSet;
import inua.util.PrintWriter;
import inua.util.Print;

public class PrintDebugLoc
    implements Print
{
    DebugLoc debugLocSection;
    Set locReferences; // Remove duplicates.

    public PrintDebugLoc (DebugLoc debugLocSection,
			  DebugInfo debugInfoSection)
    {
	this.debugLocSection = debugLocSection;

	// Iterate over the .debug_info section recording any
	// reference that section makes to this .debug_loc section.

	locReferences = new TreeSet ();
	debugInfoSection.construct (new DebugInfo.Builder () {
		Action buildAttributeValue (Format format,
					    int name, int form,
					    DebugSection slice, long value)
		{
		    switch (name) {
		    case DW.AT.frame_base:
		    case DW.AT.location:
		    case DW.AT.data_member_location:
		    case DW.AT.vtable_elem_location:
			if (form != DW.FORM.block1) {
			    Reference rangeReference
				= new Reference (format, value);
			    locReferences.add (rangeReference);
			}
			break;
		    }
		    return Action.SKIP;
		}
	    });
    }
    public PrintDebugLoc (Dwarf dwarf)
    {
	this (dwarf.getDebugLoc (), dwarf.getDebugInfo ());
    }


    class PrintLocBuilder
	extends DebugLoc.Builder
    {
	PrintWriter o;
	PrintLocBuilder (PrintWriter f)
	{
	    o = f;
	}
	void buildLocationListEntry (long offset, long begin, long end,
				     DebugSection expression, long length)
	{
	    o.print ("    ");
	    o.printx (8, '0', offset);
	    o.print (' ');
	    o.printx (8, '0', begin);
	    o.print (' ');
	    o.printx (8, '0', end);
	    o.print (' ');
	    o.print ('(');
	    print (o, expression, length);
	    o.print (')');
	    o.println ();
	}
	void buildLocationListEnd ()
	{
	    o.println ();
	}
    }

    public void print (PrintWriter o)
    {
	o.println ("Contents of the .debug_loc section:");
	o.println ();
	o.println ();
	o.println ("    Offset   Begin    End      Expression");
	Iterator locIterator = locReferences.iterator ();
	PrintLocBuilder printLocBuilder = new PrintLocBuilder (o);
	debugLocSection.position (0);
	while (locIterator.hasNext ()) {
	    Reference locReference = (Reference) locIterator.next ();
	    debugLocSection.construct (printLocBuilder, locReference.format,
				       locReference.offset);
	}
    }

    static void print (PrintWriter o, DebugSection slice, long length)
    {
	String sep = "";
	long limit = slice.position () + length;
	while (slice.position () < limit) {
	    short op = slice.getUBYTE ();
	    o.print (sep);
	    o.print (DW.OP.toString (op));
	    switch (op) {
	    case DW.OP.addr:
		// XXX: 32x64
		o.print (": ");
		o.printx (slice.getDATA4 ());
		break;
	    case DW.OP.plus_uconst:
	    case DW.OP.constu:
	    case DW.OP.piece:
	    case DW.OP.regx:
		o.print (": ");
		o.print (slice.getUnsignedLEB128 ());
		break;
	    case DW.OP.reg0:
	    case DW.OP.reg1:
	    case DW.OP.reg2:
	    case DW.OP.reg3:
	    case DW.OP.reg4:
	    case DW.OP.reg5:
	    case DW.OP.reg6:
	    case DW.OP.reg7:
	    case DW.OP.reg8:
	    case DW.OP.reg9:
	    case DW.OP.reg10:
	    case DW.OP.reg11:
	    case DW.OP.reg12:
	    case DW.OP.reg13:
	    case DW.OP.reg14:
	    case DW.OP.reg15:
	    case DW.OP.reg16:
	    case DW.OP.reg17:
	    case DW.OP.reg18:
	    case DW.OP.reg19:
	    case DW.OP.reg20:
	    case DW.OP.reg21:
	    case DW.OP.reg22:
	    case DW.OP.reg23:
	    case DW.OP.reg24:
	    case DW.OP.reg25:
	    case DW.OP.reg26:
	    case DW.OP.reg27:
	    case DW.OP.reg28:
	    case DW.OP.reg29:
	    case DW.OP.reg30:
	    case DW.OP.reg31:
		// Nothing.
		break;
	    case DW.OP.fbreg:
	    case DW.OP.breg0:
	    case DW.OP.breg1:
	    case DW.OP.breg2:
	    case DW.OP.breg3:
	    case DW.OP.breg4:
	    case DW.OP.breg5:
	    case DW.OP.breg6:
	    case DW.OP.breg7:
	    case DW.OP.breg8:
	    case DW.OP.breg9:
	    case DW.OP.breg10:
	    case DW.OP.breg11:
	    case DW.OP.breg12:
	    case DW.OP.breg13:
	    case DW.OP.breg14:
	    case DW.OP.breg15:
	    case DW.OP.breg16:
	    case DW.OP.breg17:
	    case DW.OP.breg18:
	    case DW.OP.breg19:
	    case DW.OP.breg20:
	    case DW.OP.breg21:
	    case DW.OP.breg22:
	    case DW.OP.breg23:
	    case DW.OP.breg24:
	    case DW.OP.breg25:
	    case DW.OP.breg26:
	    case DW.OP.breg27:
	    case DW.OP.breg28:
	    case DW.OP.breg29:
	    case DW.OP.breg30:
	    case DW.OP.breg31:
		o.print (": ");
		o.print (slice.getSignedLEB128 ());
		break;
	    default:
		o.print ("???");
		break;
	    }
	    sep = "; ";
	}
    }
}
