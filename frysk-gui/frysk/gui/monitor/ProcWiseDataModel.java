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
// type filter text
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


package frysk.gui.monitor;

import java.util.LinkedList;
import java.util.HashMap;
import java.util.Observable;
import java.util.Observer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.gnu.gtk.DataColumn;
import org.gnu.gtk.DataColumnBoolean;
import org.gnu.gtk.DataColumnInt;
import org.gnu.gtk.DataColumnObject;
import org.gnu.gtk.DataColumnString;
import org.gnu.gtk.TreeIter;
import org.gnu.gtk.TreePath;
import org.gnu.gtk.TreeStore;

import frysk.gui.Gui;
import frysk.proc.Manager;
import frysk.proc.Proc;
import frysk.sys.proc.Stat;

/**
 * A data model that groups PID's by executable name.
 * Also has a selected component that allows the druid to define whether
 * a process is selected
 */
public class ProcWiseDataModel
{

  private TreeStore treeStore;

  private DataColumnString nameDC;

  private DataColumnString locationDC;

  private DataColumnInt pidDC;
  
  private DataColumnString vszDC;
  
  private DataColumnString rssDC;
  
  private DataColumnString timeDC;

  private DataColumnObject objectDC;

  private DataColumnBoolean selectedDC;

  private DataColumnBoolean sensitiveDC;

  private ProcCreatedObserver procCreatedObserver;

  private ProcDestroyedObserver procDestroyedObserver;

  // private TimerEvent refreshTimer;

  private HashMap iterMap;

  private Logger errorLog = Logger.getLogger(Gui.ERROR_LOG_ID);
  
  private Stat stat;

  public ProcWiseDataModel ()
  {
    this.iterMap = new HashMap();

    this.nameDC = new DataColumnString();
    this.locationDC = new DataColumnString();
    this.pidDC = new DataColumnInt();
    this.vszDC = new DataColumnString();
    this.rssDC = new DataColumnString();
    this.timeDC = new DataColumnString();
    this.objectDC = new DataColumnObject();
    this.selectedDC = new DataColumnBoolean();
    this.sensitiveDC = new DataColumnBoolean();

    this.treeStore = new TreeStore(new DataColumn[] { this.nameDC,
                                                     this.locationDC,
                                                     this.pidDC, this.vszDC,
                                                     this.rssDC, this.timeDC,
                                                     this.objectDC,
                                                     this.selectedDC,
                                                     this.sensitiveDC });
    

    this.procCreatedObserver = new ProcCreatedObserver();
    this.procDestroyedObserver = new ProcDestroyedObserver();

    Manager.host.observableProcAddedXXX.addObserver(this.procCreatedObserver);
    Manager.host.observableProcRemovedXXX.addObserver(this.procDestroyedObserver);
    
    this.stat = new Stat();
  }

  /**
   * Run through the model and set all selectedDCs to false.
   */
  public void unFilterData ()
  {
    TreeIter iter = treeStore.getFirstIter();
    do
      {
        if (treeStore.isIterValid(iter))
          treeStore.setValue(iter, selectedDC, false);
        iter = iter.getNextIter();
      }
    while (iter != null);
  }

  /**
   * Return the first TreePath whose DebugProcess matches the parameter
   * String.
   */
  public TreePath searchName (String name)
  {
    TreeIter iter = treeStore.getFirstIter();
    while (iter != null)
      {
        if (treeStore.isIterValid(iter))
          {
            String split[] = treeStore.getValue(iter, getNameDC()).split("\t");
            if (split.length > 0)
              {
                split[0] = split[0].trim();
                if (split[0].split(" ")[0].equalsIgnoreCase(name))
                  return iter.getPath();
              }
          }
        iter = iter.getNextIter();
      }
    return null;
  }
  
  /**
   * Dump all objectDCs whose name match the String parameter into the
   * given LinkedList.
   */
  public void collectProcs (String name, LinkedList procs)
  {
    TreeIter iter = treeStore.getFirstIter();
    while (iter != null)
      {
        if (treeStore.isIterValid(iter))
          {
            String split[] = treeStore.getValue(iter, getNameDC()).split("\t");
            if (split.length > 0)
              {
                split[0] = split[0].trim();
                if (split[0].split(" ")[0].equalsIgnoreCase(name))
                  procs.add(treeStore.getValue(iter, getObjectDC()));
              }
          }
        iter = iter.getNextIter();
      }
  }
  
  /**
   * Create a new LinkedList and put all objectDCs whose name match
   * the String parameter into it.
   */
  public LinkedList searchAllNames (String name)
  {
    LinkedList treePaths = new LinkedList();
    
    TreeIter iter = treeStore.getFirstIter();
    while (iter != null)
      {
        if (treeStore.isIterValid(iter))
          {
            String split[] = treeStore.getValue(iter, getNameDC()).split("\t");
            if (split.length > 0)
              {
                split[0] = split[0].trim();
                if (split[0].split(" ")[0].equalsIgnoreCase(name))
                  treePaths.add(iter.getPath());
              }
          }
        iter = iter.getNextIter();
      }
    return treePaths;
  }
  
  /**
   * Search the model for objectDCs whose PID matches the parameter.
   */
  public TreePath searchPid (int pid)
  {
    TreeIter iter = treeStore.getFirstIter();
    int p = 0;
    while (iter != null)
      {
        if (treeStore.isIterValid(iter))
          {
            p = treeStore.getValue(iter, this.pidDC);
            if (pid == p)
              {
                return iter.getPath();
              }
          }
        iter = iter.getNextIter();
      }
    return null;
  }

