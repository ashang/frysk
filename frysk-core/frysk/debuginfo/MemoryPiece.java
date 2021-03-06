// This file is part of the program FRYSK.
//
// Copyright 2007, Red Hat Inc.
//
// FRYSK is free software; you can redistribute it and/or modify it
// under the terms of the GNU General Public License as published by
// the Free Software Foundation; version 2 of the License.
//
// FRYSK is distributed in the hope that it will be useful, but
// WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
// General Public License for more details.
// 
// You should have received a copy of the GNU General Public License
// along with FRYSK; if not, write to the Free Software Foundation,
// Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
// 
// In addition, as a special exception, Red Hat, Inc. gives You the
// additional right to link the code of FRYSK with code not covered
// under the GNU General Public License ("Non-GPL Code") and to
// distribute linked combinations including the two, subject to the
// limitations in this paragraph. Non-GPL Code permitted under this
// exception must only link to the code of FRYSK through those well
// defined interfaces identified in the file named EXCEPTION found in
// the source code files (the "Approved Interfaces"). The files of
// Non-GPL Code may instantiate templates or use macros or inline
// functions from the Approved Interfaces without causing the
// resulting work to be covered by the GNU General Public
// License. Only Red Hat, Inc. may make changes or additions to the
// list of Approved Interfaces. You must obey the GNU General Public
// License in all respects for all of the FRYSK code and other code
// used in conjunction with FRYSK except the Non-GPL Code covered by
// this exception. If you modify this file, you may extend this
// exception to your version of the file, but you are not obligated to
// do so. If you do not wish to provide this exception without
// modification, you must delete this exception statement from your
// version and license this file solely under the GPL without
// exception.

package frysk.debuginfo;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import frysk.value.Location;

import inua.eio.ByteBuffer;

/**
 *  Class to represent a piece of memory
 */
public class MemoryPiece
	extends Piece
{
    private final long memory;  
    private final ByteBuffer byteBuf;

    /**
     * Used for testing LocationExpression.
     */
    protected MemoryPiece (long memory, long size)
    {
	super (size);
	this.memory = memory;
	byteBuf = null;
    }
    
    public MemoryPiece (long memory, long size, ByteBuffer byteBuf)
    {
	super (size);
	this.memory = memory;
	this.byteBuf = byteBuf.slice(memory, size);
    }
    
    private MemoryPiece (long memory, long size, long offset, ByteBuffer byteBuf)
    {
	super (size);
	// Assuming byte-addressable memory
	this.memory = memory + offset;
	this.byteBuf = byteBuf.slice(offset, size);
    }
    
    public long getMemory()
    {
	return memory;
    }
    
    protected Piece slice (long offset, long length)
    {  
	Piece newP = new MemoryPiece(memory, length, offset, this.byteBuf);
	return newP;
    }
    
    protected byte getByte(long index) 
    {
	return byteBuf.getByte(index);
    }
    
    protected void putByte(long index, byte value) 
    {
	byteBuf.putByte(index, value);
    }
    
    public ByteBuffer getByteBuf()
    {
	return byteBuf;
    }
    
    protected void toPrint(PrintWriter writer)
    {
	writer.print("Address 0x");
	writer.print(Long.toHexString(memory));
	writer.print(" - ");
	writer.print(size);
	writer.print(" byte(s)");
    }
    
    /**
     * Creates a Location with one MemoryPiece
     */
    protected static Location createSimpleLoc (long address, long size, ByteBuffer buf)
    {
	MemoryPiece memP = new MemoryPiece(address, size, buf);
	List list =  new ArrayList();
	list.add(memP);
	return new PieceLocation (list);
    }
    
    public boolean equals (Object p)
    {
	return (this.memory == ((MemoryPiece)p).memory 
		&& this.size == ((MemoryPiece)p).size);
    }	
}