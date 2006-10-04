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


package frysk.gui.disassembler;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import java.util.Hashtable;
import java.util.Iterator;

import org.gnu.glade.LibGlade;
import org.gnu.glib.CustomEvents;
import org.gnu.gtk.event.LifeCycleEvent;
import org.gnu.gtk.event.LifeCycleListener;

import frysk.gui.Gui;
import frysk.gui.common.ProcBlockCounter;
import frysk.gui.common.prefs.PreferenceManager;
import frysk.gui.monitor.WindowManager;
import frysk.proc.Action;
import frysk.proc.Proc;
import frysk.proc.ProcAttachedObserver;
import frysk.proc.ProcObserver;
import frysk.proc.Task;

/**
 * Factory for creating DisassemblyWindows - allows multiple DisassemblyWindows to be
 * instantiated for different processes, and disallows multiple windows on the
 * same process. Uses a ProcBlockCounter to co-ordinate the un-blocking of the
 * process between the Register and SourceWindows if the other two are also 
 * running on that process. A singleton class dynamically creating DisassemblyWindows.
 *  
 * @author mcvet
 */
public class DisassemblyWindowFactory
{

  /* Instance of this class used by the SourceWindow to ensure singularity */
  public static DisassemblyWindow disWin = null;
  
  /* Keeps track of which DisassemblerWindows belong to which Task. */
  private static Hashtable procTable;
  
  /* Keeps track of which ProcBlockCounter belongs to the Task. */
  private static Hashtable blockerTable;
  
  /* Used to instantiate the glade file multiple times */
  private static String[] gladePaths;
  
  /* Bad hack to tell if we've been called from the Monitor or SourceWindow */
  /* TODO: Get rid of this when the BlockedObserver becomes a reality */
  private static boolean monitor = false;
  
  private static Logger errorLog = Logger.getLogger (Gui.ERROR_LOG_ID);
  
  private static int task_count;
  
  private static Task myTask;
  
  private final static String DIS_GLADE = "disassemblywindow.glade";
  
  /**
   * Set the paths to look in for the DisassemblyWindow glade widgets, and initialize
   * the Hashtables.
   * 
   * @param paths   An array of paths containing glade files.
   */
  public static void setPaths(String[] paths)
  {
    gladePaths = paths;
    procTable = new Hashtable();
    blockerTable = new Hashtable();
  }

  /**
   * Performs checks to ensure no other DisassemblyWindow is running on this Task;
   * if not, assigns a ProcBlockCounter and attaches an Observer if there is
   * no other Window already running on this Proc.
   * 
   * @param proc    The Proc to be examined by the new DisassemblyWindow.
   */
  public static void createDisassemblyWindow (Proc proc)
  {
    DisassemblyWindow dw = (DisassemblyWindow) procTable.get(proc);

    /* Check if there is already a DisassemblyWindow running on this task */
    if (dw != null)
      {
        dw.showAll();
        return;
      }

//    DisWinBlocker blocker = new DisWinBlocker();
//    blocker.myTask = proc;
    
     ProcAttachedObserver pao = null;
    
     if (ProcBlockCounter.getBlockCount(proc) != 0 || monitor == true)
       {
         /* If this Task is already blocked, don't try to block it again */
         dw = finishDisWin(dw, proc);
       }
     else 
       {
         /* Otherwise it is not blocked, and we should go about doing it */
         pao = new ProcAttachedObserver(proc, new DisWinBlocker());
         blockerTable.put(proc, pao);
       }

    /* Indicate that there is another window on this Task */
    ProcBlockCounter.incBlockCount(proc);

    return;
  }

  /**
   * Initializes the Glade file, the DisassemblyWindow itself, adds listeners and
   * Assigns the Proc.
   * 
   * @param dw  The DisassemblyWindow to be initialized.
   * @param proc    The Proc to be examined by mw.
   */
  public static DisassemblyWindow finishDisWin (DisassemblyWindow dw, Proc proc)
  {

    LibGlade glade = null;

    // Look for the right path to load the glade file from
    int i = 0;
    for (; i < gladePaths.length; i++)
      {
        try
          {
            glade = new LibGlade(gladePaths[i] + "/"
                                 + DIS_GLADE, null);
          }
        catch (Exception e)
          {
            if (i < gladePaths.length - 1)
              // If we don't find the glade file, look at the next file
              continue;
            else
              {
                e.printStackTrace();
                System.exit(1);
              }

          }

        // If we've found it, break
        break;
      }
    // If we don't have a glade file by this point, bail
    if (glade == null)
      {
        System.err.println("Could not file source window glade file in path "
                           + gladePaths[gladePaths.length - 1]
                           + "! Exiting.");
        return dw;
      }
    
    try
      {
        dw = new DisassemblyWindow(glade);
        procTable.put(proc, dw);
      }
    catch (Exception e)
      {
        e.printStackTrace();
      }
    
    dw.addListener(new DisWinListener());
    
    Preferences prefs = PreferenceManager.getPrefs();
    dw.load(prefs.node(prefs.absolutePath() + "/disassembler"));

    if (! dw.hasTaskSet())
      {
        dw.setIsRunning(false);
        dw.setTask(proc.getMainTask());
      }
    else
      dw.showAll();
    
    return dw;
  }
  
