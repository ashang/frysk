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

public class Elf
    extends inua.elf.Elf
    implements Dwarf
{
    public Elf (String name)
    {
	super (name);
    }

    public DebugAbbrev getDebugAbbrev ()
    {
	return new DebugAbbrev (getShdrByName (".debug_abbrev").byteBuffer ());
    }

    public DebugAranges getDebugAranges ()
    {
	return new DebugAranges (getShdrByName (".debug_aranges").byteBuffer ());
    }

    public DebugFrame[] getDebugFrames ()
    {
	DebugFrame[] debugFrames = new DebugFrame[DebugFrame.types.length];
	for (int i = 0; i < DebugFrame.types.length; i++) {
	    ByteBuffer elfSection
		= getShdrByName (DebugFrame.types[i].name).byteBuffer ();
	    debugFrames[i] =
		new DebugFrame (elfSection, DebugFrame.types[i]);
	}
	return debugFrames;
    }

    public DebugInfo getDebugInfo ()
    {
	return new DebugInfo (getShdrByName (".debug_info").byteBuffer (),
			      getShdrByName (".debug_abbrev").byteBuffer ());
    }

    public DebugLine getDebugLine ()
    {
	return new DebugLine (getShdrByName (".debug_line").byteBuffer ());
    }

    public DebugLoc getDebugLoc ()
    {
	return new DebugLoc (getShdrByName (".debug_loc").byteBuffer ());
    }

    public DebugMacinfo getDebugMacinfo ()
    {
	return new DebugMacinfo (getShdrByName (".debug_macinfo").byteBuffer ());
    }

    public DebugPubs getDebugPubnames ()
    {
	return new DebugPubs (getShdrByName (".debug_pubnames").byteBuffer ());
    }

    public DebugPubs getDebugPubtypes ()
    {
	return new DebugPubs (getShdrByName (".debug_pubtypes").byteBuffer ());
    }
    public DebugRanges getDebugRanges ()
    {
	return new DebugRanges (getShdrByName (".debug_ranges").byteBuffer ());
    }
    public DebugStr getDebugStr ()
    {
	return new DebugStr (getShdrByName (".debug_str").byteBuffer ());
    }
}
