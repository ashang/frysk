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
// type filter text
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

/*
 * Created on Oct 11, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package frysk.gui.monitor.observers;

import frysk.gui.monitor.GuiObject;
import frysk.gui.monitor.actions.GenericActionPoint;
import frysk.gui.monitor.actions.TaskActionPoint;
import frysk.gui.monitor.filters.TaskFilterPoint;
import frysk.proc.Action;
import frysk.proc.Manager;
import frysk.proc.Task;
import frysk.proc.TaskObserver;

public class TaskSyscallObserver extends TaskObserverRoot implements TaskObserver.Syscall {
	

	public TaskFilterPoint enteringTaskFilterPoint;
	public TaskFilterPoint exitingTaskFilterPoint;
	
	public TaskActionPoint enteringTaskActionPoint;
	public TaskActionPoint exitingTaskActionPoint;
	
	public GenericActionPoint enteringGenericActionPoint;
	public GenericActionPoint exitingGenericActionPoint;

	public TaskSyscallObserver(	) {
		super("Syscall Observer", "Fires when a system call is made.");
		
		this.enteringTaskFilterPoint = new TaskFilterPoint("task entering syscall","the Task when it is entering the syscall");
		this.exitingTaskFilterPoint =  new TaskFilterPoint("task exiting syscall","the Task when it is exiting the syscall");
		
		this.addFilterPoint(enteringTaskFilterPoint);
		this.addFilterPoint(exitingTaskFilterPoint);
		
		this.enteringTaskActionPoint = new TaskActionPoint("task entering syscall","the Task when it is entering the syscall");
		this.exitingTaskActionPoint =  new TaskActionPoint("task exiting syscall","the Task when it is exiting the syscall");
		
		this.addActionPoint(enteringTaskActionPoint);
		this.addActionPoint(exitingTaskActionPoint);
		
		this.enteringGenericActionPoint = new GenericActionPoint("Enter","actions run when the task enters a syscall");
		this.exitingGenericActionPoint = new GenericActionPoint("Exit", "actions run when the task exits a syscall");
		
		this.addActionPoint(enteringGenericActionPoint);
		this.addActionPoint(exitingGenericActionPoint);
	}

	public TaskSyscallObserver(TaskSyscallObserver other) {
		super(other);
		
		this.enteringTaskFilterPoint = new TaskFilterPoint(other.enteringTaskFilterPoint);
		this.exitingTaskFilterPoint  = new TaskFilterPoint(other.exitingTaskFilterPoint);
		
		this.addFilterPoint(enteringTaskFilterPoint);
		this.addFilterPoint(exitingTaskFilterPoint);
		
		this.enteringTaskActionPoint = new TaskActionPoint(other.enteringTaskActionPoint);
		this.exitingTaskActionPoint = new TaskActionPoint(other.exitingTaskActionPoint);
		
		this.addActionPoint(enteringTaskActionPoint);
		this.addActionPoint(exitingTaskActionPoint);
		
		this.enteringGenericActionPoint = new GenericActionPoint(other.enteringGenericActionPoint);
		this.exitingGenericActionPoint  = new GenericActionPoint(other.exitingGenericActionPoint);
		
		this.addActionPoint(enteringGenericActionPoint);
		this.addActionPoint(exitingGenericActionPoint);
	}

	public Action updateSyscallEnter(Task task) {
		final Task myTask = task;
		org.gnu.glib.CustomEvents.addEvent(new Runnable(){
			public void run() {
				enterBottomHalf(myTask);
			}
		});
		return Action.BLOCK;
	}

	protected void enterBottomHalf(Task task) {
		this.setInfo(this.getName()+": "+"PID: " + task.getProc().getPid() + " TID: " + task.getTid() + " Event: enter syscall" + " Host: " + Manager.host.getName());
		if(this.runEnterFilters(task)){
			this.runEnterActions(task);
		}

        Action action = this.whatActionShouldBeReturned();
        if(action == Action.CONTINUE){
          task.requestUnblock(this);
        }
	}

	private void runEnterActions(Task task) {
		super.runActions();
		this.enteringGenericActionPoint.runActions(this);
		this.enteringTaskActionPoint.runActions(task);
	}

	private boolean runEnterFilters(Task task) {
		return this.enteringTaskFilterPoint.filter(task);
	}

	public Action updateSyscallExit(Task task) {
		final Task myTask = task;
		org.gnu.glib.CustomEvents.addEvent(new Runnable(){
			public void run() {
				exitBottomHalf(myTask);
			}
		});
		return Action.BLOCK;
	}

	protected void exitBottomHalf(Task task) {
		this.setInfo("PID: " + task.getProc().getPid() + " TID: " + task.getTid() + " Event: leave " + this.getName() + " Host: " + Manager.host.getName());
		if(this.runExitFilters(task)){
			this.runExitActions(task);
		}
		task.requestUnblock(this);
	}

	private void runExitActions(Task task) {
		super.runActions();
		this.exitingGenericActionPoint.runActions(this);
		this.exitingTaskActionPoint.runActions(task);
	}

	private boolean runExitFilters(Task task) {
		return this.exitingTaskFilterPoint.filter(task);
	}

	public void apply(Task task){
		task.requestAddSyscallObserver(this);
	}
	
	public GuiObject getCopy(){
		return new TaskSyscallObserver(this);
	}

    public void unapply (Task task)
    {
      task.requestAddSyscallObserver(this);
    }
	
}
