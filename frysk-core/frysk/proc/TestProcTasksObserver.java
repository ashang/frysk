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

import java.util.logging.Level;
import frysk.sys.Signal;
import frysk.sys.Sig;

/**
 * Test that the Proc's ProcTasks Observer correctly reports the tasks
 * belonging to a process.
 */

public class TestProcTasksObserver extends TestLib {
		
    /**
     * Check that adding ProcObserver.ProcTasks to a running process
     * correctly notifies the client of the correct number of tasks.
     */
    public void manyExistingThread(AckProcess ackProcess)
    {
	final Proc proc = ackProcess.findProcUsingRefresh();
		
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
    public void singleExistingClone(AckProcess ackProcess)
    {
	//Create Process
	Proc proc = ackProcess.findProcUsingRefresh();
		
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
    public void singleExistingThread(AckProcess ackProcess)
    {
	//Create Process
	Proc proc = ackProcess.findProcUsingRefresh();
		
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
    public void doClone(AckProcess ackProcess) {
	//		Create Process
		
	Proc proc = ackProcess.findProcUsingRefresh();
		
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
    public void delete(AckProcess ackProcess) {
	//		Create Process
		
	Proc proc = ackProcess.findProcUsingRefresh();
		
	final int delcount = 1;
	final int existcount = 1;
	
	//Add observer
	MyTester observerTester = new MyTester(proc, existcount);
	new ProcTasksObserver (proc, observerTester);	
		
	assertRunUntilStop("delete 1");
		
	//Delete a clone.
	Task task = ackProcess.findTaskUsingRefresh(false);
	Signal.tkill(task.getTid(), Sig.BUS);
		
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
    public void cloneThenKill(AckProcess ackProcess)
    {
	//Create Process
		
	Proc proc = ackProcess.findProcUsingRefresh();
		
	final int existingcount = 1;
	final int addcount = 1;
	final int delcount = 1;
				
	//Add observer
	MyTester observerTester = new MyTester(proc, existingcount);
	new ProcTasksObserver (proc, observerTester);
		
	assertRunUntilStop("clone then kill");
		
	ackProcess.assertSendAddCloneWaitForAcks();
		
	Task task = ackProcess.findTaskUsingRefresh(false);
	Signal.tkill(task.getTid(), Sig.BUS);
		
	assertRunUntilStop("clone then kill 2");

	assertEquals ("taskAddedCount", addcount, observerTester.tasksAdded.size());
		
	assertEquals ("taskAddedCounter", addcount, observerTester.tasksAddedCounter);
		
	assertEquals ("taskRemovedCount", delcount, observerTester.tasksRemoved.size());
		
	assertEquals ("taskRemovedCounter", delcount, observerTester.tasksRemovedCounter);
		
	assertEquals ("existingTaskCount", existingcount, observerTester.existingTasks.size());

	assertEquals ("existingTaskCounter", existingcount, observerTester.existingTaskCounter);
		
    }
	
	
    public void testManyExistingThreadAttached() {
	AckProcess ackProcess = new AttachedAckProcess(3);
	manyExistingThread(ackProcess);
    }
	
	
    public void testSingleExistingCloneAttached() 
    {
	AckProcess ackProcess = new AttachedAckProcess ();
	singleExistingClone(ackProcess);
    }
	
    public void testCloneThenKillAttached() 
    {
	AckProcess ackProcess = new AttachedAckProcess ();
	cloneThenKill(ackProcess);
    }
	
    public void testDeleteAttached() {
	AckProcess ackProcess = new AttachedAckProcess (1);
	delete(ackProcess);
    }
	
    public void testDoCloneAttached() 
    {
	AckProcess ackProcess = new AttachedAckProcess ();
	doClone(ackProcess);
    }
	
    public void testSingleExistingThreadAttached() {
	AckProcess ackProcess = new AttachedAckProcess();
	singleExistingThread(ackProcess);
    }
	
    public void testManyExistingThreadDetached() {
	AckProcess ackProcess = new DetachedAckProcess(3);
	manyExistingThread(ackProcess);
    }
	
    public void testSingleExistingCloneDetached() 
    {
	AckProcess ackProcess = new DetachedAckProcess ();
	singleExistingClone(ackProcess);
    }
	
    public void testCloneThenKillDetached() 
    {
	AckProcess ackProcess = new DetachedAckProcess ();
	cloneThenKill(ackProcess);
    }
	
    public void testDeleteDetached() {
	AckProcess ackProcess = new DetachedAckProcess (1);
	delete(ackProcess);
    }
	
    public void testDoCloneDetached() 
    {
	AckProcess ackProcess = new DetachedAckProcess ();
	doClone(ackProcess);
    }
	
    public void testSingleExistingThreadDetached() {
	AckProcess ackProcess = new DetachedAckProcess();
	singleExistingThread(ackProcess);
    }
	
    public void testManyExistingThreadAckDaemon() {
	AckProcess ackProcess = new AckDaemonProcess(3);
	manyExistingThread(ackProcess);
    }
	
    public void testSingleExistingCloneAckDaemon() 
    {
	AckProcess ackProcess = new AckDaemonProcess ();
	singleExistingClone(ackProcess);
    }
	
    public void testCloneThenKillAckDaemon() 
    {
	AckProcess ackProcess = new AckDaemonProcess ();
	cloneThenKill(ackProcess);
    }
	
    public void testDeleteAckDaemon() {
	AckProcess ackProcess = new AckDaemonProcess (1);
	delete(ackProcess);
    }
	
    public void testDoCloneAckDaemon() 
    {
	AckProcess ackProcess = new AckDaemonProcess ();
	doClone(ackProcess);
    }
	
    public void testSingleExistingThreadAckDaemon() {
	AckProcess ackProcess = new AckDaemonProcess();
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
		
	public void taskAdded(Task task)
	{
	    logger.log(Level.FINE, "ProcTasksTester.taskAdded() task: {0}\n", task);
	    this.tasksAdded.add(task);
	    tasksAddedCounter++;
	}
		
	public void taskRemoved(Task task)
	{
	    logger.log(Level.FINE, "ProcTasksTester.taskRemoved() task: {0}\n", task);
	    this.tasksRemoved.add(task);
	    tasksRemovedCounter++;
	}
		
	public void existingTask(Task task)
	{
	    logger.log(Level.FINE, "ProcTasksTester.existingTask() task: {0}\n", task);
	    this.existingTasks.add(task);
	    existingTaskCounter++;
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
	
    class MyTester extends ProcTasksTester {
	int c;
	Proc p;
		
	public MyTester(Proc proc, int count) {
	    p = proc;
	    c = count;
	}
		
	public void existingTask(Task task) {
	    super.existingTask(task);
	    logger.log(Level.FINEST, "in MyTester existingTask, " +
		       "existingTaskCounter : {0}, count : {1}", new Object[] {new 
									       Integer(existingTaskCounter), new Integer(c)});
	    if (existingTaskCounter == c) {
		logger.log(Level.FINEST, "inside existingTask if statementpid : {0}", p);
		Manager.eventLoop.requestStop();
	    }
	}
		
	public void taskRemoved(Task task) {
	    super.taskRemoved(task);
	    logger.log(Level.FINEST, "in MyTester taskRemoved, " +
		       "taskRemovedCounter : {0}, count : {1}", new Object[] {new 
									      Integer(tasksRemovedCounter), new Integer(c)});
	    Manager.eventLoop.requestStop();
	}
    }
	
	
}
