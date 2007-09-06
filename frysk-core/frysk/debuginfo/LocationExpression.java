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

import java.lang.Math;

import java.util.LinkedList;
import java.util.ArrayList;
import java.util.List;

import frysk.proc.Isa;
import frysk.stack.Register;
import frysk.stack.Frame;
import lib.dwfl.DwarfDie;
import lib.dwfl.DwarfOp;
import lib.dwfl.DwOp;
import lib.dwfl.DwAt;

class LocationExpression {
    public final static int locationTypeRegDisp = 1,
    locationTypeAddress = 2,
    locationTypeReg = 3;
    private final Frame frame;
    DwarfDie die;
    List ops;
    int locationType;
    LinkedList stack;

    LocationExpression(Frame frame, DwarfDie die, List ops) {
	locationType = 0;
	this.frame = frame;
	this.die = die;
	this.ops = ops;
	this.stack = null;
    }

    /**
     *  Decode a location list and return the value.
     *
     */
    public long decode () {
	stack = new LinkedList();
	Isa isa = frame.getTask().getIsa();
	int nops = ops.size();

	if (nops == 0)
	    if (die.getAttrBoolean(DwAt.LOCATION_)) 
		throw new VariableOptimizedOutException();  
	    else 
		throw new ValueUavailableException();

	for(int i = 0; i < nops; i++) {
	    int operator = ((DwarfOp) ops.get(i)).operator;
	    long operand1 = ((DwarfOp) ops.get(i)).operand1;
	    long operand2 = ((DwarfOp) ops.get(i)).operand2;
	    switch (operator) {
	    case DwOp.LIT0_:
	    case DwOp.LIT1_:
	    case DwOp.LIT2_:
	    case DwOp.LIT3_:
	    case DwOp.LIT4_:
	    case DwOp.LIT5_:
	    case DwOp.LIT6_:
	    case DwOp.LIT7_:
	    case DwOp.LIT8_:
	    case DwOp.LIT9_:
	    case DwOp.LIT10_:
	    case DwOp.LIT11_:
	    case DwOp.LIT12_:
	    case DwOp.LIT13_:
	    case DwOp.LIT14_:
	    case DwOp.LIT15_:
	    case DwOp.LIT16_:
	    case DwOp.LIT17_:
	    case DwOp.LIT18_:
	    case DwOp.LIT19_:
	    case DwOp.LIT20_:
	    case DwOp.LIT21_:
	    case DwOp.LIT22_:
	    case DwOp.LIT23_:
	    case DwOp.LIT24_:
	    case DwOp.LIT25_:
	    case DwOp.LIT26_:
	    case DwOp.LIT27_:
	    case DwOp.LIT28_:
	    case DwOp.LIT29_:
	    case DwOp.LIT30_:
	    case DwOp.LIT31_:
		stack.addFirst(new Long(operator - DwOp.LIT0_));
		break;

		// Register name Operators	
	    case DwOp.REG0_:
	    case DwOp.REG1_:
	    case DwOp.REG2_:
	    case DwOp.REG3_:
	    case DwOp.REG4_:
	    case DwOp.REG5_:
	    case DwOp.REG6_:
	    case DwOp.REG7_:
	    case DwOp.REG8_:
	    case DwOp.REG9_:
	    case DwOp.REG10_:
	    case DwOp.REG11_:
	    case DwOp.REG12_:
	    case DwOp.REG13_:
	    case DwOp.REG14_:
	    case DwOp.REG15_:
	    case DwOp.REG16_:
	    case DwOp.REG17_:
	    case DwOp.REG18_:
	    case DwOp.REG19_:
	    case DwOp.REG20_:
	    case DwOp.REG21_:
	    case DwOp.REG22_:
	    case DwOp.REG23_:
	    case DwOp.REG24_:
	    case DwOp.REG25_:
	    case DwOp.REG26_:
	    case DwOp.REG27_:
	    case DwOp.REG28_:
	    case DwOp.REG29_:
	    case DwOp.REG30_:
	    case DwOp.REG31_:
		if (locationType == 0) 
		    locationType = locationTypeReg;
		Register register = DwarfRegisterMapFactory.getRegisterMap(isa)
		.getRegister(operator - DwOp.REG0_);
		long regval = frame.getRegisterValue(register).asLong();
		stack.addFirst(new Long(regval));
		break;

	    case DwOp.BREG0_:
	    case DwOp.BREG1_:
	    case DwOp.BREG2_:
	    case DwOp.BREG3_:
	    case DwOp.BREG4_:
	    case DwOp.BREG5_:
	    case DwOp.BREG6_:
	    case DwOp.BREG7_:
	    case DwOp.BREG8_:
	    case DwOp.BREG9_:
	    case DwOp.BREG10_:
	    case DwOp.BREG11_:
	    case DwOp.BREG12_:
	    case DwOp.BREG13_:
	    case DwOp.BREG14_:
	    case DwOp.BREG15_:
	    case DwOp.BREG16_:
	    case DwOp.BREG17_:
	    case DwOp.BREG18_:
	    case DwOp.BREG19_:
	    case DwOp.BREG20_:
	    case DwOp.BREG21_:
	    case DwOp.BREG22_:
	    case DwOp.BREG23_:
	    case DwOp.BREG24_:
	    case DwOp.BREG25_:
	    case DwOp.BREG26_:
	    case DwOp.BREG27_:
	    case DwOp.BREG28_:
	    case DwOp.BREG29_:
	    case DwOp.BREG30_:
	    case DwOp.BREG31_:
		locationType = locationTypeRegDisp;
		register = DwarfRegisterMapFactory.getRegisterMap(isa)
		.getRegister(operator - DwOp.BREG0_);
		regval = frame.getRegisterValue(register).asLong();
		stack.addFirst(new Long(operand1 + regval));
		break;


	    case DwOp.REGX_:
		register = DwarfRegisterMapFactory.getRegisterMap(isa)
		.getRegister((int)operand1);
		regval = frame.getRegisterValue(register).asLong();
		stack.addFirst(new Long(regval));
		break;

	    case DwOp.ADDR_:
		locationType = locationTypeAddress;
		stack.addFirst(new Long(operand1));
		break;

		// DW_OP_fbreg calls recursively and pushes that value on the stack
	    case DwOp.FBREG_:
		locationType = locationTypeRegDisp;
		long pc = frame.getAdjustedAddress();
		LocationExpression frameBaseOps = new LocationExpression (frame, die, die.getFrameBase(pc));
		stack.addFirst(new Long(operand1 + frameBaseOps.decode()));
		break;

		// ??? unsigned not properly handled (use bignum?) See DwarfDie.java
	    case DwOp.CONST1U_:
	    case DwOp.CONST1S_:
	    case DwOp.CONST2U_:
	    case DwOp.CONST2S_:
	    case DwOp.CONST4U_:
	    case DwOp.CONST4S_:
	    case DwOp.CONSTU_:
	    case DwOp.CONSTS_:
		stack.addFirst(new Long(operand1));
		break;

		// Stack Operations
	    case DwOp.DUP_:
		stack.addFirst(stack.getFirst());
		break;

	    case DwOp.OVER_:
		stack.addFirst(stack.get(1));
		break;

	    case DwOp.DROP_:
		stack.removeFirst();
		break;

	    case DwOp.SWAP_:
		Long first = (Long) stack.removeFirst();
		Long second = (Long) stack.removeFirst();
		stack.addFirst(first);
		stack.addFirst(second);
		break;	

	    case DwOp.ROT_:
		first = (Long) stack.removeFirst();
		second = (Long) stack.removeFirst();
		Long third = (Long) stack.removeFirst();
		stack.addFirst(first);
		stack.addFirst(third);
		stack.addFirst(second);
		break;			

		// Arithmetic Operations
	    case DwOp.PLUS_:
		operand1 = ((Long)stack.removeFirst()).longValue();
		operand2 = ((Long)stack.removeFirst()).longValue();
		stack.addFirst(new Long(operand1 + operand2));
		break;

	    case DwOp.PLUS_UCONST_:
		operand2 = ((Long)stack.removeFirst()).longValue();
		stack.addFirst(new Long(operand1 + operand2));
		break;

	    case DwOp.MINUS_:
		operand1 = ((Long)stack.removeFirst()).longValue();
		operand2 = ((Long)stack.removeFirst()).longValue();
		stack.addFirst(new Long(operand1 + operand2)); // - ?
		break;

	    case DwOp.MUL_:
		operand1 = ((Long)stack.removeFirst()).longValue();
		operand2 = ((Long)stack.removeFirst()).longValue();
		stack.addFirst(new Long(operand1 * operand2));
		break;

	    case DwOp.DIV_:
		operand1 = ((Long)stack.removeFirst()).longValue();
		operand2 = ((Long)stack.removeFirst()).longValue();
		// Should there be a check here for operand1!=0 ?
		stack.addFirst(new Long(operand2 / operand1));
		break;

	    case DwOp.MOD_:
		operand1 = ((Long)stack.removeFirst()).longValue();
		operand2 = ((Long)stack.removeFirst()).longValue();
		// Should there be a check here for operand1!=0 ?
		stack.addFirst(new Long(operand2 % operand1));
		break;

	    case DwOp.ABS_:
		operand1 = ((Long)stack.removeFirst()).longValue();
		stack.addFirst(new Long(Math.abs(operand1)));
		break;

	    case DwOp.AND_:
		operand1 = ((Long)stack.removeFirst()).longValue();
		operand2 = ((Long)stack.removeFirst()).longValue();
		stack.addFirst(new Long(operand1 & operand2));
		break;

	    case DwOp.OR_:
		operand1 = ((Long)stack.removeFirst()).longValue();
		operand2 = ((Long)stack.removeFirst()).longValue();
		stack.addFirst(new Long(operand1 | operand2));
		break;

	    case DwOp.SHL_:
		operand1 = ((Long)stack.removeFirst()).longValue();
		operand2 = ((Long)stack.removeFirst()).longValue();
		stack.addFirst(new Long(operand2 << operand1));
		break;

	    case DwOp.SHR_:
		operand1 = ((Long)stack.removeFirst()).longValue();
		operand2 = ((Long)stack.removeFirst()).longValue();
		stack.addFirst(new Long(operand2 >>> operand1));
		break;

	    case DwOp.SHRA_:
		operand1 = ((Long)stack.removeFirst()).longValue();
		operand2 = ((Long)stack.removeFirst()).longValue();
		stack.addFirst(new Long(operand2 >> operand1));
		break;

	    case DwOp.XOR_:
		operand1 = ((Long)stack.removeFirst()).longValue();
		operand2 = ((Long)stack.removeFirst()).longValue();
		stack.addFirst(new Long(operand1 ^ operand2));
		break;

	    case DwOp.NEG_:
		operand1 = ((Long)stack.removeFirst()).longValue();
		stack.addFirst(new Long(0-operand1));
		break;

	    case DwOp.NOT_:
		operand1 = ((Long)stack.removeFirst()).longValue();
		stack.addFirst(new Long(~operand1));
		break;

		// Control flow operations
	    case DwOp.LE_:
		operand1 = ((Long)stack.removeFirst()).longValue();
		operand2 = ((Long)stack.removeFirst()).longValue();
		stack.addFirst(new Long((operand2 <= operand1)? 1:0));
		break;	

	    case DwOp.GE_:
		operand1 = ((Long)stack.removeFirst()).longValue();
		operand2 = ((Long)stack.removeFirst()).longValue();
		stack.addFirst(new Long((operand2 >= operand1)? 1:0));
		break;	

	    case DwOp.EQ_:
		operand1 = ((Long)stack.removeFirst()).longValue();
		operand2 = ((Long)stack.removeFirst()).longValue();
		stack.addFirst(new Long((operand2 == operand1)? 1:0));
		break;	

	    case DwOp.LT_:
		operand1 = ((Long)stack.removeFirst()).longValue();
		operand2 = ((Long)stack.removeFirst()).longValue();
		stack.addFirst(new Long((operand2 < operand1)? 1:0));
		break;	

	    case DwOp.GT_:
		operand1 = ((Long)stack.removeFirst()).longValue();
		operand2 = ((Long)stack.removeFirst()).longValue();
		stack.addFirst(new Long((operand2 > operand1)? 1:0));
		break;	

	    case DwOp.NE_:
		operand1 = ((Long)stack.removeFirst()).longValue();
		operand2 = ((Long)stack.removeFirst()).longValue();
		stack.addFirst(new Long((operand2 != operand1)? 1:0));
		break;	

		// ??? Support remaining operators
	    default:
		throw new ValueUavailableException();
	    }
	}
	return ((Long)stack.removeFirst()).longValue();
    }

