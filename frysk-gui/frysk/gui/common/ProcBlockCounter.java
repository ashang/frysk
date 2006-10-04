// This file is part of the program FRYSK.
//
// Copyright 2005, Red Hat Inc.
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

package frysk.gui.common;

import java.util.Hashtable;

import frysk.proc.Proc;

/**
 * Used as kind of semaphore to co-ordinate the un-blocking of a Proc between
 * different Objects which may be using the same blocked Proc - the Register/
 * Memory/Disassembly/SourceWindows.
 * 
 * @author mcvet
 */
public class ProcBlockCounter
{
  
  /* Keep track how many Objects are watching each Task */
  public static Hashtable blockTable = new Hashtable();
  
  /**
   * Return the number of blockers on this incoming Proc.
   * 
   * @param proc    The Proc to check for blockers.
   * @return    The number of blockers.
   */
  public static int getBlockCount(Proc proc)
  {
    Integer i = (Integer)blockTable.get(proc);
    if (i == null)
      return 0;
    else
      return i.intValue();
  }
  
  /**
   * Increment the number of blockers for the incoming Proc - called when a
   * new window has been created by a factory.
   * 
   * @param proc    The Proc whose number of blockers is incremented by one.
   */
  public static void incBlockCount(Proc proc)
  {
    Integer i = (Integer)blockTable.get(proc);
    if (i == null)
      blockTable.put(proc, new Integer(1));
    else
      blockTable.put(proc, new Integer(i.intValue() + 1));
  }
  
  /**
   * Decrement the number of blockers for the incoming Proc - called when a 
   * window has been destroyed.
   * 
   * @param proc    The Proc whose number of blockers is decremented by one.
   */
  public static void decBlockCount(Proc proc)
  {
    Integer i = (Integer)blockTable.get(proc);
    if (i != null)
      blockTable.put(proc, new Integer(i.intValue() - 1));
  }
  
}
