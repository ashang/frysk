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


package frysk.gui.monitor;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Observable;
import java.util.Observer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.gnu.glib.CustomEvents;
import org.gnu.gtk.DataColumn;
import org.gnu.gtk.DataColumnBoolean;
import org.gnu.gtk.DataColumnInt;
import org.gnu.gtk.DataColumnObject;
import org.gnu.gtk.DataColumnString;
import org.gnu.gtk.TreeIter;
import org.gnu.gtk.TreeModel;
import org.gnu.gtk.TreeStore;
import org.gnu.pango.Weight;

import frysk.event.TimerEvent;
import frysk.gui.Gui;
import frysk.gui.monitor.GuiProc.GuiProcFactory;
import frysk.gui.monitor.GuiTask.GuiTaskFactory;
import frysk.gui.sessions.DebugProcess;
import frysk.gui.sessions.Session;
import frysk.proc.Manager;
import frysk.proc.Proc;
import frysk.proc.Task;
import frysk.proc.ProcTasksObserver;
import frysk.proc.ProcObserver.ProcTasks;
import frysk.sys.proc.Stat;

public class SessionProcDataModel
{

  private TreeStore treeStore;

  DataColumn[] columns;

  private DataColumnInt pidDC;

  private DataColumnString commandDC;

  private DataColumnString colorDC;

  private DataColumnObject procDataDC;

  private DataColumnInt weightDC;

  private DataColumnInt threadParentDC;

  private DataColumnBoolean isThreadDC;

  private DataColumnBoolean sensitiveDC;
  
  private DataColumnString vszDC;
  
  private DataColumnString rssDC;
  
  private DataColumnString timeDC;
  
  private DataColumnString ppidDC;
  
  private DataColumnString stateDC;
  
  private DataColumnString niceDC;
  
  private Stat stat;

  private HashMap iterHash;

  private TimerEvent refreshTimer;

  private Logger errorLog = Logger.getLogger(Gui.ERROR_LOG_ID);

  /**
   * { Local Observers
   */
  // private ProcCreatedObserver procCreatedObserver;
  // private ProcDestroyedObserver procDestroyedObserver;
  // private TaskCreatedObserver taskCreatedObserver;
  // private TaskDestroyedObserver taskDestroyedObserver;
  /** } */

  private Session currentSession;

  public SessionProcDataModel() throws IOException
  {

    this.pidDC = new DataColumnInt();
    this.commandDC = new DataColumnString();
    this.colorDC = new DataColumnString();
    this.procDataDC = new DataColumnObject();
    this.weightDC = new DataColumnInt();
    this.isThreadDC = new DataColumnBoolean();
    this.threadParentDC = new DataColumnInt();
    this.sensitiveDC = new DataColumnBoolean();
    this.vszDC = new DataColumnString();
    this.rssDC = new DataColumnString();
    this.timeDC = new DataColumnString();
    this.ppidDC = new DataColumnString();
    this.stateDC = new DataColumnString();
    this.niceDC = new DataColumnString();
    this.stat = new Stat();

    this.treeStore = new TreeStore(new DataColumn[] { pidDC, commandDC,
                                                     colorDC, procDataDC,
                                                     weightDC, threadParentDC,
                                                     isThreadDC, sensitiveDC, 
                                                     vszDC, rssDC, timeDC,
                                                     ppidDC, stateDC, niceDC});

    // Change to HashMap from HashTable
    this.iterHash = new HashMap();

    // this.procCreatedObserver = new ProcCreatedObserver();
    // this.procDestroyedObserver = new ProcDestroyedObserver();
    // this.taskCreatedObserver = new TaskCreatedObserver();
    // this.taskDestroyedObserver = new TaskDestroyedObserver ();

    // Manager.host.observableProcAddedXXX.addObserver(this.procCreatedObserver);
    // Manager.host.observableProcRemovedXXX.addObserver(this.procDestroyedObserver);
    // Manager.host.observableTaskAddedXXX.addObserver (taskCreatedObserver);
    // Manager.host.observableTaskRemovedXXX.addObserver
    // (taskDestroyedObserver);
  }
  
  public TreeStore getTreeStore()
  {
    return this.treeStore;
  }

