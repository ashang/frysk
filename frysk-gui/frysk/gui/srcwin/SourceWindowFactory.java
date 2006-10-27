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


package frysk.gui.srcwin;

import java.io.IOException;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import lib.dw.DwflLine;
import lib.dw.NoDebugInfoException;

import org.gnu.glade.LibGlade;
import org.gnu.glib.CustomEvents;
import org.gnu.gtk.event.LifeCycleEvent;
import org.gnu.gtk.event.LifeCycleListener;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import frysk.dom.DOMFactory;
import frysk.dom.DOMFrysk;
import frysk.dom.DOMFunction;
import frysk.dom.DOMImage;
import frysk.gui.Gui;
import frysk.gui.common.dialogs.WarnDialog;
import frysk.gui.common.ProcBlockCounter;
import frysk.gui.monitor.WindowManager;
import frysk.proc.Action;
import frysk.proc.Proc;
import frysk.proc.ProcBlockObserver;
import frysk.proc.Task;
import frysk.rt.StackFactory;
import frysk.rt.StackFrame;

/**
 * SourceWindow factory is the interface through which all SourceWindow objects
 * in frysk should be created. It takes care of setting paths to resource files
 * as well as making sure that at most one window is opened per Task. A
 * singleton class dynamically creating SourceWindows.
 * 
 * @author ajocksch
 */
public class SourceWindowFactory
{

  private static String[] gladePaths;

  private static HashMap map;

  private static Hashtable blockerTable;

  private static Hashtable procTable;
  
  private static int task_count = 0;

  public static SourceWindow srcWin = null;
  
  public static Task myTask;
  
  protected static boolean SW_active = false;

  private static Logger errorLog = Logger.getLogger(Gui.ERROR_LOG_ID);

  /**
   * Sets the paths to look in to find the .glade files needed for the gui
   * 
   * @param paths The possible locations of the gui glade files.
   */
  public static void setGladePaths (String[] paths)
  {
    gladePaths = paths;
  }

  static
    {
      map = new HashMap();
      blockerTable = new Hashtable();
      procTable = new Hashtable();
    }

  /**
   * Creates a new source window using the given task. The SourceWindows
   * correspond to tasks in a 1-1 relationship, so if you try to launch a
   * SourceWindow for a Task and an existing window has already been created,
   * that one will be brought to the forefront rather than creating a new
   * window.
   * 
   * @param proc The Proc to open a SourceWindow for.
   */
  public static void createSourceWindow (Proc proc)
  {
    //task_count = proc.getTasks().size();
    SourceWindow sw = (SourceWindow) procTable.get(proc);
    
    if (sw != null)
      {
        sw.showAll();
        return;
      }

    ProcBlockObserver pbo = null;

    if (procTable.get(proc) == null
        || ProcBlockCounter.getBlockCount(proc) == 0)
      pbo = new SourceWinBlocker(proc);

    ProcBlockCounter.incBlockCount(proc);
    blockerTable.put(proc, pbo);
  }

