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

import junit.framework.TestCase;
import inua.eio.ByteBuffer;
import inua.eio.ArrayByteBuffer;

class TestLib
    extends TestCase
    implements Dwarf
{
    short[] debugAbbrev;
    public DebugAbbrev getDebugAbbrev ()
    {
	return new DebugAbbrev (buffer (debugAbbrev));
    }
    short[] debugAranges;
    public DebugAranges getDebugAranges ()
    {
	return new DebugAranges (buffer (debugAranges));
    }
    short[][] debugFrames;
    public DebugFrame[] getDebugFrames ()
    {
	DebugFrame[] frames = new DebugFrame[DebugFrame.types.length];
	for (int i = 0; i < debugFrames.length; i++) {
	    frames[i] = new DebugFrame (buffer (debugFrames[i]),
					DebugFrame.types[i]);
	}
	return frames;
    }
    short[] debugInfo;
    public DebugInfo getDebugInfo ()
    {
	return new DebugInfo (buffer (debugInfo),
			      buffer (debugAbbrev));
    }
    short[] debugLine;
    public DebugLine getDebugLine ()
    {
	return new DebugLine (buffer (debugLine));
    }
    short[] debugLoc;
    public DebugLoc getDebugLoc ()
    {
	return new DebugLoc (buffer (debugLoc));
    }
    short[] debugMacinfo;
    public DebugMacinfo getDebugMacinfo ()
    {
	return new DebugMacinfo (buffer (debugMacinfo));
    }
    short[] debugPubnames;
    public DebugPubs getDebugPubnames ()
    {
	return new DebugPubs (buffer (debugPubnames));
    }
    short[] debugPubtypes;
    public DebugPubs getDebugPubtypes ()
    {
	return new DebugPubs (buffer (debugPubtypes));
    }
    short[] debugRanges;
    public DebugRanges getDebugRanges ()
    {
	return new DebugRanges (buffer (debugRanges));
    }
    short[] debugStr;
    public DebugStr getDebugStr ()
    {
	return new DebugStr (buffer (debugStr));
    }

    ByteBuffer buffer (short[] bytes)
    {
	byte[] byteArray = new byte[bytes.length];
	for (int i = 0; i < bytes.length; i++) {
	    byteArray[i] = (byte) bytes[i];
	}
	return new ArrayByteBuffer (byteArray);
    }
}
