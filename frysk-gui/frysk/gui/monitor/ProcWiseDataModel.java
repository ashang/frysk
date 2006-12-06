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

import java.util.ArrayList;
import java.util.Hashtable;
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

/**
 * A data model that groups PID's by executable name.
 * Also has a selected component that allows the druid to define whether
 * a process is selected
 */
public class ProcWiseDataModel
{

  private TreeStore treeStore;

  private DataColumnString nameDC;

  private DataColumnInt childCountDC;

  private DataColumnObject objectDC;

  private DataColumnBoolean selectedDC;

  private DataColumnBoolean sensitiveDC;

  private ProcCreatedObserver procCreatedObserver;

  private ProcDestroyedObserver procDestroyedObserver;

  // private TimerEvent refreshTimer;

  private Hashtable iterHash;

  private Logger errorLog = Logger.getLogger(Gui.ERROR_LOG_ID);

  public ProcWiseDataModel ()
  {
    this.iterHash = new Hashtable();

    this.nameDC = new DataColumnString();
    this.objectDC = new DataColumnObject();
    this.selectedDC = new DataColumnBoolean();
    this.sensitiveDC = new DataColumnBoolean();
    this.childCountDC = new DataColumnInt();

    this.treeStore = new TreeStore(new DataColumn[] { this.nameDC,
                                                     this.objectDC,
                                                     this.selectedDC,
                                                     this.sensitiveDC,
                                                     this.childCountDC });

    this.procCreatedObserver = new ProcCreatedObserver();
    this.procDestroyedObserver = new ProcDestroyedObserver();

    Manager.host.observableProcAddedXXX.addObserver(this.procCreatedObserver);
    Manager.host.observableProcRemovedXXX.addObserver(this.procDestroyedObserver);

  }

  public void unFilterData() {
	  TreeIter iter = treeStore.getFirstIter();
	  do {
		  if (treeStore.isIterValid(iter))
			  treeStore.setValue(iter, selectedDC, false);
		  iter = iter.getNextIter();
	  } while (iter != null);
	  
  }
  private void setRow (TreeIter row, String name, GuiProc data, int childcount,
                       boolean selected)
  {
    try
      {
        treeStore.setValue(row, nameDC, name);
        treeStore.setValue(row, objectDC, data);
        treeStore.setValue(row, selectedDC, selected);
        treeStore.setValue(row, childCountDC, childcount);
        treeStore.setValue(row, sensitiveDC, false);
      }
    catch (Exception e)
      {
        errorLog.log(Level.WARNING,
                     "ProcWiseDataModel.setRow: unable to store value: " + name
                         + " in treeStore");
        return;
      }
    if (data != null)
      if (data.isOwned())
        {
          treeStore.setValue(row, sensitiveDC, true);
          TreePath path = row.getPath();

          if (path != null)
            {
              String pathString = path.toString();
              if (pathString.contains(":"))
                {
                  String parentString = pathString.substring(
                                                             0,
                                                             pathString.lastIndexOf(":"));
                  TreeIter parent = null;
                  parent = treeStore.getIter(parentString);

                  if (parent != null)
                    if (treeStore.isIterValid(parent))
                      treeStore.setValue(parent, sensitiveDC, true);
                }
            }
          else
            {
              errorLog.log(Level.WARNING,
                           "ProcWiseDataModel.setRow: Cannot discern path from TreePath");
              return;
            }
        }
  }

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

  public DataColumnString getNameDC ()
  {
    return nameDC;
  }

  public DataColumnObject getObjectDC ()
  {
    return objectDC;
  }

  public DataColumnBoolean getSelectedDC ()
  {
    return selectedDC;
  }

  public DataColumnBoolean getSensitiveDC ()
  {
    return sensitiveDC;
  }

