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

package frysk.proc;

import java.util.Observable;
import java.util.Observer;
import java.util.logging.Level;


/**
 * Test that the Proc's Offspring Observer correctly reports the tasks
 * belonging to a process.
 */

public class TestOffspringObserver extends TestLib {

    /**
     * Check that adding ProcObserver.Tasks to a running process
     * correctly adds it's self to the correct number of tasks.
     */
    public void testOffspringObserver()
    {
	AckDaemonProcess ackDaemonProcess = new AckDaemonProcess(3);
	Proc proc = ackDaemonProcess.findProcUsingRefresh();
		
	OffspringObserverTester observerTester = new OffspringObserverTester();
	new OffspringObserver (proc, observerTester);
	
	proc.observableAttached.addObserver(new Observer() {
		public void update(Observable arg0, Object arg1) {
		    Manager.eventLoop.requestStop();
		}
	    });
	
	assertRunUntilStop ("running to attach");
	assertEquals ("taskAddedCount", 0, observerTester.tasksAdded.size());
	assertEquals ("taskRemovedCount", 0, observerTester.tasksRemoved.size());
	assertEquals ("existingTaskCount", 4, observerTester.existingTasks.size());
    }
    
    /**
     * Check that we can monitor a clone that forks.
     *
     */
    /*public void testCloneAndFork()
    {
    	
    }*/
	
    class OffspringObserverTester
	implements ProcObserver.Offspring
    {
	
	
	TaskSet tasksAdded = new TaskSet();
	TaskSet tasksRemoved = new TaskSet();
	TaskSet existingTasks = new TaskSet();
		
	public void taskAdded(Task task)
	{
	    this.tasksAdded.add(task);
	}

	public void taskRemoved(Task task)
	{
	    this.tasksRemoved.add(task);
	}

	public void existingTask(Task task)
	{
		logger.log(Level.FINE, "OffspringObserverTester.existingTask() task: {0}\n", task);
	    this.existingTasks.add(task);
	}

	public void addedTo(Object observable)
	{
	}

	public void addFailed(Object observable, Throwable w)
	{
	    // TODO Auto-generated method stub
	    throw new RuntimeException("You forgot to implement this method :D ");
	}

	public void deletedFrom(Object observable)
	{
	    // TODO Auto-generated method stub
	    throw new RuntimeException("You forgot to implement this method :D ");
	}
    }
}
