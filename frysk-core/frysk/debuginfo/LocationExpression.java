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

import java.util.LinkedList;
import java.util.List;

import frysk.proc.Isa;
import frysk.stack.Register;

import lib.dwfl.DwarfDie;
import lib.dwfl.DwarfOp;
import lib.dwfl.DwOpEncodings;
import lib.dwfl.DwAtEncodings;

class LocationExpression {
    public final static int locationTypeRegDisp = 1,
    locationTypeAddress = 2,
    locationTypeReg = 3;
    DebugInfoFrame frame;
    DwarfDie die;
    List ops;
    int locationType;
    
    public LocationExpression (DebugInfoFrame frame, DwarfDie die, List ops) {
	locationType = 0;
	this.frame = frame;
	this.die = die;
	this.ops = ops;
    }
    
    /**
     *  Decode a location list and return the value.
     *
     */
    public long decode () {
	LinkedList stack = new LinkedList();
	Isa isa = frame.getTask().getIsa();
	int nops = ops.size();

	if (nops == 0)
	    if (die.getAttrBoolean(DwAtEncodings.DW_AT_location_)) 
		throw new VariableOptimizedOutException();  
	    else 
		throw new ValueUavailableException();

	for(int i = 0; i < nops; i++) {
	    int operator = ((DwarfOp) ops.get(i)).operator;
	    long operand1 = ((DwarfOp) ops.get(i)).operand1;
	    long operand2 = ((DwarfOp) ops.get(i)).operand2;
	    switch (operator) {
	    case DwOpEncodings.DW_OP_lit0_:
	    case DwOpEncodings.DW_OP_lit1_:
	    case DwOpEncodings.DW_OP_lit2_:
	    case DwOpEncodings.DW_OP_lit3_:
	    case DwOpEncodings.DW_OP_lit4_:
	    case DwOpEncodings.DW_OP_lit5_:
	    case DwOpEncodings.DW_OP_lit6_:
	    case DwOpEncodings.DW_OP_lit7_:
	    case DwOpEncodings.DW_OP_lit8_:
	    case DwOpEncodings.DW_OP_lit9_:
	    case DwOpEncodings.DW_OP_lit10_:
	    case DwOpEncodings.DW_OP_lit11_:
	    case DwOpEncodings.DW_OP_lit12_:
	    case DwOpEncodings.DW_OP_lit13_:
	    case DwOpEncodings.DW_OP_lit14_:
	    case DwOpEncodings.DW_OP_lit15_:
	    case DwOpEncodings.DW_OP_lit16_:
	    case DwOpEncodings.DW_OP_lit17_:
	    case DwOpEncodings.DW_OP_lit18_:
	    case DwOpEncodings.DW_OP_lit19_:
	    case DwOpEncodings.DW_OP_lit20_:
	    case DwOpEncodings.DW_OP_lit21_:
	    case DwOpEncodings.DW_OP_lit22_:
	    case DwOpEncodings.DW_OP_lit23_:
	    case DwOpEncodings.DW_OP_lit24_:
	    case DwOpEncodings.DW_OP_lit25_:
	    case DwOpEncodings.DW_OP_lit26_:
	    case DwOpEncodings.DW_OP_lit27_:
	    case DwOpEncodings.DW_OP_lit28_:
	    case DwOpEncodings.DW_OP_lit29_:
	    case DwOpEncodings.DW_OP_lit30_:
	    case DwOpEncodings.DW_OP_lit31_:
		stack.addFirst(new Long(operator - DwOpEncodings.DW_OP_lit0_));
		break;

	    case DwOpEncodings.DW_OP_reg0_:
	    case DwOpEncodings.DW_OP_reg1_:
	    case DwOpEncodings.DW_OP_reg2_:
	    case DwOpEncodings.DW_OP_reg3_:
	    case DwOpEncodings.DW_OP_reg4_:
	    case DwOpEncodings.DW_OP_reg5_:
	    case DwOpEncodings.DW_OP_reg6_:
	    case DwOpEncodings.DW_OP_reg7_:
	    case DwOpEncodings.DW_OP_reg8_:
	    case DwOpEncodings.DW_OP_reg9_:
	    case DwOpEncodings.DW_OP_reg10_:
	    case DwOpEncodings.DW_OP_reg11_:
	    case DwOpEncodings.DW_OP_reg12_:
	    case DwOpEncodings.DW_OP_reg13_:
	    case DwOpEncodings.DW_OP_reg14_:
	    case DwOpEncodings.DW_OP_reg15_:
	    case DwOpEncodings.DW_OP_reg16_:
	    case DwOpEncodings.DW_OP_reg17_:
	    case DwOpEncodings.DW_OP_reg18_:
	    case DwOpEncodings.DW_OP_reg19_:
	    case DwOpEncodings.DW_OP_reg20_:
	    case DwOpEncodings.DW_OP_reg21_:
	    case DwOpEncodings.DW_OP_reg22_:
	    case DwOpEncodings.DW_OP_reg23_:
	    case DwOpEncodings.DW_OP_reg24_:
	    case DwOpEncodings.DW_OP_reg25_:
	    case DwOpEncodings.DW_OP_reg26_:
	    case DwOpEncodings.DW_OP_reg27_:
	    case DwOpEncodings.DW_OP_reg28_:
	    case DwOpEncodings.DW_OP_reg29_:
	    case DwOpEncodings.DW_OP_reg30_:
	    case DwOpEncodings.DW_OP_reg31_:
		if (locationType == 0) 
		    locationType = locationTypeReg;
		Register register = DwarfRegisterMapFactory.getRegisterMap(isa)
		.getRegister(operator - DwOpEncodings.DW_OP_reg0_);
		long regval = frame.getRegisterValue(register).longValue();
		stack.addFirst(new Long(regval));
		break;

	    case DwOpEncodings.DW_OP_breg0_:
	    case DwOpEncodings.DW_OP_breg1_:
	    case DwOpEncodings.DW_OP_breg2_:
	    case DwOpEncodings.DW_OP_breg3_:
	    case DwOpEncodings.DW_OP_breg4_:
	    case DwOpEncodings.DW_OP_breg5_:
	    case DwOpEncodings.DW_OP_breg6_:
	    case DwOpEncodings.DW_OP_breg7_:
	    case DwOpEncodings.DW_OP_breg8_:
	    case DwOpEncodings.DW_OP_breg9_:
	    case DwOpEncodings.DW_OP_breg10_:
	    case DwOpEncodings.DW_OP_breg11_:
	    case DwOpEncodings.DW_OP_breg12_:
	    case DwOpEncodings.DW_OP_breg13_:
	    case DwOpEncodings.DW_OP_breg14_:
	    case DwOpEncodings.DW_OP_breg15_:
	    case DwOpEncodings.DW_OP_breg16_:
	    case DwOpEncodings.DW_OP_breg17_:
	    case DwOpEncodings.DW_OP_breg18_:
	    case DwOpEncodings.DW_OP_breg19_:
	    case DwOpEncodings.DW_OP_breg20_:
	    case DwOpEncodings.DW_OP_breg21_:
	    case DwOpEncodings.DW_OP_breg22_:
	    case DwOpEncodings.DW_OP_breg23_:
	    case DwOpEncodings.DW_OP_breg24_:
	    case DwOpEncodings.DW_OP_breg25_:
	    case DwOpEncodings.DW_OP_breg26_:
	    case DwOpEncodings.DW_OP_breg27_:
	    case DwOpEncodings.DW_OP_breg28_:
	    case DwOpEncodings.DW_OP_breg29_:
	    case DwOpEncodings.DW_OP_breg30_:
	    case DwOpEncodings.DW_OP_breg31_:
		locationType = locationTypeRegDisp;
		register = DwarfRegisterMapFactory.getRegisterMap(isa)
		.getRegister(operator - DwOpEncodings.DW_OP_breg0_);
		regval = frame.getRegisterValue(register).longValue();
		stack.addFirst(new Long(operand1 + regval));
		break;


	    case DwOpEncodings.DW_OP_regx_:
		register = DwarfRegisterMapFactory.getRegisterMap(isa)
		.getRegister((int)operand1);
		regval = frame.getRegisterValue(register).longValue();
		stack.addFirst(new Long(regval));
		break;

	    case DwOpEncodings.DW_OP_addr_:
		locationType = locationTypeAddress;
		stack.addFirst(new Long(operand1));
		break;

	    case DwOpEncodings.DW_OP_fbreg_:
		locationType = locationTypeRegDisp;
		long pc = frame.getAdjustedAddress();
		LocationExpression frameBaseOps = new LocationExpression (frame, die, die.getFrameBase(pc));
		stack.addFirst(new Long(operand1 + frameBaseOps.decode()));
		break;

		// ??? unsigned not properly handled (use bignum?) See DwarfDie.java
	    case DwOpEncodings.DW_OP_const1u_:
	    case DwOpEncodings.DW_OP_const1s_:
	    case DwOpEncodings.DW_OP_const2u_:
	    case DwOpEncodings.DW_OP_const2s_:
	    case DwOpEncodings.DW_OP_const4u_:
	    case DwOpEncodings.DW_OP_const4s_:
	    case DwOpEncodings.DW_OP_constu_:
	    case DwOpEncodings.DW_OP_consts_:
		stack.addFirst(new Long(operand1));
		break;
            
            // Stack Operations
	    case DwOpEncodings.DW_OP_dup_:
		stack.addFirst(stack.getFirst());
		break;
		
	    case DwOpEncodings.DW_OP_over_:
		stack.addFirst(stack.get(1));
		break;
		
	    case DwOpEncodings.DW_OP_drop_:
		stack.removeFirst();
		break;

	    case DwOpEncodings.DW_OP_swap_:
                Long first = (Long) stack.removeFirst();
                Long second = (Long) stack.removeFirst();
                stack.addFirst(first);
                stack.addFirst(second);
		break;	
		
	    case DwOpEncodings.DW_OP_rot_:
                first = (Long) stack.removeFirst();
                second = (Long) stack.removeFirst();
                stack.addFirst(first);
                stack.addFirst(second);
		break;			

            // Arithmetic Operations
	    case DwOpEncodings.DW_OP_plus_:
		operand1 = ((Long)stack.removeFirst()).longValue();
		operand2 = ((Long)stack.removeFirst()).longValue();
		stack.addFirst(new Long(operand1 + operand2));
		break;

	    case DwOpEncodings.DW_OP_plus_uconst_:
		operand2 = ((Long)stack.removeFirst()).longValue();
		stack.addFirst(new Long(operand1 + operand2));
		break;

	    case DwOpEncodings.DW_OP_minus_:
		operand1 = ((Long)stack.removeFirst()).longValue();
		operand2 = ((Long)stack.removeFirst()).longValue();
		stack.addFirst(new Long(operand1 + operand2)); // - ?
		break;
	
	    case DwOpEncodings.DW_OP_mul_:
		operand1 = ((Long)stack.removeFirst()).longValue();
		operand2 = ((Long)stack.removeFirst()).longValue();
		stack.addFirst(new Long(operand1 * operand2));
		break;

		// ??? Support remaining operators

	    default:
		throw new ValueUavailableException();
	    }
	}
	return ((Long)stack.removeFirst()).longValue();
    }
    
    /**
     *  Return register number for a one entry DW_OP_regX location list 
     *
     */
    public Register getRegisterNumber () {
	Isa isa = frame.getTask().getIsa();
	
	if (ops.size() == 1) {
	    int operator = ((DwarfOp) ops.get(0)).operator;
	    if (operator >= DwOpEncodings.DW_OP_reg0_
		|| operator <=  DwOpEncodings.DW_OP_reg31_) {
		locationType = locationTypeReg;
		return DwarfRegisterMapFactory.getRegisterMap(isa)
		.getRegister(operator - DwOpEncodings.DW_OP_reg0_);
	    }
	}
	return null;
    }

    public int getLocationType () {
	return locationType;
    }
}
