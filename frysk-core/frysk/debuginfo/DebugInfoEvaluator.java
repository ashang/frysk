// This file is part of the program FRYSK.
//
// Copyright 2006, 2007, Red Hat Inc.
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

import inua.eio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import inua.eio.ByteOrder;
import javax.naming.NameNotFoundException;
import lib.dwfl.BaseTypes;
import lib.dwfl.DwTag;
import lib.dwfl.DwarfDie;
import lib.dwfl.Dwfl;
import lib.dwfl.DwflDieBias;
import frysk.dwfl.DwflCache;
import frysk.expr.ExprSymTab;
import frysk.isa.ISA;
import frysk.proc.Task;
import frysk.scopes.Subprogram;
import frysk.scopes.Variable;
import frysk.isa.Register;
import frysk.value.ArithmeticType;
import frysk.value.UnknownType;
import frysk.value.ArrayType;
import frysk.value.GccStructOrClassType;
import frysk.value.StandardTypes;
import frysk.value.Value;
import frysk.value.ByteBufferLocation;
import frysk.isa.Registers;
import frysk.isa.RegistersFactory;

class DebugInfoEvaluator
    implements ExprSymTab
{
    private Task task;
    private final DebugInfoFrame frame;
    private ByteBuffer buffer;
    private final ISA isa;

    private ArithmeticType byteType;
    // private ArithmeticType byteUnsignedType;
    private ArithmeticType shortType;
    // private ArithmeticType shortUnsignedType;
    private ArithmeticType intType;
    // private ArithmeticType intUnsignedType;
    private ArithmeticType longType;
    // private ArithmeticType longUnsignedType;
    private ArithmeticType floatType;
    private ArithmeticType doubleType;
  
    /**
     * Create an DebugInfoEvaluator object which is the interface between
     * DebugInfo and CppTreeParser, the expression parser.
     * 
     * @param frame StackFrame
     * @param task_p Task
     * @param pid_p Pid
     */
    DebugInfoEvaluator (DebugInfoFrame frame) {
	this.frame = frame;
	this.task = frame.getTask();
	buffer = this.task.getMemory();
	isa = task.getISA();
	ByteOrder order = isa.order();
	byteType = StandardTypes.getByteType(order);
	// byteUnsignedType = new ArithmeticType(1, byteorder,
	// BaseTypes.baseTypeUnsignedByte, "unsigned byte");
	shortType = StandardTypes.getShortType(order);
	// shortUnsignedType = new ArithmeticType(2, byteorder,
	// BaseTypes.baseTypeUnsignedShort, "unsigned short");
	intType = StandardTypes.getIntType(order);
	// intUnsignedType = new ArithmeticType(4, byteorder,
	// BaseTypes.baseTypeUnsignedInteger, "unsigned int");
	longType = StandardTypes.getLongType(order);
	// longUnsignedType = new ArithmeticType(8, byteorder,
	// BaseTypes.baseTypeUnsignedLong, "unsigned long");
	floatType = StandardTypes.getFloatType(order);
	doubleType = StandardTypes.getDoubleType(order);
    }

    private interface VariableAccessor {
	DwarfDie varDie = null;

	long getAddr (DwarfDie die) throws NameNotFoundException;

	long getLong (DwarfDie varDieP, long offset) throws NameNotFoundException;

	int getInt (DwarfDie varDieP, long offset) throws NameNotFoundException;

	short getShort (DwarfDie varDieP, long offset) throws NameNotFoundException;

	byte getByte (DwarfDie varDieP, long offset) throws NameNotFoundException;

	float getFloat (DwarfDie varDieP, long offset) throws NameNotFoundException;

	double getDouble (DwarfDie varDieP, long offset) throws NameNotFoundException;

    }

    /**
     * Access by DW_FORM_block. Typically this is a static address or ptr+disp.
     */
    private class AccessMemory
	implements VariableAccessor
    {
      
	/**
	 * @param varDieP The die for a symbol
	 * @return The address corresponding to the symbol
	 */
	protected long getBufferAddr (DwarfDie varDieP) throws NameNotFoundException {
	    long pc = frame.getAdjustedAddress();
       
	    List ops = varDieP.getFormData(pc);
      
	    LocationExpression locExp = new LocationExpression(frame, varDieP, ops);
	    long value = locExp.decode();
	    if (locExp.getLocationType() != LocationExpression.locationTypeAddress
		&& locExp.getLocationType() != LocationExpression.locationTypeRegDisp)
		throw new NameNotFoundException();
	    return value;
	}

	public long getAddr (DwarfDie die) throws NameNotFoundException {
	    return getBufferAddr(die);
	}

	public long getLong (DwarfDie varDieP, long offset) throws NameNotFoundException {
	    long addr = getBufferAddr(varDieP);
	    return buffer.getLong(addr + offset);
	}

	public int getInt (DwarfDie varDieP, long offset) throws NameNotFoundException {
	    long addr = getBufferAddr(varDieP);
	    return buffer.getInt(addr + offset);
	}

	public short getShort (DwarfDie varDieP, long offset) throws NameNotFoundException {
	    long addr = getBufferAddr(varDieP);
	    return buffer.getShort(addr + offset);
	}

	public byte getByte (DwarfDie varDieP, long offset) throws NameNotFoundException {
	    long addr = getBufferAddr(varDieP);
	    return buffer.getByte(addr + offset);
	}

	public float getFloat (DwarfDie varDieP, long offset) throws NameNotFoundException {
	    long addr = getBufferAddr(varDieP);
	    return buffer.getFloat(addr + offset);
	}

	public double getDouble (DwarfDie varDieP, long offset) throws NameNotFoundException {
	    long addr = getBufferAddr(varDieP);
	    return buffer.getDouble(addr + offset);
	}
    }

    /**
     * Access by DW_FORM_data. Typically this is a location list.
     */
    class AccessRegisters
	implements VariableAccessor
    {
	public long getAddr (DwarfDie die) throws NameNotFoundException {
	    return 0;
	}

	/**
	 * @param varDieP Die for a symbol
	 * @return The contents of the register corresponding to the value of symbol
	 * @throws NameNotFoundException
	 */    
	private long getRegister(DwarfDie varDieP) throws NameNotFoundException {
	    long pc = frame.getAdjustedAddress();
       
	    List ops = varDieP.getFormData(pc);
      
	    LocationExpression locExp = new LocationExpression(frame, varDieP, ops);
	    long value  = locExp.decode();
	    if (locExp.getLocationType() != LocationExpression.locationTypeReg)
		throw new NameNotFoundException();
	    return value;
	}

	public long getLong (DwarfDie varDieP, long offset) throws NameNotFoundException
	{
	    return getRegister(varDieP);
	}

	public int getInt (DwarfDie varDieP, long offset) throws NameNotFoundException {
	    return (int) getRegister(varDieP);
	}

	public short getShort (DwarfDie varDieP, long offset) throws NameNotFoundException {
	    return (short) getRegister(varDieP);
	}

	public byte getByte (DwarfDie varDieP, long offset) throws NameNotFoundException {
	    return (byte) getRegister(varDieP);
	}

	public float getFloat (DwarfDie varDieP, long offset) throws NameNotFoundException {
	    long val = getRegister(varDieP);
	    float fval = Float.intBitsToFloat((int)val);
	    return fval;
	}

	public double getDouble (DwarfDie varDieP, long offset) throws NameNotFoundException {
	    long val = getRegister(varDieP);
	    double dval = Double.longBitsToDouble(val);
	    return dval;
	}
    }

    /**
     * @param s Symbol s
     * @return The die for symbol s
     */
    public Variable getVariable (String s) throws NameNotFoundException {
	Dwfl dwfl;
	DwarfDie[] allDies;
	Variable variable;
	long pc = this.frame.getAdjustedAddress();

	dwfl = DwflCache.getDwfl(task);
	DwflDieBias bias = dwfl.getDie(pc);
	if (bias == null)
	    return null;
	DwarfDie die = bias.die;

 	allDies = die.getScopes(pc - bias.bias);
	Subprogram b = frame.getSubprogram();
	LinkedList vars = b.getVariables();

 	Iterator iterator = vars.iterator();
 	while (iterator.hasNext()) {
 	    variable = (Variable) iterator.next();
 	    if (variable.getName() != null && variable.getName().compareTo(s) == 0)
 		{
 		    variable.getVariableDie().setScopes(allDies);
 		    return variable;
 		}
 	}

	// Do we have something above didn't find, e.g. a static symbol?
        DwarfDie varDie = die.getScopeVar(allDies, s);
	// Do we have something above didn't find, e.g. DW_TAG_enumerator?
	if (varDie == null)
            // e.g. DW_TAG_enumerator 
	    varDie = DwarfDie.getDeclCU(allDies, s);
	if (varDie == null)
            throw new NameNotFoundException();
        variable = new Variable(varDie);
        return variable;
    }

    /**
     * @return Value for symbol s in frame f
     */
    public Value getValue (String s) throws NameNotFoundException {
	if (s.charAt(0) == '$') {
	    Registers regs = RegistersFactory.getRegisters(frame.getTask()
							   .getISA());
	    String regName = s.substring(1).trim();
	    Register reg = regs.getRegister(regName);
	    if (reg == null) {
		throw new RuntimeException("unknown register: " + regName);
	    }
	    List pieces = new LinkedList();
	    pieces.add(new RegisterPiece(reg, reg.getType().getSize()));
	    return new Value(reg.getType(), new PieceLocation(pieces));
	}

	Variable var = getVariable(s);
	return var.getValue(frame);
    }
  
    /**
     * @return Value associated with the given DwarfDie.
     * @see frysk.expr.ExprSymTab#getValue(java.lang.String)
     */
    public Value getValue (Variable var) {
	if (var == null)
	    return (null);
	return var.getValue(frame);
    }

    /**
     * Returns the value of symbol which is defined by components.
     * @param f The frame containing the symbol
     * @param components Token list of members and indices, e.g. given a.b.c[1][2]
     * {a,b,c,1,1,2,2}
     * @return Value of the symbol
     */
    public Value getValue (ArrayList components) throws NameNotFoundException {
	String s = (String)components.get(0);
	Variable variable = getVariable(s);
	if (variable == null)
	    return (null);

	Value v = getValue(s);
	if (v.getType() instanceof ArrayType)
	    return ((ArrayType)v.getType()).get(v, 1, components);
	else if (v.getType() instanceof GccStructOrClassType)
	    return ((GccStructOrClassType)v.getType()).get(v, 0, components);
	else
	    return new Value(new UnknownType(variable.getName()));
    }
  
    /**
     * @param f Frame containing symbol s
     * @param s Symbol s
     * @return Value corresponding to the memory location pointed to by symbol s.
     */
    public Value getMemory (String s) throws NameNotFoundException {     
	Variable variable= getVariable(s);
	if (variable == null)
	    return new Value(new UnknownType(variable.getName()));
    
	DwarfDie type = variable.getVariableDie().getUltimateType();
	AccessMemory access = new AccessMemory();
	long addr = access.getAddr(getVariable(s).getVariableDie()); 
	long addrIndirect = buffer.getLong(addr);
    
	switch (type.getUltimateType().getBaseType()) {
	case BaseTypes.baseTypeByte:
	case BaseTypes.baseTypeUnsignedByte:
	    return byteType.createValue(buffer.getByte(addrIndirect));
	case BaseTypes.baseTypeShort:
	case BaseTypes.baseTypeUnsignedShort:
	    return shortType.createValue(buffer.getShort(addrIndirect));
	case BaseTypes.baseTypeInteger:
	case BaseTypes.baseTypeUnsignedInteger:
	    return intType.createValue(buffer.getInt(addrIndirect));
	case BaseTypes.baseTypeLong:
	case BaseTypes.baseTypeUnsignedLong:
	    return longType.createValue(buffer.getLong(addrIndirect));
	case BaseTypes.baseTypeFloat:
	    return floatType.createValue(buffer.getFloat(addrIndirect));
	case BaseTypes.baseTypeDouble:
	    return doubleType.createValue(buffer.getDouble(addrIndirect));
	}
	int tag = type != null ? type.getTag().hashCode() : variable.getVariableDie().getTag().hashCode();
	switch (tag) {
	case DwTag.ARRAY_TYPE_: {
	    DwarfDie subrange;
	    subrange = type.getChild();
	    TypeEntry debugInfoType = new TypeEntry(isa);
	    ArrayType arrayType = debugInfoType.getArrayType(type, subrange);

	    if (arrayType == null)
		return null;
	    int typeSize = arrayType.getSize();
	    return new Value(arrayType,
			     new ByteBufferLocation(buffer, addrIndirect,
						    typeSize));
	}
	case DwTag.UNION_TYPE_:
	case DwTag.STRUCTURE_TYPE_: {
	    TypeEntry debugInfoType = new TypeEntry(isa);
//	    ClassType classType = debugInfoType.getClassType(frame, type, null);
	    GccStructOrClassType classType = debugInfoType.getGccStructOrClassType(type, null);

	    if (classType == null)
		return null;
	    return new Value(classType,
			     new ByteBufferLocation(buffer, addrIndirect,
						    classType.getSize()));
	}
	}
	return new Value(new UnknownType(variable.getVariableDie().getName()));
    }
    
    public ByteOrder getOrder()
    {
	return isa.order();
    }
}
