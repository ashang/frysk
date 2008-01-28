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

import frysk.isa.ISA;
import frysk.rsl.Log;
import frysk.scopes.SourceLocation;
import frysk.value.Access;
import frysk.value.ArrayType;
import frysk.value.CharType;
import frysk.value.ClassType;
import frysk.value.CompositeType;
import frysk.value.ConstType;
import frysk.value.EnumType;
import frysk.value.FloatingPointType;
import frysk.value.FunctionType;
import frysk.value.GccStructOrClassType;
import frysk.value.PointerType;
import frysk.value.ReferenceType;
import frysk.value.SignedType;
import frysk.value.StructType;
import frysk.value.Type;
import frysk.value.TypeDef;
import frysk.value.UnionType;
import frysk.value.UnknownType;
import frysk.value.UnsignedType;
import frysk.value.Value;
import frysk.value.VoidType;
import frysk.value.VolatileType;
import inua.eio.ByteOrder;

import java.util.ArrayList;
import java.util.HashMap;

import lib.dwfl.DwAccess;
import lib.dwfl.DwAt;
import lib.dwfl.DwAte;
import lib.dwfl.DwAttributeNotFoundException;
import lib.dwfl.DwTag;
import lib.dwfl.DwarfDie;

public class TypeEntry {
    static protected Log fine = Log.fine(TypeEntry.class);
    static protected Log finest = Log.finest(TypeEntry.class);
    private final ByteOrder byteorder;
    private final HashMap dieHash;

    public TypeEntry(ISA isa) {
	this.byteorder = isa.order();
	this.dieHash = new HashMap();
    }

    private int getByteSize(DwarfDie die) {
	return die.getAttrConstant(DwAt.BYTE_SIZE);
    }

    private void dumpDie(String s, DwarfDie die) {
	// System.out.println(s + Long.toHexString(die.getOffset()) + " "
	// + DwTag.toName(die.getTag().hashCode())
	// + " " + die.getName());
    }

    /**
     * @param dieType
     *                An array die
     * @param subrange
     *                Die for the array's first index
     * @return ArrayType for the array
     */
    public ArrayType getArrayType(DwarfDie dieType, DwarfDie subrange) {
	int elementCount = 1;
	dumpDie("arrayDie=", dieType);
	dumpDie("subrange=", subrange);
	ArrayList dims = new ArrayList();
	while (subrange != null) {
	    int arrDim = subrange.getAttrConstant(DwAt.UPPER_BOUND);
	    dims.add(new Integer(arrDim));
	    subrange = subrange.getSibling();
	    elementCount *= arrDim + 1;
	}

	ArrayType arrayType = null;
	Type type = getType(dieType);
	int typeSize = type.getSize();
	arrayType = new ArrayType(type, elementCount * typeSize, dims);
	return arrayType;
    }

