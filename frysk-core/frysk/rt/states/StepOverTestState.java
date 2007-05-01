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

package frysk.rt.states;

import lib.dw.DwflLine;
import frysk.proc.Task;
import frysk.rt.StackFactory;
import frysk.rt.Frame;
import frysk.rt.SteppingEngine;
import frysk.rt.TaskStepEngine;

public class StepOverTestState extends State
{
  public StepOverTestState (Task task)
  {
    this.task = task;
  }
  
  /**
   * Begins the process of stepping-over a line for a Task. Continues to step
   * instructions until the line changes. If, when that happens, the Task is still
   * in the same frame as before, simply stops the stepping and treats the
   * operation as a line step. Otherwise, sets a breakpoint and runs the
   * Task until it returns.
   * 
   * @param tse 	The parent TaskStepEngine
   * @return new StoppedState 	If there was no frame change
   * @return new StepOverState 	If the frame changed with the line
   * @return this 	If the line has not changed yet
   */
  public State handleUpdate (TaskStepEngine tse)
  {
    DwflLine line = tse.getDwflLine();

    int lineNum;

    if (line == null) /* We're in no-debuginfo land */
      lineNum = 0;
    else
      lineNum = line.getLineNum();
    
    int prev = tse.getLine();	

    if (lineNum != prev)
      {
	tse.setLine(lineNum);
	Frame newFrame = StackFactory.createFrame(task);

	/* The two frames are the same; treat this step-over as an instruction step. */
	if (newFrame.getFrameIdentifier().equals(
						 tse.getFrameIdentifier()))
	  {
	    return new StoppedState(this.task);
	  }
	else
	  {
	    /* There is a different innermost frame on the stack - run until
	     * it exits - success! */
	    Frame frame = newFrame.getOuter();
	    SteppingEngine.setBreakpoint(this.task, frame.getAddress());
	    return new StepOverState(this.task);
	  }	
      }
    else
      {
	this.task.requestUnblock(SteppingEngine.getSteppingObserver());
	return this;
      }
  }
  
  public boolean isStopped ()
  {
    return false;
  }
}
