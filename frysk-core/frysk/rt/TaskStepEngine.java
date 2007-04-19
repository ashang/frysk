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

public class TaskStepEngine
{
  private Task task;
  private Dwfl dwfl;
  private State state;
  private FrameIdentifier fi;
  private int line = 0;
  
  public TaskStepEngine (Task task)
  {
    this.task = task;
    this.state = new StoppedState(task);
  }
  
  public boolean isStopped ()
  {
    return this.state.isStopped();
  }
  
  public boolean handleUpdate ()
  {
    State s = this.state.handleUpdate();
    if (s.isStopped())
      {
	this.state = s;
	return true;
      }
    
    return false;
  }
  
  /**
   * Returns the Dwfl for this Task, and inserts it into the Dwfl map.
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
  
  public int getLine ()
  {
    return this.line;
  }
  
  public void setLine (int line)
  {
    this.line = line;
  }
  
  public Task getTask ()
  {
    return this.task;
  }
  
  public void setState (State newState)
  {
    this.state = newState;
  }
  
  public State getState ()
  {
    return this.state;
  }
  
  public void setFrameIdentifier (FrameIdentifier fi)
  {
    this.fi = fi;
  }
  
  public FrameIdentifier getFrameIdentifier ()
  {
    return this.fi;
  }
}
