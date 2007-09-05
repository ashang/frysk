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
import inua.eio.ByteOrder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.naming.NameNotFoundException;

import lib.dwfl.BaseTypes;
import lib.dwfl.DwTag;
import lib.dwfl.DwAt;
import lib.dwfl.DwException;
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
import frysk.value.CharType;
import frysk.value.ArithmeticType;
import frysk.value.SignedType;
import frysk.value.UnknownType;
import frysk.value.UnsignedType;
import frysk.value.FloatingPointType;
import frysk.value.ArrayType;
import frysk.value.ClassType;
import frysk.value.EnumType;
import frysk.value.FunctionType;
import frysk.value.PointerType;
import frysk.value.StandardTypes;
import frysk.value.VoidType;
import frysk.value.Type;
import frysk.value.Value;
import frysk.value.ByteBufferLocation;

class DebugInfoEvaluator
    implements CppSymTab
{
    private Task task;

    private DebugInfoFrame currentFrame;
  
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
  
    private int getByteSize(DwarfDie die) {
	return die.getAttrConstant(DwAt.BYTE_SIZE_);
    }

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

	currentFrame = frame;
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

    interface VariableAccessor {
	DwarfDie varDie = null;

	long getAddr (DwarfDie die) throws NameNotFoundException;

	long getLong (DwarfDie varDieP, long offset) throws NameNotFoundException;

	void putLong (DwarfDie varDieP, long offset, Value v) throws NameNotFoundException;

	int getInt (DwarfDie varDieP, long offset) throws NameNotFoundException;

	void putInt (DwarfDie varDieP, long offset, Value v) throws NameNotFoundException;

	short getShort (DwarfDie varDieP, long offset) throws NameNotFoundException;

	void putShort (DwarfDie varDieP, long offset, Value v) throws NameNotFoundException;

	byte getByte (DwarfDie varDieP, long offset) throws NameNotFoundException;

	void putByte (DwarfDie varDieP, long offset, Value v) throws NameNotFoundException;

	float getFloat (DwarfDie varDieP, long offset) throws NameNotFoundException;

	void putFloat (DwarfDie varDieP, long offset, Value v) throws NameNotFoundException;

	double getDouble (DwarfDie varDieP, long offset) throws NameNotFoundException;

	void putDouble (DwarfDie varDieP, long offset, Value v) throws NameNotFoundException;
    }

    /**
     * Access by DW_FORM_block. Typically this is a static address or ptr+disp.
     */
    class AccessMemory
	implements VariableAccessor
    {
      
	/**
	 * @param varDieP The die for a symbol
	 * @return The address corresponding to the symbol
	 */
	protected long getBufferAddr (DwarfDie varDieP) throws NameNotFoundException {
	    long pc = currentFrame.getAdjustedAddress();
       
	    List ops = varDieP.getFormData(pc);
      
	    LocationExpression locExp = new LocationExpression(currentFrame, varDieP, ops);
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

	public void putLong (DwarfDie varDieP, long offset, Value v) throws NameNotFoundException {
	    long addr = getBufferAddr(varDieP);
	    buffer.putLong(addr + offset, v.asLong());
	}

	public int getInt (DwarfDie varDieP, long offset) throws NameNotFoundException {
	    long addr = getBufferAddr(varDieP);
	    return buffer.getInt(addr + offset);
	}

	public void putInt (DwarfDie varDieP, long offset, Value v) throws NameNotFoundException {
	    long addr = getBufferAddr(varDieP);
	    buffer.putInt(addr + offset, (int)v.asLong());
	}

	public short getShort (DwarfDie varDieP, long offset) throws NameNotFoundException {
	    long addr = getBufferAddr(varDieP);
	    return buffer.getShort(addr + offset);
	}

	public void putShort (DwarfDie varDieP, long offset, Value v) throws NameNotFoundException {
	    long addr = getBufferAddr(varDieP);
	    buffer.putShort(addr + offset, (short)v.asLong());
	}

	public byte getByte (DwarfDie varDieP, long offset) throws NameNotFoundException {
	    long addr = getBufferAddr(varDieP);
	    return buffer.getByte(addr + offset);
	}

	public void putByte (DwarfDie varDieP, long offset, Value v) throws NameNotFoundException {
	    long addr = getBufferAddr(varDieP);
	    buffer.putByte(addr + offset, (byte)v.asLong());
	}

	public float getFloat (DwarfDie varDieP, long offset) throws NameNotFoundException {
	    long addr = getBufferAddr(varDieP);
	    return buffer.getFloat(addr + offset);
	}

	public void putFloat (DwarfDie varDieP, long offset, Value v) throws NameNotFoundException {
	    long addr = getBufferAddr(varDieP);
	    buffer.putFloat(addr + offset, (float)v.doubleValue());
	}

	public double getDouble (DwarfDie varDieP, long offset) throws NameNotFoundException {
	    long addr = getBufferAddr(varDieP);
	    return buffer.getDouble(addr + offset);
	}

	public void putDouble (DwarfDie varDieP, long offset, Value v) throws NameNotFoundException {
	    long addr = getBufferAddr(varDieP);
	    buffer.putDouble(addr + offset, v.doubleValue());
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
	 * @return Dwarf register number corresponding to the value of symbol
	 * @throws NameNotFoundException
	 */    
	private int getReg(DwarfDie varDieP) throws NameNotFoundException {
	    long pc = currentFrame.getAdjustedAddress();
	
	    List ops = varDieP.getFormData(pc);
	    Isa isa;
	
	    LocationExpression locExp = new LocationExpression(currentFrame, varDieP, ops);
	    Register register = locExp.getRegisterNumber();
	    if (register == null)
		throw new NameNotFoundException();

	    if (currentFrame.getInnerDebugInfoFrame() == null)
		isa = task.getIsa();
	    else
		isa = currentFrame.getTask().getIsa();

	    int reg = DwarfRegisterMapFactory.getRegisterMap(isa)
                .getRegisterNumber(register);
	    return reg;
	}
    
	/**
	 * @param varDieP Die for a symbol
	 * @return The contents of the register corresponding to the value of symbol
	 * @throws NameNotFoundException
	 */    
	private long getRegister(DwarfDie varDieP) throws NameNotFoundException {
	    long pc = currentFrame.getAdjustedAddress();
       
	    List ops = varDieP.getFormData(pc);
      
	    LocationExpression locExp = new LocationExpression(currentFrame, varDieP, ops);
	    long value  = locExp.decode();
	    if (locExp.getLocationType() != LocationExpression.locationTypeReg)
		throw new NameNotFoundException();
	    return value;
	}

	public long getLong (DwarfDie varDieP, long offset) throws NameNotFoundException
	{
	    return getRegister(varDieP);
	}

	public void putLong (DwarfDie varDieP, long offset, Value v) throws NameNotFoundException {
	    long reg = getReg(varDieP);
	    currentFrame.setReg(reg, v.asLong());
	}

	public int getInt (DwarfDie varDieP, long offset) throws NameNotFoundException {
	    return (int) getRegister(varDieP);
	}

	public void putInt (DwarfDie varDieP, long offset, Value v) throws NameNotFoundException {
	    long reg = getReg(varDieP);
	    currentFrame.setReg(reg, (int)v.asLong());
	}

	public short getShort (DwarfDie varDieP, long offset) throws NameNotFoundException {
	    return (short) getRegister(varDieP);
	}

	public void putShort (DwarfDie varDieP, long offset, Value v) throws NameNotFoundException {
	    long reg = getReg(varDieP);
	    currentFrame.setReg(reg, (short)v.asLong());
	}

	public byte getByte (DwarfDie varDieP, long offset) throws NameNotFoundException {
	    return (byte) getRegister(varDieP);
	}

	public void putByte (DwarfDie varDieP, long offset, Value v) throws NameNotFoundException {
	    long reg = getReg(varDieP);
	    currentFrame.setReg(reg, (byte)v.asLong());
	}

	public float getFloat (DwarfDie varDieP, long offset) throws NameNotFoundException {
	    long val = getRegister(varDieP);
	    float fval = Float.intBitsToFloat((int)val);
	    return fval;
	}

	public void putFloat (DwarfDie varDieP, long offset, Value v) throws NameNotFoundException {
	    long reg = getReg(varDieP);
	    currentFrame.setReg(reg, (long) v.doubleValue());
	}
	public double getDouble (DwarfDie varDieP, long offset) throws NameNotFoundException {
	    long val = getRegister(varDieP);
	    double dval = Double.longBitsToDouble(val);
	    return dval;
	}

	public void putDouble (DwarfDie varDieP, long offset, Value v) throws NameNotFoundException {
	    long reg = getReg(varDieP);
	    currentFrame.setReg(reg, (long) v.doubleValue());
	}
    }

    /**
     * @param s Symbol s
     * @return The die for symbol s
     */
    private DwarfDie getDie (String s) throws NameNotFoundException {
	Dwfl dwfl;
	DwarfDie[] allDies;
	DwarfDie varDie;
	long pc = this.currentFrame.getAdjustedAddress();

	dwfl = DwflCache.getDwfl(task);
	DwflDieBias bias = dwfl.getDie(pc);
	if (bias == null)
	    return null;
	DwarfDie die = bias.die;

	Subprogram b = currentFrame.getSubprogram();
	LinkedList vars = b.getVariables();
     
	Iterator iterator = vars.iterator();
	while (iterator.hasNext()) {
	    Variable variable = (Variable) iterator.next();
	    if (variable.getName() != null && variable.getName().compareTo(s) == 0)
		{
		    allDies = die.getScopes(pc - bias.bias);
		    variable.getVariableDie().setScopes(allDies);
		    return variable.getVariableDie();
		}
	}

	allDies = die.getScopes(pc - bias.bias);
	varDie = die.getScopeVar(allDies, s);
	// Do we have something above didn't find, e.g. DW_TAG_enumerator?
	if (varDie == null)
	    varDie = DwarfDie.getDeclCU(allDies, s);
	if (varDie == null)
	    return null;
	return varDie;
    }

    /**
     * @param type An array die
     * @param subrange Die for the array's first index
     * @return ArrayType for the array
     */
    private ArrayType getArrayType (DwarfDie type, DwarfDie subrange) {
	int elementCount = 1;
	// System.out.println("die=" + Long.toHexString(type.getOffset()) + " tag=" + Long.toHexString(type.getTag()) + " "+ type.getName());
	ArrayList dims = new ArrayList();
	while (subrange != null) {
	    int arrDim = subrange.getAttrConstant(DwAt.UPPER_BOUND_);
	    dims.add(new Integer(arrDim));
	    subrange = subrange.getSibling();
	    elementCount *= arrDim + 1;
	}

	ArrayType arrayType = null;
	int typeSize = BaseTypes.getTypeSize(type.getUltimateType().getBaseType());
	switch (type.getUltimateType().getBaseType()) {
	case BaseTypes.baseTypeByte:
	case BaseTypes.baseTypeUnsignedByte:
	    arrayType = new ArrayType(byteType, elementCount * typeSize, dims);
	    break;
	case BaseTypes.baseTypeShort:
	case BaseTypes.baseTypeUnsignedShort:
	    arrayType = new ArrayType(shortType, elementCount * typeSize, dims);
	    break;
	case BaseTypes.baseTypeInteger:
	case BaseTypes.baseTypeUnsignedInteger:
	    arrayType = new ArrayType(intType, elementCount * typeSize, dims);
	    break;
	case BaseTypes.baseTypeLong:
	case BaseTypes.baseTypeUnsignedLong:
	    arrayType = new ArrayType(longType, elementCount * typeSize, dims);
	    break;
	case BaseTypes.baseTypeFloat:
	    arrayType = new ArrayType(floatType, elementCount * typeSize, dims);
	    break;
	case BaseTypes.baseTypeDouble:
	    arrayType = new ArrayType(doubleType, elementCount * typeSize, dims);
	    break;
	}

	type = type.getUltimateType();
	if (type.getTag() == DwTag.STRUCTURE_TYPE_) {
	    ClassType classType = getClassType(type, null);
	    typeSize = classType.getSize();
	    arrayType = new ArrayType(classType, elementCount * typeSize, dims);
	}
	return arrayType;
    }
  
    /**
     * @return Generate a type for a typedef otherwise a basetype.
     */
    private Type fetchType (DwarfDie type, String name) {
	int size = getByteSize(type);
	int baseType = type.getBaseType();
	// XXX: Order might come from TYPE; XXX: sign vs unsigned vs
	// float can come directly from TYPE.
	ByteOrder order = intType.order();
	switch (baseType) {
	case BaseTypes.baseTypeByte:
	    // XXX: Bogus; should be switching off the DW_AT_encoding.
	    return new CharType(name, order, size, true);
	case BaseTypes.baseTypeUnsignedByte:
	    // XXX: Bogus; should be switching off the DW_AT_encoding.
	    return new CharType(name, order, size, false);
	case BaseTypes.baseTypeInteger:
	case BaseTypes.baseTypeShort:
	case BaseTypes.baseTypeLong:
	    return new SignedType(name, order, size);
	case BaseTypes.baseTypeUnsignedLong:
	case BaseTypes.baseTypeUnsignedInteger:
	case BaseTypes.baseTypeUnsignedShort:
	    return new UnsignedType(name, order, size);
	case BaseTypes.baseTypeFloat:
	case BaseTypes.baseTypeDouble:
	    return new FloatingPointType(name, order, size);
	default:
	    return new UnknownType(name);
	}
    }

    /**
     * @param classDie A struct die
     * @param name Name of the struct
     * @return ClassType for the struct
     */
    private ClassType getClassType (DwarfDie classDie, String name) {
	int typeSize = 0;
	// System.out.println("die=" + Long.toHexString(classDie.getOffset()) + " tag=" + Long.toHexString(classDie.getTag()) + " " + classDie.getName());
	ClassType classType = new ClassType(name,
					    task.getIsa().getByteOrder());
	for (DwarfDie member = classDie.getChild();
	     member != null;
	     member = member.getSibling()) {
	    // System.out.println("member=" + Long.toHexString(member.getOffset()) + " tag=" + Long.toHexString(member.getTag()) + " " + member.getName());
	    long offset;
	    boolean haveTypeDef;
	    try {
		offset = member.getDataMemberLocation();
	    } catch (DwException de) {
		offset = 0;                           // union
	    }
        
	    int access = member.getAttrConstant(DwAt.ACCESSIBILITY_);
	    DwarfDie dieType = member.getType();
	    DwarfDie memberType = member.getUltimateType();
	    Type type;
        
	    if (dieType != memberType)
		haveTypeDef = true;
	    else
		haveTypeDef = false;
        
	    if (member.getTag() == DwTag.SUBPROGRAM_) {
		Value v = getSubprogramValue(member);
		classType.addMember(member.getName(), v.getType(), offset, access);
		continue;
	    }

	    if (memberType == null)
		continue;

	    typeSize = (int)offset + BaseTypes.getTypeSize(memberType.getBaseType());
	    if (memberType.getBaseType() > 0) {
		type = fetchType(memberType, dieType.getName());
		// System V ABI Supplements discuss bit field layout
		int bitSize = member.getAttrConstant(DwAt.BIT_SIZE_);
		int bitOffset = 0;
		if (bitSize != -1) {
		    bitOffset = member.getAttrConstant(DwAt.BIT_OFFSET_);
		}
		classType.addMember(member.getName(), type, offset, access,
				    bitOffset, bitSize);
		continue;
	    }

	    // memberType is the ultimate type derived from chasing the thread of types
	    switch (memberType.getTag()) {
	    case DwTag.STRUCTURE_TYPE_: {
		ClassType memberClassType = getClassType(memberType, memberType.getName());
		if (member.getTag() != DwTag.INHERITANCE_)
		    memberClassType.setTypedefFIXME(haveTypeDef);
		else
		    memberClassType.setInheritance(true);
		typeSize += memberClassType.getSize();
		typeSize += 4 - (typeSize % 4);             // round up to mod 4
		classType.addMember(memberType.getName(), memberClassType,
				    offset, access);
		continue;
	    }
        
	    case DwTag.ARRAY_TYPE_: {
		ArrayType memberArrayType = getArrayType(memberType, memberType.getChild());
		typeSize += memberArrayType.getSize();
		classType.addMember(member.getName(), memberArrayType, offset,
				    access);
		continue;
	    }
        
	    case DwTag.POINTER_TYPE_: {
		ByteOrder byteorder = task.getIsa().getByteOrder();
		Type memberPtrType;
            
		memberPtrType = new PointerType("*", byteorder, getByteSize(memberType),
						getPointerTarget(memberType));
		classType.addMember(member.getName(), memberPtrType, offset,
				    access);
		typeSize += memberPtrType.getSize();
		continue;
	    }
	    }
	    classType.addMember(member.getName(),
				new UnknownType(member.getName()),
				offset, access);
	}

	typeSize += 4 - (typeSize % 4);             // round up to mod 4
	classType.setSize(typeSize);
	return classType;
    }
  
  
    /**
     * @return Value for symbol s in frame f
     */
    public Value get (DebugInfoFrame f, String s) throws NameNotFoundException {
	setCurrentFrame(f);
	if (s.charAt(0) == '$') {
	    // FIXME: This code doesn't need to access the dwarf register
	    // map; instead just do a direct register lookup.
	    RegisterMap regMap = DwarfRegisterMapFactory.getRegisterMap(f.getTask().getIsa());
	    Register reg = regMap.getRegister(s.substring(1).trim());
	    if (reg == null)
		return null;
	    return f.getRegisterValue(reg);
	}

	DwarfDie varDie = getDie(s);
	if (varDie == null)
	    throw new NameNotFoundException();
    
	return get(f, varDie);
    }
  
    /**
     * @return Value associated with the given DwarfDie.
     * @see frysk.expr.CppSymTab#get(java.lang.String)
     */
    public Value get (DebugInfoFrame f, DwarfDie varDie) {
	setCurrentFrame(f);
	VariableAccessor[] variableAccessor = { new AccessMemory(),
						new AccessRegisters()};
	ByteOrder byteorder = task.getIsa().getByteOrder();

	if (varDie == null)
	    return (null);

	for (int i = 0; i < variableAccessor.length; i++) {
	    try {
		DwarfDie type = varDie.getUltimateType();
		// if there is no type then setup a sentinel
		int baseType = type != null ? type.getBaseType() : 0;
		switch (baseType) {
		case BaseTypes.baseTypeLong:
		case BaseTypes.baseTypeUnsignedLong: {
		    long longVal = variableAccessor[i].getLong(varDie, 0);
		    return longType.createValue(longVal);
		}
		case BaseTypes.baseTypeInteger:
		case BaseTypes.baseTypeUnsignedInteger: {
		    int intVal = variableAccessor[i].getInt(varDie, 0);
		    return intType.createValue(intVal);
		}
		case BaseTypes.baseTypeShort:
		case BaseTypes.baseTypeUnsignedShort: {
		    short shortVal = variableAccessor[i].getShort(varDie, 0);
		    return shortType.createValue(shortVal);
		}
		case BaseTypes.baseTypeByte:
		case BaseTypes.baseTypeUnsignedByte: {
		    byte byteVal = variableAccessor[i].getByte(varDie, 0);
		    return byteType.createValue(byteVal);
		}
		case BaseTypes.baseTypeFloat: {
		    float floatVal = variableAccessor[i].getFloat(varDie, 0);
		    return floatType.createValue(floatVal);
		}
		case BaseTypes.baseTypeDouble: {
		    double doubleVal = variableAccessor[i].getDouble(varDie, 0);
		    return doubleType.createValue(doubleVal);
		}
		}
		// if there is no type then use this die's tag
		int tag = type != null ? type.getTag() : varDie.getTag();
		switch (tag) {
		case DwTag.ARRAY_TYPE_: {
		    DwarfDie subrange;
		    long addr = variableAccessor[0].getAddr(varDie);
		    if (addr == 0)
			continue;
		    subrange = type.getChild();
		    ArrayType arrayType = getArrayType(type, subrange);

		    if (arrayType == null)
			return null;
		    int typeSize = arrayType.getSize();
		    return new Value(arrayType,
				     new ByteBufferLocation(buffer, addr,
							    typeSize));
		}
		case DwTag.UNION_TYPE_:
		case DwTag.STRUCTURE_TYPE_: {
		    long addr = variableAccessor[0].getAddr(varDie);
		    if (addr == 0)
			continue;
		    ClassType classType = getClassType(type, null);
		    return new Value(classType,
				     new ByteBufferLocation(buffer, addr,
							    classType.getSize()));
		}
		case DwTag.POINTER_TYPE_: {
		    PointerType ptrType = new PointerType("*", byteorder, longType.getSize(),
							  getPointerTarget (type));
		    long  addr = variableAccessor[i].getLong(varDie, 0);
		    return ptrType.createValue(addr);
		}
		case DwTag.ENUMERATION_TYPE_: {
		    DwarfDie subrange;
		    long val = variableAccessor[0].getLong(varDie, 0);
		    subrange = type.getChild();
		    EnumType enumType = new EnumType(byteorder, getByteSize(type));
		    while (subrange != null) {
			enumType.addMember(subrange.getName(), 
					   subrange.getAttrConstant(DwAt.CONST_VALUE_));
			subrange = subrange.getSibling();
		    }
		    return enumType.createValue(val);
		}
		    // special case members of an enumeration
		case DwTag.ENUMERATOR_: {
		    /**
		     * FIXME: This should return an "enum", not an
		     * integer.
		     */
		    return longType.createValue(varDie.getAttrConstant(DwAt.CONST_VALUE_));
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
    public Value get (DebugInfoFrame f, ArrayList components) throws NameNotFoundException {
	setCurrentFrame(f);
	String s = (String)components.get(0);
	DwarfDie varDie = getDie(s);
	if (varDie == null)
	    return (null);

	Value v = get(f, s);
	if (v.getType() instanceof ArrayType)
	    return ((ArrayType)v.getType()).get(v, 1, components);
	else if (v.getType() instanceof ClassType)
	    return ((ClassType)v.getType()).get(v, 0, components);
	else
	    return new Value(new UnknownType(varDie.getName()));
    }
  
    /**
     * @param f Frame containing symbol s
     * @param s Symbol s
     * @return Value corresponding to the address of symbol s 
     */
    public Value getAddress (DebugInfoFrame f, String s) throws NameNotFoundException {
	setCurrentFrame(f);
	AccessMemory access = new AccessMemory();
	return longType.createValue(access.getAddr(getDie(s))); 
    }
  
    /**
     * @param f Frame containing symbol s
     * @param s Symbol s
     * @return Value corresponding to the memory location pointed to by symbol s.
     */
    public Value getMemory (DebugInfoFrame f, String s) throws NameNotFoundException {     
	setCurrentFrame(f);
	DwarfDie varDie = getDie(s);
    
	if (varDie == null)
	    return new Value(new UnknownType(varDie.getName()));
    
	DwarfDie type = varDie.getUltimateType();
	AccessMemory access = new AccessMemory();
	long addr = access.getAddr(getDie(s)); 
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
	int tag = type != null ? type.getTag() : varDie.getTag();
	switch (tag) {
	case DwTag.ARRAY_TYPE_: {
	    DwarfDie subrange;
	    subrange = type.getChild();
	    ArrayType arrayType = getArrayType(type, subrange);

	    if (arrayType == null)
		return null;
	    int typeSize = arrayType.getSize();
	    return new Value(arrayType,
			     new ByteBufferLocation(buffer, addrIndirect,
						    typeSize));
	}
	case DwTag.UNION_TYPE_:
	case DwTag.STRUCTURE_TYPE_: {
	    ClassType classType = getClassType(type, null);

	    if (classType == null)
		return null;
	    return new Value(classType,
			     new ByteBufferLocation(buffer, addrIndirect,
						    classType.getSize()));
	}
	}
	return new Value(new UnknownType(varDie.getName()));
    }
  
    /**
     * Return the target of a pointer, as a type.
     *
     * XXX: Why isn't this recursive?
     */
    private Type getPointerTarget(DwarfDie pointer) {
	DwarfDie type = pointer.getUltimateType();
	if (type == null)
	    return new VoidType();

	ByteOrder byteorder = task.getIsa().getByteOrder();

	switch (type.getBaseType())
	    {
	    case BaseTypes.baseTypeByte:
	    case BaseTypes.baseTypeUnsignedByte:
		return byteType;
	    case BaseTypes.baseTypeShort:
	    case BaseTypes.baseTypeUnsignedShort:
		return shortType;
	    case BaseTypes.baseTypeInteger:
	    case BaseTypes.baseTypeUnsignedInteger:
		return intType;
	    case BaseTypes.baseTypeLong:
	    case BaseTypes.baseTypeUnsignedLong:
		return longType;
	    case BaseTypes.baseTypeFloat:
		return floatType;
	    case BaseTypes.baseTypeDouble:
		return doubleType;
	    }
	switch (type.getTag())
	    {
	    case DwTag.POINTER_TYPE_: {
		return new PointerType("void*", byteorder, getByteSize(type),
				       getPointerTarget(type));
	    }
	    }
	return new UnknownType(type.getName());

    }
  
    /**
     * @param varDie The die for a symbol corresponding to a function
     * @return The value of a subprogram die
     */    
    public Value getSubprogramValue (DwarfDie varDie)
    {
	ByteOrder byteorder = task.getIsa().getByteOrder();
    
	if (varDie == null)
	    return (null);

	switch (varDie.getTag()) {
	case DwTag.SUBPROGRAM_: {
	    Type type = null;
	    if (varDie.getUltimateType() != null) {
		type = getType(varDie);
	    }
	    FunctionType functionType = new FunctionType(varDie.getName(), byteorder, type);
	    DwarfDie parm = varDie.getChild();
	    while (parm != null && parm.getTag() == DwTag.FORMAL_PARAMETER_) {
		if (parm.getAttrBoolean((DwAt.ARTIFICIAL_)) == false) {
		    type = getType(parm);
		    functionType.addParameter(type, parm.getName());
		}
		parm = parm.getSibling();
	    }
	    return new Value (functionType);
	}
	}
	return new Value(new UnknownType(varDie.getName()));
    }
  
    /**
     * @param varDie This symbol's die
     * @return a frysk.type for this varDie
     */
    public Type getType (DwarfDie varDie)
    {
	ByteOrder byteorder = task.getIsa().getByteOrder();
    
	if (varDie == null)
	    return (null);

	DwarfDie type = varDie.getUltimateType();
	if (type == null)
	    type = varDie;
    
	switch (type.getTag()) {
	case DwTag.POINTER_TYPE_: {
	    return new PointerType("*", byteorder, getByteSize(type),
				   getPointerTarget(type));
	}
	case DwTag.ARRAY_TYPE_: {
	    DwarfDie subrange = type.getChild();
	    return getArrayType(type, subrange);
	}
	case DwTag.UNION_TYPE_:
	case DwTag.STRUCTURE_TYPE_: {
	    boolean noTypeDef = (varDie.getType() == null);
	    String name = noTypeDef ? varDie.getName() 
		: varDie.getType().getName();
	    ClassType classType = getClassType(type, name);
	    if (type != varDie.getType() && noTypeDef == false)
		classType.setTypedefFIXME(true);
	    return classType;
	}
	case DwTag.ENUMERATION_TYPE_: {
	    DwarfDie subrange = type.getChild();
	    EnumType enumType = new EnumType(byteorder, getByteSize(type));
	    while (subrange != null) {
		enumType.addMember(subrange.getName(), 
				   subrange.getAttrConstant(DwAt.CONST_VALUE_));
		subrange = subrange.getSibling();
	    }
	    return enumType;
	}
	}
    
	type = varDie.getUltimateType();
	if (type == null)
	    return new UnknownType(varDie.getName());
	DwarfDie dieType = varDie.getType();
	return fetchType(type, dieType.getName());
    }

  
    /**
     * Get the current stack frame.
     * 
     * @return StackFrame
     */
    DebugInfoFrame getCurrentFrame () {
	return currentFrame;
    }

    /**
     * Set the current stack frame.
     * 
     * @param sf_p
     */
    void setCurrentFrame (DebugInfoFrame f) {
	currentFrame = f;
    }

    /**
     * Get the most recent stack frame.
     * 
     * @return StackFrame
     */
    DebugInfoFrame getInnerMostFrame () {
	DebugInfoFrame curr = currentFrame;
	while (curr.getInnerDebugInfoFrame() != null)
	    curr = curr.getInnerDebugInfoFrame();
	return curr;
    }
}
