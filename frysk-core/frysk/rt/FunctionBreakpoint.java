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


package frysk.rt;

import java.util.ArrayList;
import java.util.LinkedList;

import lib.dw.DwarfDie;
import lib.dw.Dwfl;
import lib.dw.DwflModule;
import lib.dw.SymbolBuilder;
import lib.dw.die.InlinedSubroutine;

import frysk.proc.Proc;

public class FunctionBreakpoint
  extends SourceBreakpoint
{
  private String name;
  private boolean containsInlineInstances = false;

  /**
   * Set a breakpoint based on a DwarfDie.
   */
  public FunctionBreakpoint(Proc proc, String name, DwarfDie die)
  {
    this.name = name;
    ArrayList entryAddrs = die.getEntryBreakpoints();
    ArrayList inlineDies = null;
    if (die.isInlineDeclaration())
      {
        inlineDies = die.getInlinedInstances();
      }
    LinkedList addrs;
    if (entryAddrs == null)
      addrs = new LinkedList();
    else
      addrs = new LinkedList(entryAddrs);
    if (inlineDies != null)
      {
        addrs.addAll(inlineDies);
        containsInlineInstances = true;
      }
    setAddrs(proc, addrs);
  }

  /**
   * Set a breakpoint based on a name which will be looked up in the
   * Elf symbol table.
   */
  public FunctionBreakpoint(Proc proc, final String name)
  {
    this.name = name;
    Dwfl dwfl = new Dwfl(proc.getPid()); // XXX cache Dwfls somewhere?
    DwflModule[] modules = dwfl.getModules();
    final LinkedList addrs = new LinkedList();
    SymbolBuilder builder = new SymbolBuilder() {
	public void symbol(String name, long value, long size, int type,
			   int bind, int visibility)
	{
	  addrs.add(new Long(value));
	}
      };
    for (int i = 0; i < modules.length; i++)
      {
	DwflModule module = modules[i];
	module.getSymbolByName(name, builder);
      }
    if (addrs.size() == 0)
      throw new RuntimeException("Couldn't find symbol " + name);
    else
      setAddrs(proc, addrs);
  }

  public long getRawAddress(Object addr)
  {
    if (addr instanceof Long)
      return ((Long)addr).longValue();
    else if (addr instanceof InlinedSubroutine)
      return ((InlinedSubroutine)addr).getLowPC();
    else
      throw new RuntimeException("Can't get address from " + addr.toString());
  }

  public String getName()
  {
    return name;
  }

  public boolean containsInlineInstances()
  {
    // XXX What about in different processes?
    return containsInlineInstances;
  }
}
