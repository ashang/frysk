// This file is part of the program FRYSK.
// 
// Copyright 2005, 2006, 2007, 2008, Red Hat Inc.
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

import frysk.sys.ProcessIdentifier;
import frysk.sys.ProcessIdentifierFactory;
import frysk.rsl.Log;
import frysk.sys.Signal;
import frysk.testbed.TestLib;
import frysk.testbed.TaskSet;
import frysk.testbed.SlaveOffspring;

/**
 * Test that the Proc's ProcTasks Observer correctly reports the tasks
 * belonging to a process.
 */

public class TestProcTasksObserver extends TestLib {
    private static final Log fine = Log.fine(TestProcTasksObserver.class);
		
    /**
     * Check that adding ProcObserver.ProcTasks to a running process
     * correctly notifies the client of the correct number of tasks.
     */
    public void manyExistingThread(SlaveOffspring ackProcess)
    {
	final Proc proc = ackProcess.assertRunToFindProc();
		
	final int count = 4;
			
	MyTester observerTester = new MyTester(proc, count);
	new ProcTasksObserver (proc, observerTester);
		
	assertRunUntilStop("manyExistingThread");
		
	//Ensure there are no calls to taskAdded.
	assertEquals ("taskAddedCount", 0, observerTester.tasksAdded.size());
		
	//Ensure there are no calls to taskRemoved.
	assertEquals ("taskRemovedCounter", 0, observerTester.tasksRemovedCounter);
		
	//Ensure there are 4 independent calls to existingTask.
	assertEquals ("existingTaskCount", count, observerTester.existingTasks.size());
		
	//Ensure there are 4 total calls to existingTask.
	assertEquals ("existingTaskCounter", count, observerTester.existingTaskCounter);
    }
	
    /**
     * Check that adding ProcObserver to a process that has cloned once returns
     * the correct number of existing Threads.
     */
    public void singleExistingClone(SlaveOffspring ackProcess)
    {
	//Create Process
	Proc proc = ackProcess.assertRunToFindProc();
		
	ackProcess.assertSendAddCloneWaitForAcks();
		
	final int count = 2;
		
	//Add observer
	MyTester observerTester = new MyTester(proc, count);
	new ProcTasksObserver (proc, observerTester);
		
	assertRunUntilStop("single existing clone");

	assertEquals ("taskAddedCount", 0, observerTester.tasksAdded.size());

	assertEquals ("taskRemovedCounter", 0, observerTester.tasksRemovedCounter);

	assertEquals ("existingTaskCount", count, observerTester.existingTasks.size());

	assertEquals ("existingTaskCounter", count, observerTester.existingTaskCounter);
    }
	
    /**
     * Check that adding ProcObserver to a process that hasn't cloned returns
     * the correct number of existing threads.
     */
    public void singleExistingThread(SlaveOffspring ackProcess)
    {
	//Create Process
	Proc proc = ackProcess.assertRunToFindProc();
		
	final int count = 1;

	//Add observer
	MyTester observerTester = new MyTester(proc, count);
	new ProcTasksObserver (proc, observerTester);
		
	assertRunUntilStop("single existing thread");
		
	assertEquals ("taskAddedCount", 0, observerTester.tasksAdded.size());
			
	assertEquals ("taskRemovedCounter", 0, observerTester.tasksRemovedCounter);
		
	assertEquals ("existingTaskCount", count, observerTester.existingTasks.size());
		
	assertEquals ("existingTaskCounter", count, observerTester.existingTaskCounter);
    }
	
    /**
     * Check that a new task after the observer is attached is detected.
     */
    public void doClone(SlaveOffspring ackProcess) {
	//		Create Process
		
	Proc proc = ackProcess.assertRunToFindProc();
		
	final int addcount = 1;
	final int existcount = 1;
		
	//Add observer
	ProcTasksTester observerTester = new ProcTasksTester();
	new ProcTasksObserver (proc, observerTester);

	//To make sure that the add observer at least starts being processed
	//before the addClone().
	runPending();
		
	//Add a clone.	
	ackProcess.assertSendAddCloneWaitForAcks();
		
	assertEquals ("taskAddedCount", addcount, observerTester.tasksAdded.size());
		
	assertEquals ("taskAddedCounter", addcount, observerTester.tasksAddedCounter);

	assertEquals ("taskRemovedCounter", 0, observerTester.tasksRemovedCounter);
		
	assertEquals ("existingTaskCount", existcount, observerTester.existingTasks.size());

	assertEquals ("existingTaskCounter", existcount, observerTester.existingTaskCounter);
		
    }
	
