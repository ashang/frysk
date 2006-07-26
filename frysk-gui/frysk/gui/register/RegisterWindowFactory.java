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


package frysk.gui.register;

import java.util.prefs.Preferences;

import org.gnu.glade.LibGlade;
import org.gnu.gtk.event.LifeCycleEvent;
import org.gnu.gtk.event.LifeCycleListener;

import frysk.gui.common.prefs.PreferenceManager;
import frysk.proc.Task;

/**
 * @author mcvet
 *
 */
public class RegisterWindowFactory
{

  private static LibGlade glade;

  public static RegisterWindow regWin = null;
  
  private static Task myTask;

  public RegisterWindowFactory (LibGlade glade)
  {

  }

  public static void setGladePath (LibGlade libGlade)
  {
    glade = libGlade;
  }

  public static void createRegisterWindow (Task task)
  {
    if (task.getBlockers().length != 0)
      finishRegWin(task);
  }

  private static void finishRegWin (Task task)
  {
    myTask = task;
    
    try
      {
        regWin = new RegisterWindow(glade);
      }
    catch (Exception e)
      {
        e.printStackTrace();
      }


    regWin.addListener(new LifeCycleListener()
    {
      public boolean lifeCycleQuery (LifeCycleEvent arg0)
      {
        if (arg0.isOfType(LifeCycleEvent.Type.DELETE))
          {
            regWin.hideAll();
            return true;
          }

        return false;
      }

      public void lifeCycleEvent (LifeCycleEvent arg0)
      {
        if (arg0.isOfType(LifeCycleEvent.Type.HIDE))
          regWin.hideAll();
      }
    });
    
    Preferences prefs = PreferenceManager.getPrefs();
    regWin.load(prefs.node(prefs.absolutePath() + "/registers"));

    if (! regWin.hasTaskSet())
      {
        regWin.setIsRunning(false);
        regWin.setTask(myTask);
      }
    else
      regWin.showAll();
  }
  
  public static void killRegWin()
  {
    regWin.setIsRunning(true);
    Preferences prefs = PreferenceManager.getPrefs();
    regWin.hideAll();
    regWin.save(prefs.node(prefs.absolutePath() + "/registers"));
  }
  
}
