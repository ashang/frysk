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

import java.util.ArrayList;
import java.util.HashMap;

import lib.dwfl.BaseTypes;
import lib.dwfl.DwException;
import lib.dwfl.DwarfDie;
import lib.dwfl.DwTag;
import lib.dwfl.DwAt;
import frysk.proc.Isa;
import frysk.value.ArrayType;
import frysk.value.ClassType;
import frysk.value.EnumType;
import frysk.value.FunctionType;
import frysk.value.PointerType;
import frysk.value.SignedType;
import frysk.value.StandardTypes;
import frysk.value.Type;
import frysk.value.UnknownType;
import frysk.value.UnsignedType;
import frysk.value.Value;
import frysk.value.VoidType;
import inua.eio.ByteOrder;

class TypeEntry
{
    DebugInfoFrame f;
    HashMap dieHash;

    public TypeEntry ()
    {
	f = null;
	dieHash = null;
    }

    private int getByteSize(DwarfDie die) {
	return die.getAttrConstant(DwAt.BYTE_SIZE_);
    }
    
    private void dumpDie(String s, DwarfDie die)
    {
      // ??? convert this to use tracing
//    	System.out.println(s + Long.toHexString(die.getOffset()) + " "
//		+ DwTag.toName(die.getTag())
//		+ " " + die.getName());
    }
    /**
     * @param dieType
     *                An array die
     * @param subrange
     *                Die for the array's first index
     * @return ArrayType for the array
     */
    public ArrayType getArrayType(DebugInfoFrame f, DwarfDie dieType, DwarfDie subrange) {
	int elementCount = 1;
	dumpDie("arrayDie=", dieType);
	dumpDie("subrange=", subrange);
	ArrayList dims = new ArrayList();
	while (subrange != null) {
	    int arrDim = subrange
	    .getAttrConstant(DwAt.UPPER_BOUND_);
	    dims.add(new Integer(arrDim));
	    subrange = subrange.getSibling();
	    elementCount *= arrDim + 1;
	}

	ArrayType arrayType = null;
	Type type = getType (f, dieType);
	int typeSize = type.getSize();
	arrayType = new ArrayType(type, elementCount * typeSize, dims);
	return arrayType;
    }

    /**
     * @param classDie
     *                A struct die
     * @param name
     *                Name of the struct
     * @return ClassType for the struct
     */
    public ClassType getClassType(DebugInfoFrame f, DwarfDie classDie, String name) {
	dumpDie("classDie=", classDie);
	ClassType classType = new ClassType(name, getByteSize(classDie));
	

	for (DwarfDie member = classDie.getChild(); member != null; member = member
	.getSibling()) {
	    dumpDie("member=", member);
	    long offset;
	    try {
		offset = member.getDataMemberLocation();
	    } catch (DwException de) {
		offset = 0; // union
	    }

	    int access = member
	    .getAttrConstant(DwAt.ACCESSIBILITY_);
	    DwarfDie memberDieType = member.getUltimateType();

	    if (member.getTag() == DwTag.SUBPROGRAM_) {
		Value v = getSubprogramValue(f, member);
		classType.addMember(member.getName(), v.getType(), offset,
			access);
		continue;
	    }
	    
	    if (memberDieType == null)
		continue;

	    Type memberType = getType (f, member.getType());
	    if (memberType instanceof UnknownType == false) {
		// System V ABI Supplements discuss bit field layout
		int bitSize = member
		.getAttrConstant(DwAt.BIT_SIZE_);
		if (bitSize != -1) {
		    int bitOffset = member
		    .getAttrConstant(DwAt.BIT_OFFSET_);
		    classType.addMember(member.getName(), memberType, offset, access,
			    bitOffset, bitSize);
		}
		else
		    classType.addMember(member.getName(), memberType, offset, access);
		
		continue;
	    }
	    else
		classType.addMember(member.getName(), new UnknownType(member
			.getName()), offset, access);
	}

	return classType;
    }

    
    /**
     * @param varDie
     *                The die for a symbol corresponding to a function
     * @return The value of a subprogram die
     */
    public Value getSubprogramValue(DebugInfoFrame f, DwarfDie varDie) {
	if (varDie == null)
	    return (null);

	switch (varDie.getTag()) {
	case DwTag.SUBPROGRAM_: {
	    Type type = null;
	    if (varDie.getUltimateType() != null) {
		type = getType(f, varDie);
	    }
	    FunctionType functionType = new FunctionType(varDie.getName(), type);
	    DwarfDie parm = varDie.getChild();
	    while (parm != null
		    && parm.getTag() == DwTag.FORMAL_PARAMETER_) {
		if (parm.getAttrBoolean((DwAt.ARTIFICIAL_)) == false) {
		    type = getType(f, parm);
		    functionType.addParameter(type, parm.getName());
		}
		parm = parm.getSibling();
	    }
	    return new Value(functionType);
	}
	}
	return new Value(new UnknownType(varDie.getName()));
    }


