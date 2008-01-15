// This file is part of the program FRYSK.
//
// Copyright 2008, Red Hat Inc.
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

package frysk.stepping;

import lib.dwfl.DwflLine;
import frysk.proc.Task;
import frysk.stack.Frame;
import frysk.stack.StackFactory;

public class InstructionStepThroughState extends State {

    private String name;
    private int steppingOut = 0;

    public InstructionStepThroughState(Task task) {
	this.task = task;
	this.name = null;
    }

    public InstructionStepThroughState(Task task, String name) {
	this.task = task;
	this.name = name;
    }

    /**
     * State used to represent when a task has entered some section of the program (such
     * as a .plt section) which must be carefully stepped through.
     * 
     * @param tse
     *                The TaskStepEngine for this State.
     * @return this	If more stepping needs to be done.
     * @return StoppedState	If we have returned to user code. 
     */
    public State handleUpdate(TaskStepEngine tse) {

	Frame frame = StackFactory.createFrame(task);
	
	/* This object has already performed a step-out operation on the task.
	 * Here, clean up and perform another to return to user code. */
	if (this.steppingOut == 1) {

	    this.steppingOut = 2;
	    tse.getSteppingEngine().removeBreakpoint(this.task);
	    
	    /* Here, we're back in  _dl_runtime_resolve - step out of this as well. */
	    tse.getSteppingEngine().stepOut(this.task, frame, this); 
	    return this;
	    
	} else if (this.steppingOut == 2) {
	    
	    this.steppingOut = 0;
	    tse.getSteppingEngine().removeBreakpoint(this.task);
	}

	/* The frame being searched for has been reached. Perform a step-out. */
	if (frame.getSymbol().getDemangledName().equals(this.name)) {
	    
	    this.steppingOut = 1;
	    tse.getSteppingEngine().stepOut(this.task, frame, this);
	    return this;

	} else if (frame.getSymbol().getDemangledName().contains("_start")) {
	    /* Avoid handling any code having to do with the initialization of a process */
	    
	    return new StoppedState(this.task);
	    
	} else {
	    
	    /* Inside some code without debuginfo, and isn't the function we're interested in.
	     * Continue stepping through it. */
	    if (tse.getLine() == 0) {
		
		DwflLine line = tse.getDwflLine();

		if (line == null) {
		    tse.getSteppingEngine()
			    .continueForStepping(this.task, true);
		    return this;
		} else {
		    /* Back in debuginfo. Finish the stepping operation. */
		    return new StoppedState(this.task);
		}
	    } else {
		/* Back in debuginfo. Finish the stepping operation. */
		return new StoppedState(this.task);
	    }
	}
    }

    public boolean isStopped() {
	return false;
    }

    public boolean isAlive() {
	return true;
    }
}