    private void addMembers(DwarfDie classDie, CompositeType classType) {
	LocationExpression locationExpression = null;

	for (DwarfDie member = classDie.getChild(); 
	     member != null; 
	     member = member.getSibling()) {

	    dumpDie("member=", member);

	    boolean staticMember = false;
	    long offset;
	    try {
		offset = member.getDataMemberLocation();
	    } catch (DwAttributeNotFoundException de) {
		offset = 0; // union
		staticMember = true;
		if (member.isDeclaration()) {
		    locationExpression = new LocationExpression(member
			    .getDefinition());
		} else {
		    locationExpression = new LocationExpression(member);
		}

	    }

	    SourceLocation sourceLocation;
	    try {
		sourceLocation = new SourceLocation(member.getDeclFile(),
			member.getDeclLine(), member.getDeclColumn());
	    } catch (DwAttributeNotFoundException e) {
		sourceLocation = SourceLocation.UNKNOWN;
	    }

	    Access access = null;
	    switch (member.getAttrConstant(DwAt.ACCESSIBILITY)) {
	    case DwAccess.PUBLIC_:
		access = Access.PUBLIC;
		break;
	    case DwAccess.PROTECTED_:
		access = Access.PROTECTED;
		break;
	    case DwAccess.PRIVATE_:
		access = Access.PRIVATE;
		break;
	    }

	    if (member.getTag() == DwTag.SUBPROGRAM) {
		Value v = getSubprogramValue(member);
		if (hasArtifitialParameter(member)) {
		    classType.addMember(member.getName(), sourceLocation, v
			    .getType(), offset, access);
		} else {
		    classType.addStaticMember(locationExpression, member
			    .getName(), sourceLocation, v.getType(), offset,
			    access);
		}
		continue;
	    }

	    // DwarfDie memberDieType = member.getUltimateType();
	    // if (memberDieType == null)
	    // continue;

	    Type memberType = getType(member.getType());
	    if (memberType instanceof UnknownType == false) {
		// System V ABI Supplements discuss bit field layout
		int bitSize = member.getAttrConstant(DwAt.BIT_SIZE);
		if (bitSize != -1) {
		    int bitOffset = member.getAttrConstant(DwAt.BIT_OFFSET);
		    if (staticMember) {
			classType.addStaticBitFieldMember(locationExpression,
				member.getName(), sourceLocation, memberType,
				offset, access, bitOffset, bitSize);
		    } else {
			classType.addBitFieldMember(member.getName(),
				sourceLocation, memberType, offset, access,
				bitOffset, bitSize);
		    }
		} else {
		    if (staticMember) {
			classType.addStaticMember(locationExpression, member
				.getName(), sourceLocation, memberType, offset,
				access);
		    } else {
			classType.addMember(member.getName(), sourceLocation,
				memberType, offset, access);
		    }

		}
		continue;
	    } else {
		if (staticMember) {
		    classType.addStaticMember(locationExpression, member
			    .getName(), sourceLocation, new UnknownType(member
			    .getName()), offset, access);
		} else {
		    classType.addMember(member.getName(), sourceLocation,
			    new UnknownType(member.getName()), offset, access);
		}
	    }
	}
    }

    /**
     * @param classDie
     *                A struct die
     * @param name
     *                Name of the struct
     * @return GccStructOrClassType for the struct
     */
    public CompositeType getGccStructOrClassType(DwarfDie classDie, 
	    					 String name) {
	dumpDie("structOrClassDie=", classDie);

	CompositeType type;

	String compiler = classDie.getProducer();
	boolean supportsClassType = CompilerVersionFactory.getCompilerVersion(
		compiler).supportsClassType();
	fine.log("Compiler support determined as:" + supportsClassType);

	/*
	 * If the compiler supported class types and this was a class, it would
	 * have been detected as a ClassType. Since it was not, it must be a
	 * struct
	 */
	if (supportsClassType)
	    type = new StructType(name, getByteSize(classDie));
	else
	    type = new GccStructOrClassType(name, getByteSize(classDie));

	addMembers(classDie, type);

	return type;
    }

    /**
     * 
     * @param classDie
     *                A class die
     * @param name
     *                Name of the class
     * @return ClassType for the class.
     */
    public ClassType getClassType(DwarfDie classDie, String name) {
	dumpDie("classDie=", classDie);

	ClassType classType = new ClassType(name, getByteSize(classDie));
	addMembers(classDie, classType);
	return classType;
    }

    /**
     * Return true of the given die represents a subprogram or inlined
     * subroutine
     * 
     * @param die
     * @return
     */
    private boolean hasArtifitialParameter(DwarfDie die) {
	if (die == null
		|| !(die.getTag().equals(DwTag.SUBPROGRAM) || die.getTag()
			.equals(DwTag.INLINED_SUBROUTINE))) {
	    throw new IllegalArgumentException("Die [" + die + "] is not"
		    + " a subprogram or inlined subroutine die");
	}

	DwarfDie param = die.getChild();
	while (param != null && 
	       param.getTag().equals(DwTag.FORMAL_PARAMETER)) {
	    if (param.getAttrBoolean((DwAt.ARTIFICIAL)) == true) {
		return true;
	    }
	    param = param.getSibling();
	}
	return false;
    }

