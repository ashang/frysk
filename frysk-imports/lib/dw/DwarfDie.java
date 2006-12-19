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

public class DwarfDie
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

  // public DwarfDie getContainingCompilationUnit(){
  // long val = dwarf_diecu();
  // if(val == 0)
  // return null;
  //		
  // return new DwarfDie(val);
  // }

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


  public DwarfDie[] getScopes (long addr)
  {
    long[] vals = get_scopes(addr);
    DwarfDie[] dies = new DwarfDie[vals.length];
    for(int i = 0; i < vals.length; i++)
      if(vals[i] != 0)
        dies[i] = new DwarfDie(vals[i], this.parent);
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
        die = new DwarfDie(die_and_scope[0], this.parent);
        die.scopes = scopes;
        die.scopeIndex = (int)die_and_scope[1];
      }
    return die;
  }
    
  public long getScopeIndex ()
  {
      return this.scopeIndex;
  }
  
  public long getScope (int index)
  {
      return this.scopes[index].pointer;
  }
  
  /**
   * @param fbreg_and_disp - Return ptr+disp.   Typically this is a static address or ptr+disp.
   */
  public void getAddr (long[] fbreg_and_disp)
  {
    get_addr(fbreg_and_disp, this.getPointer());
  }

  /**
   * @return The type die for the current die.
   */
  public DwarfDie getType ()
  {
    long type = get_type(this.getPointer());
    DwarfDie die = new DwarfDie(type, this.parent);
    return die;
  }

  /**
   * @return the scalar type for this type die.
   */
  public int getBaseType ()
  {
    return get_base_type(this.getPointer());
  }
  
  /**
   * @return The upper bound for this subrange die.
   */
  public int getUpperBound ()
  {
    return get_upper_bound(this.getPointer());
  }

  /**
   * @return True if die describes an array.
   */
  public boolean isArrayType()
  {
    return is_array_type(this.getPointer());
  }
  
  /**
   * @return True if die describes a class.
   */
  public boolean isClassType()
  {
    return is_class_type(this.getPointer());
  }
  
  /**
   * @return True if die describes a formal parameter
   */
  public boolean isFormalParameter()
  {
    return is_formal_parameter(this.getPointer());
  }
 
  /**
   * @return True if die describes a hidden parameter
   */
  public boolean isArtificial()
  {
    return is_artificial(this.getPointer());
  }
  
  /**
   * @return True if die describes an extern
   */
  public boolean isExternal()
  {
    return is_external(this.getPointer());
  }
  
  /**
   * @return The child for the current die.
   */
  public DwarfDie getChild ()
  {
    long child = get_child(this.getPointer());
    DwarfDie die = null;
    if (child != 0)
      die = new DwarfDie(child, this.parent);
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
      die = new DwarfDie(sibling, this.parent);
    return die;
  }
  
  protected long getPointer ()
  {
    return this.pointer;
  }
 
  /**
   * @param fbreg_and_disp Base pointer and displacement (out).
   * @param scope Scope DW_AT_frame_base is desired for.
   * @param pc PC DW_AT_frame_base is desired for.
   */
  public void getFrameBase (long[] fbreg_and_disp, long pc)
  {
    for (int i = this.scopeIndex; i < this.scopes.length; i++)
      {
        get_framebase(fbreg_and_disp, this.getPointer(), this.scopes[i].pointer, pc);
        if (fbreg_and_disp[0] != -1)
          break;
      }
  }

  /**
   * @param fbreg_and_disp Get DW_FORM_data for current die.
   * Typically this is from a location list.
   * @param scope - Scope of current die. 
   * @param pc - PC
   */
  public void getFormData (long[] fbreg_and_disp, long pc)
  {
    for (int i = this.scopeIndex; i < this.scopes.length; i++)
      {
        get_formdata(fbreg_and_disp, this.getPointer(), this.scopes[i].pointer, pc);
        if (fbreg_and_disp[0] != -1)
          break;
      }
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
        die = new DwarfDie(result, null);
        die.scopes = null;
        die.scopeIndex = 0;
      }
    return die;
  }

  private native long get_lowpc ();

  private native long get_highpc ();

  private native String get_diename ();
  
  private native String get_decl_file (long var_die);
  
  private native long get_decl_line (long var_die);
  
  private native long[] get_scopes (long addr);

  private native long get_scopevar (long[] die_scope, long[] scopes, String variable);

  private native void get_addr (long[] fbreg_and_disp, long addr);
  
  private native long get_type (long addr);
  
  private native long get_child (long addr);
  
  private native long get_sibling (long addr);
  
  private native int get_base_type (long addr);

  private native int get_upper_bound (long addr);
  
  private native boolean is_array_type (long addr);
  
  private native boolean is_class_type (long addr);
  
  private native boolean is_formal_parameter (long addr);
  
  private native boolean is_artificial (long addr);
  
  private native boolean is_external (long addr);
  
  private native void get_framebase (long[] fbreg_and_disp, long addr, long scope, long pc);

  private native void get_formdata (long[] fbreg_and_disp, long addr, long scope, long pc);
  
  private native boolean is_inline_func ();

  private static native long get_decl (long dw, String sym);
}
