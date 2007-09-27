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

import frysk.isa.Register;
import frysk.stack.Frame;
import java.io.PrintWriter;

public class RegisterPiece 
	extends Piece
{
    private final long base;
    private final Register register;  
    private final Frame frame;

    private RegisterPiece(Register register, long size, Frame frame,
			  long base) {
	super(size);
	this.register = register;
	this.frame = frame;
	this.base = base;
    }

    /**
     * Used for testing LocationExpression.
     */
    public RegisterPiece(Register register, long size)
    {
	this(register, size, null, 0);
    }
    
    public RegisterPiece(Register register, long size, Frame frame)
    {
	this(register, size, frame, 0);
    }  
    
    public Register getRegister()
    {
	return register;
    }
        
    protected void putByte(long index, byte value) 
    {
	byte[] bytes = new byte[] { value };
	frame.setRegister(register, base + index, 1, bytes, 0);
    }
       
    protected byte getByte(long index) 
    {
	byte[] bytes = new byte[1];
	frame.getRegister(register, base + index, 1, bytes, 0);
	return bytes[0];
    }
    
    protected Piece slice (long offset, long length) {
	return new RegisterPiece(register, length, frame, base + offset);
    }
    
    public boolean equals(Object p)
    {
	return ( this.size == ((RegisterPiece)p).size 
		&& register.equals(((RegisterPiece)p).register) );
    }	
    
    protected void toPrint(PrintWriter writer)
    {
	writer.print("Register ");
	writer.print(register.name);
	writer.print(" - ");
	writer.print(size);
	writer.print(" byte(s)");
    }
}
