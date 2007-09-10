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

import javax.naming.NameNotFoundException;
import lib.dwfl.BaseTypes;
import lib.dwfl.DwTag;
import lib.dwfl.DwAt;
import lib.dwfl.DwarfDie;
import lib.dwfl.Dwfl;
import lib.dwfl.DwflDieBias;
import frysk.dwfl.DwflCache;
import frysk.expr.CppSymTab;
import frysk.proc.Isa;
import frysk.proc.Task;
import frysk.stack.Register;
import frysk.stack.RegisterMap;
import frysk.sys.Errno;
import frysk.value.ArithmeticType;
import frysk.value.CharType;
import frysk.value.ClassType;
import frysk.value.IntegerType;
import frysk.value.TypeDef;
import frysk.value.UnknownType;
import frysk.value.FloatingPointType;
import frysk.value.ArrayType;
import frysk.value.ConfoundedType;
import frysk.value.EnumType;
import frysk.value.PointerType;
import frysk.value.StandardTypes;
import frysk.value.Type;
import frysk.value.Value;
import frysk.value.ByteBufferLocation;

class DebugInfoEvaluator
    implements CppSymTab
{
    private Task task;

    private final DebugInfoFrame frame;
  
    private ByteBuffer buffer;

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
	this.task = frame.getTask();
	buffer = this.task.getMemory();

	this.frame = frame;
	Isa isa = this.task.getIsa();
	byteType = StandardTypes.getByteType(isa);
	// byteUnsignedType = new ArithmeticType(1, byteorder,
	// BaseTypes.baseTypeUnsignedByte, "unsigned byte");
	shortType = StandardTypes.getShortType(isa);
	// shortUnsignedType = new ArithmeticType(2, byteorder,
	// BaseTypes.baseTypeUnsignedShort, "unsigned short");
	intType = StandardTypes.getIntType(isa);
	// intUnsignedType = new ArithmeticType(4, byteorder,
	// BaseTypes.baseTypeUnsignedInteger, "unsigned int");
	longType = StandardTypes.getLongType(isa);
	// longUnsignedType = new ArithmeticType(8, byteorder,
	// BaseTypes.baseTypeUnsignedLong, "unsigned long");
	floatType = StandardTypes.getFloatType(isa);
	doubleType = StandardTypes.getDoubleType(isa);
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
     private Variable getDie (String s) throws NameNotFoundException {
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
    public Value get (String s) throws NameNotFoundException {
	if (s.charAt(0) == '$') {
	    // FIXME: This code doesn't need to access the dwarf register
	    // map; instead just do a direct register lookup.
	    RegisterMap regMap = DwarfRegisterMapFactory.getRegisterMap(frame.getTask().getIsa());
	    Register reg = regMap.getRegister(s.substring(1).trim());
	    if (reg == null)
		return null;
	    return frame.getRegisterValue(reg);
	}

	Variable var = getDie(s);
	DwarfDie varDie = var.getVariableDie();
	if (varDie == null)
	    throw new NameNotFoundException();
    
	return get(var);
    }
  
    /**
     * @return Value associated with the given DwarfDie.
     * @see frysk.expr.CppSymTab#get(java.lang.String)
     */
    public Value get (Variable var) {
	VariableAccessor[] variableAccessor = { new AccessMemory(),
						new AccessRegisters()};
	if (var == null)
	    return (null);

        DwarfDie varDie = var.getVariableDie();
        
        Isa isa = this.task.getIsa();

	for (int i = 0; i < variableAccessor.length; i++) {
	    try {
		Type varType = var.getType(frame);
	    	if (varType instanceof TypeDef) {
	    	    varType = varType.getUltimateType();
	    	}
		if (varType instanceof ArrayType) {
		    int typeSize = varType.getSize();
		    long addr = variableAccessor[0].getAddr(varDie);
                    if (addr == 0)
                        continue;
		    return new Value(varType, 
			    new ByteBufferLocation(buffer, addr, typeSize));
		}
		else if (varType instanceof ConfoundedType
			|| varType instanceof ClassType) {
		    long addr = variableAccessor[0].getAddr(varDie);
		    if (addr == 0)
			continue;
		    return new Value(varType, 
			    new ByteBufferLocation(buffer, addr, varType.getSize()));
		}
		else if (varType instanceof PointerType) {
		    long addr = variableAccessor[i].getLong(varDie, 0);
		    return ((PointerType)varType).createValue(addr);
		}
		else if (varType instanceof EnumType) {
		    long val = 0;
		    switch (varType.getSize()) {
		    case (2): val = variableAccessor[0].getShort(varDie, 0);
		    case (4): val = variableAccessor[0].getInt(varDie, 0);
		    case (8): val = variableAccessor[0].getLong(varDie, 0);
		    }
		    return ((EnumType)varType).createValue(val);
		}
		// special case members of an enumeration ??? improve this
		else if (var.getVariableDie().getTag() == DwTag.ENUMERATOR_) {
		    return longType.createValue(varDie
			    .getAttrConstant(DwAt.CONST_VALUE_));
		}
		else if (varType instanceof IntegerType) {
		    if (varType.getSize() == StandardTypes.getLongType(isa).getSize()) {
			long longVal = variableAccessor[i].getLong(varDie, 0);
			return ((ArithmeticType)varType).createValue(longVal);
		    }
		    else if (varType.getSize() == StandardTypes.getIntType(isa).getSize()) {
			int intVal = variableAccessor[i].getInt(varDie, 0);
			return ((ArithmeticType)varType).createValue(intVal);
		    }
		    else if (varType.getSize() == StandardTypes.getShortType(isa).getSize()) {
			short shortVal = variableAccessor[i].getShort(varDie, 0);
			return ((ArithmeticType)varType).createValue(shortVal);
		    }
		    else if (varType.getSize() == StandardTypes.getByteType(isa).getSize()) {
			byte byteVal = variableAccessor[i].getByte(varDie, 0);
			return ((ArithmeticType)varType).createValue(byteVal);
		    }
		}
		else if (varType instanceof CharType) {
		    	char charVal = (char)variableAccessor[i].getShort(varDie, 0);
			return ((CharType)varType).createValue(charVal);
		}
		else if (varType instanceof FloatingPointType) {
		    if (varType.getSize() == StandardTypes.getFloatType(isa).getSize()) {
			float floatVal = variableAccessor[i].getFloat(varDie, 0);
			return ((ArithmeticType)varType).createValue(floatVal);
		    }
		    else if (varType.getSize() == StandardTypes.getDoubleType(isa).getSize()) {
			double doubleVal = variableAccessor[i].getDouble(varDie, 0);
			return ((ArithmeticType)varType).createValue(doubleVal);
		    }
		}
	    } catch (NameNotFoundException ignore) {
	    } catch (Errno ignore) {
	    }
	}
	return new Value(new UnknownType(varDie.getName()));
    }

    /**
     * Returns the value of symbol which is defined by components.
     * @param f The frame containing the symbol
     * @param components Token list of members and indices, e.g. given a.b.c[1][2]
     * {a,b,c,1,1,2,2}
     * @return Value of the symbol
     */
    public Value get (ArrayList components) throws NameNotFoundException {
	String s = (String)components.get(0);
	Variable variable = getDie(s);
	if (variable == null)
	    return (null);

	Value v = get(s);
	if (v.getType() instanceof ArrayType)
	    return ((ArrayType)v.getType()).get(v, 1, components);
	else if (v.getType() instanceof ConfoundedType)
	    return ((ConfoundedType)v.getType()).get(v, 0, components);
	else
	    return new Value(new UnknownType(variable.getName()));
    }
  
    /**
     * @param f Frame containing symbol s
     * @param s Symbol s
     * @return Value corresponding to the address of symbol s 
     */
    public Value getAddress (String s) throws NameNotFoundException {
	AccessMemory access = new AccessMemory();
	return longType.createValue(access.getAddr(getDie(s).getVariableDie())); 
    }
  
    /**
     * @param f Frame containing symbol s
     * @param s Symbol s
     * @return Value corresponding to the memory location pointed to by symbol s.
     */
    public Value getMemory (String s) throws NameNotFoundException {     
	Variable variable= getDie(s);
	if (variable == null)
	    return new Value(new UnknownType(variable.getName()));
    
	DwarfDie type = variable.getVariableDie().getUltimateType();
	AccessMemory access = new AccessMemory();
	long addr = access.getAddr(getDie(s).getVariableDie()); 
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
	int tag = type != null ? type.getTag() : variable.getVariableDie().getTag();
	switch (tag) {
	case DwTag.ARRAY_TYPE_: {
	    DwarfDie subrange;
	    subrange = type.getChild();
	    TypeEntry debugInfoType = new TypeEntry();
	    ArrayType arrayType = debugInfoType.getArrayType(frame, type, subrange);

	    if (arrayType == null)
		return null;
	    int typeSize = arrayType.getSize();
	    return new Value(arrayType,
			     new ByteBufferLocation(buffer, addrIndirect,
						    typeSize));
	}
	case DwTag.UNION_TYPE_:
	case DwTag.STRUCTURE_TYPE_: {
	    TypeEntry debugInfoType = new TypeEntry();
//	    ClassType classType = debugInfoType.getClassType(frame, type, null);
	    ConfoundedType classType = debugInfoType.getConfoundedType(frame, type, null);

	    if (classType == null)
		return null;
	    return new Value(classType,
			     new ByteBufferLocation(buffer, addrIndirect,
						    classType.getSize()));
	}
	}
	return new Value(new UnknownType(variable.getVariableDie().getName()));
    }
}
