// This file is part of the program FRYSK.
//
// Copyright 2005, 2006, Red Hat Inc.
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

import java.util.ArrayList;
import java.util.List;

abstract public class DwarfDie
{

  private long pointer;
  
  private DwarfDie[] scopes;
  
  private int scopeIndex;

  private Dwfl parent;
  
  protected DwarfDie (long pointer, Dwfl parent)
  {
    this.pointer = pointer;
    this.parent = parent;
  }

  protected Dwfl getParent ()
  {
    return this.parent;
  }

  public long getHighPC ()
  {
    return get_highpc();
  }

  public long getLowPC ()
  {
    return get_lowpc();
  }

  public String getName ()
  {
    return get_diename();
  }
  
  public String getDeclFile ()
  {
    return get_decl_file(this.getPointer());
  }
  
  public long getDeclLine ()
  {
    return get_decl_line(this.getPointer());
  }

  public void setScopes (DwarfDie[] scopes)
  {
    this.scopes = scopes;
  }
  
  /**
   * 
   * @param addr PC address.
   * @return Scope DwarfDies containing addr.
   */
  public DwarfDie[] getScopes (long addr)
  {
    long[] vals = get_scopes(addr);
    DwarfDie[] dies = new DwarfDie[vals.length];
    DwarfDieFactory factory = DwarfDieFactory.getFactory();
    for(int i = 0; i < vals.length; i++)
      if(vals[i] != 0)
        dies[i] = factory.makeDie(vals[i], this.parent);
      else
        dies[i] = null;

    return dies;
  }
  
  /**
   * @param scopes
   * @param variable
   * @return Die of variable in scopes
   */
  public DwarfDie getScopeVar (DwarfDie[] scopes, String variable)
  {
    long[] vals = new long[scopes.length];
    long[] die_and_scope = new long[2];
    for(int i = 0; i < scopes.length; i++)
    vals[i] = scopes[i].getPointer();

    DwarfDie die = null;
    long val = get_scopevar(die_and_scope, vals, variable);
    if (val >= 0)
      {
        die = DwarfDieFactory.getFactory().makeDie(die_and_scope[0],
						   this.parent);
        die.scopes = scopes;
        die.scopeIndex = (int)die_and_scope[1];
      }
    return die;
  }
  
  private ArrayList varNames;
  /**
   * @param scopes
   * @param variable
   * @return List of names in scopes matching variable
   */
  public List getScopeVarNames (DwarfDie[] scopes, String variable)
  {
    varNames = new ArrayList();    
    long[] vals = new long[scopes.length];
    for(int i = 0; i < scopes.length; i++)
      vals[i] = scopes[i].getPointer();

    get_scopevar_names(vals, variable);
   return varNames; 
  }
 
  public void addScopeVarName (String name)
  {
    varNames.add(name);
  }
  
  public class DwarfOp
  {
    public int operator;
    public int operand1;
    public int operand2;
    public int offset;
    DwarfOp (int op, int op1, int op2, int off)
    {
      operator = op;
      operand1 = op1;
      operand2 = op2;
      offset = off;
    }
  }
  private ArrayList DwarfOps;
  
  public void addOps (int operator, int operand1, int operand2, int offset)
  {
    DwarfOp dwarfOp = new DwarfOp(operator, operand1, operand2, offset);
    DwarfOps.add(dwarfOp);
  }
  
  /**
   * @return Scopes index of this die.
   */
  public long getScopeIndex ()
  {
      return this.scopeIndex;
  }
  
  /**
   * @param index Scopes index.
   * @return Die of scope.
   */
  public long getScope (int index)
  {
      return this.scopes[index].pointer;
  }
  
  /**
   * @param Return address of die.   Typically this is a static address or ptr+disp.
   */
  public List getAddr ()
  {
    DwarfOps = new ArrayList();
    get_addr(this.getPointer(), 0);
    return DwarfOps;
  }

  /**
   * @return The type die for the current die.
   */
  public DwarfDie getType ()
  {
    DwarfDie die = null;
    long type = get_type(this.getPointer());
    if (type != 0)
      die = DwarfDieFactory.getFactory().makeDie(type, this.parent);
    return die;
  }

  /**
   * @return the scalar type for this type die.
   */
  public int getBaseType ()
  {
    return get_base_type(this.getPointer());
  }
  
  public boolean getAttrBoolean(int attr)
  {
    return get_attr_boolean(this.getPointer(), attr);
  }
  
  public int getTag()
  {
    return get_tag(this.getPointer());
  }
  
  /**
   * @return The upper bound for this subrange die.
   */
  public int getAttrConstant (int attr)
  {
	  return get_attr_constant(this.getPointer(), attr);
  }