  /**
   * Initializes the Glade file, the SourceWindow itself, adds listeners and
   * Assigns the Proc. Sets up the DOM information and the Stack information.
   * 
   * @param mw The MemoryWindow to be initialized.
   * @param proc The Proc to be examined by mw.
   */
  private static void finishSourceWin (Proc proc)
  {
    int size;
    //Task[] tasks;
    DOMFrysk dom = null;

    if (map.containsKey(proc))
      {
        // Do something here to revive the existing window
        srcWin = (SourceWindow) map.get(proc);
        srcWin.showAll();
        srcWin.grabFocus();
      }
    else
      {
        LibGlade glade = null;

        // Look for the right path to load the glade file from
        int i = 0;
        for (; i < gladePaths.length; i++)
          {
            try
              {
                glade = new LibGlade(gladePaths[i] + "/"
                                     + SourceWindow.GLADE_FILE, null);
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
            return;
          }

        size = proc.getTasks().size();
        StackFrame[] frames = new StackFrame[size];
        frames = generateProcStackTrace(frames, null, dom, proc, size);

        srcWin = new SourceWindow(glade, gladePaths[i], dom,
                            frames, (SourceWinBlocker)blockerTable.get(proc));
        procTable.put(proc, srcWin);
        srcWin.setMyProc(proc);
        srcWin.addListener(new SourceWinListener());
        srcWin.grabFocus();

        // Store the reference to the source window
        map.put(proc, srcWin);
      }
  }
  
  public static StackFrame[] generateProcStackTrace(StackFrame[] frames, 
                                        Task[] tasks, DOMFrysk dom, Proc proc, int size)
  {
    if (proc == null)
      return null;
    
    
    if (frames == null || tasks == null)
      {
        if (tasks == null)
          {
            tasks = new Task[size];
            Iterator iter = proc.getTasks().iterator();
            for (int k = 0; k < size; k++)
              tasks[k] = (Task) iter.next();
          }

        size = tasks.length;
        frames = new StackFrame[size];
      }
    
    for (int j = 0; j < size; j++)
      {
        DwflLine line;
        DOMFunction f = null;

        /** Create the stack frame * */

        StackFrame curr = null;
        try
          {
            frames[j] = StackFactory.createStackFrame(tasks[j]);
            curr = frames[j];
          }
        catch (Exception e)
          {
            System.out.println(e.getMessage());
          }

        /** Stack frame created * */

        while (curr != null) /*
                               * Iterate and initialize information for all
                               * frames, not just the top one
                               */
          {
            
            if (dom == null && tasks[j].equals(proc.getMainTask())
                && curr.getDwflLine() != null)
              {
                try
                  {
                    //dom = DOMFactory.createDOM(proc.getMainTask());
                    dom = DOMFactory.createDOM(curr, proc);
                  }

                // If we don't have a dom, tell the task to continue
                catch (NoDebugInfoException e)
                  {
                  }
                catch (IOException e)
                  {
                    unblockProc(proc);
                    WarnDialog dialog = new WarnDialog("File not found",
                                                       "Error loading source code: "
                                                           + e.getMessage());
                    dialog.showAll();
                    dialog.run();
                    return null;
                  }
              }

            line = curr.getDwflLine();
            
            if (line != null)
              {
                String filename = line.getSourceFile();
                filename = filename.substring(filename.lastIndexOf("/") + 1);
                
                try
                {
                  f = getFunctionXXX(
                                   dom.getImage(tasks[j].getProc().getMainTask().getName()),
                                   filename, line.getLineNum());
                }
                catch (NullPointerException npe)
                {
                  f = null;
                }
              }
            
            curr.setDOMFunction(f);
            curr = curr.getOuter();
          }
      }
    return frames;
  }

  /**
   * Print out the DOM in XML format
   * 
   * @param dom The DOMFrysk to output.
   */
  public static void printDOM (DOMFrysk dom)
  {
    try
      {
        XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
        outputter.output(dom.getDOMFrysk(), System.out);
      }
    catch (IOException e)
      {
        e.printStackTrace();
      }
  }

  /**
   * Unblocks the Task being examined and removes all Observers on it, and
   * removes it from the tables watching it.
   * 
   * @param task The Task to be unblocked.
   */
  private static void unblockProc (Proc proc)
  {
    if (blockerTable.containsKey(proc)
        && ProcBlockCounter.getBlockCount(proc) == 1)
      {
        SourceWinBlocker swb = (SourceWinBlocker) blockerTable.get(proc);
        Iterator i = proc.getTasks().iterator();
        while (i.hasNext())
          {
            Task t = (Task) i.next();
            t.requestUnblock(swb);
            t.requestDeleteInstructionObserver(swb);
          }

        procTable.remove(proc);
        blockerTable.remove(proc);
      }
    ProcBlockCounter.decBlockCount(proc);
  }

  /**
   * Returns a DOMFunction matching the incoming function information from the
   * DOMImage.
   * 
   * @param image The DOMImage containing the source information.
   * @param filename The name of the source file.
   * @param linenum The line number of the function.
   * @return The found DOMFunction.
   */
  private static DOMFunction getFunctionXXX (DOMImage image, String filename,
                                             int linenum)
  {
    Iterator functions = image.getFunctions();

    System.out.println("Looking for " + filename + ": " + linenum);

    DOMFunction found = null;

    while (functions.hasNext())
      {
        DOMFunction function = (DOMFunction) functions.next();
//        System.out.println("\t" + function.getSource().getFileName() + ": "
//                           + function.getStartingLine() + " - "
//                           + function.getEndingLine());
        if (function.getSource().getFileName().equals(filename)
            && function.getStartingLine() <= linenum)
          {
            if (found == null
                || function.getStartingLine() > found.getStartingLine())
              found = function;
          }
      }

    return found;
  }

  /*
   * The responsability of this class that whever a SourceWindow is closed the
   * corresponding task is removed from the HashMap. This tells
   * createSourceWindow to create a new window the next time that task is passed
   * to it.
   */
  private static class SourceWinListener
      implements LifeCycleListener
  {

    public void lifeCycleEvent (LifeCycleEvent arg0)
    {
    }

    public boolean lifeCycleQuery (LifeCycleEvent arg0)
    {

      /*
       * If the window is closing we want to remove it and it's task from the
       * map, so that we know to create a new instance next time
       */
      if (arg0.isOfType(LifeCycleEvent.Type.DELETE))
        {
          if (map.containsValue(arg0.getSource()))
            {
              SourceWindow s = (SourceWindow) arg0.getSource();
              Proc proc = s.getMyProc();
              map.remove(proc);

              unblockProc(proc);

              WindowManager.theManager.sessionManager.show();
              s.hideAll();
              SW_active = false;
            }
        }

      return true;
    }

  }

  /**
   * A wrapper for TaskObserver.Attached which initializes the MemoryWindow 
   * upon call, and blocks the task it is to examine.
   */
  private static class SourceWinBlocker
      extends ProcBlockObserver
  {

    public SourceWinBlocker (Proc theProc)
    {
      super(theProc);
    }

    public Action updateAttached (Task task)
    {
      myTask = task;
      return Action.BLOCK;
    }
    
    public void existingTask (Task task)
    {
      myTask = task;
      if (task_count == 0)
        task_count = ((ProcBlockObserver) blockerTable.get(task.getProc())).getNumTasks();
        

      CustomEvents.addEvent(new Runnable()
      {
        public void run ()
        {
          handleTask(myTask);
        }

      });
    }

    public void addFailed (Object observable, Throwable w)
    {
      errorLog.log(Level.WARNING, "addFailed (Object observable, Throwable w)",
                   w);
      throw new RuntimeException(w);
    }

    public void deletedFrom (Object observable)
    {
      // TODO Auto-generated method stub
    }
  }
  
  public static synchronized void handleTask (Task task)
  {
    myTask = task;

    if (SW_active == false)
      {
        CustomEvents.addEvent(new Runnable()
        {
          public void run ()
          {
            --task_count;
            if (task_count == 0)
              {
                SW_active = true;
                finishSourceWin(myTask.getProc());
              }
          }
        });
      }
    else
      {
        --task_count;
        if (task_count == 0)
          {
            StackFrame[] frames = generateProcStackTrace(null, null,
                                                         srcWin.getDOM(),
                                                         task.getProc(), 0);
            srcWin.populateStackBrowser(frames);
            srcWin.procReblocked();
            }
      }
  }
}
