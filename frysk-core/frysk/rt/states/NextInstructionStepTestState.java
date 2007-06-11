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

import frysk.proc.Task;
import frysk.rt.TaskStepEngine;
import frysk.stack.Frame;
import frysk.stack.StackFactory;

public class NextInstructionStepTestState extends State
{
  public NextInstructionStepTestState (Task task)
  {
    this.task = task;
  }
  
  /**
   * Checks to see if the new current frame's FrameIdentifier is equivalent
   * to the old frame's FrameIdentifier. If so, there has been no movement
   * into a new frame and the operation is treated as an instruction step.
   * Otherwise, the breakpoint is set and the instruction step-out begins.
   * 
   * @param tse	the Parent TaskStepEngine
   * @return new StoppedState	No frame change has happened
   * @return new NextInstructionStepState	The current frame has changed
   */
  public State handleUpdate (TaskStepEngine tse)
  {
    Frame newFrame = null;
    newFrame = StackFactory.createFrame(task, 2);
   
    /* The two frames are the same; treat this step-over as an instruction step. */
    if (newFrame.getFrameIdentifier().equals(tse.getFrameIdentifier()))
      {
        return new StoppedState(this.task);
      }
    else
      {
        /* There is a different innermost frame on the stack - run until
         * it exits - success! */
        Frame frame = newFrame.getOuter();
	tse.getSteppingEngine().setBreakpoint(this.task, frame.getAddress());
        return new NextInstructionStepState(this.task);
      }
  }
  
  public boolean isStopped ()
  {
    return false;
  }
}
