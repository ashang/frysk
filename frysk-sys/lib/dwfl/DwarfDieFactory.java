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


package lib.dwfl;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Factory class for creating DwarfDie objects with a class corresponding to
 * the tag in the Dwarf_Die object.
 *
 */
public class DwarfDieFactory {
    private static DwarfDieFactory singleton;

    /**
     * Get the singleton factory.
     * @return the factory
     */
    public static synchronized DwarfDieFactory getFactory() {
	if (singleton == null)
	    singleton = new DwarfDieFactory();
	return singleton;
    }

    // map from Dwarf tags to Constructor objects for the corresponding
    // wrapper object.
    private HashMap constructorMap;
    DieVisitor visitor;	// XXX

    private static String tagPrefix = "DW_TAG";
  
    private DwarfDieFactory() {
	constructorMap = new HashMap();
	visitor = new DieVisitor();
	DwTag[] tagEncodings = DwTag.values();
	Pattern pattern = Pattern.compile("_.");
	HashMap dieClasses = loadDies();
	Class[] constructorArgTypes = new Class[] {Long.TYPE,
						   lib.dwfl.Dwfl.class};
	for (int i = 0; i < tagEncodings.length; i++) {
	    DwTag tag = tagEncodings[i];
	    int tagValue = tag.hashCode();
	    String enumName = tag.toPrint();
	    if (!enumName.startsWith(tagPrefix, 0)) {
		throw new DwarfException("enum name <" + enumName
					 + "> is bogus.");
	    }
	    Matcher m = pattern.matcher(enumName.substring(tagPrefix.length()));
	    StringBuffer sb = new StringBuffer();
	    while (m.find()) {
		m.appendReplacement(sb, m.group().substring(1).toUpperCase());
	    }
	    m.appendTail(sb);
	    String className = sb.toString();
	    Class wrapper = (Class)dieClasses.get(className);
	    if (wrapper == null) {
		throw new DwarfException("No class " + className);
	    }
	    Constructor[] constructors = wrapper.getConstructors();
	    Constructor constructor = null;
	    outer:
	    for (int j = 0; j < constructors.length; j++) {
		Class[] args = constructors[j].getParameterTypes();
		if (args.length != 2)
		    continue;
		for (int k = 0; k < 2; k++) {
		    if (args[k] != constructorArgTypes[k])
			continue outer;
		}
		constructor = constructors[j];
		break;
	    }
	    if (constructor == null) {
		throw new DwarfException("Couldn't get constructor for "
					 + className);
	    }
	    constructorMap.put(new Integer(tagValue), constructor);
	}
    }

    /**
     * Create a subclass of DwarfDie based on the tag in the Dwarf_Die object
     * pointed to by pointer.
     * @param pointer raw Dwarf_Die from libdw
     * @param parent Dwfl object associated with the DIE, if any
     * @return subclass of DwarfDie
     */
    public DwarfDie makeDie(long pointer, Dwfl parent) {
	int tag = DwarfDie.get_tag(pointer);
	Constructor constructor
	    = (Constructor)constructorMap.get(new Integer(tag));
	if (constructor == null) {
	    throw new DwarfException("No constructor for tag " + tag);
	}
	try {
	    return (DwarfDie)constructor.newInstance(new Object[]
		{new Long(pointer), parent});
	}
	catch (InstantiationException e) {
	    throw new DwarfException("creating tag " + tag, e);
	}
	catch (IllegalAccessException e) {
	    throw new DwarfException("creating tag " + tag, e);
	}
	catch (java.lang.reflect.InvocationTargetException e) {
	    throw new DwarfException("creating tag " + tag, e);
	}
    }

    // XXX Needed to suck in all the Die classes when linking
    // statically. Kind of moots all the reflection done above...

    static HashMap loadDies() {
	Class[] cls = {
	    lib.dwfl.die.ArrayType.class,
	    lib.dwfl.die.ClassType.class,
	    lib.dwfl.die.EntryPoint.class,
	    lib.dwfl.die.EnumerationType.class,
	    lib.dwfl.die.FormalParameter.class,
	    lib.dwfl.die.ImportedDeclaration.class,
	    lib.dwfl.die.Label.class,
	    lib.dwfl.die.LexicalBlock.class,
	    lib.dwfl.die.Member.class,
	    lib.dwfl.die.PointerType.class,
	    lib.dwfl.die.ReferenceType.class,
	    lib.dwfl.die.CompileUnit.class,
	    lib.dwfl.die.StringType.class,
	    lib.dwfl.die.StructureType.class,
	    lib.dwfl.die.SubroutineType.class,
	    lib.dwfl.die.Typedef.class,
	    lib.dwfl.die.UnionType.class,
	    lib.dwfl.die.UnspecifiedParameters.class,
	    lib.dwfl.die.Variant.class,
	    lib.dwfl.die.CommonBlock.class,
	    lib.dwfl.die.CommonInclusion.class,
	    lib.dwfl.die.Inheritance.class,
	    lib.dwfl.die.InlinedSubroutine.class,
	    lib.dwfl.die.Module.class,
	    lib.dwfl.die.PtrToMemberType.class,
	    lib.dwfl.die.SetType.class,
	    lib.dwfl.die.SubrangeType.class,
	    lib.dwfl.die.WithStmt.class,
	    lib.dwfl.die.AccessDeclaration.class,
	    lib.dwfl.die.BaseType.class,
	    lib.dwfl.die.CatchBlock.class,
	    lib.dwfl.die.ConstType.class,
	    lib.dwfl.die.Constant.class,
	    lib.dwfl.die.Enumerator.class,
	    lib.dwfl.die.FileType.class,
	    lib.dwfl.die.Friend.class,
	    lib.dwfl.die.Namelist.class,
	    lib.dwfl.die.NamelistItem.class,
	    lib.dwfl.die.PackedType.class,
	    lib.dwfl.die.Subprogram.class,
	    lib.dwfl.die.TemplateTypeParameter.class,
	    lib.dwfl.die.TemplateValueParameter.class,
	    lib.dwfl.die.ThrownType.class,
	    lib.dwfl.die.TryBlock.class,
	    lib.dwfl.die.VariantPart.class,
	    lib.dwfl.die.Variable.class,
	    lib.dwfl.die.VolatileType.class,
	    lib.dwfl.die.DwarfProcedure.class,
	    lib.dwfl.die.RestrictType.class,
	    lib.dwfl.die.InterfaceType.class,
	    lib.dwfl.die.Namespace.class,
	    lib.dwfl.die.ImportedModule.class,
	    lib.dwfl.die.UnspecifiedType.class,
	    lib.dwfl.die.PartialUnit.class,
	    lib.dwfl.die.ImportedUnit.class,
	    lib.dwfl.die.MutableType.class,
	    lib.dwfl.die.Condition.class,
	    lib.dwfl.die.SharedType.class,
	    lib.dwfl.die.LoUser.class,
	    lib.dwfl.die.MIPSLoop.class,
	    lib.dwfl.die.FormatLabel.class,
	    lib.dwfl.die.FunctionTemplate.class,
	    lib.dwfl.die.ClassTemplate.class,
	    lib.dwfl.die.HiUser.class
	};
	HashMap map = new HashMap();
	for (int i = 0; i < cls.length; i++) {
	    Class dieClass = cls[i];
	    int nameIndex = dieClass.getName().lastIndexOf('.') + 1;
	    String dieName = dieClass.getName().substring(nameIndex);
	    map.put(dieName, dieClass);
	}
	return map;
    }
}
