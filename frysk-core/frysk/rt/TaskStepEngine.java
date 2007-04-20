// This file is part of the program FRYSK.
//
// Copyright 2007, Red Hat Inc.
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

package frysk.rt;

import frysk.proc.Task;
import frysk.rt.states.*;
import lib.dw.Dwfl;
import lib.dw.DwflLine;

/**
 * Maintains stepping-relevant information for a given task, including its
 * Dwfl object, its FrameIdentifier, line number and State. Used by
 * the SteppingEngine to manipulate the State of a given Task. 
 * 
 *  Also performs state-transition work when called by SteppingEngine, by
 *  calling State.handleUpdate().
 */
public class TaskStepEngine
{
  /* The Task for this TaskStepEngine */
  private Task task;
  
  /* The Dwfl object for task */
  private Dwfl dwfl;
  
  /* The current State of task */
  private State state;
  
  /* The FrameIdentifier for task */
  private FrameIdentifier fi;
  
  /* task's current source line number */
  private int line = 0;
  
  /**
   * Builds a new TaskStepEngine, setting only the Task field and defaulting
   * to the StoppedState.
   * 
   * @param task The Task for this TaskStepEngine to manage
   */
  public TaskStepEngine (Task task)
  {
    this.task = task;
    this.state = new StoppedState(task);
  }
  
  /**
   * Returns true if the current State of this TaskStepEngine's Task field
   * is a StoppedState.
   * 
   * @return true The Task is stopped
   * @return false The Task is not stopped
   */
  public boolean isStopped ()
  {
    return this.state.isStopped();
  }
  
  /**
   * Handles calls from the SteppingObserver in the SteppingEngine class. 
   * Called when the TaskObserver.Instruction stepping callback is reached,
   * which prompts this TaskStepEngine's State class to perform the next
   * step in this particular State's state transition. 
   *  
   * @return true If the returned State is stopped
   * @return false If the return State is no stopped
   */
  public boolean handleUpdate ()
  {
    /* Perform the next tentative state transition */
    State s = this.state.handleUpdate();
    if (s.isStopped())
      {
	this.state = s;
	return true;
      }
    
    return false;
  }
  
  /**
   * Creates the Dwfl object if it hasn't been created yet, and returns its 
   * current DwflLine object.
   * 
   * @return dline The DwflLine for this Engine's Task
   */
  public DwflLine getDwflLine ()
  {
    if (this.dwfl == null)
      this.dwfl = new Dwfl(task.getTid());
    
    DwflLine dline = this.dwfl.getSourceLine(this.task.getIsa().pc(task));
    return dline;
  }
  
  /**
   * Returns the current source line for this Task.
   * 
   * @return line The current source line for this Task
   */
  public int getLine ()
  {
    return this.line;
  }
  
  /**
   * Sets the current source line for this Task
   * 
   * @param line The current source line for this Task
   */
  public void setLine (int line)
  {
    this.line = line;
  }
  
  /**
   * Returns this TaskStepEngine's Task.
   * 
   * @return task The Task for this TaskStepEngine
   */
  public Task getTask ()
  {
    return this.task;
  }
  
  /**
   * Returns the current State of this TaskStepEngine's Task.
   * 
   * @param newState The current State of this TaskStepEngine's Task
   */
  public void setState (State newState)
  {
    this.state = newState;
  }
  
  /**
   * Sets the current State of this TaskStepEngine's Task.
   * 
   * @return state The current State of this TaskStepEngine's Task.
   */
  public State getState ()
  {
    return this.state;
  }
  
  /**
   * Sets the FrameIdentifier for this TaskStepEngine.
   * 
   * @param fi The new FrameIdentifier
   */
  public void setFrameIdentifier (FrameIdentifier fi)
  {
    this.fi = fi;
  }
  
  /**
   * Returns this TaskStepEngine's FrameIdentifier field.
   * 
   * @return fi This TaskStepEngine's FrameIdentifier
   */
  public FrameIdentifier getFrameIdentifier ()
  {
    return this.fi;
  }
}
