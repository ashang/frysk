// This file is part of the program FRYSK.
//
// Copyright 2005, 2006 Red Hat Inc.
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

package frysk.proc;

/**
 * The binding between an Observer and its Observable.
 */

abstract class TaskObservation
    extends Observation
{
    private final Task task;
    private final Runnable action;
    private final boolean adding;

    public TaskObservation (Task task, TaskObservable observable,
			    TaskObserver observer, boolean adding)
    {
      this(task, observable, observer, null, adding);
    }

    /**
     * Create a new Observer binding.
     *
     * @param task The Task the observer will be added to.
     * @param observable The TaskObservable (of the specified task)
     * that the observer should be added to.
     * @param observer The TaskObserver to add.
     * @param action An action to run, or null if none, before adding
     * or after deletion while the Task is (temporarily) suspended.
     */
    public TaskObservation (Task task, TaskObservable observable,
			    TaskObserver observer, Runnable action,
			    boolean adding)
    {
	super (observable, observer);
	this.task = task;
	this.action = action;
	this.adding = adding;
    }
    /**
     * Handle adding the Observer to the Observable.
     * The Task should call <code>add()</code> when it is actually
     * ready to bind the observer to the observable.
     *
     * @see TaskObservation#needsSuspendedAction()
     */
    public void handleAdd ()
    {
	task.handleAddObservation(this);
    }
    /**
     * Handle deleting the Observer from the Observable.
     * The Task should call <code>delete()</code> when it is actually
     * ready to bind the observer to the observable.
     *
     * @see TaskObservation#needsSuspendedAction()
     */
    public void handleDelete ()
    {
	task.handleDeleteObservation(this);
    }

    public TaskObservable getTaskObservable()
    {
      return (TaskObservable) observable;
    }

    public TaskObserver getTaskObserver()
    {
      return (TaskObserver) observer;
    }

    /**
     * Returns true if this is an addition, false if it is a deletion.
     */
    public boolean isAddition()
    {
      return adding;
    }

    /**
     * Returns true if an action was supplied to the constructor to be
     * run when the Task is (temporarily) suspended. If this method
     * returns true then the Task should be suspended before calling
     * <code>add()</code> or <code>delete()</code> on this
     * TaskObservation.
     */
    public boolean needsSuspendedAction()
    {
      return action != null;
    }

    /**
     * Runs any action (if suplied) and then adds the observer to the
     * observable. If <code>needsSuspendedAction()</code> returns true
     * then this method should only be called if the Task is
     * (temporarily) suspended.
     */
    public void add()
    {
      if (action != null)
	action.run();
      observable.add(observer);
    }

    /**
     * Deletes the observer from the observable and then runs any
     * action (if suplied). If <code>needsSuspendedAction()</code>
     * returns true then this method should only be called if the Task
     * is (temporarily) suspended.
     */
    public void delete()
    {
      observable.delete(observer);
      if (action != null)
	action.run();
    }
}
