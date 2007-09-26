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

import frysk.value.Location;

import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

public class PieceLocation
extends Location
{
    private final List pieces; 

    public PieceLocation (List pieces)
    {
	this.pieces = pieces;
    }

    public List getPieces()
    {
	return pieces;
    }
    
    public void toPrint(PrintWriter writer)
    {
	int i = 0;
	for (Iterator it = pieces.iterator(); it.hasNext(); )
	{
	    Piece o = (Piece)it.next();
	    
	    // Show piece index only in case of memory split.
	    if (pieces.size() > 1)
		writer.print("Piece "+ i++ + ": ");
	    
	    o.toPrint(writer);
	}
    }

    /**
     * Function to map overall byte index to piece byte index.
     * 
     * @param offset - overall byte offset of value
     * @return - piece index
     */
    protected long indexOf(long offset)
    {
	// indexCount will be set to contain the overall byte index of
	// first byte of every piece.
	long indexCount = 0;

	for (Iterator it = pieces.iterator(); it.hasNext(); )
	{
	    Object o = it.next();
	    long len = ((Piece)o).getSize();

	    if ( offset >= indexCount && offset < indexCount+len )
	    {   
		// If condition will satisfy if (overall) offset is within 
		// current piece. Then, offset-indexCount will give the byte
		// index within piece.
		return (offset - indexCount);	
	    }

	    else
		indexCount += len;
	}
	return -1;	
    }

    /**
     * Function that returns the piece of given offset.
     * 
     * @param offset - overall byte offset of value
     * @return - piece that contains the byte at OFFSET
     */
    protected Piece pieceOf(long offset)
    {
	// indexCount will be set to contain the overall byte 
	// index of first byte of every piece.
	long indexCount = 0;

	for (Iterator it = pieces.iterator(); it.hasNext(); )
	{
	    Object o = it.next();
	    long len = ((Piece)o).getSize();

	    if ( offset >= indexCount && offset < indexCount+len )
	    {   
		// If condition will satisfy if (overall) offset 
		//is within current piece.
		return (Piece)o;	
	    }

	    else
		indexCount += len;
	}
	return null;	
    }

    /**
     * Return the byte at OFFSET.
     */
    protected byte getByte(long offset) 
    {
	Piece p = pieceOf(offset);
	long index = indexOf (offset);
	return p.getByte(index);
    }

    /**
     * Stores the byte VALUE at OFFSET.
     */
    protected void putByte(long offset, byte value) 
    {
	Piece p = pieceOf(offset);
	long index = indexOf (offset);
	p.putByte(index, value);
    }

    /**
     * Returns the number of bytes in location.
     */
    protected long length() 
    {
	long length = 0;
	for (Iterator it=pieces.iterator(); it.hasNext(); )
	    length +=  ((Piece)it.next()).getSize();
	return length;
    }

    /**
     * Return a slice of this Location starting at byte OFFSET, and
     * going for LENGTH bytes. The slice can contain multiple pieces.
     */
    protected Location slice(long offset, long length)
    {
	List slice = new ArrayList();
	Piece oldP = null;
	long newLen = length;  // used to track # of bytes sliced.

	while (sliceLength(slice) < length)
	{
	    oldP = pieceOf(offset);
	    long idx = indexOf (offset);
	    Piece newP = null;

	    // If remaining slice is within current piece.
	    if (idx+newLen-1 < oldP.getSize())
	    {
		// Slice the piece from idx going to newLen bytes.
		newP = oldP.slice(idx, newLen);
	    }

	    else 
	    {
		// Slice the piece from idx to the end of piece. 
		// (oldP.getSize()-idx) gives the number of remaining bytes.
		newP = oldP.slice(idx, oldP.getSize()-idx);
	    }

	    slice.add(newP);

	    // Adjust newLen and offset according to previous slice.
	    newLen -= newP.getSize();
	    offset += newP.getSize();
	}   
	return new PieceLocation(slice);
    }

    /**
     * Helper function for slice - returns the number of bytes in slice list.
     * Similar to length but takes List instead of PieceLocation.
     */
    private long sliceLength(List slice)
    {
	long length = 0;
	for (Iterator it=slice.iterator(); it.hasNext(); )
	    length +=  ((Piece)it.next()).getSize();
	return length;	
    }
}