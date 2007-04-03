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


package frysk.gui.memory;

import java.util.HashMap;

import org.gnu.glade.LibGlade;
import org.gnu.gtk.event.LifeCycleEvent;
import org.gnu.gtk.event.LifeCycleListener;

import frysk.Config;
import frysk.gui.srcwin.SourceWindowFactory;
import frysk.proc.Proc;
import frysk.proc.Task;
import frysk.rt.RunState;

/**
 * Factory for creating MemoryWindows - allows multiple MemoryWindows to be
 * instantiated for different processes, and disallows multiple windows on the
 * same process. Uses a ProcBlockCounter to co-ordinate the un-blocking of the
 * process between the Register and SourceWindows if the other two are also 
 * running on that process. A singleton class dynamically creating MemoryWindows.
 */
public class MemoryWindowFactory
{
  /* Instance of this class used by the SourceWindow to ensure singularity */
  public static MemoryWindow memWin = null;
  
  /* Keeps track of which MemoryWindows belong to which Task. */
  private static HashMap map = new HashMap();
  
  private final static String MEM_GLADE = "memorywindow.glade";

  /**
   * Performs checks to ensure no other MemoryWindow is running on this Task;
   * if not, assigns a ProcBlockCounter and attaches an Observer if there is
   * no other Window already running on this Task.
   * 
   * @param proc    The Proc to be examined by the new MemoryWindow.
   */
  public static void createMemoryWindow (Proc proc)
		{
				MemoryWindow mw = (MemoryWindow) map.get(proc);

				/* Check if there is already a MemoryWindow running on this task */
				if (mw != null)
						{
								mw = (MemoryWindow) map.get(proc);
								SourceWindowFactory.runState.addObserver(mw.getLockObserver());
								mw.showAll();
								return;
						}

				LibGlade glade;
				try
						{
								glade = new LibGlade(Config.getGladeDir() + MEM_GLADE, null);
						}
				catch (Exception e)
						{
								throw new RuntimeException(e);
						}

				if (SourceWindowFactory.runState == null)
						{
								SourceWindowFactory.runState = new RunState();
								SourceWindowFactory.runState.setProc(proc);
								mw = new MemoryWindow(glade);
								SourceWindowFactory.runState.addObserver(mw.getLockObserver());
						}
				else
						{
								mw = new MemoryWindow(glade);
								SourceWindowFactory.runState.addObserver(mw.getLockObserver());
								mw.finishMemWin(proc);
								mw.setObservable(SourceWindowFactory.runState);
						}

				map.put(proc, mw);
				mw.addListener(new MemWinListener());
				mw.grabFocus();
		}
  
  /**
   * Used by the SourceWindow to assign the static memWin object which it uses
   * to ensure there is only one MemoryWindow running for its Task.
   * 
   * @param proc  The Proc used to find the MemoryWindow representing it.
   */
  public static void setMemWin(Proc proc)
  {
    memWin = (MemoryWindow) map.get(proc);
  }
  
  /**
   * A wrapper for LifeCycleListener which cleans up when the MemoryWindow 
   * is closed.
   */
  private static class MemWinListener
      implements LifeCycleListener
  {

    public void lifeCycleEvent (LifeCycleEvent arg0)
    {
    }

    /**
     * If the MemoryWindow is closed, let the Task know that it isn't being
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
          MemoryWindow mw = (MemoryWindow) arg0.getSource();
          Task t = mw.getMyTask();
          Proc p = t.getProc();
          
          if (SourceWindowFactory.runState.removeObserver(mw.getLockObserver(), p) == 1)
            {
              map.remove(p);
            }

          mw.hideAll();
          return true;
        }

      return false;
    }
  }
  
}

  