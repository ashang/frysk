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

import java.util.logging.Logger;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
import frysk.isa.ISA;
import frysk.stack.StackFactory;
import frysk.proc.Task;
import frysk.testbed.DaemonBlockedAtSignal;
import frysk.testbed.FryskAsm;
import frysk.testbed.TestLib;
import frysk.stack.Frame;
import lib.dwfl.DwOp;
import lib.dwfl.DwarfDie;
import lib.dwfl.DwarfOp;

public class TestLocationExpression 
	extends TestLib
{
    Logger logger = Logger.getLogger("frysk");
       
    /*
     * Test for DW_OP_breg3/DW_OP_breg5 and DW_OP_dup
     */
    public void testBregxDup() {
	if (unresolvedOnPPC(4964))
	    return;
	List ops = new ArrayList();

	Task task = getStoppedTask();
	ISA isa = task.getISA();

	// Note: REG1 in frysk-asm.h corresponds to registers 3 and 5
	// in i386 and x86_64 resp.  FIXME: Can use
	// frysk.testbed.FryskAsm to determine the register
	// name/number.
	if (isa == ISA.IA32) {
	    ops.add( new DwarfOp(DwOp.BREG3_, 2, 0, 0) ); // Value in register ebx plus 2
	} else if (isa == ISA.X8664) {
	    ops.add( new DwarfOp(DwOp.BREG5_, 2, 0, 0) ); // Value in register rdi plus 2
	} else {
	    throw new RuntimeException("unknown isa: " + isa);
	}    
	ops.add( new DwarfOp(DwOp.DUP_, 0, 0, 0) ) ;
	
	List expectedLoc = new ArrayList();
	expectedLoc.add(new MemoryPiece((long)989, 12));
	
	checkLocExpected(ops, expectedLoc, 2);
    }
    
    /*
     * Test for DW_OP_bregx
     */  
    public void testBregx()
    {
	List ops = new ArrayList();

	FryskAsm asmRegs = FryskAsm.createFryskAsm(getStoppedTask().getISA());
	Number dwarfReg1 = DwarfRegisterMapFactory.getRegisterMap(getStoppedTask().getISA())
	                           .getRegisterNumber(asmRegs.REG1);
	ops.add( new DwarfOp(DwOp.BREGX_, dwarfReg1.intValue(), 2, 0) );
		
	List expectedLoc = new ArrayList();
	expectedLoc.add(new MemoryPiece((long)989, 12));
	
	checkLocExpected(ops, expectedLoc, 1);
    }
    
    /*
     * Tests DW_OP_piece, DW_OP_regX, DW_OP_addr and creation of lists
     */
    public void testPieceRegxAddr()
    {
	List ops = new ArrayList();

	// First 6 bytes unavailable, next 4 bytes in Register and next 2 
	// bytes in memory address 0x1234
	ops.add( new DwarfOp(DwOp.PIECE_, 6, 0, 0) );
	ops.add( new DwarfOp(DwOp.REG0_, 0, 0, 0) );  // ECX or RDX
	ops.add( new DwarfOp(DwOp.PIECE_, 4, 0, 0) );
	ops.add( new DwarfOp(DwOp.ADDR_, 0x1234, 0, 0) );
	ops.add( new DwarfOp(DwOp.PIECE_, 2, 0, 0) );
	
	List expectedLoc = new ArrayList();
	Task task = getStoppedTask();
	Frame frame = StackFactory.createFrame(task);
	FryskAsm asmRegs = FryskAsm.createFryskAsm(task.getISA());
	expectedLoc.add(new UnavailablePiece (6));
	expectedLoc.add(new RegisterPiece(asmRegs.REG0, 4, frame));  
	expectedLoc.add(new MemoryPiece((long)0x1234, 2));
	
	checkLocExpected(ops, expectedLoc, 2);
    }

    /*
     * Tests DW_OP_mul and DW_OP_pick 
     */
    public void testPickMul()
    {
	List ops = new ArrayList();
	ops.add( new DwarfOp(DwOp.LIT10_, 0, 0, 0) );
	ops.add( new DwarfOp(DwOp.LIT7_, 0, 0, 0) );
	ops.add( new DwarfOp(DwOp.LIT6_, 0, 0, 0) ) ;
	ops.add( new DwarfOp(DwOp.PICK_, 2, 0, 0) );
	ops.add( new DwarfOp(DwOp.MUL_, 0, 0, 0) ) ;
	
	List expectedLoc = new ArrayList();
	expectedLoc.add(new MemoryPiece((long)60, 12));
	
	checkLocExpected(ops, expectedLoc, 3);
    }

    /*
     * Tests DW_OP_plus and DW_OP_over 
     */    
    public void testOverPlus()
    {
	// Create dwarf operation list
	List ops = new ArrayList();
	ops.add( new DwarfOp(DwOp.LIT10_, 0, 0, 0) );
	ops.add( new DwarfOp(DwOp.LIT7_, 0, 0, 0) );
	ops.add( new DwarfOp(DwOp.OVER_, 0, 0, 0) );
	ops.add( new DwarfOp(DwOp.PLUS_, 0, 0, 0) ) ;
	
	// Created expected result list
	List expectedLoc = new ArrayList();
	expectedLoc.add(new MemoryPiece((long)17, 12));
	
	checkLocExpected(ops, expectedLoc, 2);
    }
    
    public void testDiv()
    {
	List ops = new ArrayList();
	ops.add( new DwarfOp(DwOp.LIT30_, 0, 0, 0) );
	ops.add( new DwarfOp(DwOp.LIT10_, 0, 0, 0) );
	ops.add( new DwarfOp(DwOp.DIV_, 0, 0, 0) ) ;
	
	List expectedLoc = new ArrayList();
	expectedLoc.add(new MemoryPiece((long)3, 12));
	
	checkLocExpected(ops, expectedLoc, 1);
    }
    
    public void testMod()
    {
	List ops = new ArrayList();
	ops.add( new DwarfOp(DwOp.LIT31_, 0, 0, 0) );
	ops.add( new DwarfOp(DwOp.LIT10_, 0, 0, 0) );
	ops.add( new DwarfOp(DwOp.MOD_, 0, 0, 0) ) ;
	
	List expectedLoc = new ArrayList();
	expectedLoc.add(new MemoryPiece((long)1, 12));
	
	checkLocExpected(ops, expectedLoc, 1);
    }

    public void testDrop()
    {
	List ops = new ArrayList();
	ops.add( new DwarfOp(DwOp.LIT30_, 0, 0, 0) );
	ops.add( new DwarfOp(DwOp.LIT1_, 0, 0, 0) );
	ops.add( new DwarfOp(DwOp.DROP_, 0, 0, 0) ) ;
	
	List expectedLoc = new ArrayList();
	expectedLoc.add(new MemoryPiece((long)30, 12));
	
	checkLocExpected(ops, expectedLoc, 1);
    }

    public void testSwap()
    {
	List ops = new ArrayList();
	ops.add( new DwarfOp(DwOp.LIT12_, 0, 0, 0) );
	ops.add( new DwarfOp(DwOp.LIT15_, 0, 0, 0) );
	ops.add( new DwarfOp(DwOp.SWAP_, 0, 0, 0) ) ;
	
	List expectedLoc = new ArrayList();
	expectedLoc.add(new MemoryPiece((long)12, 12));
	
	checkLocExpected(ops, expectedLoc, 2);
    }  

    public void testRot()
    {
	List ops = new ArrayList();
	ops.add( new DwarfOp(DwOp.LIT31_, 0, 0, 0) );
	ops.add( new DwarfOp(DwOp.LIT7_, 0, 0, 0) );
	ops.add( new DwarfOp(DwOp.LIT5_, 0, 0, 0) );
	ops.add( new DwarfOp(DwOp.ROT_, 0, 0, 0) ) ;
	
	List expectedLoc = new ArrayList();
	expectedLoc.add(new MemoryPiece((long)7, 12));
	
	checkLocExpected(ops, expectedLoc, 3);
    }  

    public void testAbs()
    {
	List ops = new ArrayList();
	ops.add( new DwarfOp(DwOp.CONST1S_, -5, 0, 0) ) ;
	ops.add( new DwarfOp(DwOp.ABS_, 0, 0, 0) );
	
	List expectedLoc = new ArrayList();
	expectedLoc.add(new MemoryPiece((long)5, 12));
	
	checkLocExpected(ops, expectedLoc, 1);
    }
    
    public void testNeg()
    {
	List ops = new ArrayList();
	ops.add( new DwarfOp(DwOp.CONST1S_, 5, 0, 0) ) ;
	ops.add( new DwarfOp(DwOp.NEG_, 0, 0, 0) );
	
	List expectedLoc = new ArrayList();
	expectedLoc.add(new MemoryPiece((long)-5, 12));
	
	checkLocExpected(ops, expectedLoc, 1);
    }
    
    public void testNot()
    {
	List ops = new ArrayList();
	ops.add( new DwarfOp(DwOp.CONST1S_, 5, 0, 0) ) ;
	ops.add( new DwarfOp(DwOp.NOT_, 0, 0, 0) );
	
	List expectedLoc = new ArrayList();
	expectedLoc.add(new MemoryPiece((long)-6, 12));
	
	checkLocExpected(ops, expectedLoc, 1);
    }
    
    public void testAnd()
    {
	List ops = new ArrayList();
	ops.add( new DwarfOp(DwOp.LIT3_, 0, 0, 0) );
	ops.add( new DwarfOp(DwOp.LIT4_, 0, 0, 0) );
	ops.add( new DwarfOp(DwOp.AND_, 0, 0, 0) ) ;
	
	List expectedLoc = new ArrayList();
	expectedLoc.add(new MemoryPiece((long)0, 12));
	
	checkLocExpected(ops, expectedLoc, 1);
    }

    public void testOr()
    {
	List ops = new ArrayList();

	ops.add( new DwarfOp(DwOp.LIT3_, 0, 0, 0) );
	ops.add( new DwarfOp(DwOp.LIT4_, 0, 0, 0) );
	ops.add( new DwarfOp(DwOp.OR_, 0, 0, 0) ) ;
	
	List expectedLoc = new ArrayList();
	expectedLoc.add(new MemoryPiece((long)7, 12));
	
	checkLocExpected(ops, expectedLoc, 1);
    }
    
    public void testShl()
    {
	List ops = new ArrayList();

	ops.add( new DwarfOp(DwOp.LIT9_, 0, 0, 0) );
	ops.add( new DwarfOp(DwOp.LIT3_, 0, 0, 0) );
	ops.add( new DwarfOp(DwOp.SHL_, 0, 0, 0) ) ;
	
	List expectedLoc = new ArrayList();
	expectedLoc.add(new MemoryPiece((long)72, 12));
	
	checkLocExpected(ops, expectedLoc, 1);
    }
    
    public void testShr()
    {
	List ops = new ArrayList();

	ops.add( new DwarfOp(DwOp.CONST1S_, 12, 0, 0) ) ;
	ops.add( new DwarfOp(DwOp.LIT2_, 0, 0, 0) );
	ops.add( new DwarfOp(DwOp.SHR_, 0, 0, 0) ) ;
	
	List expectedLoc = new ArrayList();
	expectedLoc.add(new MemoryPiece((long)3, 12));
	
	checkLocExpected(ops, expectedLoc, 1);
    }
    
    public void testShra()
    {
	List ops = new ArrayList();

	ops.add( new DwarfOp(DwOp.CONST1S_, -28, 0, 0) ) ;
	ops.add( new DwarfOp(DwOp.LIT2_, 0, 0, 0) );
	ops.add( new DwarfOp(DwOp.SHRA_, 0, 0, 0) ) ;
	
	List expectedLoc = new ArrayList();
	expectedLoc.add(new MemoryPiece((long)-7, 12));
	
	checkLocExpected(ops, expectedLoc, 1);
    }
    
    public void testXor()
    {
	List ops = new ArrayList();

	ops.add( new DwarfOp(DwOp.LIT9_, 0, 0, 0) );
	ops.add( new DwarfOp(DwOp.LIT14_, 0, 0, 0) );
	ops.add( new DwarfOp(DwOp.XOR_, 0, 0, 0) ) ;
	
	List expectedLoc = new ArrayList();
	expectedLoc.add(new MemoryPiece((long)7, 12));
	
	checkLocExpected(ops, expectedLoc, 1);
    }
    
    public void testLe()
    {
	List ops = new ArrayList();

	ops.add( new DwarfOp(DwOp.LIT9_, 0, 0, 0) );
	ops.add( new DwarfOp(DwOp.LIT14_, 0, 0, 0) );
	ops.add( new DwarfOp(DwOp.LE_, 0, 0, 0) ) ;
	
	List expectedLoc = new ArrayList();
	expectedLoc.add(new MemoryPiece((long)1, 12));
	
	checkLocExpected(ops, expectedLoc, 1);
    }
    
    public void testGe()
    {
	List ops = new ArrayList();

	ops.add( new DwarfOp(DwOp.LIT9_, 0, 0, 0) );
	ops.add( new DwarfOp(DwOp.LIT14_, 0, 0, 0) );
	ops.add( new DwarfOp(DwOp.GE_, 0, 0, 0) ) ;
	
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
	Frame frame = StackFactory.createFrame(task);
	
	LocationExpression locExp = new LocationExpression(die, ops);
	List loc = locExp.decode(frame,12);  
	
	assertEquals ("Stack size", stackSize, locExp.getStackSize());
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
	    for (Iterator it=loc.iterator(), it2=locExpect.iterator(); 
	         it.hasNext () && it2.hasNext(); )
	    {
		Object o = it.next();
		Object oExpect = it2.next();

		if (o.getClass().getName().equals(oExpect.getClass().getName()))
		{
		    // Note: equals() overridden for the pieces.
		    isEqual = o.equals(oExpect);

		    if (o instanceof MemoryPiece)
			assertEquals ("Memory", ((MemoryPiece)oExpect).getMemory(), 
				      ((MemoryPiece)o).getMemory());		  

		    else if (o instanceof RegisterPiece)  	
			assertEquals ("Register", ((RegisterPiece)oExpect).getRegister(), 
				      ((RegisterPiece)o).getRegister());		    

		    if (!isEqual)
			break;
		} 
	    }
	}
	assertEquals ("Result", true, isEqual);
    }
    
    private Task getStoppedTask()
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
}  