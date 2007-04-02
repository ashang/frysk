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
public class DwarfDieFactory
{
  private static DwarfDieFactory singleton;

  /**
   * Get the singleton factory.
   * @return the factory
   */
  public static synchronized DwarfDieFactory getFactory()
  {
    if (singleton == null)
      singleton = new DwarfDieFactory();
    return singleton;
  }

  // map from Dwarf tags to Constructor objects for the corresponding
  // wrapper object.
  private HashMap constructorMap;
  DieVisitor visitor;	// XXX

  private static String tagPrefix = "DW_TAG";
  
  private DwarfDieFactory()
  {
    constructorMap = new HashMap();
    visitor = new DieVisitor();
    DwTagEncodings[] tagEncodings = DwTagEncodings.values();
    Pattern pattern = Pattern.compile("_.");
    Class[] constructorArgTypes
      = new Class[] {Long.TYPE,
		     lib.dw.Dwfl.class};
		     

    for (int i = 0; i < tagEncodings.length; i++)
      {
	DwTagEncodings tag = tagEncodings[i];
	int tagValue = tag.hashCode();
	String enumName = tag.toName();
	if (!enumName.startsWith(tagPrefix, 0))
	  {
	    throw new DwarfException("enum name " + enumName + " is bogus.");
	  }
	Matcher m = pattern.matcher(enumName.substring(tagPrefix.length()));
	StringBuffer sb = new StringBuffer();
	while (m.find())
	  {
	    m.appendReplacement(sb, m.group().substring(1).toUpperCase());
	  }
	m.appendTail(sb);
	String className = sb.toString();
	Class wrapper;
	try
	  {
	     wrapper = Class.forName("lib.dw.die." + className);
	  }
	catch (ClassNotFoundException e)
	  {
	    throw new DwarfException("No class " + className);
	  }
	Constructor[] constructors = wrapper.getConstructors();
	Constructor constructor = null;
      outer:
	for (int j = 0; j < constructors.length; j++)
	  {
	    Class[] args = constructors[j].getParameterTypes();
	    if (args.length != 2)
	      continue;
	    for (int k = 0; k < 2; k++)
	      {
		if (args[k] != constructorArgTypes[k])
		  continue outer;
	      }
	    constructor = constructors[j];
	    break;
	  }
	if (constructor == null)
	  {
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
  public DwarfDie makeDie(long pointer, Dwfl parent)
  {
    int tag = DwarfDie.get_tag(pointer);
    Constructor constructor = (Constructor)constructorMap.get(new Integer(tag));
    if (constructor == null)
      {
	throw new DwarfException("No constructor for tag " + tag);
      }
    try
      {
	return (DwarfDie)constructor.newInstance(new Object[]
	    {new Long(pointer), parent});
      }
    catch (InstantiationException e)
      {
	throw new DwarfException("creating tag " + tag, e);
      }
    catch (IllegalAccessException e)
      {
	throw new DwarfException("creating tag " + tag, e);
      }
    catch (java.lang.reflect.InvocationTargetException e)
      {
	throw new DwarfException("creating tag " + tag, e);
      }
  }

  // XXX Needed to suck in all the Die classes when linking
  // statically. Kind of moots all the reflection done above...

  static Class loadDies()
  {
    Class cls;

    cls = lib.dw.die.ArrayType.class;
    cls = lib.dw.die.ClassType.class;
    cls = lib.dw.die.EntryPoint.class;
    cls = lib.dw.die.EnumerationType.class;
    cls = lib.dw.die.FormalParameter.class;
    cls = lib.dw.die.ImportedDeclaration.class;
    cls = lib.dw.die.Label.class;
    cls = lib.dw.die.LexicalBlock.class;
    cls = lib.dw.die.Member.class;
    cls = lib.dw.die.PointerType.class;
    cls = lib.dw.die.ReferenceType.class;
    cls = lib.dw.die.CompileUnit.class;
    cls = lib.dw.die.StringType.class;
    cls = lib.dw.die.StructureType.class;
    cls = lib.dw.die.SubroutineType.class;
    cls = lib.dw.die.Typedef.class;
    cls = lib.dw.die.UnionType.class;
    cls = lib.dw.die.UnspecifiedParameters.class;
    cls = lib.dw.die.Variant.class;
    cls = lib.dw.die.CommonBlock.class;
    cls = lib.dw.die.CommonInclusion.class;
    cls = lib.dw.die.Inheritance.class;
    cls = lib.dw.die.InlinedSubroutine.class;
    cls = lib.dw.die.Module.class;
    cls = lib.dw.die.PtrToMemberType.class;
    cls = lib.dw.die.SetType.class;
    cls = lib.dw.die.SubrangeType.class;
    cls = lib.dw.die.WithStmt.class;
    cls = lib.dw.die.AccessDeclaration.class;
    cls = lib.dw.die.BaseType.class;
    cls = lib.dw.die.CatchBlock.class;
    cls = lib.dw.die.ConstType.class;
    cls = lib.dw.die.Constant.class;
    cls = lib.dw.die.Enumerator.class;
    cls = lib.dw.die.FileType.class;
    cls = lib.dw.die.Friend.class;
    cls = lib.dw.die.Namelist.class;
    cls = lib.dw.die.NamelistItem.class;
    cls = lib.dw.die.PackedType.class;
    cls = lib.dw.die.Subprogram.class;
    cls = lib.dw.die.TemplateTypeParameter.class;
    cls = lib.dw.die.TemplateValueParameter.class;
    cls = lib.dw.die.ThrownType.class;
    cls = lib.dw.die.TryBlock.class;
    cls = lib.dw.die.VariantPart.class;
    cls = lib.dw.die.Variable.class;
    cls = lib.dw.die.VolatileType.class;
    cls = lib.dw.die.DwarfProcedure.class;
    cls = lib.dw.die.RestrictType.class;
    cls = lib.dw.die.InterfaceType.class;
    cls = lib.dw.die.Namespace.class;
    cls = lib.dw.die.ImportedModule.class;
    cls = lib.dw.die.UnspecifiedType.class;
    cls = lib.dw.die.PartialUnit.class;
    cls = lib.dw.die.ImportedUnit.class;
    cls = lib.dw.die.MutableType.class;
    cls = lib.dw.die.Condition.class;
    cls = lib.dw.die.SharedType.class;
    cls = lib.dw.die.LoUser.class;
    cls = lib.dw.die.MIPSLoop.class;
    cls = lib.dw.die.FormatLabel.class;
    cls = lib.dw.die.FunctionTemplate.class;
    cls = lib.dw.die.ClassTemplate.class;
    cls = lib.dw.die.HiUser.class;
    return cls;	// Damn you ecj!
  }
}
