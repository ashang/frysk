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
import java.util.HashMap;
import java.util.Iterator;
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
import org.gnu.gtk.TreeModel;
import org.gnu.gtk.TreeStore;
import org.gnu.pango.Weight;

import frysk.gui.Gui;
import frysk.gui.sessions.DebugProcess;
import frysk.gui.sessions.Session;
import frysk.proc.Proc;
import frysk.proc.Task;
import frysk.sys.proc.Stat;

public class SessionProcDataModel
{

  private TreeStore treeStore;

  DataColumn[] columns;

  private DataColumnInt tidDC;

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

  private Logger errorLog = Logger.getLogger(Gui.ERROR_LOG_ID);

  private Session currentSession;

  public SessionProcDataModel() throws IOException
  {

    this.tidDC = new DataColumnInt();
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

    this.treeStore = new TreeStore(new DataColumn[] { tidDC, commandDC,
                                                     colorDC, procDataDC,
                                                     weightDC, threadParentDC,
                                                     isThreadDC, sensitiveDC, 
                                                     vszDC, rssDC, timeDC,
                                                     ppidDC, stateDC, niceDC});

    this.iterHash = new HashMap();
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

    Iterator i = this.currentSession.getProcesses().iterator();
    while (i.hasNext())
      {
        DebugProcess debugProcess = (DebugProcess) i.next();
        Iterator j = debugProcess.getProcs().iterator();
        while (j.hasNext())
          {
            GuiProc gp = (GuiProc) j.next();
            this.addProc(gp);
          }

        debugProcess.getProcs().itemAdded.addObserver(new Observer()
        {
          public void update (Observable arg0, Object obj)
          {
            addProc((GuiProc) obj);
          }
        });

        debugProcess.getProcs().itemRemoved.addObserver(new Observer()
        {
          public void update (Observable arg0, Object obj)
          {
            removeProc((GuiProc)obj);
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
    if (proc == null)
      {
        errorLog.log(Level.WARNING,
                     "SessionProcDataModel.addProck: Trying to add proc "
                         + guiProc + "but failed as proc=null.");
        return;
      }

    TreeIter iter;
        iter = treeStore.appendRow(null);
        iterHash.put(proc.getId(), iter);

    try
      {
    	// XXX: Hack, hack, hack. Need to do this properly.
    	if (guiProc.getName() == "Frysk Terminal Process")
    		treeStore.setValue(iter, commandDC, guiProc.getName());
    	else
    		treeStore.setValue(iter, commandDC, guiProc.getExecutableName());
        
        treeStore.setValue(iter, tidDC, proc.getPid());
        treeStore.setValue(iter, procDataDC,
                           (GuiProc.GuiProcFactory.getGuiProc(proc)));
        treeStore.setValue(iter, weightDC, Weight.NORMAL.getValue());
        treeStore.setValue(iter, isThreadDC, false);

        treeStore.setValue(iter, threadParentDC, 0);
        treeStore.setValue(iter, sensitiveDC, true);
        
        /* Read from /proc/pid/stat the VSZ, RSS, and CPU Time. */
        statRead(proc, null, iter);
      }
    catch (Exception e)
      {
        errorLog.log(Level.WARNING,
                     "SessionProcDataModel.addTask: Trying to add proc  "
                         + proc + "to treestore, but failed.", e);
        return;
      }
  }

  /**
   * Read the Vsize, RSS, and CPU time from /proc/pid/stat, and set the 
   * information into its relevant place in the TreeStore.
   */
  public void statRead(Proc proc, Task task, TreeIter iter)
  {
    if (task == null && proc != null)
      stat.refresh(proc.getPid());
    else if (task != null && proc == null)
      stat.refreshThread(task.getProc().getPid(), task.getTid());
    else
      return;
    
    /* stat.vsize returns in bytes */
    treeStore.setValue(iter, vszDC, (stat.vsize / 1024) + " kB");
    
    /* stat.rss returns the number of pages in use - multiply by page size
     * to find actual memory use >>> Assumes 4kb page size!! Hack alert! <<< */
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
   * Update the information listed in the process TreeView, specifically the
   * new memory information and updated CPU time.
   */
  public void refreshProcRead ()
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
            statRead(p, null, iter);
          }
      }
  }
  
  public void removeProc(GuiProc guiProc)
  {
    Proc proc = guiProc.getProc();
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

  public DataColumnInt getTidDC()
  {
    return this.tidDC;
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

}
