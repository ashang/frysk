// This file is part of INUA.  Copyright 2004, 2005, Andrew Cagney
// Copyright 2007, Red Hat Inc.
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
package inua.eio;

public class MmapByteBuffer
    extends ByteBuffer
{
    private static final class Mmap
    {
	static private native final long mmap (String name, long length);
	long capacity;
	long byteData;
	private Mmap (String name)
	{
	    java.io.File file = new java.io.File (name);
	    capacity = file.length ();
	    byteData = mmap (name, capacity);
	}
    }

    Mmap map;

    private MmapByteBuffer (Mmap map, long lowExtreem, long highExtreem)
    {
	super (lowExtreem, highExtreem);
	this.map = map;
    }

    private MmapByteBuffer (Mmap map)
    {
	this (map, 0, map.capacity);
    }

    public MmapByteBuffer (String name)
    {
	this (new Mmap (name));
    }

    protected ByteBuffer subBuffer (ByteBuffer parent, long lowExtreem,
				    long highExtreem)
    {
	return new MmapByteBuffer (((MmapByteBuffer)parent).map,
				   lowExtreem, highExtreem);
    }


    native protected final int peek (long index);
    native protected final void poke (long index, int val);
    
    native protected final int peek (long index, byte[] bytes,
				      int off, int len);
}