  /**
   * @return The child for the current die.
   */
  public DwarfDie getChild ()
  {
    long child = get_child(this.getPointer());
    DwarfDie die = null;
    if (child != 0)
      die = DwarfDieFactory.getFactory().makeDie(child, this.parent);
    return die;
  }

  /**
   * @return The sibling for the current die.
   */
  public DwarfDie getSibling ()
  {
    long sibling = get_sibling(this.getPointer());
    DwarfDie die = null;
    if (sibling != 0)
      die = DwarfDieFactory.getFactory().makeDie(sibling, this.parent);
    return die;
  }
  
  protected long getPointer ()
  {
    return this.pointer;
  }
 
  /**
   * @param pc Program Counter
   * @return DW_AT_frame_base for current die.
   */
  public List getFrameBase (long pc)
  {
    DwarfOps = new ArrayList();
    for (int i = this.scopeIndex; i < this.scopes.length; i++)
      {
        get_framebase(this.getPointer(), this.scopes[i].pointer, pc);
        if (DwarfOps.size() != 0)
          break;
      }
    return DwarfOps;
  }

  /**
   * @param pc - PC
   * @return DW_FORM_data for current die.  Typically this is from a location list.
   */
  public List getFormData (long pc)
  {
    DwarfOps = new ArrayList();
    get_addr(this.getPointer(), pc);
    return DwarfOps;
  }

  public long getDataMemberLocation ()
  {
    return get_data_member_location(this.getPointer());
  }
  
  /**
   * @return True if this is an inlined instance of a function, false otherwise
   */
  public boolean isInlinedFunction ()
  {
    return is_inline_func();
  }
  
  public String toString ()
  {
    String typeStr;
    DwarfDie type = getType();
    if (type.getBaseType() == BaseTypes.baseTypeLong)
      typeStr = "long";
    else if (type.getBaseType() == BaseTypes.baseTypeInteger)
      typeStr = "int";
    else if (type.getBaseType() == BaseTypes.baseTypeShort)
      typeStr = "short";
    else if (type.getBaseType() == BaseTypes.baseTypeChar)
      typeStr = "short";
    else if (type.getBaseType() == BaseTypes.baseTypeFloat)
      typeStr = "float";
    else if (type.getBaseType() == BaseTypes.baseTypeDouble)
      typeStr = "double";
    else
      typeStr = "";
    return typeStr;
  }
  
/**
 * Get die for static symbol sym in dw. 
 * @param dw
 * @param sym
 * @return die
 */
  public static DwarfDie getDecl (Dwarf dw, String sym)
  {
    long result = get_decl (dw.getPointer(), sym);
    DwarfDie die = null;
    if (result > 0)
      {
        die = DwarfDieFactory.getFactory().makeDie(result, null);
        die.scopes = null;
        die.scopeIndex = 0;
      }
    return die;
  }

  /**
   * Get die for static symbol sym in CU dw.
   * @param dw
   * @param sym
   * @return die
   */
  public static DwarfDie getDeclCU (DwarfDie dw, String sym)
  {
    long result = get_decl_cu (dw.getPointer(), sym);
    DwarfDie die = null;
    if (result > 0)
      {
	die = DwarfDieFactory.getFactory().makeDie(result, null);
        die.scopes = null;
        die.scopeIndex = 0;
      }
    return die;
  }
  
  abstract public void accept(DieVisitor visitor);

  public native ArrayList getEntryBreakpoints();

  public native boolean isInlineDeclaration();

  public native ArrayList getInlinedInstances();
  
  private native long get_lowpc ();

  private native long get_highpc ();

  protected native long get_entrypc();

  private native String get_diename ();
  
  private native String get_decl_file (long var_die);
  
  private native long get_decl_line (long var_die);
  
  private native long[] get_scopes (long addr);

  private native long get_scopevar (long[] die_scope, long[] scopes, String variable);

  private native long get_scopevar_names (long[] scopes, String variable);
  
  private native void get_addr (long addr, long pc);
  
  private native long get_type (long addr);
  
  private native long get_child (long addr);
  
  private native long get_sibling (long addr);
  
  private native int get_base_type (long addr);

  private native boolean get_attr_boolean (long addr, int attr);
  
  private native int get_attr_constant (long addr, int attr);
  
  // Package access for DwarfDieFactory
  static native int get_tag (long var_die);
  
  private native void get_framebase (long addr, long scope, long pc);

  private native long get_data_member_location (long addr);
  
  private native boolean is_inline_func ();

  private static native long get_decl (long dw, String sym);

  private static native long get_decl_cu (long dw, String sym);
}
