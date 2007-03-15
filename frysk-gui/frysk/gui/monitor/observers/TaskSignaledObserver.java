// This file is part of the program FRYSK.
//
// Copyright 2006, Red Hat Inc.
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
package frysk.gui.monitor.observers;

import frysk.gui.monitor.GuiObject;
import frysk.gui.monitor.GuiTask;
import frysk.gui.monitor.actions.TaskActionPoint;
import frysk.gui.monitor.eventviewer.Event;
import frysk.gui.monitor.eventviewer.EventManager;
import frysk.gui.monitor.filters.TaskFilterPoint;
import frysk.proc.Action;
import frysk.proc.Manager;
import frysk.proc.Task;
import frysk.proc.TaskObserver;
import frysk.sys.Sig;

public class TaskSignaledObserver extends TaskObserverRoot implements
		TaskObserver.Signaled {

	public TaskFilterPoint taskFilterPoint;

	public TaskActionPoint taskActionPoint;

	/**
	 * TaskSignalObserver. Extends TaskObserverRoot and implements TaskObserver.Signaled
	 * 
	 * Provides functionality for the Signaled Frysk Core observer in the UI.
	 */
	public TaskSignaledObserver() {
		super("Signaled Observer",
				"Fires every time this tasks has a pending signal l");

		taskFilterPoint = new TaskFilterPoint("Pending Signal Thread",
				"The thread that has the pending signal");
		addFilterPoint(taskFilterPoint);

		taskActionPoint = new TaskActionPoint(taskFilterPoint.getName(),
				taskFilterPoint.getToolTip());
		addActionPoint(taskActionPoint);

	}

	/**
	 * TaskSignalObserver. Extends TaskObserverRoot and implements TaskObserver.Signaled
	 * 
	 * Provides functionality for the Signaled Frysk Core observer in the UI. Produces a
	 * new TaskSignalOberser from the observer passed as a parameter. This is used in cloning
	 * the observer.
	 */
	public TaskSignaledObserver(final TaskSignaledObserver other) {
		super(other);

		taskFilterPoint = new TaskFilterPoint(other.taskFilterPoint);
		addFilterPoint(taskFilterPoint);

		taskActionPoint = new TaskActionPoint(other.taskActionPoint);
		addActionPoint(taskActionPoint);
	}

	/* (non-Javadoc)
	 * @see frysk.gui.monitor.observers.TaskObserverRoot#apply(frysk.proc.Task)
	 */
	public void apply(final Task task) {
		task.requestAddSignaledObserver(this);
	}

	/* (non-Javadoc)
	 * @see frysk.gui.monitor.observers.TaskObserverRoot#unapply(frysk.proc.Task)
	 */
	public void unapply(final Task task) {
		task.requestDeleteSignaledObserver(this);
	}

	/* (non-Javadoc)
	 * @see frysk.proc.TaskObserver$Signaled#updateSignaled(frysk.proc.Task, int)\]
	 * 
	 */
	public Action updateSignaled(final Task task, final int signal) {
		org.gnu.glib.CustomEvents.addEvent(new Runnable() {
			public void run() {
				bottomHalf(task, signal);
			}
		});
		return Action.BLOCK;
	}

	private void bottomHalf(final Task task, final int signal) {
		setInfo(getName() + ": " + "PID: " + task.getProc().getPid()
				+ " TID: " + task.getTid() + " Event: has pending signal: "
				+ Sig.toPrintString(signal) + " Host: " + Manager.host.getName());
		if (runFilters(task, signal)) {
			this.runActions(task, signal);
		}

		final Action action = whatActionShouldBeReturned();
		if (action == Action.CONTINUE) {
			task.requestUnblock(this);
		}
	}

	private void runActions(final Task task, int signal) {
        Event event = new Event("signaled " + Sig.toString(signal), "task recieved signal " + Sig.toString(signal), GuiTask.GuiTaskFactory.getGuiTask(task), this);
		super.runActions();
		taskActionPoint.runActions(task, this, event);
        EventManager.theManager.addEvent(event);
	}

	private boolean runFilters(final Task task, int signal) {
		return filter(task);
	}

	public GuiObject getCopy() {
		return new TaskSignaledObserver(this);
	}

	private boolean filter(final Task task) {
		return taskFilterPoint.filter(task);
	}

}
