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

import java.util.logging.Level;
import frysk.event.TimerEvent;
import frysk.proc.ProcObserver.ProcTasks;

/**
 * Check that the observer TaskObserver.Forked works.
 */

public class StressAttachDetachRapidlyForkingMainTask
extends TestLib
{
	static int numberOfForks = 450;
	static int numberOfForksResident = 2;
	
	/**
	 * Test that the fork count from a sub-program that, in turn,
	 * creates lots and lots of sub-processes matches the expected.
	 */
	public void testTaskForkedObserver ()
	{

		// This test will fail when run with ./TestRunner -c
		// as the intense logging will simulate enough load on the core
		// for the refresh to happen and the child to exit, causeing the 
		// race condition.

		// Test for bz 2803. Have to exit here as the test will fail.

		if (brokenXXX ()) return;

		// Run a program that forks wildly.
		AttachedDaemonProcess child = new AttachedDaemonProcess (new String[]
		                                                                    {
				getExecPrefix () + "funit-forks",
				Integer.toString (numberOfForks),
				Integer.toString (numberOfForksResident)
		                                                                    });

		// Watch for any Task fork events, accumulating them as they
		// arrive.
		class ForkObserver extends TaskObserverBase
		implements TaskObserver.Forked
		{
			public int count;
			public int failedCount =0;
			
			public void addFailed (Object o, Throwable w)
			{
				failedCount++;
			}
			public Action updateForkedParent (Task parent, Task offspring)
			{
				count++;
				
				logger.log(Level.INFO, "updatedForkedParent count of: " + count + " for: " + 
				parent.getProc().getCommand());
				parent.requestUnblock (this);
				return Action.BLOCK;
			}
			
			public Action updateForkedOffspring(Task parent,
					final Task offspring) {
				
				logger.log(Level.INFO, "updatedForkedOffspring count of: " + count + 
				" belonging to parent: " + parent.getProc().getCommand() + 
				". My child ID is: " + offspring.getTid());
				
				offspring.requestAddForkedObserver(ForkObserver.this);
				offspring.requestUnblock(ForkObserver.this);
				if (count == numberOfForks)
					Manager.eventLoop.requestStop();
				return Action.BLOCK;
			}
		}
		ForkObserver forkObserver = new ForkObserver ();

		// Add a tasks observer to add observers to fork's children
		new ProcTasksObserver(child.mainTask.getProc(), new ProcTasks(){
			public void deletedFrom(Object observable)
			{
				logger.log(Level.INFO,"ProcTasksObserver.deleted from fired");
			}
			
			public void addFailed(Object observable, Throwable w)
			{
				logger.log(Level.INFO,"ProcTasksObserver.addFailed failed");
			}
			
			public void addedTo(Object observable)
			{
				logger.log(Level.INFO,"ProcTasksObserver.addedTo fired");
			}
			
			public void existingTask(Task task)
			{
				logger.log(Level.INFO,"ProcTasksObserver.existingTask fired");				      
			}
			
			public void taskRemoved(final Task task)
			{
				logger.log(Level.INFO,"ProcTasksObserver.taskRemoved fired");				   
			}
			
			public void taskAdded(final Task task)
			{
				logger.log(Level.INFO,"ProcTasksObserver.taskAdded fired");				   
			}
		}
		);
		
		// Add the fork observer
		child.mainTask.requestAddForkedObserver (forkObserver);
	
		// Create a refresh time with a low refresh.
                 TimerEvent refreshTimer = new TimerEvent(0, 500){
                 	public void execute() {
                        	Manager.host.requestRefreshXXX (true);
                        }
                 };

                Manager.eventLoop.add (refreshTimer);
	
		// Go ....
		child.resume();
		assertRunUntilStop ("run \"fork\" until exit");
		
		assertEquals ("number of child processes created",
				numberOfForks, forkObserver.count);
		
		assertEquals("Failed count",0,forkObserver.failedCount);
		
	}
}

