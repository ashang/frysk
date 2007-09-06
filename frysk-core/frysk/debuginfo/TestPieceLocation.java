//This file is part of the program FRYSK.

//Copyright 2007, Red Hat Inc.

//FRYSK is free software; you can redistribute it and/or modify it
//under the terms of the GNU General Public License as published by
//the Free Software Foundation; version 2 of the License.

//FRYSK is distributed in the hope that it will be useful, but
//WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
//General Public License for more details.

//You should have received a copy of the GNU General Public License
//along with FRYSK; if not, write to the Free Software Foundation,
//Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.

//In addition, as a special exception, Red Hat, Inc. gives You the
//additional right to link the code of FRYSK with code not covered
//under the GNU General Public License ("Non-GPL Code") and to
//distribute linked combinations including the two, subject to the
//limitations in this paragraph. Non-GPL Code permitted under this
//exception must only link to the code of FRYSK through those well
//defined interfaces identified in the file named EXCEPTION found in
//the source code files (the "Approved Interfaces"). The files of
//Non-GPL Code may instantiate templates or use macros or inline
//functions from the Approved Interfaces without causing the
//resulting work to be covered by the GNU General Public
//License. Only Red Hat, Inc. may make changes or additions to the
//list of Approved Interfaces. You must obey the GNU General Public
//License in all respects for all of the FRYSK code and other code
//used in conjunction with FRYSK except the Non-GPL Code covered by
//this exception. If you modify this file, you may extend this
//exception to your version of the file, but you are not obligated to
//do so. If you do not wish to provide this exception without
//modification, you must delete this exception statement from your
//version and license this file solely under the GPL without
//exception.

package frysk.debuginfo;

import inua.eio.ArrayByteBuffer;

import frysk.junit.TestCase;
import frysk.value.Location;

import java.util.List;
import java.util.ArrayList;

public class TestPieceLocation 
extends TestCase
{
    private PieceLocation l;

    public void setUp() 
    {

	//  Creating: { 5 6 7 8 9 } { 1 2 3 } { 12 14 16 }
	List pieces = new ArrayList();
	pieces.add(new MemoryPiece(1234, 5, 
		   new ArrayByteBuffer(new byte[] { 5, 6, 7, 8, 9 })));
	pieces.add(new MemoryPiece(5678, 3, 
		   new ArrayByteBuffer(new byte[] { 1, 2, 3 })));
	pieces.add(new MemoryPiece(1111, 3, 
		   new ArrayByteBuffer(new byte[] { 12, 14, 16 })));

	l = new PieceLocation (pieces);
    }

    public void tearDown() 
    {
	l = null;
    }

    public void testMapping() 
    {
	// Test for length
	assertEquals ("total bytes", l.length(), 11);

	// Test for index and piece mapping
	assertEquals("piece index", 1, l.indexOf(6));
	assertEquals("piece", l.getPieces().get(1), l.pieceOf(6));
    }

    public void testGetPut()
    {
	// Test for putByte & getByte
	l.putByte(6, (byte)99);
	//  New list should be: { 5 6 7 8 9 } { 1 99 3 } { 12 14 16 } 
	assertEquals("byte", 99, l.getByte(6));
    }

    public void testSlice() 
    {
	// Test for slice
	Location slice = l.slice(4, 6);
	PieceLocation pSlice = (PieceLocation)slice;
	assertEquals("# of pieces", 3, pSlice.getPieces().size());
	assertEquals("# of bytes", 6, pSlice.length());
	assertEquals("byte", 2, pSlice.getByte(2));
	assertEquals("address", 1238, 
		     ((MemoryPiece)pSlice.getPieces().get(0)).getMemory());
	assertEquals("address", 5678, 
		     ((MemoryPiece)pSlice.getPieces().get(1)).getMemory());
    }
}
