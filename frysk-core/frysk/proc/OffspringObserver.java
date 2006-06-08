// This file is part of the program FRYSK.
//
// Copyright 2005, 2006, Red Hat Inc.
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

package frysk.proc;

import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import frysk.Config;
import frysk.event.Event;

/**
 * Provides a mechanism for tracing all clone events within a process.
 */

public final class OffspringObserver
    implements TaskObserver.Cloned, TaskObserver.Forked
{
    protected static final Logger logger = Logger.getLogger (Config.FRYSK_LOG_ID);
    private final Proc proc;
    private final ProcObserver.Offspring offspringObserver;
    private Task mainTask;
    /**
     * An observer that monitors all Task-s of a process notifying the
     * caller of each new Task as it is added.
     */
    public OffspringObserver (Proc theProc, ProcObserver.Offspring theOffspringObserver)
    {
	logger.log (Level.FINE, "{0} new\n", this); 
	proc = theProc;
	offspringObserver = theOffspringObserver;

	// The rest of the construction must be done synchronous to
	// the EventLoop, schedule it.
	Manager.eventLoop.add (new Event ()
	    {
		public void execute ()
		{
		    // Get a preliminary list of tasks - XXX: hack really.
		    proc.sendRefresh ();
		    
		    mainTask = Manager.host.get (new TaskId (proc.getPid ()));
		    if (mainTask == null) {
			offspringObserver.addFailed (proc,
						 new RuntimeException ("Process lost"));
			return;
		    }
		    mainTask.requestAddClonedObserver (OffspringObserver.this);
		}
	    });
    }

    // Never block the parent.
    public Action updateClonedParent (Task parent,
				      Task offspring)
    {
	return Action.CONTINUE;
    }
    
    /**
     * Whenever a new offspring appears notify the observer,
     * and add both a cloned observer and a forked observer to it.
     * @param parent The parent task
     * @param offspring The offspring task
     * @return The action to take, BLOCK
     */
    public Action updateOffspring (Task parent,
    				Task offspring)
    {
    	offspringObserver.taskAdded (offspring);
    	offspring.requestAddClonedObserver (this);
    	offspring.requestAddForkedObserver (this);
    	// Need to BLOCK and UNBLOCK so that the
    	// request to add an observer has enough time
    	// to be processed before the task continues.
    	offspring.requestUnblock (this);
    	return Action.BLOCK;
    }
    
    /**
     * When ever a new cloned offspring appears notify the observer,
     * and add a cloned observer to it.
     */
    public Action updateClonedOffspring (Task parent,
					 Task offspring)
    {
    	return updateOffspring(parent, offspring);
    }
    
    public void addedTo(Object observable)
    {
	Task addedTask = (Task) observable;
	if (addedTask == mainTask) {
	    // XXX: Is there a race here with a rapidly cloning task?
	    for (Iterator iterator = proc.getTasks().iterator();
		 iterator.hasNext(); ) {
		Task task = (Task) iterator.next();
		offspringObserver.existingTask (addedTask);
		if (task != mainTask)
		    task.requestAddClonedObserver (this);
			task.requestAddForkedObserver (this);
	    }
	}
    }

    public void addFailed(Object observable, Throwable w)
    {
	throw new RuntimeException("How did this (addFailed) happen ?!");
    }

    public void deletedFrom(Object observable)
    {
	offspringObserver.taskRemoved ((Task) observable);
    }

	public Action updateForkedParent(Task parent, Task offspring) {
		return Action.CONTINUE;
	}

	public Action updateForkedOffspring(Task parent, Task offspring) {
		return updateClonedOffspring(parent, offspring);
	}
}
