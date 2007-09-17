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

import frysk.stack.Register;
import frysk.stack.RegisterMap;
import frysk.stack.Frame;

import java.math.BigInteger;

public class RegisterPiece 
	extends Piece
{
    private final Register register;  
    private final Frame frame;
    private byte[] byteArray;

    /**
     * Used for testing LocationExpression.
     */
    public RegisterPiece(Register register, long size)
    {
	super (size);
	this.register = register;
	frame = null;
    }
    
    public RegisterPiece(Register register, long size, Frame frame)
    {
	super (size);
	this.register = register;
  	this.frame = frame;
  	
  	// Get the value inside the register as a byte array of size SIZE.
	BigInteger big = new BigInteger(Long.toString(frame.getRegisterValue(register).asLong()));
	byte[] regBytes = big.toByteArray();
	byteArray = new byte[(int)size];
	if (regBytes.length <= byteArray.length)
	    for (int i=0; i<regBytes.length; i++)
		byteArray[(int)(byteArray.length-regBytes.length+i)] = regBytes[i];
	else
	    for (int i=0; i<byteArray.length; i++)
		byteArray[i] = regBytes [regBytes.length-byteArray.length+i];
    }  
    
    public Register getRegister()
    {
	return register;
    }
        
    protected void putByte(long index, byte value) 
    {
	// Set byte at specified index, convert value to long 
	// and write to register.
	byteArray[(int)(size-index-1)] = value;          
	BigInteger regVal = new BigInteger(byteArray);
	RegisterMap map = DwarfRegisterMapFactory.getRegisterMap(frame.getTask().getIsa());
	// FIXME: setReg fails
	frame.setReg(map.getRegisterNumber(register), regVal.longValue()); 
    }
       
    protected byte getByte(long index) 
    {
	// Bytes in byteBuffer is in opposite order.
	return byteArray[(int)(byteArray.length-index-1)];
    }
    
    protected Piece slice (long offset, long length)
    {
	byte[] slice = new byte[(int)length];
	// Since, byteArray has bytes in opposite order
	// adjust offset accordingly.
	long offAdjust = size-offset-1;
	
	// Write bytes from offset going to length to slice
	int iSlice = (int)length;
	for (int i=(int)offAdjust; i>=size-length; i--)
	    slice[--iSlice] = byteArray[i];

	// Create new frame and set its register value to slice
	Frame newFrame = DebugInfoStackFactory.createDebugInfoStackTrace(frame.getTask());
	RegisterMap map = DwarfRegisterMapFactory.getRegisterMap(newFrame.getTask().getIsa());
	// FIXME: setReg fails
	newFrame.setReg(map.getRegisterNumber(register), new BigInteger(slice).longValue());

	Piece newP =  new RegisterPiece (register, length, newFrame);    
	return newP;
    }
    
    public boolean equals(Object p)
    {
	return ( this.size == ((RegisterPiece)p).size 
		&& register.equals(((RegisterPiece)p).register) );
    }	
}