    /**
     * Decode a location list and return the value
     * @param size - Size of variable 
     * @return List of memory or register pieces
     */
    public List decode (int size)
    {
	stack = new LinkedList();
	Isa isa = frame.getTask().getIsa();
	int nops = ops.size();

	//pieces will contain a list of MemoryPiece, RegisterPiece or UnavaiablePiece
	ArrayList pieces = new ArrayList(); 

	if (nops == 0)
	    if (die.getAttrBoolean(DwAt.LOCATION_)) 
		throw new VariableOptimizedOutException();  
	    else 
		throw new ValueUavailableException();

	for(int i = 0; i < nops; i++) {

	    int operator = ((DwarfOp) ops.get(i)).operator;
	    long operand1 = ((DwarfOp) ops.get(i)).operand1;
	    long operand2 = ((DwarfOp) ops.get(i)).operand2;

	    switch (operator) {

	    // Literal Encodings
	    case DwOp.LIT0_:
	    case DwOp.LIT1_:
	    case DwOp.LIT2_:
	    case DwOp.LIT3_:
	    case DwOp.LIT4_:
	    case DwOp.LIT5_:
	    case DwOp.LIT6_:
	    case DwOp.LIT7_:
	    case DwOp.LIT8_:
	    case DwOp.LIT9_:
	    case DwOp.LIT10_:
	    case DwOp.LIT11_:
	    case DwOp.LIT12_:
	    case DwOp.LIT13_:
	    case DwOp.LIT14_:
	    case DwOp.LIT15_:
	    case DwOp.LIT16_:
	    case DwOp.LIT17_:
	    case DwOp.LIT18_:
	    case DwOp.LIT19_:
	    case DwOp.LIT20_:
	    case DwOp.LIT21_:
	    case DwOp.LIT22_:
	    case DwOp.LIT23_:
	    case DwOp.LIT24_:
	    case DwOp.LIT25_:
	    case DwOp.LIT26_:
	    case DwOp.LIT27_:
	    case DwOp.LIT28_:
	    case DwOp.LIT29_:
	    case DwOp.LIT30_:
	    case DwOp.LIT31_:
		stack.addFirst(new Long(operator - DwOp.LIT0_));
		break;

		// Register name Operators	
	    case DwOp.REG0_:
	    case DwOp.REG1_:
	    case DwOp.REG2_:
	    case DwOp.REG3_:
	    case DwOp.REG4_:
	    case DwOp.REG5_:
	    case DwOp.REG6_:
	    case DwOp.REG7_:
	    case DwOp.REG8_:
	    case DwOp.REG9_:
	    case DwOp.REG10_:
	    case DwOp.REG11_:
	    case DwOp.REG12_:
	    case DwOp.REG13_:
	    case DwOp.REG14_:
	    case DwOp.REG15_:
	    case DwOp.REG16_:
	    case DwOp.REG17_:
	    case DwOp.REG18_:
	    case DwOp.REG19_:
	    case DwOp.REG20_:
	    case DwOp.REG21_:
	    case DwOp.REG22_:
	    case DwOp.REG23_:
	    case DwOp.REG24_:
	    case DwOp.REG25_:
	    case DwOp.REG26_:
	    case DwOp.REG27_:
	    case DwOp.REG28_:
	    case DwOp.REG29_:
	    case DwOp.REG30_:
	    case DwOp.REG31_:
		if (locationType == 0) 
		    locationType = locationTypeReg;
		Register register = DwarfRegisterMapFactory.getRegisterMap(isa)
		.getRegister(operator - DwOp.REG0_);
		// Push the register onto the dwfl stack
		stack.addFirst(register);
		break;

	    case DwOp.BREG0_:
	    case DwOp.BREG1_:
	    case DwOp.BREG2_:
	    case DwOp.BREG3_:
	    case DwOp.BREG4_:
	    case DwOp.BREG5_:
	    case DwOp.BREG6_:
	    case DwOp.BREG7_:
	    case DwOp.BREG8_:
	    case DwOp.BREG9_:
	    case DwOp.BREG10_:
	    case DwOp.BREG11_:
	    case DwOp.BREG12_:
	    case DwOp.BREG13_:
	    case DwOp.BREG14_:
	    case DwOp.BREG15_:
	    case DwOp.BREG16_:
	    case DwOp.BREG17_:
	    case DwOp.BREG18_:
	    case DwOp.BREG19_:
	    case DwOp.BREG20_:
	    case DwOp.BREG21_:
	    case DwOp.BREG22_:
	    case DwOp.BREG23_:
	    case DwOp.BREG24_:
	    case DwOp.BREG25_:
	    case DwOp.BREG26_:
	    case DwOp.BREG27_:
	    case DwOp.BREG28_:
	    case DwOp.BREG29_:
	    case DwOp.BREG30_:
	    case DwOp.BREG31_:
		locationType = locationTypeRegDisp;
		register = DwarfRegisterMapFactory.getRegisterMap(isa)
		.getRegister(operator - DwOp.BREG0_);
		long regval = frame.getRegisterValue(register).asLong();
		stack.addFirst(new Long(operand1 + regval));
		break;

	    case DwOp.REGX_:
		if (locationType == 0) 
		    locationType = locationTypeReg;
		register = DwarfRegisterMapFactory.getRegisterMap(isa)
		.getRegister((int)operand1);
		stack.addFirst(register);
		break;

	    case DwOp.BREGX_:
		locationType = locationTypeRegDisp;
		register = DwarfRegisterMapFactory.getRegisterMap(isa)
		.getRegister((int)operand1);
		regval = frame.getRegisterValue(register).asLong();
		stack.addFirst(new Long(operand2 + regval));
		break;

	    case DwOp.ADDR_:
		locationType = locationTypeAddress;
		stack.addFirst(new Long(operand1));
		break;

		// DW_OP_fbreg calls recursively and pushes that value on the stack
	    case DwOp.FBREG_:
		locationType = locationTypeRegDisp;
		long pc = frame.getAdjustedAddress();
		LocationExpression frameBaseOps = new LocationExpression (frame, die, die.getFrameBase(pc));
		stack.addFirst(new Long(operand1 + frameBaseOps.decode()));
		break;

		// ??? unsigned not properly handled (use bignum?) See DwarfDie.java
	    case DwOp.CONST1U_:
	    case DwOp.CONST1S_:
	    case DwOp.CONST2U_:
	    case DwOp.CONST2S_:
	    case DwOp.CONST4U_:
	    case DwOp.CONST4S_:
	    case DwOp.CONSTU_:
	    case DwOp.CONSTS_:
		stack.addFirst(new Long(operand1));
		break;

		// Stack Operations
	    case DwOp.DUP_:
		stack.addFirst(stack.getFirst());
		break;

	    case DwOp.DROP_:
		stack.removeFirst();
		break;

	    case DwOp.SWAP_:
		Long first = (Long) stack.removeFirst();
		Long second = (Long) stack.removeFirst();
		stack.addFirst(first);
		stack.addFirst(second);
		break;	

	    case DwOp.ROT_:
		first = (Long) stack.removeFirst();
		second = (Long) stack.removeFirst();
		Long third = (Long) stack.removeFirst();
		stack.addFirst(first);
		stack.addFirst(third);
		stack.addFirst(second);
		break;		
		
	    case DwOp.OVER_:
		stack.addFirst(stack.get(1));
		break;

	    case DwOp.PICK_:
		stack.addFirst (stack.get((int)(operand1))); 
		break;
		
		// Arithmetic Operations
	    case DwOp.PLUS_:
		operand1 = ((Long)stack.removeFirst()).longValue();
		operand2 = ((Long)stack.removeFirst()).longValue();
		stack.addFirst(new Long(operand1 + operand2));
		break;

	    case DwOp.PLUS_UCONST_:
		operand2 = ((Long)stack.removeFirst()).longValue();
		stack.addFirst(new Long(operand1 + operand2));
		break;

	    case DwOp.MINUS_:
		operand1 = ((Long)stack.removeFirst()).longValue();
		operand2 = ((Long)stack.removeFirst()).longValue();
		stack.addFirst(new Long(operand2 - operand1)); 
		break;

	    case DwOp.MUL_:
		operand1 = ((Long)stack.removeFirst()).longValue();
		operand2 = ((Long)stack.removeFirst()).longValue();
		stack.addFirst(new Long(operand1 * operand2));
		break;

	    case DwOp.DIV_:
		operand1 = ((Long)stack.removeFirst()).longValue();
		operand2 = ((Long)stack.removeFirst()).longValue();
		// Should there be a check here for operand1!=0 ?
		stack.addFirst(new Long(operand2 / operand1));
		break;

	    case DwOp.MOD_:
		operand1 = ((Long)stack.removeFirst()).longValue();
		operand2 = ((Long)stack.removeFirst()).longValue();
		// Should there be a check here for operand1!=0 ?
		stack.addFirst(new Long(operand2 % operand1));
		break;

	    case DwOp.ABS_:
		operand1 = ((Long)stack.removeFirst()).longValue();
		stack.addFirst(new Long(Math.abs(operand1)));
		break;

	    case DwOp.AND_:
		operand1 = ((Long)stack.removeFirst()).longValue();
		operand2 = ((Long)stack.removeFirst()).longValue();
		stack.addFirst(new Long(operand1 & operand2));
		break;

	    case DwOp.OR_:
		operand1 = ((Long)stack.removeFirst()).longValue();
		operand2 = ((Long)stack.removeFirst()).longValue();
		stack.addFirst(new Long(operand1 | operand2));
		break;

	    case DwOp.SHL_:
		operand1 = ((Long)stack.removeFirst()).longValue();
		operand2 = ((Long)stack.removeFirst()).longValue();
		stack.addFirst(new Long(operand2 << operand1));
		break;

	    case DwOp.SHR_:
		operand1 = ((Long)stack.removeFirst()).longValue();
		operand2 = ((Long)stack.removeFirst()).longValue();
		stack.addFirst(new Long(operand2 >>> operand1));
		break;

	    case DwOp.SHRA_:
		operand1 = ((Long)stack.removeFirst()).longValue();
		operand2 = ((Long)stack.removeFirst()).longValue();
		stack.addFirst(new Long(operand2 >> operand1));
		break;

	    case DwOp.XOR_:
		operand1 = ((Long)stack.removeFirst()).longValue();
		operand2 = ((Long)stack.removeFirst()).longValue();
		stack.addFirst(new Long(operand1 ^ operand2));
		break;

	    case DwOp.NEG_:
		operand1 = ((Long)stack.removeFirst()).longValue();
		stack.addFirst(new Long(0-operand1));
		break;

	    case DwOp.NOT_:
		operand1 = ((Long)stack.removeFirst()).longValue();
		stack.addFirst(new Long(~operand1));
		break;

		// Control flow operations
	    case DwOp.LE_:
		operand1 = ((Long)stack.removeFirst()).longValue();
		operand2 = ((Long)stack.removeFirst()).longValue();
		stack.addFirst(new Long((operand2 <= operand1)? 1:0));
		break;	

	    case DwOp.GE_:
		operand1 = ((Long)stack.removeFirst()).longValue();
		operand2 = ((Long)stack.removeFirst()).longValue();
		stack.addFirst(new Long((operand2 >= operand1)? 1:0));
		break;	

	    case DwOp.EQ_:
		operand1 = ((Long)stack.removeFirst()).longValue();
		operand2 = ((Long)stack.removeFirst()).longValue();
		stack.addFirst(new Long((operand2 == operand1)? 1:0));
		break;	

	    case DwOp.LT_:
		operand1 = ((Long)stack.removeFirst()).longValue();
		operand2 = ((Long)stack.removeFirst()).longValue();
		stack.addFirst(new Long((operand2 < operand1)? 1:0));
		break;	

	    case DwOp.GT_:
		operand1 = ((Long)stack.removeFirst()).longValue();
		operand2 = ((Long)stack.removeFirst()).longValue();
		stack.addFirst(new Long((operand2 > operand1)? 1:0));
		break;	

	    case DwOp.NE_:
		operand1 = ((Long)stack.removeFirst()).longValue();
		operand2 = ((Long)stack.removeFirst()).longValue();
		stack.addFirst(new Long((operand2 != operand1)? 1:0));
		break;	

		//Special Operations
	    case DwOp.NOP_:
		// Do nothing 
		break;

		// Composition Operators
	    case DwOp.PIECE_:
		// Case where some bytes of value is unavailable 
		if (i==0 || ((DwarfOp)(ops.get(i-1))).operator==DwOp.PIECE_)
		{
		    pieces.add(new UnavailablePiece(operand1));
		    break;
		}	
		// Otherwise, check the type of element on stack top and add to list
		addToList (pieces, operand1);	
		break;

	    default:
		throw new ValueUavailableException();
	    }
	}

	/* 
	 * If pieces is empty, its the case where there is no memory split between registers and memory
	 * Then add element on stack top to the empty list
	 */
	if (pieces.isEmpty())
	{    
	    addToList (pieces, size);
	}    

	return pieces;
    }

