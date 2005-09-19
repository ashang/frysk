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

class DebugInfo
    extends DebugSection
{
    DebugAbbrev debugAbbrev;
    public DebugInfo (ByteBuffer infoSection, ByteBuffer abbrevSection)
    {
	super (infoSection);
	debugAbbrev = new DebugAbbrev (abbrevSection);
	debugAbbrev.construct (new CachedAbbreviationBuilder ());
    }

    private static class AbbreviationKey
    {
	private long debugAbbrevOffset;
	private long abbreviationCode;
	AbbreviationKey (long debugAbbrevOffset, long abbreviationCode)
	{
	    this.debugAbbrevOffset = debugAbbrevOffset;
	    this.abbreviationCode = abbreviationCode;
	}

	AbbreviationKey (long debugAbbrevOffset)
	{
	    this.debugAbbrevOffset = debugAbbrevOffset;
	}
	void setCode (long abbreviationCode)
	{
	    this.abbreviationCode = abbreviationCode;
	}

	public int hashCode ()
	{
	    return (int) (debugAbbrevOffset + abbreviationCode);
	}
	public boolean equals (Object obj)
	{
	    if (! (obj instanceof AbbreviationKey))
		return false;
	    AbbreviationKey rhs = (AbbreviationKey) obj;
	    return (debugAbbrevOffset == rhs.debugAbbrevOffset
		    && abbreviationCode == rhs.abbreviationCode);
	}
	public String toString ()
	{
	    return ("[AbbreviationKey"
		    + ",debugAbbrevOffset=" + debugAbbrevOffset
		    + ",abbreviationCode=" + abbreviationCode
		    + "]");
	}
    }

    public static class CachedAbbreviation
	extends AbbreviationKey
    {
	public int tag;
	public boolean hasChildren;
	public long attributesOffset;
	CachedAbbreviation (long debugAbbrevOffset,
			    long abbreviationCode,
			    int tag,
			    boolean hasChildren,
			    long attributesOffset)
	{
	    super (debugAbbrevOffset, abbreviationCode);
	    this.tag = tag;
	    this.hasChildren = hasChildren;
	    this.attributesOffset = attributesOffset;
	}
	public String toString ()
	{
	    return ("[CachedAbbreviation"
		    + super.toString ()
		    + ",tag=" + tag
		    + ",hasChildren=" + hasChildren
		    + "]");
	}
    }

    java.util.HashMap abbrevMap = new java.util.HashMap ();

    private class CachedAbbreviationBuilder
	extends DebugAbbrev.Builder
    {
	Builder builder;
	long declarationOffset;
	Action buildAbbreviationTableHeader ()
	{
	    // Save the current offset
	    declarationOffset = debugAbbrev.position ();
	    // System.out.println ("Offset " + declarationOffset);
	    return Action.PARSE;
	}
	Action buildAbbreviationHeader (long position, long code, int tag,
					boolean hasChildren)
	{
	    CachedAbbreviation decl
		= new CachedAbbreviation (declarationOffset, code,
					  tag, hasChildren,
					  debugAbbrev.position ());
	    // System.out.println (decl);
	    abbrevMap.put (decl, decl);
	    return Action.SKIP;
	}
    }

    private class ParseAttribBuilder
	extends DebugAbbrev.Builder
    {
	Builder builder;
	Format format;
	long compilationUnitHeaderPosition;
	ParseAttribBuilder (Builder builder, Format format,
			    long compilationUnitHeaderPosition)
	{
	    this.builder = builder;
	    this.format = format;
	    this.compilationUnitHeaderPosition = compilationUnitHeaderPosition;
	}
	Action buildAbbreviationAttributeSpecification (int name, int form)
	{
	    long value;
	    long skip = 0;
	    switch (form) {
	    case DW.FORM.data1:
		value = getDATA1 ();
		break;
	    case DW.FORM.data2:
		value = getDATA2 ();
		break;
	    case DW.FORM.data4:
		value = getDATA4 ();
		break;
	    case DW.FORM.data8:
		value = getDATA8 ();
		break;
	    case DW.FORM.strp:
		value = getSectionOffset (format);
		break;
	    case DW.FORM.addr:
		value = getUWORD (format);
		break;
	    case DW.FORM.ref4:
		value = compilationUnitHeaderPosition + getDATA4 ();
		break;
	    case DW.FORM.flag:
		value = getUBYTE ();
		break;
	    case DW.FORM.block1:
		value = getUBYTE ();
		skip = position () + value;
		break;
	    case DW.FORM.string:
		skip = position ();
		value = 0;
		while (getUBYTE (skip + value) != 0)
		    value++;
		skip += value + 1; // Trailing NUL.
		break;
	    case DW.FORM.sdata:
		value = getSignedLEB128 ();
		break;
	    case DW.FORM.udata:
		value = getUnsignedLEB128 ();
		break;
	    default:
		System.out.println ("Unhandled form "
				    + DW.FORM.toPrintString (form));
		value = 0;
	    }
	    builder.buildAttributeValue (format, name, form,
					 DebugInfo.this, value);
	    if (skip != 0)
		position (skip);
	    return Action.PARSE;
	}
    }

    public abstract static class Builder
	extends DebugBuilder
    {
	Action buildCompilationUnitHeader (Format format,
					   long position,
					   long unitLength, int version,
					   long debugAbbrevOffset,
					   short addressSize)
	{
	    return Action.PARSE;
	}
	Action buildDebuggingInformationEntry (long position,
					       long abbreviationCode,
					       CachedAbbreviation abbrev)
	{
	    return Action.PARSE;
	}
	Action buildDebuggingInformationEnd ()
	{
	    return Action.PARSE;
	}
	Action buildAttributeValue (Format format,
				    int name, int form,
				    DebugSection slice, long value)
	{
	    return Action.PARSE;
	}
    }

    public void construct (Builder builder)
    {
	while (hasRemaining ()) {
	    parseCompilationUnitContribution (builder);
	}
    }

    private final void parseCompilationUnitContribution (Builder builder)
    {
	long position = position ();
	Format format = getFormat ();
	long unitLength = getInitialLength (format);
	long compilationUnitLimit = position () + unitLength;
	int version = getUHALF ();
	long debugAbbrevOffset = getSectionOffset (format);
	short addressSize = getUBYTE ();

	ParseAttribBuilder parseAttribBuilder
	    = new ParseAttribBuilder (builder, format, position);
	// Use the abbreviation key to pass around the offset.
	AbbreviationKey offsetAbbreviationKey = new AbbreviationKey (debugAbbrevOffset);


	if (builder.buildCompilationUnitHeader (format, position, unitLength,
						version, debugAbbrevOffset,
						addressSize)
	    == Builder.Action.PARSE)
	    parseDebuggingInformationEntries (builder, offsetAbbreviationKey,
					      parseAttribBuilder,
					      compilationUnitLimit);
	position (compilationUnitLimit);
    }

    private final void parseDebuggingInformationEntries (Builder builder,
							 AbbreviationKey offsetAbbreviationKey,
							 ParseAttribBuilder parseAttribBuilder,
							 long compilationUnitLimit)
    {
	while (position () < compilationUnitLimit) {
	    parseDebuggingInformationEntry (builder, offsetAbbreviationKey,
					    parseAttribBuilder);
	}
    }

    private final void parseDebuggingInformationEntry (Builder builder,
						       AbbreviationKey offsetAbbreviationKey,
						       ParseAttribBuilder parseAttribBuilder)
    {
	long position = position ();
	long abbreviationCode = getUnsignedLEB128 ();
	if (abbreviationCode == 0) {
	    // An empty entry, move onto the next one.
	    builder.buildDebuggingInformationEnd ();
	    return;
	}
	offsetAbbreviationKey.setCode (abbreviationCode);
	CachedAbbreviation abbrev
	    = (CachedAbbreviation) abbrevMap.get (offsetAbbreviationKey);
	if (abbrev == null) {
	    System.out.println ("Missing abbreviation, key "
				+ offsetAbbreviationKey);
	    // System.out.println (abbrevMap);
	    return;
	}
	builder.buildDebuggingInformationEntry (position, abbreviationCode,
						abbrev);
	debugAbbrev.position (abbrev.attributesOffset);
	debugAbbrev.constructAbbreviationAttributes (parseAttribBuilder);
    }
}