    // ??? Reduce getGccStructOrClassType/getUnionType duplication
    public UnionType getUnionType(DwarfDie classDie, String name) {
	dumpDie("unionDie=", classDie);

	UnionType classType = new UnionType(name, getByteSize(classDie));
	for (DwarfDie member = classDie.getChild(); 
	     member != null; 
	     member = member.getSibling()) {
	    
	    dumpDie("member=", member);
	    long offset;
	    try {
		offset = member.getDataMemberLocation();
	    } catch (DwAttributeNotFoundException de) {
		offset = 0; // union
	    }

	    SourceLocation lineColPair;
	    try {
		lineColPair = new SourceLocation(member.getDeclFile(), member
			.getDeclLine(), member.getDeclColumn());
	    } catch (DwAttributeNotFoundException e) {
		lineColPair = SourceLocation.UNKNOWN;
	    }

	    Access access = null;
	    switch (member.getAttrConstant(DwAt.ACCESSIBILITY)) {
	    case DwAccess.PUBLIC_:
		access = Access.PUBLIC;
		break;
	    case DwAccess.PROTECTED_:
		access = Access.PROTECTED;
		break;
	    case DwAccess.PRIVATE_:
		access = Access.PRIVATE;
		break;
	    }
	    DwarfDie memberDieType = member.getUltimateType();

	    if (member.getTag() == DwTag.SUBPROGRAM) {
		Value v = getSubprogramValue(member);
		classType.addMember(member.getName(), lineColPair, v.getType(),
			offset, access);
		continue;
	    }

	    if (memberDieType == null)
		continue;

	    Type memberType = getType(member.getType());
	    if (memberType instanceof UnknownType == false) {
		// System V ABI Supplements discuss bit field layout
		int bitSize = member.getAttrConstant(DwAt.BIT_SIZE);
		if (bitSize != -1) {
		    int bitOffset = member.getAttrConstant(DwAt.BIT_OFFSET);
		    classType.addBitFieldMember(member.getName(), lineColPair,
			    memberType, offset, access, bitOffset, bitSize);
		} else
		    classType.addMember(member.getName(), lineColPair,
			    memberType, offset, access);

		continue;
	    } else
		classType.addMember(member.getName(), lineColPair,
			new UnknownType(member.getName()), offset, access);
	}

	return classType;
    }

