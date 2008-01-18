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


package frysk.proc.live;

import frysk.proc.Manager;
import frysk.event.RequestStopEvent;
import frysk.testbed.TestLib;
import frysk.testbed.SlaveOffspring;
import frysk.proc.Proc;
import frysk.proc.Task;
import frysk.proc.ProcObserver;
import frysk.proc.ProcBlockAction;

public class TestProcStopped extends TestLib {

    private void stopped(SlaveOffspring ackProc) {
	ackProc.assertSendStop();
	Proc proc = ackProc.assertFindProcAndTasks();
	new ProcBlockAction(proc, new MyTester());
    }

    private void running(SlaveOffspring ackProc) {
	Proc proc = ackProc.assertFindProcAndTasks();
	new ProcBlockAction(proc, new MyTester());
    }

    public void testStoppedAckDaemon() {
	SlaveOffspring ackProc = SlaveOffspring.createDaemon();
	stopped(ackProc);
	assertRunUntilStop("testStoppedAckDaemon");
    }

    public void testStoppedDetached() {
	SlaveOffspring ackProc = SlaveOffspring.createChild();
	stopped(ackProc);
	assertRunUntilStop("testStoppedDetached");
    }

    public void testStoppedAttached() {
	SlaveOffspring ackProc = SlaveOffspring.createAttachedChild();
	stopped(ackProc);
	assertRunUntilStop("testStoppedAttached");
    }

    public void testRunningAckDaemon() {
	SlaveOffspring ackProc = SlaveOffspring.createDaemon();
	running(ackProc);
	assertRunUntilStop("testRunningAckDaemon");
    }

    public void testRunningDetached() {
	SlaveOffspring ackProc = SlaveOffspring.createChild();
	running(ackProc);
	assertRunUntilStop("testRunningDetached");
    }

    public void testRunningAttached() {
	SlaveOffspring ackProc = SlaveOffspring.createAttachedChild();
	running(ackProc);
	assertRunUntilStop("testRunningAttached");
    }

    public void testMultiThreadedStoppedAckDaemon() {
	SlaveOffspring ackProc = SlaveOffspring.createDaemon()
	    .assertSendAddClonesWaitForAcks(2);
	stopped(ackProc);
	assertRunUntilStop("testStoppedAckDaemon");
    }

    public void testMultiThreadedStoppedDetached() {
	SlaveOffspring ackProc = SlaveOffspring.createChild()
	    .assertSendAddClonesWaitForAcks(2);
	stopped(ackProc);
	assertRunUntilStop("testStoppedDetached");
    }
    
    public void testMultiThreadedStoppedAttached() {
	SlaveOffspring ackProc = SlaveOffspring.createAttachedChild()
	    .assertSendAddClonesWaitForAcks(2);
	stopped(ackProc);
	assertRunUntilStop("testStoppedAttached");
    }

    public void testMultiThreadedRunningAckDaemon() {
	SlaveOffspring ackProc = SlaveOffspring.createDaemon()
	    .assertSendAddClonesWaitForAcks(2);
	running(ackProc);
	assertRunUntilStop("testRunningAckDaemon");
    }

    public void testMultiThreadedRunningDetached() {
	SlaveOffspring ackProc = SlaveOffspring.createChild()
	    .assertSendAddClonesWaitForAcks(2);
	running(ackProc);
	assertRunUntilStop("testRunningDetached");
    }

    public void testMultiThreadedRunningAttached() {
	SlaveOffspring ackProc = SlaveOffspring.createAttachedChild()
	    .assertSendAddClonesWaitForAcks(2);
	running(ackProc);
	assertRunUntilStop("testRunningAttached");
    }

    public class MyTester implements ProcObserver.ProcAction {   
	public void existingTask(Task task) {
	}
	public void deletedFrom(Object observable) {
	}
	public void allExistingTasksCompleted() {
	    Manager.eventLoop.add(new RequestStopEvent(Manager.eventLoop));
	}
	public void addFailed(Object observable, Throwable w) {
	    fail("Proc add failed: " + w.getMessage());
	}
	public void addedTo(Object observable) {
	}
	public void taskAddFailed(Object task, Throwable w) {
	}
    }
}
