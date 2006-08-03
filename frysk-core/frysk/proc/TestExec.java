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

import frysk.sys.Signal;

/**
 * Test the exec event.
 *
 * The exec needs to completely replace the existing (possibly
 * multi-threaded) process with an entirely new one.
 */

public class TestExec
    extends TestLib
{
    /**
     * Count the number of task exec calls.
     */
    protected class ExecBlockCounter
	extends TaskObserverBase
	implements TaskObserver.Execed
    {
	int numberExecs;
	public Action updateExeced (Task task)
	{
	    numberExecs++;
	    Manager.eventLoop.requestStop ();
	    return Action.BLOCK;
	}
	public void addedTo (Object obj)
	{
	    super.addedTo (obj);
	    Manager.eventLoop.requestStop ();
	}
	ExecBlockCounter (Task task)
	{
	    task.requestAddExecedObserver (this);
	}
    }

    /**
     * A simple (single threaded) program performs an exec, check that
     * the exec blocks and resumes the process.
     */
    public void testProcBlockExec ()
    {
	// Create a temp file that the exec'd program will remove.
	// That way it's possible to confirm that the exec did work.
	TmpFile tmpFile = new TmpFile ();
	AckProcess child = new DetachedAckProcess
	    ((String)null, new String[] { "/bin/rm", tmpFile.toString (), });
	Task task = child.findTaskUsingRefresh (true);

	// Create an exec observer attached to Task, forcing an
	// attach.
	ExecBlockCounter execBlockCounter = new ExecBlockCounter (task);
	assertRunUntilStop ("add execBlockCounter");

	// Trigger the exec, when it occurs the task will be blocked.
	Signal.tkill (task.getTid (), execSig);
	assertRunUntilStop ("wait for exec");
	assertTrue ("tmp file exists", tmpFile.stillExists ());

	// Unblock the process, let it exit.
	new StopEventLoopWhenChildProcRemoved ();
	task.requestUnblock (execBlockCounter);
	assertRunUntilStop ("wait for exec program exit");

	assertEquals ("number of execs", 1, execBlockCounter.numberExecs);
	assertFalse ("tmp file exists", tmpFile.stillExists ());
    }

    /**
     * A multi-tasked program's non main task performs an exec, check
     * that it is correctly tracked.
     *
     * This case is messy, the exec blows away all but the exec task,
     * making the exec task the new main task.
     */
    public void testTaskBlockExec ()
    {
	TaskCounter taskCounter = new TaskCounter (true);

	// Create a temp file, the exec will remove.  That way it's
	// possible to confirm that the exec did work.
	TmpFile tmpFile = new TmpFile ();
	AckProcess child = new DetachedAckProcess
	    ((String)null, new String[] { "/bin/rm", tmpFile.toString (), });
	child.assertSendAddCloneWaitForAcks ();
	Task mainTask = child.findTaskUsingRefresh (true);
	Task clone = child.findTaskUsingRefresh (false);

	// Add an exec observer causing the task to be attached.
	ExecBlockCounter execBlockCounter = new ExecBlockCounter (mainTask);
	assertRunUntilStop ("add exec observer to mainTask");

	// Trigger the exec
	Signal.tkill (clone.getTid (), execSig);
	assertRunUntilStop ("wait for exec");

	// Set things up to stop once the exec task exits.
	new StopEventLoopWhenChildProcRemoved ();
	mainTask.requestUnblock (execBlockCounter);
	assertRunUntilStop ("wait for exec program exit");

	assertEquals ("number of child exec's", 1,
		      execBlockCounter.numberExecs);
	assertEquals ("number of child tasks created", 2,
		      taskCounter.added.size ());
	// The exec makes one task disappear.
	assertEquals ("number of tasks destroyed", 1,
		      taskCounter.removed.size ());
	assertFalse ("tmp file exists", tmpFile.stillExists ());
    }

    /**
     * A single threaded program performs an exec, check that it is correctly
     * tracked. 
     */
    public void testAttachedSingleExec ()
    {
	// Watch for any Task exec events, accumulating them as they
	// arrive.
	class SingleExecObserver
	    extends TaskObserverBase
	    implements TaskObserver.Execed
	{
	    int savedTid = 0;
	    public Action updateExeced (Task task)
	    {
		assertEquals ("savedTid", 0, savedTid);
		savedTid = task.getTid ();
		assertEquals ("argv[0]",
			      savedTid + ":" + savedTid,
			      task.getProc ().getCmdLine () [0]);
		return Action.CONTINUE;
	    }
	    public void addedTo (Object o)
	    {
		Manager.eventLoop.requestStop ();
	    }
	}
	SingleExecObserver execObserver = new SingleExecObserver ();

	// Create an unattached child process.
	AckProcess child = new DetachedAckProcess ();

	// Attach to the process using the exec observer.  The event
	// loop is kept running until SingleExecObserver .addedTo is
	// called indicating that the attach succeeded.
	Task task = child.findTaskUsingRefresh (true);
	task.requestAddExecedObserver (execObserver);
	assertRunUntilStop ("adding exec observer causing attach");

	// Do the exec; this call keeps the event loop running until
	// the child process has notified this process that the exec
	// has finished which is well after SingleExecObserver
	// .updateExeced has been called.
	child.assertSendExecWaitForAcks ();

	assertEquals ("pid after attached single exec", child.getPid (),
		      execObserver.savedTid);
    }

    /**
     * A multiple threaded program performs an exec, check that it is correctly
     * tracked. 
     */
    public void testAttachedMultipleParentExec ()
    {
	// Watch for any Task exec events, accumulating them as they arrive.
	class ExecParentObserver
	    extends TaskObserverBase
	    implements TaskObserver.Execed
	{
	    int savedTid = 0;
	    public Action updateExeced (Task task)
	    {
		assertEquals ("savedTid", 0, savedTid);
		savedTid = task.getTid ();
		assertEquals ("argv[0]",
			      savedTid + ":" + savedTid,
			      task.getProc ().getCmdLine () [0]);
		return Action.CONTINUE;
	    }
	    public void addedTo (Object o)
	    {
		Manager.eventLoop.requestStop ();
	    }
	}

	// Create an unattached child process.
	AckProcess child = new DetachedAckProcess ();

	Proc proc = child.findProcUsingRefresh (true);
	ExecParentObserver execParentObserver = new ExecParentObserver ();

	// Attach to the process using the exec observer.  The event
	// loop is kept running until ExecParentObserver .addedTo is
	// called indicating that the attach succeeded.
	Task task = child.findTaskUsingRefresh (true);
	task.requestAddExecedObserver (execParentObserver);
	assertRunUntilStop ("adding exec observer causing attach");

	// Add the clones, then do the exec; this call keeps the event
	// loop running until the child process has notified this
	// process that the exec has finished which is well after
	// ExecParentObserver .updateExeced has been called.
	child.assertSendAddCloneWaitForAcks ();
	child.assertSendAddCloneWaitForAcks ();
	child.assertSendExecWaitForAcks ();

	assertTrue ("task after attached multiple parent exec",
		    proc.getPid () == task.getTid ()); // not main task
	
	assertEquals ("proc's getCmdLine[0]",
		      proc.getPid () + ":" + task.getTid (),
		      proc.getCmdLine ()[0]);
	
	assertEquals ("pid after attached multiple parent exec",
		      task.getTid (), execParentObserver.savedTid);

	assertEquals ("number of children", proc.getChildren ().size (), 0);
    }

    /**
     * A multiple threaded program's child performs an exec, check that it is 
     * correctly tracked. 
     */
    public void testAttachedMultipleChildExec ()
    {
	// Watch for any Task exec events, accumulating them as they arrive.
	class ExecChildObserver
	    extends TaskObserverBase
	    implements TaskObserver.Execed
	{
	    int savedTid = 0;
	    public Action updateExeced (Task task)
	    {
		assertEquals ("savedTid", 0, savedTid);
		savedTid = task.getTid ();
 		return Action.CONTINUE;
	    }
	    public void addedTo (Object o)
	    {
		Manager.eventLoop.requestStop ();
	    }
	}

	// Create an unattached child process.
	AckProcess child = new DetachedAckProcess ("funit-child-alias", null);

	Proc proc = child.findProcUsingRefresh (true);
	ExecChildObserver execObserverParent = new ExecChildObserver ();
	ExecChildObserver execObserverChild = new ExecChildObserver ();

	// Attach to the process using the exec observer.  The event
	// loop is kept running until execObserverParent .addedTo is
	// called indicating that the attach succeeded.
	Task task = child.findTaskUsingRefresh (true);
	task.requestAddExecedObserver (execObserverParent);
	assertRunUntilStop ("adding exec observer causing attach");

	// Add the clones, then do the exec; this call keeps the event
	// loop running until the child process has notified this
	// process that the exec has finished which is well after
	// execObserverParent .updateExeced has been called.
	// execObserverChild .updateExeced should not be called 
	// since the event only arrives after the exec has completed, and only 
	// the main thread is left and is therefore the only thread that 
	// can receive the event. 

	child.assertSendAddCloneWaitForAcks ();
	child.assertSendAddCloneWaitForAcks ();

	String[] beforeCmdLine = proc.getCmdLine ();
	String beforeCommand = proc.getCommand ();
	
	Task childtask = child.findTaskUsingRefresh (false);
	childtask.requestAddExecedObserver (execObserverChild);
	child.assertSendExecWaitForAcks (childtask.getTid ());

	assertEquals ("task after attached multiple clone exec", proc,
		      task.getProc()); // parent/child relationship
	assertTrue ("task after attached multiple clone exec",
		    proc.getPid () == task.getTid ());
	
	assertEquals ("proc's getCmdLine[0]",
		      proc.getPid () + ":" + childtask.getTid (),
		      proc.getCmdLine ()[0]);
	
	assertEquals ("Parent pid after attached multiple clone exec", proc.getPid (), execObserverParent.savedTid);

	assertEquals ("Child pid after attached multiple clone exec", execObserverChild.savedTid, 0);

	assertEquals ("number of children", proc.getChildren ().size (), 0);
	assertEquals ("proc's getCommand after exec", proc.getCommand (), "funit-child-ali");
	assertFalse ("proc's getCommand before/after exec equals",
		     beforeCommand.equals (proc.getCommand ()));
	assertFalse ("proc's getCmdLine[0] before/after exec equals",
		     beforeCmdLine[0].equals (proc.getCmdLine ()[0]));
    }
}
