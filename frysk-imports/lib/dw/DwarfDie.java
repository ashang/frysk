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
  
  private long scope;

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
        // new DwarfDie(die_and_scope[1], this.parent);
        die.scope = die_and_scope[1];
      }
    return die;
  }
    
  public long getScope ()
  {
      return this.scope;
  }
  
  public long getAddr ()
  {
    return get_addr(this.getPointer());
  }

  public String getType ()
  {
    return get_type(this.getPointer());
  }
  
  protected long getPointer ()
  {
    return this.pointer;
  }
 
  public boolean fbregVariable ()
  {
    long is_fb = fbreg_variable (this.getPointer());
    if (is_fb == 1)
      return true;
    else
      return false;
  }

  public long getFrameBase (long scope, long pc)
  {
    return get_framebase(this.getPointer(), scope, pc);
  }

  
  // protected native long dwarf_diecu();
  private native long get_lowpc ();

  private native long get_highpc ();

  private native String get_diename ();
  
  private native String get_decl_file (long var_die);
  
  private native long get_decl_line (long var_die);
  
  private native long[] get_scopes (long addr);

  private native long get_scopevar (long[] die_scope, long[] scopes, String variable);

  private native long get_addr (long addr);
  
  private native String get_type (long addr);
  
  private native long fbreg_variable (long addr);
  
  private native long get_framebase (long addr, long scope, long pc);
}
