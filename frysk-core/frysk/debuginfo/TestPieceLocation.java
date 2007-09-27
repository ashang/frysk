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
import inua.eio.ByteOrder;

import frysk.isa.IA32Registers;
import frysk.isa.X8664Registers;
import frysk.testbed.TestLib;
import frysk.proc.Task;
import frysk.testbed.DaemonBlockedAtSignal;
import frysk.value.Location;

import java.util.List;
import java.util.ArrayList;

import lib.dwfl.ElfEMachine;

public class TestPieceLocation 
extends TestLib
{
    private PieceLocation l;

    public void setUp() 
    {
	//  Creating: { 5 6 7 8 9 } { 1 2 3 } { 12 14 16 } { (REG1=987) or -37 3 0 0 }
	List pieces = new ArrayList();
	pieces.add(new MemoryPiece( 3, 5, 
		   new ArrayByteBuffer(new byte[] { 127,127,127, 5, 6, 7, 8, 9, 127, 127 })));
	pieces.add(new MemoryPiece( 1, 3, 
		   new ArrayByteBuffer(new byte[] { 127, 1, 2, 3 })));
	pieces.add(new MemoryPiece( 0, 3, 
		   new ArrayByteBuffer(new byte[] { 12, 14, 16 })));
	
	DebugInfoFrame frame = DebugInfoStackFactory.createDebugInfoStackTrace(getStoppedTask());
	switch (getArch())
	{
	    case ElfEMachine.EM_386:
		pieces.add(new RegisterPiece(IA32Registers.EBX, 4,frame));  	// Reg 1 mapped to EBX in 386
		break;
	    case ElfEMachine.EM_X86_64:
		pieces.add(new RegisterPiece(X8664Registers.RDI, 4,frame)); 	//Reg 1 mapped to RDI in X86_64   
		break;
	    default:	
		if (unresolvedOnPPC(4964))
		    return;
	} 

	l = new PieceLocation (pieces);
    }

    public void tearDown() 
    {
	l = null;
    }

    public void testMapping() 
    {
	// Test for length
	assertEquals ("total bytes", l.length(), 15); 

	// Test for index and piece mapping
	assertEquals("piece index", 1, l.indexOf(6));
	assertEquals("piece", l.getPieces().get(1), l.pieceOf(6));
    }

    public void testGetPutByte()
    {
	// Test for putByte & getByte of MemoryPiece
	l.putByte(6, (byte)88);
	//  New list should be: { 5 6 7 8 9 } { 1 99 3 } { 12 14 16 } { -37 3 0 0}
	assertEquals("byte", 88, l.getByte(6));
	
	// Test for putByte & getByte of RegisterPiece
	l.putByte(13, (byte)1);
	assertEquals("byte", 1, l.getByte(13));  	
	
	assertEquals("byte", -37, l.getByte(11));
	assertEquals("byte", 3, l.getByte(12));
	assertEquals("byte", 0, l.getByte(14));
    }

    public void testSlice() 
    {
	// Test for slice
	Location slice = l.slice(7, 5);
	PieceLocation pSlice = (PieceLocation)slice;
	
	// Slice should be { 3 } { 12 14 16 } { (reg) -37 }
	assertEquals("# of pieces", 3, pSlice.getPieces().size());
	assertEquals("# of bytes", 5, pSlice.length());
	assertEquals("byte", 14, pSlice.getByte(2));
	assertEquals("byte", 3, pSlice.getByte(0));
	assertEquals("byte", -37, pSlice.getByte(4));
	assertEquals("memory offset", 3, 
		     ((MemoryPiece)pSlice.getPieces().get(0)).getMemory());
    }
    
    public void testGet() 
    {
	byte bytes[] = l.get(ByteOrder.BIG_ENDIAN);
	assertEquals ("bytes",  new byte[] { 5, 6, 7, 8, 9, 1, 2, 3, 12, 14, 16, -37, 3,  0, 0}, 
		      bytes);
    }
    
    private Task getStoppedTask ()
    {
	return this.getStoppedTask("funit-location");
    }
    
    private Task getStoppedTask (String process)
    {
	// Starts program and runs it to crash/signal.
	DaemonBlockedAtSignal daemon = new DaemonBlockedAtSignal 
	                               (new String[] { getExecPath(process) });
	return daemon.getMainTask();
    }  
    
    /**
     * Function that returns the Machine type as defined in ElfEMachine.java 
     */
    private int getArch ()
    {
	Task task = getStoppedTask();
	return task.getIsa().getElfMachineType();
    }
}