    /**
     * @param varDie
     *                This symbol's die
     * @return a frysk.type for this varDie
     */
    public Type getType(DebugInfoFrame f, DwarfDie typeDie) {
	ByteOrder byteorder = f.getTask().getIsa().getByteOrder();

	if (typeDie == null)
	    return (null);

	dumpDie("getType typeDie=", typeDie);
	DwarfDie type;
	if (typeDie.getTag() == DwTag.FORMAL_PARAMETER_
		|| typeDie.getTag() == DwTag.VARIABLE_) {
	    type = typeDie.getType();
	    dumpDie("getType type=", type);
	}
	else
	    type = typeDie;

	if (this.f != f) {
	    this.f = f;
	    dieHash = new HashMap();
	}
	Type mappedType = (Type)dieHash.get(new Integer(type.getOffset()));
	if (mappedType != null)
	    return mappedType;
	else if (dieHash.containsKey(new Integer(type.getOffset()))) {
	    // ??? will this always be a pointer to ourselves?
	    // VoidType is obviously no correct need a way to reference ourselves
	    return new PointerType("", byteorder, getByteSize(type),  
		    new VoidType());
	}
	dieHash.put(new Integer(type.getOffset()), null);
	Type returnType = null;
	
	Isa isa = f.getTask().getIsa();

	switch (type.getTag()) {
	case DwTag.TYPEDEF_: {
	    // ??? Need to hook this up to TypeDef.java
	    Type tagType = getType(f, type.getType());
	    if (tagType instanceof SignedType)
		returnType = new SignedType(type.getName(), isa.getByteOrder(),
			tagType.getSize());
	    else {
		returnType = tagType;
		returnType.setTypedefFIXME(true);
	    }
	    break;
	}
	case DwTag.POINTER_TYPE_: {
	    Type ptrTarget = getType(f, type.getUltimateType());
	    if (ptrTarget == null)
		ptrTarget = new VoidType();
	    returnType = new PointerType("*", byteorder, getByteSize(type),
		    ptrTarget);
	    break;
	}
	case DwTag.ARRAY_TYPE_: {
	    DwarfDie subrange = type.getChild();
	    returnType = getArrayType(f, type.getType(), subrange);
	    break;
	}
	case DwTag.UNION_TYPE_:
	case DwTag.STRUCTURE_TYPE_: {
	    boolean noTypeDef = (typeDie.getType() == null);
	    String name = noTypeDef ? typeDie.getName() : typeDie.getType()
		    .getName();
	    ClassType classType = getClassType(f, type, name);
	    if (type != typeDie.getType() && noTypeDef == false)
		classType.setTypedefFIXME(true);
	    returnType = classType;
	    break;
	}
	case DwTag.ENUMERATION_TYPE_: {
	    DwarfDie subrange = type.getChild();
	    EnumType enumType = new EnumType(byteorder, 
		    typeDie.getAttrConstant(DwAt.BYTE_SIZE_));
	    while (subrange != null) {
		enumType.addMember(subrange.getName(), subrange
			.getAttrConstant(DwAt.CONST_VALUE_));
		subrange = subrange.getSibling();
	    }
	    returnType = enumType;
	    break;
	}
	case DwTag.BASE_TYPE_: {
	    switch (type.getBaseType()) {
	    case BaseTypes.baseTypeLong:
	        returnType = new SignedType(type.getName(), byteorder, 8);
		break;
	    case BaseTypes.baseTypeUnsignedLong:
		returnType = new UnsignedType(type.getName(), byteorder, 8);
		break;
	    case BaseTypes.baseTypeInteger:
		returnType = new SignedType(type.getName(), byteorder, 4);
		break;
	    case BaseTypes.baseTypeUnsignedInteger:
		returnType = new UnsignedType(type.getName(), byteorder, 4);
		break;
	    case BaseTypes.baseTypeShort:
		returnType = new SignedType(type.getName(), byteorder, 2);
		break;
	    case BaseTypes.baseTypeUnsignedShort:
		returnType = new UnsignedType(type.getName(), byteorder, 2);
		break;
	    case BaseTypes.baseTypeByte:
		returnType = new SignedType(type.getName(), byteorder, 1);
		break;
	    case BaseTypes.baseTypeUnsignedByte:
		returnType = new UnsignedType(type.getName(), byteorder, 1);
		break;
	    case BaseTypes.baseTypeFloat:
		returnType = StandardTypes.getFloatType(isa);
		break;
	    case BaseTypes.baseTypeDouble:
		returnType = StandardTypes.getDoubleType(isa);
		break;
	    }
	}
	}

	if (returnType != null) {
	    dieHash.put(new Integer(type.getOffset()), returnType);
	    return returnType;
	}
	else
	    return new UnknownType(typeDie.getName());
    }

}
