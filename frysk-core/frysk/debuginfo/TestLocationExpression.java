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
import frysk.testbed.DaemonBlockedAtEntry;
import frysk.testbed.TestLib;
import frysk.value.RegisterPiece;
import frysk.value.MemoryPiece;

import lib.dwfl.DwOpEncodings;
import lib.dwfl.DwarfDie;
import lib.dwfl.DwarfOp;

public class TestLocationExpression
	extends TestLib
{
    Logger logger = Logger.getLogger("frysk");
    
    /*
     * Tests for Dwarf Operations
     */
    
    public void testDup()
    {
	List ops = new ArrayList();
	// Create ops
	ops.add( new DwarfOp(DwOpEncodings.DW_OP_lit15_, 0, 0, 0) );
	ops.add( new DwarfOp(DwOpEncodings.DW_OP_dup_, 0, 0, 0) ) ;

	// Create expected value
	List expectedLoc = new ArrayList();
	expectedLoc.add(new MemoryPiece((long)15, LocationExpression.NOMEMORYSPLIT));
	
	checkLocExpected(ops, expectedLoc, 1); //Expect 1 and not 2 since stack top popped by decode()
    }

    public void testPlus()
    {
	List ops = new ArrayList();
	ops.add( new DwarfOp(DwOpEncodings.DW_OP_lit10_, 0, 0, 0) );
	ops.add( new DwarfOp(DwOpEncodings.DW_OP_lit7_, 0, 0, 0) );
	ops.add( new DwarfOp(DwOpEncodings.DW_OP_plus_, 0, 0, 0) ) ;
	
	List expectedLoc = new ArrayList();
	expectedLoc.add(new MemoryPiece((long)17, LocationExpression.NOMEMORYSPLIT));
	
	checkLocExpected(ops, expectedLoc, 0);
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
	expectedLoc.add(new MemoryPiece((long)70, LocationExpression.NOMEMORYSPLIT));
	
	checkLocExpected(ops, expectedLoc, 2);
    }

    public void testDiv()
    {
	List ops = new ArrayList();
	ops.add( new DwarfOp(DwOpEncodings.DW_OP_lit30_, 0, 0, 0) );
	ops.add( new DwarfOp(DwOpEncodings.DW_OP_lit10_, 0, 0, 0) );
	ops.add( new DwarfOp(DwOpEncodings.DW_OP_div_, 0, 0, 0) ) ;
	
	List expectedLoc = new ArrayList();
	expectedLoc.add(new MemoryPiece((long)3, LocationExpression.NOMEMORYSPLIT));
	
	checkLocExpected(ops, expectedLoc, 0);
    }
    
    public void testMod()
    {
	List ops = new ArrayList();
	ops.add( new DwarfOp(DwOpEncodings.DW_OP_lit31_, 0, 0, 0) );
	ops.add( new DwarfOp(DwOpEncodings.DW_OP_lit10_, 0, 0, 0) );
	ops.add( new DwarfOp(DwOpEncodings.DW_OP_mod_, 0, 0, 0) ) ;
	
	List expectedLoc = new ArrayList();
	expectedLoc.add(new MemoryPiece((long)1, LocationExpression.NOMEMORYSPLIT));
	
	checkLocExpected(ops, expectedLoc, 0);
    }
    
    public void testOver()
    {
	List ops = new ArrayList();
	ops.add( new DwarfOp(DwOpEncodings.DW_OP_lit10_, 0, 0, 0) );
	ops.add( new DwarfOp(DwOpEncodings.DW_OP_lit7_, 0, 0, 0) );
	ops.add( new DwarfOp(DwOpEncodings.DW_OP_over_, 0, 0, 0) ) ;
	
	List expectedLoc = new ArrayList();
	expectedLoc.add(new MemoryPiece((long)10, LocationExpression.NOMEMORYSPLIT));
	
	checkLocExpected(ops, expectedLoc, 2);
    }

    public void testDrop()
    {
	List ops = new ArrayList();
	ops.add( new DwarfOp(DwOpEncodings.DW_OP_lit30_, 0, 0, 0) );
	ops.add( new DwarfOp(DwOpEncodings.DW_OP_lit1_, 0, 0, 0) );
	ops.add( new DwarfOp(DwOpEncodings.DW_OP_drop_, 0, 0, 0) ) ;
	
	List expectedLoc = new ArrayList();
	expectedLoc.add(new MemoryPiece((long)30, LocationExpression.NOMEMORYSPLIT));
	
	checkLocExpected(ops, expectedLoc, 0);
    }

    public void testSwap()
    {
	List ops = new ArrayList();
	ops.add( new DwarfOp(DwOpEncodings.DW_OP_lit12_, 0, 0, 0) );
	ops.add( new DwarfOp(DwOpEncodings.DW_OP_lit15_, 0, 0, 0) );
	ops.add( new DwarfOp(DwOpEncodings.DW_OP_swap_, 0, 0, 0) ) ;
	
	List expectedLoc = new ArrayList();
	expectedLoc.add(new MemoryPiece((long)12, LocationExpression.NOMEMORYSPLIT));
	
	checkLocExpected(ops, expectedLoc, 1);
    }  

    public void testRot()
    {
	List ops = new ArrayList();
	ops.add( new DwarfOp(DwOpEncodings.DW_OP_lit31_, 0, 0, 0) );
	ops.add( new DwarfOp(DwOpEncodings.DW_OP_lit7_, 0, 0, 0) );
	ops.add( new DwarfOp(DwOpEncodings.DW_OP_lit5_, 0, 0, 0) );
	ops.add( new DwarfOp(DwOpEncodings.DW_OP_rot_, 0, 0, 0) ) ;
	
	List expectedLoc = new ArrayList();
	expectedLoc.add(new MemoryPiece((long)7, LocationExpression.NOMEMORYSPLIT));
	
	checkLocExpected(ops, expectedLoc, 2);
    }  

    public void testAbs()
    {
	List ops = new ArrayList();
	ops.add( new DwarfOp(DwOpEncodings.DW_OP_const1s_, -5, 0, 0) ) ;
	ops.add( new DwarfOp(DwOpEncodings.DW_OP_abs_, 0, 0, 0) );
	
	List expectedLoc = new ArrayList();
	expectedLoc.add(new MemoryPiece((long)5, LocationExpression.NOMEMORYSPLIT));
	
	checkLocExpected(ops, expectedLoc, 0);
    }
    
    public void testNeg()
    {
	List ops = new ArrayList();
	ops.add( new DwarfOp(DwOpEncodings.DW_OP_const1s_, 5, 0, 0) ) ;
	ops.add( new DwarfOp(DwOpEncodings.DW_OP_neg_, 0, 0, 0) );
	
	List expectedLoc = new ArrayList();
	expectedLoc.add(new MemoryPiece((long)-5, LocationExpression.NOMEMORYSPLIT));
	
	checkLocExpected(ops, expectedLoc, 0);
    }
    
    public void testNot()
    {
	List ops = new ArrayList();
	ops.add( new DwarfOp(DwOpEncodings.DW_OP_const1s_, 5, 0, 0) ) ;
	ops.add( new DwarfOp(DwOpEncodings.DW_OP_not_, 0, 0, 0) );
	
	List expectedLoc = new ArrayList();
	expectedLoc.add(new MemoryPiece((long)-6, LocationExpression.NOMEMORYSPLIT));
	
	checkLocExpected(ops, expectedLoc, 0);
    }
    
    public void testAnd()
    {
	List ops = new ArrayList();
	ops.add( new DwarfOp(DwOpEncodings.DW_OP_lit3_, 0, 0, 0) );
	ops.add( new DwarfOp(DwOpEncodings.DW_OP_lit4_, 0, 0, 0) );
	ops.add( new DwarfOp(DwOpEncodings.DW_OP_and_, 0, 0, 0) ) ;
	
	List expectedLoc = new ArrayList();
	expectedLoc.add(new MemoryPiece((long)0, LocationExpression.NOMEMORYSPLIT));
	
	checkLocExpected(ops, expectedLoc, 0);
    }

    public void testOr()
    {
	List ops = new ArrayList();

	ops.add( new DwarfOp(DwOpEncodings.DW_OP_lit3_, 0, 0, 0) );
	ops.add( new DwarfOp(DwOpEncodings.DW_OP_lit4_, 0, 0, 0) );
	ops.add( new DwarfOp(DwOpEncodings.DW_OP_or_, 0, 0, 0) ) ;
	
	List expectedLoc = new ArrayList();
	expectedLoc.add(new MemoryPiece((long)7, LocationExpression.NOMEMORYSPLIT));
	
	checkLocExpected(ops, expectedLoc, 0);
    }
    
    public void testShl()
    {
	List ops = new ArrayList();

	ops.add( new DwarfOp(DwOpEncodings.DW_OP_lit9_, 0, 0, 0) );
	ops.add( new DwarfOp(DwOpEncodings.DW_OP_lit3_, 0, 0, 0) );
	ops.add( new DwarfOp(DwOpEncodings.DW_OP_shl_, 0, 0, 0) ) ;
	
	List expectedLoc = new ArrayList();
	expectedLoc.add(new MemoryPiece((long)72, LocationExpression.NOMEMORYSPLIT));
	
	checkLocExpected(ops, expectedLoc, 0);
    }
    
    public void testShr()
    {
	List ops = new ArrayList();

	ops.add( new DwarfOp(DwOpEncodings.DW_OP_const1s_, 12, 0, 0) ) ;
	ops.add( new DwarfOp(DwOpEncodings.DW_OP_lit2_, 0, 0, 0) );
	ops.add( new DwarfOp(DwOpEncodings.DW_OP_shr_, 0, 0, 0) ) ;
	
	List expectedLoc = new ArrayList();
	expectedLoc.add(new MemoryPiece((long)3, LocationExpression.NOMEMORYSPLIT));
	
	checkLocExpected(ops, expectedLoc, 0);
    }
    
    public void testShra()
    {
	List ops = new ArrayList();

	ops.add( new DwarfOp(DwOpEncodings.DW_OP_const1s_, -28, 0, 0) ) ;
	ops.add( new DwarfOp(DwOpEncodings.DW_OP_lit2_, 0, 0, 0) );
	ops.add( new DwarfOp(DwOpEncodings.DW_OP_shra_, 0, 0, 0) ) ;
	
	List expectedLoc = new ArrayList();
	expectedLoc.add(new MemoryPiece((long)-7, LocationExpression.NOMEMORYSPLIT));
	
	checkLocExpected(ops, expectedLoc, 0);
    }
    
    public void testXor()
    {
	List ops = new ArrayList();

	ops.add( new DwarfOp(DwOpEncodings.DW_OP_lit9_, 0, 0, 0) );
	ops.add( new DwarfOp(DwOpEncodings.DW_OP_lit14_, 0, 0, 0) );
	ops.add( new DwarfOp(DwOpEncodings.DW_OP_xor_, 0, 0, 0) ) ;
	
	List expectedLoc = new ArrayList();
	expectedLoc.add(new MemoryPiece((long)7, LocationExpression.NOMEMORYSPLIT));
	
	checkLocExpected(ops, expectedLoc, 0);
    }
    
    public void testLe()
    {
	List ops = new ArrayList();

	ops.add( new DwarfOp(DwOpEncodings.DW_OP_lit9_, 0, 0, 0) );
	ops.add( new DwarfOp(DwOpEncodings.DW_OP_lit14_, 0, 0, 0) );
	ops.add( new DwarfOp(DwOpEncodings.DW_OP_le_, 0, 0, 0) ) ;
	
	List expectedLoc = new ArrayList();
	expectedLoc.add(new MemoryPiece((long)1, LocationExpression.NOMEMORYSPLIT));
	
	checkLocExpected(ops, expectedLoc, 0);
    }
    
    public void testGe()
    {
	List ops = new ArrayList();

	ops.add( new DwarfOp(DwOpEncodings.DW_OP_lit9_, 0, 0, 0) );
	ops.add( new DwarfOp(DwOpEncodings.DW_OP_lit14_, 0, 0, 0) );
	ops.add( new DwarfOp(DwOpEncodings.DW_OP_ge_, 0, 0, 0) ) ;
	
	List expectedLoc = new ArrayList();
	expectedLoc.add(new MemoryPiece((long)0, LocationExpression.NOMEMORYSPLIT));
	
	checkLocExpected(ops, expectedLoc, 0);
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
	List loc = locExp.decode(1); // Bogus value 1 
	
	assertEquals ("Stack size", locExp.getStackSize(), stackSize);
	
	/*
	 * Compare if the two lists are equal and set boolean isEqual
	 */
	boolean isEqual = false;
	if (loc.size() == expectedLoc.size())
	{    
	    for (Iterator it=loc.iterator(), it2=expectedLoc.iterator(); it.hasNext () && it2.hasNext(); )
	    {
		Object o = it.next();
		Object oExpect = it2.next();

		if ( o instanceof MemoryPiece && oExpect instanceof MemoryPiece)
		    isEqual = ((MemoryPiece)o).isEqual((MemoryPiece)oExpect);
		else if ( o instanceof RegisterPiece && oExpect instanceof RegisterPiece)
		    isEqual = ((RegisterPiece)o).isEqual((RegisterPiece)oExpect);
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
	return this.getStoppedTask("funit-stacks");
    }

    private Task getStoppedTask (String process)
    {
	DaemonBlockedAtEntry ackProc = new DaemonBlockedAtEntry(new String[] { getExecPath(process) });
	Task task = ackProc.getMainTask();
	return task;
    }  
}  