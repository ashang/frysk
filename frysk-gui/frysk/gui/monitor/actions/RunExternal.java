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


package frysk.gui.monitor.actions;

import frysk.gui.Gui;
import java.util.logging.Level;
import java.util.logging.Logger;
import frysk.gui.monitor.GuiObject;
import frysk.gui.monitor.ObservableLinkedList;
import frysk.proc.Task;
import frysk.proc.TaskObserver;

import frysk.proc.Action;
import frysk.proc.Manager;

import java.io.File;
import java.util.Date;

import org.gnu.glib.CustomEvents;

/**
 * Provides the ability to execute a binary or other program externally to
 * Frysk, allowing the flexibility of user-defined actions following some event.
 * 
 * @author mcvet
 */
public class RunExternal
    extends TaskAction
{

  /* The input string to be executed */
  private String execString;

  private RunExBlocker blocker;

  private Task theTask;

  private static Logger errorLog = Logger.getLogger (Gui.ERROR_LOG_ID);
  
  public RunExternal ()
  {
    super("Execute an external program from",
          "Execute an external script or binary");
    this.execString = new String();
    this.needInfo = 1;  /* This Action requires user input - the binary path */
  }

  public RunExternal (RunExternal other)
  {
    super(other);
    this.execString = other.execString;
  }

  public void execute (Task task)
  {
    // Manager.host.requestCreateAttachedProc(execString.split(" "), new
    // AttachedObserver());
    this.blocker = new RunExBlocker();
    this.theTask = task;
    task.requestAddAttachedObserver(this.blocker);
  }

  public GuiObject getCopy ()
  {
    return new RunExternal(this);
  }

  public boolean setArgument (String argument)
  {
    String[] temp = argument.split(" ");
    File bin = new File(temp[0]);

    if (bin.exists())
      {
        this.execString = argument;
        return true;
      }
    else
      {
        errorLog.log(Level.SEVERE, new Date() + 
                     " RunExternal.setArgument(String argument): " +
                     "The executable: " + temp[0] + " does not exist!");
        this.execString = temp[0];
        return false;
      }
  }

  public String getArgument ()
  {
    return this.execString;
  }

  public ObservableLinkedList getArgumentCompletionList ()
  {
    return null;
  }

  /**
   * Attach to this executed program so that we know whats going on - we'll be
   * able to gather information or manipulate it in other ways, should the need
   * arise later.
   */
  class AttachedObserver
      implements TaskObserver.Attached
  {

    public void addedTo (Object observable)
    {
    }

    public Action updateAttached (Task task)
    {
      task.requestAddTerminatedObserver(new TaskTerminatedObserver());
      return Action.CONTINUE;
    }

    public void deletedFrom (Object observable)
    {
    }

    public void addFailed (Object observable, Throwable w)
    {
      throw new RuntimeException("Failed to attach to created proc", w);
    }
  }

  /**
   * We want to know when the externally-executed program has quit, so that we
   * can continue or optionally run another program.
   */
  class TaskTerminatedObserver
      implements TaskObserver.Terminated
  {
    public void addedTo (Object observable)
    {
    }

    public Action updateTerminated (Task task, boolean signal, int value)
    {
      theTask.requestUnblock(blocker);
      theTask.requestDeleteAttachedObserver(blocker);
      return Action.CONTINUE;
    }

    public void deletedFrom (Object observable)
    {
    }

    public void addFailed (Object observable, Throwable w)
    {
      throw new RuntimeException("Failed to attach to created proc", w);
    }
  }

  class RunExBlocker
      implements TaskObserver.Attached
  {

    public void addedTo (Object observable)
    {
    }

    public Action updateAttached (Task task)
    {
      System.out.println("blocking");
      CustomEvents.addEvent(new Runnable()
      {
        public void run ()
        {
          Manager.host.requestCreateAttachedProc(execString.split(" "),
                                                 new AttachedObserver());
        }
      });

      return Action.BLOCK;
    }

    public void deletedFrom (Object observable)
    {
    }

    public void addFailed (Object observable, Throwable w)
    {
      throw new RuntimeException("Failed to attach to created proc", w);
    }
  }
}
