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


package lib.dw;

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
	DwTagEncodings[] tagEncodings = DwTagEncodings.values();
	Pattern pattern = Pattern.compile("_.");
	HashMap dieClasses = loadDies();
	Class[] constructorArgTypes = new Class[] {Long.TYPE,
						   lib.dw.Dwfl.class};
	for (int i = 0; i < tagEncodings.length; i++) {
	    DwTagEncodings tag = tagEncodings[i];
	    int tagValue = tag.hashCode();
	    String enumName = tag.toName();
	    if (!enumName.startsWith(tagPrefix, 0)) {
		throw new DwarfException("enum name " + enumName
					 + " is bogus.");
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
	    lib.dw.die.ArrayType.class,
	    lib.dw.die.ClassType.class,
	    lib.dw.die.EntryPoint.class,
	    lib.dw.die.EnumerationType.class,
	    lib.dw.die.FormalParameter.class,
	    lib.dw.die.ImportedDeclaration.class,
	    lib.dw.die.Label.class,
	    lib.dw.die.LexicalBlock.class,
	    lib.dw.die.Member.class,
	    lib.dw.die.PointerType.class,
	    lib.dw.die.ReferenceType.class,
	    lib.dw.die.CompileUnit.class,
	    lib.dw.die.StringType.class,
	    lib.dw.die.StructureType.class,
	    lib.dw.die.SubroutineType.class,
	    lib.dw.die.Typedef.class,
	    lib.dw.die.UnionType.class,
	    lib.dw.die.UnspecifiedParameters.class,
	    lib.dw.die.Variant.class,
	    lib.dw.die.CommonBlock.class,
	    lib.dw.die.CommonInclusion.class,
	    lib.dw.die.Inheritance.class,
	    lib.dw.die.InlinedSubroutine.class,
	    lib.dw.die.Module.class,
	    lib.dw.die.PtrToMemberType.class,
	    lib.dw.die.SetType.class,
	    lib.dw.die.SubrangeType.class,
	    lib.dw.die.WithStmt.class,
	    lib.dw.die.AccessDeclaration.class,
	    lib.dw.die.BaseType.class,
	    lib.dw.die.CatchBlock.class,
	    lib.dw.die.ConstType.class,
	    lib.dw.die.Constant.class,
	    lib.dw.die.Enumerator.class,
	    lib.dw.die.FileType.class,
	    lib.dw.die.Friend.class,
	    lib.dw.die.Namelist.class,
	    lib.dw.die.NamelistItem.class,
	    lib.dw.die.PackedType.class,
	    lib.dw.die.Subprogram.class,
	    lib.dw.die.TemplateTypeParameter.class,
	    lib.dw.die.TemplateValueParameter.class,
	    lib.dw.die.ThrownType.class,
	    lib.dw.die.TryBlock.class,
	    lib.dw.die.VariantPart.class,
	    lib.dw.die.Variable.class,
	    lib.dw.die.VolatileType.class,
	    lib.dw.die.DwarfProcedure.class,
	    lib.dw.die.RestrictType.class,
	    lib.dw.die.InterfaceType.class,
	    lib.dw.die.Namespace.class,
	    lib.dw.die.ImportedModule.class,
	    lib.dw.die.UnspecifiedType.class,
	    lib.dw.die.PartialUnit.class,
	    lib.dw.die.ImportedUnit.class,
	    lib.dw.die.MutableType.class,
	    lib.dw.die.Condition.class,
	    lib.dw.die.SharedType.class,
	    lib.dw.die.LoUser.class,
	    lib.dw.die.MIPSLoop.class,
	    lib.dw.die.FormatLabel.class,
	    lib.dw.die.FunctionTemplate.class,
	    lib.dw.die.ClassTemplate.class,
	    lib.dw.die.HiUser.class
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
