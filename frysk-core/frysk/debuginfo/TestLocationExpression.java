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

import java.util.logging.Logger;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;

import frysk.proc.Task;
import frysk.testbed.DaemonBlockedAtSignal;
import frysk.testbed.TestLib;
import frysk.value.RegisterPiece;
import frysk.value.MemoryPiece;
import frysk.value.UnavailablePiece;
import frysk.stack.IA32Registers;
import frysk.stack.X8664Registers;

import lib.dwfl.ElfEMachine;
import lib.dwfl.DwOpEncodings;
import lib.dwfl.DwarfDie;
import lib.dwfl.DwarfOp;

public class TestLocationExpression
	extends TestLib
{
    Logger logger = Logger.getLogger("frysk");
    
    public void testPlus()
    {
	// Create dwarf operation list
	List ops = new ArrayList();
	ops.add( new DwarfOp(DwOpEncodings.DW_OP_lit10_, 0, 0, 0) );
	ops.add( new DwarfOp(DwOpEncodings.DW_OP_lit7_, 0, 0, 0) );
	ops.add( new DwarfOp(DwOpEncodings.DW_OP_plus_, 0, 0, 0) ) ;
	
	// Created expected result list
	List expectedLoc = new ArrayList();
	expectedLoc.add(new MemoryPiece((long)17, 12));
	
	checkLocExpected(ops, expectedLoc, 1);
    }
    
    /*
     * Test for DW_OP_bregX and DW_OP_dup
     */
    public void testBregxDup()
    {
	List ops = new ArrayList();

	// Note: REG1 in frysk-asm.h corresponds to registers 3 and 5 in i386 and x86_64 resp.
	switch (getArch())
	{
	    case ElfEMachine.EM_386:
		ops.add( new DwarfOp(DwOpEncodings.DW_OP_breg3_, 2, 0, 0) ); // Value in register ebx plus 2
		break;
	    case ElfEMachine.EM_X86_64:
		ops.add( new DwarfOp(DwOpEncodings.DW_OP_breg5_, 2, 0, 0) ); // Value in register rdi plus 2
		break;
	    default:	
		if (unresolvedOnPPC(4964))
		    return;
	}    
	ops.add( new DwarfOp(DwOpEncodings.DW_OP_dup_, 0, 0, 0) ) ;
	
	List expectedLoc = new ArrayList();
	expectedLoc.add(new MemoryPiece((long)101, 12));
	
	checkLocExpected(ops, expectedLoc, 2);
    }
    
    /*
     * Tests DW_OP_piece, DW_OP_regX, DW_OP_addr and creation of lists
     */
    public void testPieceRegxAddr()
    {
	List ops = new ArrayList();

	// First 6 bytes unavailable, next 4 bytes in Reg 1 and next 2 byes in memory address 0x1234
	ops.add( new DwarfOp(DwOpEncodings.DW_OP_piece_, 6, 0, 0) );
	ops.add( new DwarfOp(DwOpEncodings.DW_OP_reg1_, 0, 0, 0) );
	ops.add( new DwarfOp(DwOpEncodings.DW_OP_piece_, 4, 0, 0) );
	ops.add( new DwarfOp(DwOpEncodings.DW_OP_addr_, 0x1234, 0, 0) );
	ops.add( new DwarfOp(DwOpEncodings.DW_OP_piece_, 2, 0, 0) );
	
	List expectedLoc = new ArrayList();
	expectedLoc.add(new UnavailablePiece (6));
	
	switch (getArch())
	{
	    case ElfEMachine.EM_386:
		expectedLoc.add(new RegisterPiece(IA32Registers.ECX, 4)); // Reg 1 mapped to ECX in 386
		break;
	    case ElfEMachine.EM_X86_64:
		expectedLoc.add(new RegisterPiece(X8664Registers.RDX, 4)); //Reg 1 mapped to RDX in X86_64
		break;
	    default:	
		if (unresolvedOnPPC(4964))
		    return;
	} 

	expectedLoc.add(new MemoryPiece((long)0x1234, 2));
	
	checkLocExpected(ops, expectedLoc, 2);
    }
    
    public void testMul()
    {
	List ops = new ArrayList();
	ops.add( new DwarfOp(DwOpEncodings.DW_OP_lit10_, 0, 0, 0) );
	ops.add( new DwarfOp(DwOpEncodings.DW_OP_lit7_, 0, 0, 0) );	
	ops.add( new DwarfOp(DwOpEncodings.DW_OP_lit10_, 0, 0, 0) );
	ops.add( new DwarfOp(DwOpEncodings.DW_OP_lit7_, 0, 0, 0) );
	ops.add( new DwarfOp(DwOpEncodings.DW_OP_mul_, 0, 0, 0) ) ;
	
	List expectedLoc = new ArrayList();
	expectedLoc.add(new MemoryPiece((long)70, 12));
	
	checkLocExpected(ops, expectedLoc, 3);
    }

    public void testDiv()
    {
	List ops = new ArrayList();
	ops.add( new DwarfOp(DwOpEncodings.DW_OP_lit30_, 0, 0, 0) );
	ops.add( new DwarfOp(DwOpEncodings.DW_OP_lit10_, 0, 0, 0) );
	ops.add( new DwarfOp(DwOpEncodings.DW_OP_div_, 0, 0, 0) ) ;
	
	List expectedLoc = new ArrayList();
	expectedLoc.add(new MemoryPiece((long)3, 12));
	
	checkLocExpected(ops, expectedLoc, 1);
    }
    
    public void testMod()
    {
	List ops = new ArrayList();
	ops.add( new DwarfOp(DwOpEncodings.DW_OP_lit31_, 0, 0, 0) );
	ops.add( new DwarfOp(DwOpEncodings.DW_OP_lit10_, 0, 0, 0) );
	ops.add( new DwarfOp(DwOpEncodings.DW_OP_mod_, 0, 0, 0) ) ;
	
	List expectedLoc = new ArrayList();
	expectedLoc.add(new MemoryPiece((long)1, 12));
	
	checkLocExpected(ops, expectedLoc, 1);
    }
    
    public void testOver()
    {
	List ops = new ArrayList();
	ops.add( new DwarfOp(DwOpEncodings.DW_OP_lit10_, 0, 0, 0) );
	ops.add( new DwarfOp(DwOpEncodings.DW_OP_lit7_, 0, 0, 0) );
	ops.add( new DwarfOp(DwOpEncodings.DW_OP_over_, 0, 0, 0) ) ;
	
	List expectedLoc = new ArrayList();
	expectedLoc.add(new MemoryPiece((long)10, 12));
	
	checkLocExpected(ops, expectedLoc, 3);
    }

    public void testDrop()
    {
	List ops = new ArrayList();
	ops.add( new DwarfOp(DwOpEncodings.DW_OP_lit30_, 0, 0, 0) );
	ops.add( new DwarfOp(DwOpEncodings.DW_OP_lit1_, 0, 0, 0) );
	ops.add( new DwarfOp(DwOpEncodings.DW_OP_drop_, 0, 0, 0) ) ;
	
	List expectedLoc = new ArrayList();
	expectedLoc.add(new MemoryPiece((long)30, 12));
	
	checkLocExpected(ops, expectedLoc, 1);
    }

    public void testSwap()
    {
	List ops = new ArrayList();
	ops.add( new DwarfOp(DwOpEncodings.DW_OP_lit12_, 0, 0, 0) );
	ops.add( new DwarfOp(DwOpEncodings.DW_OP_lit15_, 0, 0, 0) );
	ops.add( new DwarfOp(DwOpEncodings.DW_OP_swap_, 0, 0, 0) ) ;
	
	List expectedLoc = new ArrayList();
	expectedLoc.add(new MemoryPiece((long)12, 12));
	
	checkLocExpected(ops, expectedLoc, 2);
    }  

    public void testRot()
    {
	List ops = new ArrayList();
	ops.add( new DwarfOp(DwOpEncodings.DW_OP_lit31_, 0, 0, 0) );
	ops.add( new DwarfOp(DwOpEncodings.DW_OP_lit7_, 0, 0, 0) );
	ops.add( new DwarfOp(DwOpEncodings.DW_OP_lit5_, 0, 0, 0) );
	ops.add( new DwarfOp(DwOpEncodings.DW_OP_rot_, 0, 0, 0) ) ;
	
	List expectedLoc = new ArrayList();
	expectedLoc.add(new MemoryPiece((long)7, 12));
	
	checkLocExpected(ops, expectedLoc, 3);
    }  

    public void testAbs()
    {
	List ops = new ArrayList();
	ops.add( new DwarfOp(DwOpEncodings.DW_OP_const1s_, -5, 0, 0) ) ;
	ops.add( new DwarfOp(DwOpEncodings.DW_OP_abs_, 0, 0, 0) );
	
	List expectedLoc = new ArrayList();
	expectedLoc.add(new MemoryPiece((long)5, 12));
	
	checkLocExpected(ops, expectedLoc, 1);
    }
    
    public void testNeg()
    {
	List ops = new ArrayList();
	ops.add( new DwarfOp(DwOpEncodings.DW_OP_const1s_, 5, 0, 0) ) ;
	ops.add( new DwarfOp(DwOpEncodings.DW_OP_neg_, 0, 0, 0) );
	
	List expectedLoc = new ArrayList();
	expectedLoc.add(new MemoryPiece((long)-5, 12));
	
	checkLocExpected(ops, expectedLoc, 1);
    }
    
    public void testNot()
    {
	List ops = new ArrayList();
	ops.add( new DwarfOp(DwOpEncodings.DW_OP_const1s_, 5, 0, 0) ) ;
	ops.add( new DwarfOp(DwOpEncodings.DW_OP_not_, 0, 0, 0) );
	
	List expectedLoc = new ArrayList();
	expectedLoc.add(new MemoryPiece((long)-6, 12));
	
	checkLocExpected(ops, expectedLoc, 1);
    }
    
    public void testAnd()
    {
	List ops = new ArrayList();
	ops.add( new DwarfOp(DwOpEncodings.DW_OP_lit3_, 0, 0, 0) );
	ops.add( new DwarfOp(DwOpEncodings.DW_OP_lit4_, 0, 0, 0) );
	ops.add( new DwarfOp(DwOpEncodings.DW_OP_and_, 0, 0, 0) ) ;
	
	List expectedLoc = new ArrayList();
	expectedLoc.add(new MemoryPiece((long)0, 12));
	
	checkLocExpected(ops, expectedLoc, 1);
    }

    public void testOr()
    {
	List ops = new ArrayList();

	ops.add( new DwarfOp(DwOpEncodings.DW_OP_lit3_, 0, 0, 0) );
	ops.add( new DwarfOp(DwOpEncodings.DW_OP_lit4_, 0, 0, 0) );
	ops.add( new DwarfOp(DwOpEncodings.DW_OP_or_, 0, 0, 0) ) ;
	
	List expectedLoc = new ArrayList();
	expectedLoc.add(new MemoryPiece((long)7, 12));
	
	checkLocExpected(ops, expectedLoc, 1);
    }
    
    public void testShl()
    {
	List ops = new ArrayList();

	ops.add( new DwarfOp(DwOpEncodings.DW_OP_lit9_, 0, 0, 0) );
	ops.add( new DwarfOp(DwOpEncodings.DW_OP_lit3_, 0, 0, 0) );
	ops.add( new DwarfOp(DwOpEncodings.DW_OP_shl_, 0, 0, 0) ) ;
	
	List expectedLoc = new ArrayList();
	expectedLoc.add(new MemoryPiece((long)72, 12));
	
	checkLocExpected(ops, expectedLoc, 1);
    }
    
    public void testShr()
    {
	List ops = new ArrayList();

	ops.add( new DwarfOp(DwOpEncodings.DW_OP_const1s_, 12, 0, 0) ) ;
	ops.add( new DwarfOp(DwOpEncodings.DW_OP_lit2_, 0, 0, 0) );
	ops.add( new DwarfOp(DwOpEncodings.DW_OP_shr_, 0, 0, 0) ) ;
	
	List expectedLoc = new ArrayList();
	expectedLoc.add(new MemoryPiece((long)3, 12));
	
	checkLocExpected(ops, expectedLoc, 1);
    }
    
    public void testShra()
    {
	List ops = new ArrayList();

	ops.add( new DwarfOp(DwOpEncodings.DW_OP_const1s_, -28, 0, 0) ) ;
	ops.add( new DwarfOp(DwOpEncodings.DW_OP_lit2_, 0, 0, 0) );
	ops.add( new DwarfOp(DwOpEncodings.DW_OP_shra_, 0, 0, 0) ) ;
	
	List expectedLoc = new ArrayList();
	expectedLoc.add(new MemoryPiece((long)-7, 12));
	
	checkLocExpected(ops, expectedLoc, 1);
    }
    
    public void testXor()
    {
	List ops = new ArrayList();

	ops.add( new DwarfOp(DwOpEncodings.DW_OP_lit9_, 0, 0, 0) );
	ops.add( new DwarfOp(DwOpEncodings.DW_OP_lit14_, 0, 0, 0) );
	ops.add( new DwarfOp(DwOpEncodings.DW_OP_xor_, 0, 0, 0) ) ;
	
	List expectedLoc = new ArrayList();
	expectedLoc.add(new MemoryPiece((long)7, 12));
	
	checkLocExpected(ops, expectedLoc, 1);
    }
    
    public void testLe()
    {
	List ops = new ArrayList();

	ops.add( new DwarfOp(DwOpEncodings.DW_OP_lit9_, 0, 0, 0) );
	ops.add( new DwarfOp(DwOpEncodings.DW_OP_lit14_, 0, 0, 0) );
	ops.add( new DwarfOp(DwOpEncodings.DW_OP_le_, 0, 0, 0) ) ;
	
	List expectedLoc = new ArrayList();
	expectedLoc.add(new MemoryPiece((long)1, 12));
	
	checkLocExpected(ops, expectedLoc, 1);
    }
    
    public void testGe()
    {
	List ops = new ArrayList();

	ops.add( new DwarfOp(DwOpEncodings.DW_OP_lit9_, 0, 0, 0) );
	ops.add( new DwarfOp(DwOpEncodings.DW_OP_lit14_, 0, 0, 0) );
	ops.add( new DwarfOp(DwOpEncodings.DW_OP_ge_, 0, 0, 0) ) ;
	
	List expectedLoc = new ArrayList();
	expectedLoc.add(new MemoryPiece((long)0, 12));
	
	checkLocExpected(ops, expectedLoc, 1);
    }
    

    
    /**
     * Function that creates Dwarf stack and checks its values
     * 
     * @param ops - List of operations
     * @param expectedLoc - Expected resultant list of location
     * @param stackSize - Expected stack size
     */
    private void checkLocExpected (List ops, List expectedLoc, int stackSize)
    {
	DwarfDie die = null;
	Task task = getStoppedTask();
	DebugInfoFrame frame = DebugInfoStackFactory.createDebugInfoStackTrace(task);
	
	LocationExpression locExp = new LocationExpression(frame, die, ops);
	List loc = locExp.decode(12);  
	
	assertEquals ("Stack size", locExp.getStackSize(), stackSize);
	compareLocations (loc, expectedLoc);

    }
   
    private void compareLocations (List loc, List locExpect)
    {
	/*
	 * Compare if the two lists are equal and set boolean isEqual
	 */
	boolean isEqual = false;
	if (loc.size() == locExpect.size())
	{    
	    for (Iterator it=loc.iterator(), it2=locExpect.iterator(); it.hasNext () && it2.hasNext(); )
	    {
		Object o = it.next();
		Object oExpect = it2.next();

		if ( o instanceof MemoryPiece && oExpect instanceof MemoryPiece)
		    isEqual = ((MemoryPiece)o).equals((MemoryPiece)oExpect);
		else if ( o instanceof RegisterPiece && oExpect instanceof RegisterPiece)
		{   
		    isEqual = ((RegisterPiece)o).equals((RegisterPiece)oExpect);
		    assertEquals ("Register", ((RegisterPiece)o).getRegister(), ((RegisterPiece)oExpect).getRegister());
		}
		else if (o instanceof UnavailablePiece && oExpect instanceof UnavailablePiece)
		    isEqual = ((UnavailablePiece)o).equals((UnavailablePiece)oExpect);
		else 
		    isEqual = false;

		if (!isEqual)
		    break;
	    } 
	}
	assertEquals ("Result", isEqual, true);
    }
    
    private Task getStoppedTask()
    {
	return this.getStoppedTask("funit-location");
    }
    
    private Task getStoppedTask (String process)
    {
	// Starts program and runs it to crash.
	DaemonBlockedAtSignal daemon = new DaemonBlockedAtSignal (new String[] { getExecPath(process) });
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