  public void setSelected (TreeIter iter, boolean type, boolean setChildren)
  {
    if (iter != null)
      if (treeStore.isIterValid(iter))
        {
          treeStore.setValue(iter, getSelectedDC(), type);
          if (setChildren)
            {
              int children = iter.getChildCount();
              for (int count = 0; count < children; count++)
                if (iter.getChild(count) != null)
                  if (treeStore.isIterValid(iter.getChild(count)))
                    treeStore.setValue(iter.getChild(count), getSelectedDC(),
                                       type);
            }
        }
  }

  public ArrayList dumpSelectedProcesses ()
  {

    ArrayList processData = new ArrayList();
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
          TreeIter parent = null;
          try
            {
              guiProc = GuiProc.GuiProcFactory.getGuiProc(proc);
            }
          catch (Exception e)
            {
              errorLog.log(Level.WARNING,
                           "ProcWiseDataModel.ProcCreatedObserver: Cannot "
                               + "get proc: " + proc + " from factory");
              return;
            }

          if (guiProc != null)
            {
              try
                {
                  parent = (TreeIter) iterHash.get(guiProc.getExecutableName());
                }
              catch (Exception e)
                {
                  errorLog.log(Level.WARNING,
                               "ProcWiseDataModel.ProcCreatedObserver: Cannot "
                                   + "get proc: " + proc + " from hash");
                  return;
                }
            }
          else
            {
              errorLog.log(Level.WARNING,
                           "ProcWiseDataModel.ProcCreatedObserver: GuiProc == null");
              return;
            }

          if (parent == null)
            {
              // new process name
              parent = treeStore.appendRow(null);
              if (parent != null)
                {
                  iterHash.put(guiProc.getExecutableName(), parent);
                  setRow(parent, guiProc.getExecutableName(), guiProc, 1, false);
                }
            }
          else
            {
              int childCount = treeStore.getValue(parent, childCountDC);
              childCount++;
              treeStore.setValue(parent, childCountDC, childCount);
              String parentName = treeStore.getValue(parent, nameDC);
              String[] comp = parentName.split("\\(");
              String processVerb = "process";
              processVerb = "processes";
              parentName = comp[0].trim() + " (" + childCount + " "
                           + processVerb + ")";

              treeStore.setValue(parent, nameDC, parentName);
            }
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
          GuiProc guiProc = null;
          TreeIter parent = null;
          try
            {
              guiProc = GuiProc.GuiProcFactory.getGuiProc(proc);
            }
          catch (Exception e)
            {
              errorLog.log(Level.WARNING,
                           "ProcWiseDataModel.ProcDestroyedObserver: Cannot "
                               + "get proc: " + proc + " from factory");
              return;
            }
          if (guiProc != null)
            {
              try
                {
                  parent = (TreeIter) iterHash.get(guiProc.getExecutableName());
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
                               + "Not found in TreeIter HasTable. Cannot be removed");
              return;
            }

          if (! treeStore.isIterValid(parent))
            {
              errorLog.log(
                           Level.WARNING,
                           "ProcWiseDataModel.ProcDestroyedObserver: TreeIter has parent, but isIterValid returns false.");
              return;
            }

          int n = treeStore.getValue(parent, childCountDC);

          if (n == 1)
            {
              try
                {
                  treeStore.removeRow(parent);
                  iterHash.remove(guiProc.getExecutableName());
                }
              catch (Exception e)
                {
                  errorLog.log(Level.WARNING,
                               "ProcWiseDataModel.ProcDestroyedObserver: Cannot remove parents' children");
                }
              return;
            }

          if (n > 1)
            {

              int childCount = treeStore.getValue(parent, childCountDC);
              childCount--;
              treeStore.setValue(parent, childCountDC, childCount);
              String parentName = treeStore.getValue(parent, nameDC);
              String[] comp = parentName.split("\\(");
              String processVerb = "process";
              //if (childCount > 1)
              processVerb = "processes";

              if (childCount > 1)
            	  parentName = comp[0].trim() + " (" + childCount + " "
            	  		+ processVerb + ")";
              else
            	  parentName = comp[0].trim();

              treeStore.setValue(parent, nameDC, parentName);

            }

        }
      });
    }
  }

  public TreeStore getModel ()
  {
    return this.treeStore;
  }

}