    /**
     * @param varDie
     *                The die for a symbol corresponding to a function
     * @return The value of a subprogram die
     */
    public Value getSubprogramValue(DwarfDie varDie) {
	if (varDie == null)
	    return (null);

	switch (varDie.getTag().hashCode()) {
	case DwTag.SUBROUTINE_TYPE_:
	case DwTag.SUBPROGRAM_: {
	    Type type = null;
	    if (varDie.getUltimateType() != null) {
		type = getType(varDie.getType());
	    }
	    FunctionType functionType = new FunctionType(varDie.getName(), 
		    					 type);
	    DwarfDie parm = varDie.getChild();
	    while (parm != null && 
		   parm.getTag().equals(DwTag.FORMAL_PARAMETER)) {
		if (parm.getAttrBoolean((DwAt.ARTIFICIAL)) == false) {
		    type = getType(parm);
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
    public Type getType(DwarfDie typeDie) {

	if (typeDie == null)
	    return (null);

	dumpDie("getType typeDie=", typeDie);
	DwarfDie type;
	if (typeDie.getTag().equals(DwTag.FORMAL_PARAMETER)
		|| typeDie.getTag().equals(DwTag.VARIABLE)) {
	    type = typeDie.getType();
	    dumpDie("getType type=", type);
	} else
	    type = typeDie;

	Type mappedType = (Type) dieHash.get(new Long(type.getOffset()));
	if (mappedType != null)
	    return mappedType;
	else if (dieHash.containsKey(new Long(type.getOffset()))) {
	    // ??? will this always be a pointer to ourselves?
	    // Instead of VoidType, we need a way to reference ourselves
	    return new PointerType("", byteorder, getByteSize(type),
		    new VoidType());
	}
	dieHash.put(new Long(type.getOffset()), null);
	Type returnType = null;

	switch (type.getTag().hashCode()) {
	case DwTag.TYPEDEF_:
	    DwarfDie typedefDieType = type.getType();
	    Type typedefType = (typedefDieType != null) 
		? getType(typedefDieType)
	    	: new UnknownType(type.getName());
	    returnType = new TypeDef(type.getName(), typedefType);
	    break;
	case DwTag.POINTER_TYPE_: {
	    Type ptrTarget = getType(type.getType());
	    if (ptrTarget == null)
		ptrTarget = new VoidType();
	    returnType = new PointerType("*", byteorder, getByteSize(type),
		    ptrTarget);
	    break;
	}
	case DwTag.REFERENCE_TYPE_:
	    returnType = new ReferenceType(getType(type.getType()));
	    break;
	case DwTag.ARRAY_TYPE_: {
	    DwarfDie subrange = type.getChild();
	    returnType = getArrayType(type.getType(), subrange);
	    break;
	}
	case DwTag.UNION_TYPE_: {
	    UnionType unionType = getUnionType(type, typeDie.getName());
	    returnType = unionType;
	    break;
	}
	case DwTag.STRUCTURE_TYPE_: {
	    CompositeType classType = getGccStructOrClassType(type, typeDie
		    .getName());
	    returnType = classType;
	    break;
	}
	case DwTag.CLASS_TYPE_: {
	    ClassType classType = getClassType(type, typeDie.getName());
	    returnType = classType;
	    break;
	}
	case DwTag.ENUMERATION_TYPE_: {
	    DwarfDie subrange = type.getChild();
	    EnumType enumType = new EnumType(typeDie.getName(), byteorder,
		    getByteSize(type));
	    while (subrange != null) {
		enumType.addMember(subrange.getName(), subrange
			.getAttrConstant(DwAt.CONST_VALUE));
		subrange = subrange.getSibling();
	    }
	    returnType = enumType;
	    break;
	}
	case DwTag.VOLATILE_TYPE_:
	    returnType = new VolatileType(getType(type.getType()));
	    break;
	case DwTag.CONST_TYPE_:
	    returnType = new ConstType(getType(type.getType()));
	    break;
	case DwTag.SUBROUTINE_TYPE_:
	    returnType = getSubprogramValue(type).getType();
	    break;
	case DwTag.BASE_TYPE_:
	    switch (type.getAttrConstant(DwAt.ENCODING)) {
	    case DwAte.SIGNED_:
		returnType = new SignedType(type.getName(), byteorder,
			getByteSize(type));
		break;
	    case DwAte.SIGNED_CHAR_:
		returnType = new CharType(type.getName(), byteorder,
			getByteSize(type), true);
		break;
	    case DwAte.UNSIGNED_:
		returnType = new UnsignedType(type.getName(), byteorder,
			getByteSize(type));
		break;
	    case DwAte.UNSIGNED_CHAR_:
		returnType = new CharType(type.getName(), byteorder,
			getByteSize(type), false);
		break;
	    case DwAte.FLOAT_:
		returnType = new FloatingPointType(type.getName(), byteorder,
			getByteSize(type));
	    }
	}

	if (returnType != null) {
	    dieHash.put(new Long(type.getOffset()), returnType);
	    return returnType;
	} else
	    return new UnknownType(typeDie.getName());
    }
}
