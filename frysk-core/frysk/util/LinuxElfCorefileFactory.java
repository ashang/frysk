//This file is part of the program FRYSK.

//Copyright 2007, Red Hat Inc.

//FRYSK is free software; you can redistribute it and/or modify it
//under the terms of the GNU General Public License as published by
//the Free Software Foundation; version 2 of the License.

//FRYSK is distributed in the hope that it will be useful, but
//WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
//General Public License for more details.

//You should have received a copy of the GNU General Public License
//along with FRYSK; if not, write to the Free Software Foundation,
//Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.

//In addition, as a special exception, Red Hat, Inc. gives You the
//additional right to link the code of FRYSK with code not covered
//under the GNU General Public License ("Non-GPL Code") and to
//distribute linked combinations including the two, subject to the
//limitations in this paragraph. Non-GPL Code permitted under this
//exception must only link to the code of FRYSK through those well
//defined interfaces identified in the file named EXCEPTION found in
//the source code files (the "Approved Interfaces"). The files of
//Non-GPL Code may instantiate templates or use macros or inline
//functions from the Approved Interfaces without causing the
//resulting work to be covered by the GNU General Public
//License. Only Red Hat, Inc. may make changes or additions to the
//list of Approved Interfaces. You must obey the GNU General Public
//License in all respects for all of the FRYSK code and other code
//used in conjunction with FRYSK except the Non-GPL Code covered by
//this exception. If you modify this file, you may extend this
//exception to your version of the file, but you are not obligated to
//do so. If you do not wish to provide this exception without
//modification, you must delete this exception statement from your
//version and license this file solely under the GPL without
//exception.

package frysk.util;


import frysk.proc.Isa;
import frysk.proc.Proc;
import frysk.proc.Task;

public class LinuxElfCorefileFactory
{

  public static LinuxElfCorefile getCorefile(Proc process, Task blockedTasks[]) 
  {
    // Get machine architecture
    String arch_test = getArch(process);

    if (arch_test.equals("frysk.proc.LinuxIa32") || arch_test.equals("frysk.proc.LinuxIa32On64"))
	return new LinuxElfCorefilex86(process, blockedTasks);
    if (arch_test.equals("frysk.proc.LinuxX8664"))
	return new LinuxElfCorefilex8664(process, blockedTasks);
    if (arch_test.equals("frysk.proc.LinuxPPC32"))
	return new LinuxElfCorefilePPC32(process, blockedTasks);
    if (arch_test.equals("frysk.proc.LinuxPPC64"))
	return new LinuxElfCorefilePPC64(process, blockedTasks);
    if (arch_test.equals("frysk.proc.LinuxPPC32On64"))
	return new LinuxElfCorefilePPC32on64(process, blockedTasks);

    return null;
  }

  /**
   * Function to return a string denoting architecture name.
   * 
   * @return String describe architecture.
   */
  private static String getArch (Proc proc)
  {

    // XXX: I hate this, there must be a better way to get architecture than
    // this ugly, ugly hack

    Isa arch = null;
    arch = proc.getMainTask().getIsa();

    String arch_test = arch.toString();
    String type = arch_test.substring(0, arch_test.lastIndexOf("@"));

    return type;
  }

}