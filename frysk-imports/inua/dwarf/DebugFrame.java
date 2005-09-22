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

public class DebugFrame
    extends DebugSection
{
    static class Type
    {
	String name;
	boolean eh;
	private Type (String name, boolean eh)
	{
	    this.name = name;
	    this.eh = eh;
	}
    }
    public static Type[] types  = new Type[] {
	new Type (".eh_frame", true),
	new Type (".debug_frame", false),
    };

    Type type;
    public DebugFrame (ByteBuffer section, Type type)
    {
	super (section);
	this.type = type;
    }

    public String name ()
    {
	return type.name;
    }

    public static class Builder
	extends DebugBuilder
    {
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
	    return Builder.Action.PARSE;
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
	    return Builder.Action.PARSE;
	}
	Action buildTerminalEntry (long position)
	{
	    return Builder.Action.PARSE;
	}
	Action buildAugmentationData (long augmentationDataPosition,
				      long augmentationDataLength)
	{
	    return Builder.Action.PARSE;
	}
	Action buildCallFrameInstruction (int opcode, long[] operand,
					  FrameUnwindTable table)
	{
	    return Builder.Action.PARSE;
	}
	Action buildFrameUnwindTable (FrameUnwindTable table)
	{
	    return Builder.Action.PARSE;
	}
    }

    public void construct (Builder builder)
    {
	while (hasRemaining ()) {
	    parseTable (builder);
	}
    }

    private final void parseTable (Builder builder)
    {
	long position = position ();
	Format format = getFormat ();
	long length = getInitialLength (format);
	long limit = position () + length;

	if (length == 0) {
	    builder.buildTerminalEntry (position);
	    return;
	}
	long cieID = getUWORD (format);
	if (type.eh ? cieID == 0 : cieID == 0xffffffffL)
	    parseCommonInformationEntry (builder, format, position, length,
					 limit, cieID);
	else
	    parseFrameDescriptionEntry (builder, format, position, length,
					limit, cieID);
	position (limit);
    }


    // XXX: Need to trim this back to just what is needed when maping
    // from an FDE to a CIE.
    private java.util.ArrayList commonInformationEntries =
	new java.util.ArrayList ();
    private class CommonInformationEntry
    {
	Format format;
	long position;
	long length;
	long limit;
	long cieID;
	short version;
	long augmentation;
	long codeAlignmentFactor;
	long dataAlignmentFactor;
	long returnAddressRegister;
	long initialInstructions; // A position

	// Augmentation data (for Z).
	long augmentationDataLength;
	long augmentationDataPosition;

	// The table of registers
	FrameUnwindTable registerRules;

	CommonInformationEntry (Format theFormat, long thePosition,
				long theLength, long theCidID)
	{
	    format = theFormat;
	    position = thePosition;
	    length = theLength;
	    cieID = theCidID;
	    version = getUBYTE ();
	    augmentation = position ();
	    skipToZeroUBYTE ();
	    codeAlignmentFactor = getUnsignedLEB128 ();
	    dataAlignmentFactor = getSignedLEB128 ();
	    returnAddressRegister = getUnsignedLEB128 ();
	    // XXX: Should this be an ect?
	    if (getUBYTE (augmentation) == 'z') {
		augmentationDataLength = getSignedLEB128 ();
		augmentationDataPosition = position ();
		position (augmentationDataPosition
			  + augmentationDataLength);
	    }
	    initialInstructions = position ();
	    registerRules = new FrameUnwindTable (returnAddressRegister);
	}
    }
    private final void parseCommonInformationEntry (Builder builder,
						    Format format,
						    long position,
						    long length, long limit,
						    long CidID)
    {
	CommonInformationEntry cie
	    = new CommonInformationEntry (format, position, length, CidID);
	commonInformationEntries.add (cie);
	if (builder.buildCommonInformationEntry (cie.position,
						 cie.length,
						 cie.limit,
						 cie.cieID,
						 cie.version,
						 cie.augmentation,
						 cie.codeAlignmentFactor,
						 cie.dataAlignmentFactor,
						 cie.returnAddressRegister,
						 cie.initialInstructions)
	    == DebugBuilder.Action.PARSE) {
	    builder.buildAugmentationData (cie.augmentationDataPosition,
					   cie.augmentationDataLength);
	    parseCallFrameInstructions (builder, cie.registerRules,
					cie, limit);
	}
    }

    private final void parseFrameDescriptionEntry (Builder builder,
						   Format format,
						   long position,
						   long length,
						   long limit,
						   long ciePointer)
    {
	long ciePosition = (type.eh
			    ? limit - length - ciePointer
			    : ciePointer);
	CommonInformationEntry cie = null;
	for (int i = 0; i < commonInformationEntries.size (); i++) {
	    CommonInformationEntry e = (CommonInformationEntry)
		commonInformationEntries.get (i);
	    if (e.position == ciePosition) {
		cie = e;
		break;
	    }
	}
	if (cie == null) {
	    System.out.println ("Warning: failed to find CIE at 0x"
				+ Long.toHexString (ciePointer));
	    return;
	}

	long initialLocation = getUWORD (format);
	long addressRange = getUWORD (format);
	// Augmentation data (for Z), based on the CIE.
	long augmentationDataLength = 0;
	long augmentationDataPosition = 0;
	if (getUBYTE (cie.augmentation) == 'z') {
	    augmentationDataLength = getSignedLEB128 ();
	    augmentationDataPosition = position ();
	    position (augmentationDataPosition + augmentationDataLength);
	}
	long instructions = position ();
	if (builder.buildFrameDescriptionEntry (position, length, limit,
						ciePointer, ciePosition,
						initialLocation,
						addressRange, instructions)
	    == DebugBuilder.Action.PARSE) {
	    builder.buildAugmentationData (augmentationDataPosition,
					   augmentationDataLength);
	    // Establish the initial row
	    RegisterRuleSet initialRow =
		(RegisterRuleSet) cie.registerRules.finalSet.clone ();
	    initialRow.location = initialLocation;
	    FrameUnwindTable registerRules
		= new FrameUnwindTable (initialRow, cie.returnAddressRegister);
	    parseCallFrameInstructions (builder, registerRules,
					cie, limit);
	}
    }

    private final void parseCallFrameInstructions (Builder builder,
						   FrameUnwindTable unwindTable,
						   CommonInformationEntry cie,
						   long limit)
    {
	while (position () < limit) {
	    parseCallFrameInstruction (builder, unwindTable, cie);
	}
	builder.buildFrameUnwindTable (unwindTable);
    }

    long unsignedOffset (CommonInformationEntry cie)
    {
	long factoredOffset = getUnsignedLEB128 ();
	return (factoredOffset * cie.dataAlignmentFactor);
    }
    private long signedOffset (CommonInformationEntry cie)
    {
	long factoredOffset = getSignedLEB128 ();
	return (factoredOffset * cie.dataAlignmentFactor);
    }
    private void parseSimpleCallFrameInstruction (int opcode,
						  long[] operand,
						  FrameUnwindTable unwindTable,
						  CommonInformationEntry cie)
    {
	switch (opcode) {
	case DW.CFA.nop:
	    break;
	case DW.CFA.def_cfa:
	    operand[0] = getUnsignedLEB128 ();
	    operand[1] = getUnsignedLEB128 ();
	    unwindTable.finalSet.cfaRegister = (int) operand[0];
	    unwindTable.finalSet.cfaOffset = operand[1];
	    break;
	case DW.CFA.def_cfa_offset:
	    operand[0] = getUnsignedLEB128 ();
	    unwindTable.finalSet.cfaOffset = operand[0];
	    break;
	case DW.CFA.def_cfa_register:
	    operand[0] = getUnsignedLEB128 ();
	    unwindTable.finalSet.cfaRegister = operand[0];
	    break;
	case DW.CFA.offset_extended:
	    operand[0] = getUnsignedLEB128 ();
	    operand[1] = unsignedOffset (cie);
	    unwindTable.finalSet.put (operand[0], RegisterRule.OFFSET,
				      operand[1]);
	    break;
	case DW.CFA.offset_extended_sf:
	    operand[0] = getUnsignedLEB128 ();
	    operand[1] = signedOffset (cie);
	    unwindTable.finalSet.put (operand[0], RegisterRule.OFFSET,
				      operand[1]);
	    break;
	case DW.CFA.advance_loc1:
	    operand[0] = getUBYTE ();
	    unwindTable.newRow (unwindTable.finalSet.location
				+ operand[0]);
	    break;
	case DW.CFA.advance_loc2:
	    operand[0] = getUHALF ();
	    unwindTable.newRow (unwindTable.finalSet.location
				+ operand[0]);
	    break;
	case DW.CFA.register:
	    operand[0] = getUnsignedLEB128 ();
	    operand[1] = getUnsignedLEB128 ();
	    unwindTable.finalSet.put (operand[0], RegisterRule.REGISTER,
				      operand[1]);
	    break;
	case DW.CFA.GNU_args_size:
	    operand[0] = getUnsignedLEB128 ();
	    // Do nothing.
	    break;
	default:
	    System.out.println ("Warning: unsupported opcode "
				+ DW.CFA.toString (opcode)
				+ " (" + opcode
				+ ") in .eh_frame or .debug_frame");
	}
    }
    private final void parseCallFrameInstruction (Builder builder,
						  FrameUnwindTable unwindTable,
						  CommonInformationEntry cie)
    {
	long[] operand = new long[4];

	int opcode = getUBYTE ();
	switch (opcode & DW.CFA.mask) {
	case DW.CFA.advance_loc:
	    long delta = opcode & ~DW.CFA.mask;
	    operand[0] = (delta * cie.codeAlignmentFactor);
	    opcode &= DW.CFA.mask;
	    unwindTable.newRow (unwindTable.finalSet.location
				  + operand[0]);
	    break;
	case DW.CFA.offset:
	    operand[0] = opcode & ~DW.CFA.mask;
	    operand[1] = unsignedOffset (cie);
	    opcode &= DW.CFA.mask;
	    unwindTable.finalSet.put (operand[0],
				      RegisterRule.OFFSET,
				      operand[1]);
	    break;
	default:
	    parseSimpleCallFrameInstruction (opcode, operand,
					     unwindTable, cie);
	    break;
	}
	builder.buildCallFrameInstruction (opcode, operand, unwindTable);
    }

    public class RegisterRule
	implements Comparable
    {
	public long register;
	public int rule;
	public long operand;
	RegisterRule (long theRegister, int theRule, long theOperand)
	{
	    register = theRegister;
	    rule = theRule;
	    operand = theOperand;
	}
	public int compareTo (Object o)
	{
	    return (int) (register - ((RegisterRule) o).register);
	}
	public static final int UNDEFINED = 1;
	public static final int SAME_VALUE = 2;
	public static final int OFFSET = 3;
	public static final int REGISTER = 4;
	public static final int EXPRESSION = 5;
	public static final int ARCHITECTURAL = 6;
    }

    public class RegisterRuleSet
    {
	public long location;
	public long cfaRegister;
	public long cfaOffset;
	// Unlike a Set, a map lets you replace new-for-old.
	java.util.TreeMap columns = new java.util.TreeMap ();
	void put (long register, int rule, long operand)
	{
	    RegisterRule registerRule
		= new RegisterRule (register, rule, operand);
	    columns.put (registerRule/*key*/, registerRule);
	}
	void copy (long dstRegister, long srcRegister)
	{
	    // Use the new rule as the lookup for the old rule.
	    RegisterRule newRule
		= new RegisterRule (srcRegister, 0, 0);
	    RegisterRule oldRule = (RegisterRule) columns.get (newRule);
	    newRule.register = dstRegister;
	    newRule.rule = oldRule.rule;
	    newRule.operand = oldRule.operand;
	    System.out.println (columns + " " + newRule);
	    columns.put (newRule, newRule);
	}
	public RegisterRule get (RegisterRule key)
	{
	    return (RegisterRule) columns.get (key);
	}
	public class SetIterator
	{
	    java.util.Iterator list;
	    SetIterator (java.util.Iterator theList)
	    {
		list = theList;
	    }
	    public boolean hasNext ()
	    {
		return list.hasNext ();
	    }
	    public RegisterRule next ()
	    {
		return (RegisterRule) list.next ();
	    }
	}
	public SetIterator setIterator ()
	{
	    return new SetIterator (columns.values().iterator ());
	}
	public Object clone ()
	{
	    RegisterRuleSet newRow = new RegisterRuleSet ();
	    newRow.location = location;
	    newRow.cfaRegister = cfaRegister;
	    newRow.cfaOffset = cfaOffset;
	    newRow.columns = (java.util.TreeMap) columns.clone ();
	    return newRow;
	}
    }

    public class FrameUnwindTable
    {
	java.util.ArrayList rows;
	public long returnAddressRegister;
	RegisterRuleSet initialRow;
	public RegisterRuleSet finalSet;
	FrameUnwindTable (RegisterRuleSet initialRow,
			  long returnAddressRegister)
	{
	    rows = new java.util.ArrayList ();
	    rows.add (initialRow);
	    finalSet = initialRow;
	    this.initialRow = initialRow;
	    this.returnAddressRegister = returnAddressRegister;
	}
	FrameUnwindTable (long returnAddressRegister)
	{
	    this (new RegisterRuleSet (), returnAddressRegister);
	}
	void newRow (long location)
	{
	    try {
		finalSet = (RegisterRuleSet) finalSet.clone ();
	    }
	    catch (Exception oops) {
		throw new RuntimeException ("can't happen");
	    }
	    finalSet.location = location;
	    rows.add (finalSet);
	}
	public class RowIterator
	{
	    java.util.ListIterator list;
	    RowIterator (java.util.ListIterator theList)
	    {
		list = theList;
	    }
	    public boolean hasNext ()
	    {
		return list.hasNext ();
	    }
	    public RegisterRuleSet next ()
	    {
		return (RegisterRuleSet) list.next ();
	    }
	    int nextIndex ()
	    {
		return list.nextIndex ();
	    }
	}
	public RowIterator rowIterator ()
	{
	    return new RowIterator (rows.listIterator ());
	}
    }
}