    /**
     * Check that deleting a clone is detected.
     */
    public void delete(SlaveOffspring ackProcess) {
	//		Create Process
		
	Proc proc = ackProcess.assertRunToFindProc();
		
	final int delcount = 1;
	final int existcount = 1;
	
	//Add observer
	MyTester observerTester = new MyTester(proc, existcount);
	new ProcTasksObserver (proc, observerTester);	
		
	assertRunUntilStop("delete 1");
		
	//Delete a clone.
	Task task = ackProcess.findTaskUsingRefresh(false);
	ProcessIdentifier tid = ProcessIdentifierFactory.create(task.getTid());
	Signal.BUS.tkill(tid);
		
	assertRunUntilStop("delete 2");
		
	assertEquals ("taskAddedCounter", 0, observerTester.tasksAddedCounter);
		
	assertEquals ("taskRemovedCount", delcount, observerTester.tasksRemoved.size());
		
	assertEquals ("taskRemovedCounter", delcount, observerTester.tasksRemovedCounter);
		
	assertEquals ("existingTaskCount", delcount + existcount, observerTester.existingTasks.size());
		
	assertEquals ("existingTaskCounter", delcount + existcount, observerTester.existingTaskCounter);
		
    }
	
    /**
     * Check that we can add and remove a task and that it will be detected.
     *
     */
    public void cloneThenKill(SlaveOffspring ackProcess)
    {
	//Create Process
		
	Proc proc = ackProcess.assertRunToFindProc();
		
	final int existingcount = 1;
	final int addcount = 1;
	final int delcount = 1;
				
	//Add observer
	MyTester observerTester = new MyTester(proc, existingcount);
	new ProcTasksObserver (proc, observerTester);
		
	assertRunUntilStop("clone then kill");
		
	ackProcess.assertSendAddCloneWaitForAcks();
		
	Task task = ackProcess.findTaskUsingRefresh(false);
	ProcessIdentifier tid = ProcessIdentifierFactory.create(task.getTid());
	Signal.BUS.tkill(tid);
		
	assertRunUntilStop("clone then kill 2");

	assertEquals ("taskAddedCount", addcount, observerTester.tasksAdded.size());
		
	assertEquals ("taskAddedCounter", addcount, observerTester.tasksAddedCounter);
		
	assertEquals ("taskRemovedCount", delcount, observerTester.tasksRemoved.size());
		
	assertEquals ("taskRemovedCounter", delcount, observerTester.tasksRemovedCounter);
		
	assertEquals ("existingTaskCount", existingcount, observerTester.existingTasks.size());

	assertEquals ("existingTaskCounter", existingcount, observerTester.existingTaskCounter);
		
    }
	
	
    public void testManyExistingThreadAttached() {
	SlaveOffspring ackProcess = SlaveOffspring.createAttachedChild()
	    .assertSendAddClonesWaitForAcks(3);
	manyExistingThread(ackProcess);
    }
	
	
    public void testSingleExistingCloneAttached() 
    {
	SlaveOffspring ackProcess = SlaveOffspring.createAttachedChild ();
	singleExistingClone(ackProcess);
    }
	
    public void testCloneThenKillAttached() 
    {
	SlaveOffspring ackProcess = SlaveOffspring.createAttachedChild ();
	cloneThenKill(ackProcess);
    }
	
    public void testDeleteAttached() {
	SlaveOffspring ackProcess = SlaveOffspring.createAttachedChild()
	    .assertSendAddClonesWaitForAcks(1);
	delete(ackProcess);
    }
	
    public void testDoCloneAttached() 
    {
	SlaveOffspring ackProcess = SlaveOffspring.createAttachedChild ();
	doClone(ackProcess);
    }
	
    public void testSingleExistingThreadAttached() {
	SlaveOffspring ackProcess = SlaveOffspring.createAttachedChild();
	singleExistingThread(ackProcess);
    }
	
