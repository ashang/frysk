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

package frysk.util;

import java.util.HashSet;

import frysk.isa.signals.Signal;
import frysk.proc.Action;
import frysk.proc.Task;
import frysk.proc.TaskAttachedObserverXXX;
import frysk.proc.TaskObserver;
import frysk.proc.TaskObserver.Cloned;
import frysk.proc.TaskObserver.Execed;
import frysk.proc.TaskObserver.Forked;
import frysk.proc.TaskObserver.Instruction;
import frysk.proc.TaskObserver.Signaled;
import frysk.proc.TaskObserver.Syscalls;
import frysk.proc.TaskObserver.Terminated;
import frysk.proc.TaskObserver.Terminating;
import frysk.util.ProcRunUtil.ProcRunObserver;
import frysk.util.ProcRunUtil.RunUtilOptions;
import gnu.classpath.tools.getopt.OptionGroup;


public class ProcFollowUtil {

    private final HashSet knownTasks = new HashSet();
 
    private TaskObserver[] observers;

    ProcRunUtil procRunUtil;
    public ProcFollowUtil(String utilName, String usage, String[] args,
		       TaskObserver[] observers, OptionGroup[] customOptions,
		       RunUtilOptions options) {
	
      this.procRunUtil = new ProcRunUtil(utilName,
            usage,
            args,
            procRunObserver,
            customOptions,
            options
      );
	
      this.observers = observers;
    }

    private void addObservers(Task task) {
	if (knownTasks.add(task)) {
	    
	    for (int i = 0; i < observers.length; i++) {
		this.addTaskObserver(observers[i], task);
	    }
	}
    }
    

    //XXX: this is to handle adding observers according to their types
    //     since task does not provide overloaded functions for adding
    //     observers.
    private void addTaskObserver(TaskObserver observer, Task task){
	boolean handled = false;
	
	if(observer instanceof TaskAttachedObserverXXX){
	    task.requestAddAttachedObserver((TaskAttachedObserverXXX) observer);
	    handled = true;
	}
	if(observer instanceof TaskObserver.Cloned){
	    task.requestAddClonedObserver((Cloned) observer);
	    handled = true;
	}
	if(observer instanceof TaskObserver.Forked){
	    task.requestAddForkedObserver((Forked) observer);
	    handled = true;
	}
	if(observer instanceof TaskObserver.Execed){
	    task.requestAddExecedObserver((Execed) observer);
	    handled = true;
	}
	if(observer instanceof TaskObserver.Terminating){
	    task.requestAddTerminatingObserver((Terminating) observer);
	    handled = true;
	}
	if(observer instanceof TaskObserver.Terminated){
	    task.requestAddTerminatedObserver((Terminated) observer);
	    handled = true;
	}
	if(observer instanceof TaskObserver.Syscalls){
	    task.requestAddSyscallsObserver((Syscalls) observer);
	    handled = true;
	}
	if(observer instanceof TaskObserver.Signaled){
	    task.requestAddSignaledObserver((Signaled) observer);
	    handled = true;
	}
	if(observer instanceof TaskObserver.Instruction){
	    task.requestAddInstructionObserver((Instruction) observer);
	    handled = true;
	}
	
	if(!handled){
	    throw new RuntimeException("Observer type not handled");
	}
    }

    private ProcRunObserver procRunObserver = new ProcRunObserver(){

      public Action updateAttached (Task task)
      {
	addObservers(task);
	return Action.CONTINUE;
      }

      public Action updateForkedOffspring (Task parent, Task offspring){
	addObservers(offspring);
	offspring.requestUnblock(this);
	return Action.BLOCK;
      }

      public void existingTask (Task task)
      {
	addObservers(task);
      }

      public Action updateClonedOffspring (Task parent, Task offspring){
	addObservers(offspring);
	return Action.CONTINUE;
      }

      public Action updateForkedParent (Task parent, Task offspring){return Action.CONTINUE;}
      public Action updateExeced (Task task){return Action.CONTINUE;}
      public Action updateClonedParent (Task task, Task clone){return Action.CONTINUE;}
      public Action updateTerminated (Task task, Signal signal, int value){ return Action.CONTINUE;}
      
      public void taskAdded (Task task){}
      public void taskRemoved (Task task){}

      public void addFailed (Object observable, Throwable w){}
      public void addedTo (Object observable){}
      public void deletedFrom (Object observable){}
      
    };
    
    public void start() {
	procRunUtil.start();
    }
}
