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


package frysk.dwfl;

import java.util.Iterator;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import frysk.proc.Auxv;
import frysk.proc.MemoryMap;
import frysk.proc.Proc;
import frysk.proc.Task;
import lib.dw.Dwfl;
import lib.dw.DwflModule;

/**
 * Factory for creating Dwfl objects for Procs and Tasks.
 */
public class DwflFactory
{
  static Logger logger = Logger.getLogger("frysk");

  /**
   * Used to cache Dwfl objects per proc.
   */
  private static WeakHashMap dwflMap = new WeakHashMap();
  
  /**
   * Used to link tasks to proc based dwfls and
   */
  private static WeakHashMap taskMap = new WeakHashMap();
  
  /**
   * Check whether a given {@link frysk.proc.MemoryMap} from a
   * {@link frysk.proc.Proc} refers to the vdso section.
   * 
   * @param proc the {@link frysk.proc.Proc} the map refers to.
   * @param map the Map to check.
   * @return true if map is not null and refers to the vdso section.
   */
  public static boolean isVDSO (Proc proc, MemoryMap map)
  {
    if (map == null)
      return false;
    return VDSOAddressLow(proc) == map.addressLow;
  }

  /**
   * Find the low address of the vdso map section of a given
   * {@link frysk.proc.Proc}.
   * 
   * @param proc the given {@link frysk.proc.Proc}.
   * @return the low address of the vdso map section of proc.
   */
  private static long VDSOAddressLow (Proc proc)
  {
    Auxv[] auxv = proc.getAuxv();
    for (int i = 0; i < auxv.length; i++)
      {
        if (auxv[i].type == inua.elf.AT.SYSINFO_EHDR)
          return auxv[i].val;
      }
    logger.log(Level.FINE, "Couldn't get vdso address\n");
    return 0;
  }

  /**
   * Check if a given {@link frysk.proc.MemoryMap} does not refer to an elf
   * image.
   * 
   * @param map the given {@link frysk.proc.MemoryMap}.
   * @return true if the map section does not refer to an elf image.
   */
  private static boolean isEmptyMap (MemoryMap map)
  {
    return map.name.equals("")
           || (map.inode == 0 && map.devMinor == 0 && map.devMajor == 0);
  }

  private static Dwfl doDwfl (Proc proc)
  {
    MemoryMap[] maps = proc.getMaps();

    Dwfl dwfl = new Dwfl();
    dwfl.dwfl_report_begin();

    int count = 0;
    String name = null;
    long low = 0, high = 0, devMinor = 0, devMajor = 0;
    int inode = 0;

    // Creating Dwfl_Modules for each elf image and the vdso section.
    // Condensing elf mappings into a single Dwfl_Module per elf image.

    // Base case:
    // While the map is empty skip.
    while (! isVDSO(proc, maps[count]) && isEmptyMap(maps[count]))
      count++;
    // If map represents the vdso section, report vdso.
    if (isVDSO(proc, maps[count]))
      {
        logger.log(Level.FINE, "Found the vdso!\n");
        dwfl.dwfl_report_module(maps[count].name, maps[count].addressLow,
                                maps[count].addressHigh);
      }
    // If map represents an elf mapping store its data..
    else
      {
        name = maps[count].name;
        low = maps[count].addressLow;
        high = maps[count].addressHigh;
        inode = maps[count].inode;
        devMinor = maps[count].devMinor;
        devMajor = maps[count].devMajor;
      }

    // Induction Step:
    while (++count < maps.length)
      {

        // if vdso report old (if old), flush old, then report vdso.
        if (isVDSO(proc, maps[count]))
          {
            if (name != null)
              dwfl.dwfl_report_module(name, low, high);

            name = null;
            dwfl.dwfl_report_module(maps[count].name, maps[count].addressLow,
                                    maps[count].addressHigh);
            continue;
          }
        // if empty, report old (if old), flush old.
        else if (isEmptyMap(maps[count]))
          {
            if (name != null)
              dwfl.dwfl_report_module(name, low, high);

            name = null;
            continue;
          }
        // if old elf, increase highAddress.
        else if (maps[count].name.equals(name) && maps[count].inode == inode
            && maps[count].devMinor == devMinor
            && maps[count].devMajor == devMajor)
          high = maps[count].addressHigh;

        // if new elf, report old, store new
        else
          {
            if (name != null)
              dwfl.dwfl_report_module(name, low, high);

            name = maps[count].name;
            low = maps[count].addressLow;
            high = maps[count].addressHigh;
            inode = maps[count].inode;
            devMinor = maps[count].devMinor;
            devMajor = maps[count].devMajor;
          }
      }

    // if last is elf, report elf.
    if (! isEmptyMap(maps[maps.length - 1])
        && ! isVDSO(proc, maps[maps.length - 1]))
      dwfl.dwfl_report_module(name, low, high);

    dwfl.dwfl_report_end();
    DwflModule module = dwfl.getModule(VDSOAddressLow(proc));

    logger.log(Level.FINE, "Main task {0}", proc.getMainTask());
    logger.log(Level.FINE, "Memory {0}", proc.getMainTask().getMemory());
    logger.log(Level.FINE, "Dwfl module: {0}\n", module);
    if (module != null)
      module.setUserData(proc.getMainTask().getMemory());

    return dwfl;
  }
  
  /**
   * Create a Dwfl for a {@link frysk.proc.Proc}
   * 
   * @param proc the given {@link frysk.proc.Proc}.
   * @return a Dwfl created with proc's maps.
   */
  public static Dwfl createDwfl (Proc proc)
  {
    if (dwflMap.containsKey(proc.getId()))
      return (Dwfl) dwflMap.get(proc.getId());

    Dwfl dwfl = doDwfl(proc);
    
    Iterator iter = proc.getTasks().iterator();
    
    while (iter.hasNext())
      {
        Task task = (Task) iter.next();
        taskMap.put(task, new Integer(task.getMod()));
      }
    
    dwflMap.put(proc, dwfl);
    return dwfl;
  }

  /**
   * Create a Dwfl for a {@link frysk.proc.Task}.
   * 
   * @param task the given {@link frysk.proc.Task}.
   * @return a Dwfl created using the tasks maps.
   */
  public static Dwfl createDwfl (Task task)
  {
    /* Check if this task has changed since (if) a dwfl was last created.
     * If it hasn't changed returned the cached dwfl. 
     * If it has changed recreate the dwfl and update the maps.
     */
    if (taskMap.containsKey(task))
      {
        Integer count = (Integer) taskMap.get(task);
        if (count.intValue() == task.getMod())      
            return (Dwfl) dwflMap.get(task.getProc());
          
      }

    taskMap.put(task, new Integer(task.getMod()));
    if (dwflMap.containsKey(task.getProc()))
      dwflMap.remove(task.getProc());
    
    return createDwfl(task.getProc());
  }

  /**
   * Clear a Dwfl created for a {@link frysk.proc.Proc}. (Example: after an
   * exec.)
   * 
   * @param proc the given {@link frysk.proc.Proc}.
   */
  public static void clearDwfl (Proc proc)
  {
    if (dwflMap.containsKey(proc.getId()))
      dwflMap.remove(proc.getId());
  }

  /**
   * Clear a Dwfl created for a {@link frysk.proc.Task}. (Example: after an
   * exec.)
   * 
   * @param task the given {@link frysk.proc.Task}.
   */
  public static void clearDwfl (Task task)
  {
    clearDwfl(task.getProc());
  }

}