  public void setSession(Session session)
  {

    /* Don't reset the session to itself */
    // if (this.currentSession != session) { // removed until session window is
    // re-implemented
    this.currentSession = session;
    //session.populateProcs();

    Iterator i = this.currentSession.getProcesses().iterator();
    while (i.hasNext())
      {
        DebugProcess debugProcess = (DebugProcess) i.next();
        Iterator j = debugProcess.getProcs().iterator();
        while (j.hasNext())
          {
            this.addProc((GuiProc) j.next());
          }

        debugProcess.getProcs().itemAdded.addObserver(new Observer()
        {
          public void update (Observable arg0, Object obj)
          {
            //System.out.println(this + ": .update() " + obj);
            addProc((GuiProc) obj);
          }
        });

        debugProcess.getProcs().itemRemoved.addObserver(new Observer()
        {
          public void update (Observable arg0, Object obj)
          {
            removeProc(((GuiProc)obj).getProc());
          }
        });
      }

  }
  
  public Session getSession()
  {
    return this.currentSession;
  }

  public void addProc(GuiProc guiProc)
  {
    Proc proc = guiProc.getProc();
    //System.out.println(this + ": SessionProcDataModel.addProc() " + proc );
    if (proc == null)
      {
        errorLog.log(Level.WARNING,
                     "SessionProcDataModel.addProck: Trying to add proc "
                         + guiProc + "but failed as proc=null.");
        return;
      }

    TreeIter iter;
    try
      {
        iter = treeStore.appendRow(null);
        iterHash.put(proc.getId(), iter);
      }
    catch (Exception e)
      {
        errorLog.log(Level.WARNING,
                     "SessionProcDataModel.addTask: Cannot store proc " + proc
                         + " in hash.", e);
        return;
      }

    try
      {
        treeStore.setValue(iter, commandDC, proc.getCommand());
        treeStore.setValue(iter, pidDC, proc.getPid());
        treeStore.setValue(iter, procDataDC,
                           (GuiProc.GuiProcFactory.getGuiProc(proc)));
        treeStore.setValue(iter, weightDC, Weight.NORMAL.getValue());
        treeStore.setValue(iter, isThreadDC, false);

        treeStore.setValue(iter, threadParentDC, 0);
        treeStore.setValue(iter, sensitiveDC, true);
        
        /* Read from /proc/pid/stat the VSZ, RSS, and CPU Time. */
        statRead(proc, iter);
      }
    catch (Exception e)
      {
        errorLog.log(Level.WARNING,
                     "SessionProcDataModel.addTask: Trying to add proc  "
                         + proc + "to treestore, but failed.", e);
        return;
      }

    new ProcTasksObserver(proc, new ProcTasks()
    {
      public void deletedFrom(Object observable)
      {
      }

      public void addFailed(Object observable, Throwable w)
      {
        errorLog.log(Level.WARNING, new Date() + "EventLogger.addFailed"
                                   + "(Object o, Throwable w): " + observable
                                   + " failed " + "to add");
//        WindowManager.theManager.logWindow.print(new Date()
//                                                 + "EventLogger.addFailed(Object o, Throwable w): "
//                                                 + observable
//                                                 + " failed to add");
//        throw new RuntimeException(w);
      }

      public void addedTo(Object observable)
      {
      }

      public void existingTask(Task task)
      {
        addTask(task);
      }

      public void taskRemoved(final Task task)
      {
        CustomEvents.addEvent(new Runnable()
        {
          Task realTask = task;

          public void run()
          {
            removeTask(realTask);
          }

        });
      }

      public void taskAdded(final Task task)
      {
        CustomEvents.addEvent(new Runnable()
        {
          Task realTask = task;

          public void run()
          {
            addTask(realTask);
          }

        });
      }
    });
  }

  /**
   * Read the Vsize, RSS, and CPU time from /proc/pid/stat, and set the 
   * information into its relevant place in the TreeStore.
   */
  public void statRead(Proc proc, TreeIter iter)
  {
    stat.refresh(proc.getPid());
    
    /* stat.vsize returns in bytes */
    treeStore.setValue(iter, vszDC, (stat.vsize / 1024) + " kB");
    
    /* stat.rss returns the number of pages in use - multiply by page size
     * to find actual memory use. >>> Assumes 4kb page size!! Hack alert!<<< */
    treeStore.setValue(iter, rssDC, (stat.rss * 4) + " kB");
    
    /* Sum up individual values for clock jiffies - 100 jiffies in 
     * each second. */
    long t = (stat.cstime + stat.cutime + stat.stime + stat.utime) / 100;
    
    long sec = t % 60;
    long min = t / 60;
    
    if (sec < 10)
      treeStore.setValue(iter, timeDC, min + ":0" + sec);
    else
      treeStore.setValue(iter, timeDC, min + ":" + sec);
    
    treeStore.setValue(iter, ppidDC, "" + stat.ppid);
    treeStore.setValue(iter, stateDC, "" + stat.state);
    treeStore.setValue(iter, niceDC, "" + stat.nice);
  }
  
