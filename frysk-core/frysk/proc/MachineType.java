// This file is part of the program FRYSK.
//
// Copyright 2006 Red Hat Inc.
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

package frysk.proc;

import frysk.sys.Uname;

/**
 * Determine the machine type on which the program is running. This is
 * mostly intended for machine-specific tests.
 */
public class MachineType 
{
  private final String name;
  
  protected MachineType(String name) 
  {
    this.name = name;
  }

  public String getName() 
  {
    return name;
  }
  
  public static final MachineType IA32 = new MachineType("IA32");
  public static final MachineType X8664 = new MachineType("X8664");
  public static final MachineType PPC = new MachineType("PPC");
  public static final MachineType PPC64 = new MachineType("PPC64");

  /**
   * Exception type for unknown machines. This is a RuntimeException
   * because one would expect, most of the time, that tests will be
   * run on known machine types.
   */
  public static class UnknownMachineException extends RuntimeException 
  {
    private static final long serialVersionUID = 200607310000L;

    public UnknownMachineException(String s) 
    {
      super(s);
    }
  }
  
  /**
   * Return a constant describing the machine type.
   *
   * @return machine type constant
   */
  public static MachineType getMachineType()
  {
    Uname uname = Uname.get();
    String machine = uname.getMachine();
    
    if (machine.equals("i386")
	|| machine.equals("i486")
	|| machine.equals("i586")
	|| machine.equals("i686"))
      return IA32;
    else if (machine.equals("x86_64"))
      return X8664;
    else if (machine.equals("ppc"))
      return PPC;
    else if (machine.equals("ppc64"))
      return PPC64;
    else
      throw new UnknownMachineException(machine);
  }
}

	
    