    public void testManyExistingThreadDetached() {
	SlaveOffspring ackProcess = SlaveOffspring.createChild()
	    .assertSendAddClonesWaitForAcks(3);
	manyExistingThread(ackProcess);
    }
	
    public void testSingleExistingCloneDetached() 
    {
	SlaveOffspring ackProcess = SlaveOffspring.createChild ();
	singleExistingClone(ackProcess);
    }
	
    public void testCloneThenKillDetached() 
    {
	SlaveOffspring ackProcess = SlaveOffspring.createChild ();
	cloneThenKill(ackProcess);
    }
	
    public void testDeleteDetached() {
	SlaveOffspring ackProcess = SlaveOffspring.createChild()
	    .assertSendAddClonesWaitForAcks(1);
	delete(ackProcess);
    }
	
    public void testDoCloneDetached() 
    {
	SlaveOffspring ackProcess = SlaveOffspring.createChild ();
	doClone(ackProcess);
    }
	
    public void testSingleExistingThreadDetached() {
	SlaveOffspring ackProcess = SlaveOffspring.createChild();
	singleExistingThread(ackProcess);
    }
	
    public void testManyExistingThreadAckDaemon() {
	SlaveOffspring ackProcess = SlaveOffspring.createDaemon()
	    .assertSendAddClonesWaitForAcks(3);
	manyExistingThread(ackProcess);
    }
	
    public void testSingleExistingCloneAckDaemon() 
    {
	SlaveOffspring ackProcess = SlaveOffspring.createDaemon ();
	singleExistingClone(ackProcess);
    }
	
    public void testCloneThenKillAckDaemon() 
    {
	SlaveOffspring ackProcess = SlaveOffspring.createDaemon ();
	cloneThenKill(ackProcess);
    }
	
    public void testDeleteAckDaemon() {
	SlaveOffspring ackProcess = SlaveOffspring.createDaemon()
	    .assertSendAddClonesWaitForAcks(1);
	delete(ackProcess);
    }
	
    public void testDoCloneAckDaemon() 
    {
	SlaveOffspring ackProcess = SlaveOffspring.createDaemon ();
	doClone(ackProcess);
    }
	
    public void testSingleExistingThreadAckDaemon() {
	SlaveOffspring ackProcess = SlaveOffspring.createDaemon();
	singleExistingThread(ackProcess);
    }
	
    class ProcTasksTester
	implements ProcObserver.ProcTasks
    {
		
	TaskSet tasksAdded = new TaskSet();
	TaskSet tasksRemoved = new TaskSet();
	TaskSet existingTasks = new TaskSet();
	int existingTaskCounter;
	int tasksAddedCounter;
	int tasksRemovedCounter;
		
	public void taskAdded(Task task) {
	    fine.log("ProcTasksTester.taskAdded() task", task);
	    this.tasksAdded.add(task);
	    tasksAddedCounter++;
	}
		
	public void taskRemoved(Task task)
	{
	    fine.log("ProcTasksTester.taskRemoved() task", task);
	    this.tasksRemoved.add(task);
	    tasksRemovedCounter++;
	}
		
	public void existingTask(Task task)
	{
	    fine.log("ProcTasksTester.existingTask() task", task);
	    this.existingTasks.add(task);
	    existingTaskCounter++;
	}
		
	public void addedTo(Object observable)
	{
	}
		
	public void addFailed(Object observable, Throwable w)
	{
	   throw new RuntimeException("You forgot to implement this method :D ");
	}
		
	public void deletedFrom(Object observable)
	{
	    throw new RuntimeException("You forgot to implement this method :D ");
	}
    }
	
    class MyTester extends ProcTasksTester {
	int c;
	Proc p;
		
	public MyTester(Proc proc, int count) {
	    p = proc;
	    c = count;
	}
		
	public void existingTask(Task task) {
	    super.existingTask(task);
	    fine.log("in MyTester existingTask, existingTaskCounter",
		     existingTaskCounter, "count", c);
	    if (existingTaskCounter == c) {
		fine.log("inside existingTask if statementpid", p);
		Manager.eventLoop.requestStop();
	    }
	}
		
	public void taskRemoved(Task task) {
	    super.taskRemoved(task);
	    fine.log("in MyTester taskRemoved, taskRemovedCounter",
		     tasksRemovedCounter, "count", c);
	    Manager.eventLoop.requestStop();
	}
    }
	
	
}