    /**
     * Function that checks the type of element on the stack top and adds it to the
     * list of location
     */
    private void addToList (List pieces, long size)
    {
	/*
	 * If stackTop is a Register, add it as a RegisterPiece to list pieces 
	 * If it is a long value, add it as a MemoryPiece 
	 */
	Object stackTop = stack.getFirst();

	if (stackTop instanceof Register)
	    pieces.add(new RegisterPiece((Register)stackTop, size));
	else if (stackTop instanceof Long)
	    pieces.add(new MemoryPiece(((Long)stackTop).longValue(), size));
    }


    /**
     *  Return register number for a one entry DW_OP_regX location list 
     *
     */
    public Register getRegisterNumber () {
	Isa isa = frame.getTask().getIsa();

	if (ops.size() == 1) {
	    int operator = ((DwarfOp) ops.get(0)).operator;
	    if (operator >= DwOp.REG0_
		    || operator <=  DwOp.REG31_) {
		locationType = locationTypeReg;
		return DwarfRegisterMapFactory.getRegisterMap(isa)
		.getRegister(operator - DwOp.REG0_);
	    }
	}
	return null;
    }

    public int getLocationType () {
	return locationType;
    }

    public int getStackSize() {
	return ((stack != null) ? stack.size() : 0);
    }
}