  public void setSelected (TreeIter iter, boolean type)
  {
    if (iter != null)
      if (treeStore.isIterValid(iter))
        {
          treeStore.setValue(iter, getSelectedDC(), type);
        }
  }

  public LinkedList dumpSelectedProcesses ()
  {

    LinkedList processData = new LinkedList();
    // TODO: Very unsafe (process might be deleted by observers
    // behind the scenes. Rewrite
    for (int i = 0; true; i++)
      {
        TreeIter item = treeStore.getIter(new Integer(i).toString());
        if (item == null)
          break;

        if (treeStore.isIterValid(item))
          {
            // We only care about process groups, so top level run only.
            if (treeStore.getValue(item, selectedDC) == true)
              processData.add(treeStore.getValue(item, nameDC));
          }
      }

    return processData;
  }

  class ProcCreatedObserver
      implements Observer
  {
    public void update (Observable o, Object obj)
    {
      final Proc proc = (Proc) obj;

      org.gnu.glib.CustomEvents.addEvent(new Runnable()
      {
        public void run ()
        {
          GuiProc guiProc = null;
          
          try
          {
            guiProc = GuiProc.GuiProcFactory.getGuiProc(proc);
          }
          catch (Exception e)
          {
            errorLog.log(Level.WARNING, "ProcWiseDataModel.ProcCreatedObserver: Cannot get " + proc + " from factory");
          }
          
          if (guiProc == null)
            {
              errorLog.log(Level.WARNING,
                           "ProcWiseDataModel.ProcCreatedObserver: GuiProc == null");
              return;
            }
          
          if (!guiProc.isOwned())
            return;
          
          TreeIter parent = (TreeIter) iterMap.get(proc.getId());

          if (parent == null)
            {
              // new process name
              parent = treeStore.appendRow(null);
              if (parent != null)
                iterMap.put(proc.getId(), parent);
            }
          
              treeStore.setValue(parent, nameDC, guiProc.getExecutableName());
              treeStore.setValue(parent, locationDC, guiProc.getNiceExecutablePath());
              treeStore.setValue(parent, pidDC, proc.getPid());
              treeStore.setValue(parent, selectedDC, false);
              treeStore.setValue(parent, sensitiveDC, false);
              treeStore.setValue(parent, objectDC, guiProc);

              ProcWiseDataModel.this.stat.refresh(proc.getPid());
              treeStore.setValue(parent, vszDC, "" + (stat.vsize / 1024));
              treeStore.setValue(parent, rssDC, "" + (stat.rss * 4));
              treeStore.setValue(parent, selectedDC, false);
              
              long t = (stat.cstime + stat.cutime + stat.stime + stat.utime) / 100;
              
              long sec = t % 60;
              long min = t / 60;
              
              if (sec < 10)
                treeStore.setValue(parent, timeDC, min + ":0" + sec);
              else
                treeStore.setValue(parent, timeDC, min + ":" + sec);
        }
      });
    }
  }

  class ProcDestroyedObserver
      implements Observer
  {
    public void update (Observable o, Object obj)
    {
      final Proc proc = (Proc) obj;

      org.gnu.glib.CustomEvents.addEvent(new Runnable()
      {
        public void run ()
        {
          TreeIter parent = null;

          if (proc != null)
            {
              try
                {
                  parent = (TreeIter) iterMap.get(proc.getId());
                }
              catch (Exception e)
                {
                  errorLog.log(Level.WARNING,
                               "ProcWiseDataModel.ProcDestroyedObserver: Cannot "
                                   + "get proc: " + proc + " from hash");
                  return;
                }
            }
          else
            {
              errorLog.log(Level.WARNING,
                           "ProcWiseDataModel.ProcDestroyedObserver: GuiProc == null");
              return;
            }

          if (parent == null)
            {
              errorLog.log(
                           Level.WARNING,
                           "ProcWiseDataModel.ProcDestroyedObserver: proc "
                               + proc
                               + "Not found in TreeIter HashMap. Cannot be removed");
              return;
            }

          if (! treeStore.isIterValid(parent))
            {
              errorLog.log(
                           Level.WARNING,
                           "ProcWiseDataModel.ProcDestroyedObserver: TreeIter has parent, but isIterValid returns false.");
              return;
            }

          treeStore.removeRow(parent);
          iterMap.remove(proc.getId());

          return;
        }

      });
    }
  }

  public TreeStore getModel ()
  {
    return this.treeStore;
  }
  
  public DataColumnString getNameDC ()
  {
    return this.nameDC;
  }
  
  public DataColumnString getLocationDC ()
  {
    return this.locationDC;
  }
  
  public DataColumnInt getPIDDC ()
  {
    return this.pidDC;
  }
  
  public DataColumnString getVszDC ()
  {
    return this.vszDC;
  }
  
  public DataColumnString getRssDC ()
  {
    return this.rssDC;
  }
  
  public DataColumnString getTimeDC ()
  {
    return this.timeDC;
  }
  
  public DataColumnObject getObjectDC ()
  {
    return this.objectDC;
  }
  
  public DataColumnBoolean getSelectedDC ()
  {
    return this.selectedDC;
  }

  public DataColumnBoolean getSensitiveDC ()
  {
    return this.sensitiveDC;
  }

}