  /**
   * Update the information listed in the TreeView, specifically the new memory
   * information and updated CPU time.
   */
  public void refreshRead ()
  {
    Iterator i = this.currentSession.getProcesses().iterator();
    while (i.hasNext())
      {
        DebugProcess debugProcess = (DebugProcess) i.next();
        Iterator j = debugProcess.getProcs().iterator();
        while (j.hasNext())
          {
            Proc p = ((GuiProc)j.next()).getProc();
            TreeIter iter = (TreeIter)this.iterHash.get(p.getId());
            statRead(p, iter);
          }
      }
  }
  
  
  public void addTask(Task task)
  {
    // get an iterator pointing to the parent
    TreeIter parent;
    parent = (TreeIter) iterHash.get(task.getProc().getId());
    TreeIter iter = null;

    try
      {
        if (treeStore.isIterValid(parent))
          iter = treeStore.appendRow(parent);
      }
    catch (Exception e)
      {
        errorLog.log(Level.WARNING,
                     "SessionProcDataModel.addTask: Trying to add task " + task
                         + "but failed.", e);
        return;
      }

    try
      {
        iterHash.put(task.getTaskId(), iter);
      }
    catch (Exception e)
      {
        errorLog.log(Level.WARNING,
                     "SessionProcDataModel.addTask: Cannot place task " + task
                         + " in iterHash.", e);
        return;
      }

    try
      {
        treeStore.setValue(iter, commandDC,
                           Long.toHexString(task.getEntryPointAddress()));
        treeStore.setValue(iter, pidDC, task.getTid());
        treeStore.setValue(iter, weightDC, Weight.NORMAL.getValue());
        treeStore.setValue(iter, threadParentDC, task.getProc().getPid());
        treeStore.setValue(iter, isThreadDC, true);

        treeStore.setValue(iter, procDataDC,
                           GuiTask.GuiTaskFactory.getGuiTask(task));
        treeStore.setValue(iter, sensitiveDC, true);

        GuiTask guiTask = GuiTaskFactory.getGuiTask(task);
        GuiProc guiProc = GuiProcFactory.getGuiProc(task.getProc());
        guiProc.addGuiTask(guiTask);

      }
    catch (Exception e)
      {
        errorLog.log(Level.WARNING,
                     "SessionProcDataModel.addTask: treeStore setvalue on  "
                         + task + " failed.", e);
        return;
      }

  }

  public void removeTask(Task task)
  {

    TreeIter iter;
    try
      {
        iter = (TreeIter) iterHash.get(task.getTaskId());
      }
    catch (Exception e)
      {
        errorLog.log(Level.WARNING,
                     "SessionProcDataModel.removeTask: Cannot find value in Hash for: "
                         + task, e);
        return;
      }

    try
      {
        GuiTask guiTask = GuiTaskFactory.getGuiTask(task);

        treeStore.removeRow(iter);
        iterHash.remove(task.getTaskId());

        GuiProc guiProc = GuiProcFactory.getGuiProc(task.getProc());
        guiProc.removeGuiTask(guiTask);

      }
    catch (Exception e)
      {
        errorLog.log(Level.WARNING,
                     "SessionProcDataModel.removeTask: Cannot remove from treeStore for: "
                         + task, e);
        return;
      }
  }

  public void removeProc(Proc proc)
  {
    TreeIter iter;
    try
      {
        iter = (TreeIter) iterHash.get(proc.getId());
      }
    catch (Exception e)
      {
        errorLog.log(Level.WARNING,
                     "SessionProcDataModel.removeProc: Cannot find value in Hash for: "
                         + proc, e);
        return;
      }

    try
      {
        treeStore.removeRow(iter);
        iterHash.remove(proc.getId());
      }
    catch (Exception e)
      {
        errorLog.log(Level.WARNING,
                     "SessionProcDataModel.removeProc: Cannot remove from treeStore for: "
                         + proc, e);
        return;
      }

  }