  /**
   * Used by the SourceWindow to assign the static memWin object which it uses
   * to ensure there is only one DisassemblyWindow running for its Proc.
   * 
   * @param proc    The Proc used to find the DisassemblyWindow representing it.
   */
  public static void setDisWin(Proc proc)
  {
    disWin = (DisassemblyWindow) procTable.get(proc);
  }
  
  /**
   * Tells this factory it is being called from the Monitor.
   */
  public static void setMonitor()
  {
    monitor = true;
  }
  
  /**
   * Check to see if this instance is the last one blocking the Proc - if so,
   * request to unblock it. If not, then just decrement the block count and
   * clean up.
   * 
   * @param proc    The Proc to be unblocked.
   */
  private static void unblockTask (Proc proc)
  {
    if (procTable.get(proc) != null)
      {
        if (ProcBlockCounter.getBlockCount(proc) == 1 && monitor != true)
          {
            ProcAttachedObserver pao = (ProcAttachedObserver) blockerTable.get(proc);
            Iterator i = proc.getTasks().iterator();
            while (i.hasNext())
              {
                Task t = (Task) i.next();
                t.requestUnblock(pao);
                t.requestDeleteAttachedObserver(pao);
              }
            
            blockerTable.remove(proc);
            
//            try
//              {
//                TaskObserver.Attached o = (TaskObserver.Attached) blockerTable.get(task);
//                task.requestUnblock(o);
//                task.requestDeleteAttachedObserver(o);
//                blockerTable.remove(task);
//              }
//            catch (Exception e)
//              {
//                Preferences prefs = PreferenceManager.getPrefs();
//                DisassemblyWindow dw = (DisassemblyWindow) procTable.get(task);
//                dw.save(prefs);
//                return;
//              }
          }
        ProcBlockCounter.decBlockCount(proc);
        Preferences prefs = PreferenceManager.getPrefs();
        DisassemblyWindow dw = (DisassemblyWindow) procTable.get(proc);
        dw.save(prefs);
        procTable.remove(proc);
      }
  }
  
  /**
   * A wrapper for LifeCycleListener which cleans up when the DisassemblyWindow 
   * is closed.
   */
  private static class DisWinListener
      implements LifeCycleListener
  {

    public void lifeCycleEvent (LifeCycleEvent arg0)
    {
    }

    /**
     * If the DisassemblyWindow is closed, let the Task know that it isn't being
     * examined anymore and then hide the window.
     * 
     * @param arg0  The LifeCycleEvent affecting this window.
     */
    public boolean lifeCycleQuery (LifeCycleEvent arg0)
    {

      /*
       * If the window is closing we want to remove it and it's task from the
       * map, so that we know to create a new instance next time
       */
      if (arg0.isOfType(LifeCycleEvent.Type.DELETE)
          || arg0.isOfType(LifeCycleEvent.Type.DESTROY)
          || arg0.isOfType(LifeCycleEvent.Type.HIDE))
        {
          DisassemblyWindow dw = (DisassemblyWindow) arg0.getSource();
          Task t = dw.getMyTask();

          unblockTask(t.getProc());

          if (!dw.equals(disWin))
            WindowManager.theManager.mainWindow.showAll();

          dw.hideAll();
          return true;
        }

      return false;
    }

  }

  /**
   * A wrapper for TaskObserver.Attached which initializes the DisassemblyWindow 
   * upon call, and blocks the task it is to examine.
   */
  private static class DisWinBlocker
      implements ProcObserver.ProcTasks
  {

    private Task myTask;

    /**
     * Finish the DisassemblyWindow initialization and block the task.
     * 
     * @param task  The Task being blocked by this DisassemblyWindow.
     */
    public Action updateAttached (Task task)
    {
      myTask = task;
      return Action.BLOCK;
    }
    
    public void taskAdded(final Task task)
    {
      // TODO Auto-generated method stub
    }

    public void addedTo (Object observable)
    {
      // TODO Auto-generated method stub
    }

    public void addFailed (Object observable, Throwable w)
    {
      errorLog.log(Level.WARNING, "addFailed (Object observable, Throwable w)", w);
      throw new RuntimeException(w);
    }

    public void deletedFrom (Object observable)
    {
      // TODO Auto-generated method stub
    }
    
    public void taskRemoved (Task task)
    {   
      // TODO Auto-generated method stub
    }
    
    public void existingTask (Task task)
    {
        myTask = task;
        CustomEvents.addEvent(new Runnable()
        {
          public void run()
          {
            handleTask(myTask);
          }

        });
    }
  }
  
  public static synchronized void handleTask (Task task)
  {
    myTask = task;
    CustomEvents.addEvent(new Runnable()
    {
      public void run ()
      {
        --task_count;
        if (task_count == 0)
          {
            DisassemblyWindow dw = (DisassemblyWindow) procTable.get(myTask.getProc());
            finishDisWin(dw, myTask.getProc());
          }
      }
    }); 
  }

}
