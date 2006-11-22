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

import org.gnu.glade.LibGlade;
import org.gnu.gtk.event.LifeCycleEvent;
import org.gnu.gtk.event.LifeCycleListener;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import frysk.dom.DOMFrysk;
import frysk.gui.monitor.WindowManager;
import frysk.proc.Proc;
import frysk.proc.Task;
import frysk.rt.RunState;

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

  public static Hashtable stateTable;

  public static SourceWindow srcWin = null;
  
  public static Task myTask;

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
      stateTable = new Hashtable();
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
    SourceWindow sw = (SourceWindow) map.get(proc);

    if (sw != null)
      {
        sw = (SourceWindow) map.get(proc);
        RunState rs = sw.getRunState();
        rs.addObserver(sw.getLockObserver());
        sw.showAll();
        return;
      }

    int i = 0;
    LibGlade glade = null;

    for (; i < gladePaths.length; i++)
      {
        try
          {
            glade = new LibGlade(gladePaths[i] + "/" + SourceWindow.GLADE_FILE,
                                 null);
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
                           + gladePaths[gladePaths.length - 1] + "! Exiting.");
        return;
      }

    sw = new SourceWindow(glade, gladePaths[i], proc);

    stateTable.put(proc, sw.getRunState());
    sw.addListener(new SourceWinListener());
    sw.grabFocus();

    // Store the reference to the source window
    map.put(proc, sw);
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
    RunState rs = (RunState) stateTable.get(proc);
    if (rs.getNumObservers() == 0)
      {
        stateTable.remove(proc);
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
          if (map.containsValue(arg0.getSource()))
            {
              SourceWindow s = (SourceWindow) arg0.getSource();
              
              RunState rs = s.getRunState();
              
              if (rs.removeObserver(s.getLockObserver()) == 1)
                {
                  Proc proc = s.getSwProc();
                  map.remove(proc);

                  unblockProc(proc);
                }

              WindowManager.theManager.sessionManager.show();
              s.hideAll();
            }
        }

      return true;
    }
  }
  
}