  public void stopRefreshing()
  {
    Manager.eventLoop.remove(refreshTimer);
  }

  public void setRefreshTime(int sec)
  {
    Manager.eventLoop.remove(refreshTimer);
    this.refreshTimer = new TimerEvent(0, sec * 1000)
    {
      public void execute()
      {
        Manager.host.requestRefreshXXX(true);
      }
    };
    Manager.eventLoop.add(refreshTimer);
  }

  public void refresh() throws IOException
  {
    Manager.host.requestRefreshXXX(true);
  }

  public DataColumnInt getPidDC()
  {
    return this.pidDC;
  }

  public DataColumnString getCommandDC()
  {
    return this.commandDC;
  }

  public DataColumnString getColorDC()
  {
    return this.colorDC;
  }

  public DataColumnObject getProcDataDC()
  {
    return this.procDataDC;
  }

  public DataColumnInt getWeightDC()
  {
    return this.weightDC;
  }

  public DataColumnInt getThreadParentDC()
  {
    return this.threadParentDC;
  }

  public DataColumnBoolean getIsThreadDC()
  {
    return this.isThreadDC;
  }

  public DataColumnBoolean getHasParentDC()
  {
    return this.isThreadDC;
  }

  public DataColumnBoolean getSensitiveDC()
  {
    return this.sensitiveDC;
  }
  
  public DataColumnString getVszDC()
  {
    return this.vszDC;
  }
  
  public DataColumnString getRssDC()
  {
    return this.rssDC;
  }
  
  public DataColumnString getTimeDC()
  {
    return this.timeDC;
  }
  
  public DataColumnString getPPIDDC()
  {
    return this.ppidDC;
  }

  public DataColumnString getStateDC()
  {
    return this.stateDC;
  }
  
  public DataColumnString getNiceDC()
  {
    return this.niceDC;
  }
  
  public TreeModel getModel()
  {
    return this.treeStore;
  }

  class TaskDestroyedObserver
      implements Observer
  {
    public void update(Observable o, final Object obj)
    {
      org.gnu.glib.CustomEvents.addEvent(new Runnable()
      {
        public void run()
        {
          final Task task = (Task) obj;
          TreeIter iter = (TreeIter) iterHash.get(task.getTaskId());
          // System.out.println(" TaskDestroyedObserver.update() trying to
          // remove Task " + task.getTid()+ " " + iter );
          try
            {
              if (iter == null)
                {
                  throw new NullPointerException(
                                                 "task " + task + "Not found in TreeIter HasTable. Cannot be removed"); //$NON-NLS-1$ //$NON-NLS-2$
                }

              // get an iterator pointing to the parent
              TreeIter parent;
              if (task.getProc() == null)
                {
                  parent = null;
                }
              else
                {
                  parent = (TreeIter) iterHash.get(task.getProc().getId());
                }

              if (getThreadCount(parent) == 1)
                {
                  // treeStore.setValue(parent, sensitiveDC, true);
                  treeStore.setValue(parent, sensitiveDC, false);
                }
              else
                {
                  // treeStore.setValue(parent, sensitiveDC, false);
                  treeStore.setValue(parent, sensitiveDC, true);
                }

              treeStore.removeRow(iter);
              iterHash.remove(task.getTaskId());

            }
          catch (NullPointerException e)
            {
              errorLog.log(
                           Level.WARNING,
                           "trying to remove task " + task + "before it is added", e); //$NON-NLS-1$ //$NON-NLS-2$
            }
        }
      });
    }
  }

  private int getThreadCount(TreeIter iter)
  {
    int n = iter.getChildCount();
    int threadCount = 0;
    // System.out.println("ProcDestroyedObserver.update() iter not null checking
    // children n: " + n );
    for (int i = 0; i < n; i++)
      {
        if (treeStore.getValue(iter.getChild(i), isThreadDC) == true)
          {
            threadCount++;
          }
      }
    // System.out.println(this + ": ProcDataModel.getThreadCount() " +
    // treeStore.getValue(iter, commandDC) + " " + treeStore.getValue(iter,
    // pidDC) + " "+ threadCount);
    return threadCount;

  }

}
