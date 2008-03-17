// This file is part of the program FRYSK.
//
// Copyright 2005, 2007, 2008, Red Hat Inc.
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

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;

import org.gnu.glade.LibGlade;
import org.gnu.gtk.event.LifeCycleEvent;
import org.gnu.gtk.event.LifeCycleListener;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import frysk.proc.dead.LinuxCoreFactory;
import frysk.config.Config;
import frysk.proc.TaskAttachedObserverXXX;
import frysk.debuginfo.DebugInfoFrame;
import frysk.debuginfo.DebugInfoStackFactory;
import frysk.dom.DOMFrysk;
import frysk.gui.Gui;
import frysk.gui.monitor.WindowManager;
import frysk.proc.Action;
import frysk.proc.Manager;
import frysk.proc.Proc;
import frysk.proc.Task;

/**
 * SourceWindow factory is the interface through which all SourceWindow objects
 * in frysk should be created. It takes care of setting paths to resource files
 * as well as making sure that at most one window is opened per Task. A
 * singleton class dynamically creating SourceWindows.
 */
public class SourceWindowFactory
{
  protected static SourceWindow srcWin = null;
  
  public static Task myTask;
  
  public static AttachedObserver newProcObserver = null;
  
//  private static TaskAttachedObserverXXX attachedObserver = null;

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

    LibGlade glade;
    try 
    {
      glade = new LibGlade (Config.getGladeDir () + SourceWindow.GLADE_FILE, null);
    }
    catch (Exception e) 
    {
      throw new RuntimeException (e);
    }
    
    srcWin = new SourceWindow(glade, Config.getGladeDir (), proc);

    srcWin.addListener(new SourceWinListener());
//    runState = srcWin.getRunState();
    
    srcWin.grabFocus();
  }
  
  public static void createSourceWindow (Proc[] procs)
  {

    LibGlade glade;
    try 
    {
      glade = new LibGlade (Config.getGladeDir () + SourceWindow.GLADE_FILE, null);
    }
    catch (Exception e) 
    {
      throw new RuntimeException (e);
    }
    
    srcWin = new SourceWindow(glade, Config.getGladeDir (), procs);
    srcWin.addListener(new SourceWinListener());
    
    srcWin.grabFocus();
  }
  
  public static void createSourceWindow (DebugInfoFrame frame)
  {
    LibGlade glade;
    try
      {
        glade = new LibGlade(Config.getGladeDir() + SourceWindow.GLADE_FILE,
                             null);
      }
    catch (Exception e)
      {
        throw new RuntimeException(e);
      }
    
    SourceWindow srcWin = new SourceWindow(glade, Config.getGladeDir(), frame);
    srcWin.addListener(new SourceWinListener());
    
    srcWin.grabFocus();
  }
  
  public static void createSourceWindow (DebugInfoFrame[] frames)
  {
    LibGlade glade;
    try
      {
        glade = new LibGlade(Config.getGladeDir() + SourceWindow.GLADE_FILE,
                             null);
      }
    catch (Exception e)
      {
        throw new RuntimeException(e);
      }
    
    srcWin = new SourceWindow(glade, Config.getGladeDir(), frames);
    srcWin.addListener(new SourceWinListener());
    
    srcWin.grabFocus();
  }
  
    public static void attachToCore(File coreFile) {
	Proc proc = LinuxCoreFactory.createProc(coreFile);

    LinkedList tasks = proc.getTasks();
    DebugInfoFrame[] framez = new DebugInfoFrame[tasks.size()];
    Iterator iter = tasks.iterator();
    for (int i = 0; iter.hasNext(); i++)
      {
        Task task = (Task) iter.next();
        framez[i] = DebugInfoStackFactory.createDebugInfoStackTrace(task);
      }
    createSourceWindow(framez);
  }
  
  public static AttachedObserver startNewProc (String file, String env_variables, String options,
                                               String stdin, String stdout, String stderr)
  {
    File file_path = new File(file);
    String[] cmd = new String[1];
    if (env_variables.equals("") && options.equals(""))
        cmd[0] = file;
    else if (options.equals(""))
        cmd[0] = env_variables + " " + file;
    else if (env_variables.equals(""))
        cmd[0] = file + " " + options;
    else 
	cmd[0] = env_variables + " " + file + " " + options;
    
    if (env_variables.equals("") && options.equals(""))
        cmd[0] = file;
    else if (options.equals(""))
        cmd[0] = env_variables + " " + file;
    else if (env_variables.equals(""))
        cmd[0] = file + " " + options;
    else 
	cmd[0] = env_variables + " " + file + " " + options;
    // trim off any extraneous whitespace as the execvp level doesn't like it
    cmd[0] = cmd[0].trim();
    if (file_path.exists())
      {
	newProcObserver = new AttachedObserver();
        Manager.host.requestCreateAttachedProc(stdin, stdout, stderr, cmd, newProcObserver);
      }
    // else, do nothing.
    return newProcObserver;
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

  /*
   * The responsibility of this class that whenever a SourceWindow is closed the
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
              Proc p = srcWin.getSwProc();
              
              if (p != null)
        	srcWin.getSteppingEngine().removeObserver(srcWin.getLockObserver(), p, true);

             srcWin.hideAll();
             srcWin = null;
              
              if (WindowManager.theManager.sessionManagerDialog != null)
                WindowManager.theManager.sessionManagerDialog.showAll();
              else
                Gui.quitFrysk();
            }

      return true;
    }
  }
  
 protected static class AttachedObserver implements TaskAttachedObserverXXX
  {
    public void addedTo (Object o)
    {
      //System.err.println(this + " added to " + (Task) o);
    }
    
    public Action updateAttached (Task task)
    {
      //System.err.println("updateAttached " + task);
      Proc proc = task.getProc();

      if (srcWin != null)
	{
	  srcWin.getSteppingEngine().addProc(proc);
	  return Action.BLOCK;
	}

      LibGlade glade;
      try
	{
	  glade = new LibGlade(Config.getGladeDir() + SourceWindow.GLADE_FILE,
			       null);
	}
      catch (Exception e)
	{
	  throw new RuntimeException(e);
	}

      srcWin = new SourceWindow(glade, Config.getGladeDir(), proc, this);
      srcWin.addListener(new SourceWinListener());
      srcWin.grabFocus();

      return Action.BLOCK;
    }
    
    public void addFailed  (Object observable, Throwable w)
    {
      
    }
    
    public void deletedFrom (Object o)
    {
      
    }
  }

//  starting-process stuff
  
 public static void removeAttachedObserver (Task task, AttachedObserver attachedObserver)
 {
     task.requestDeleteAttachedObserver(attachedObserver);
 }
  
}
