// This file is part of the program FRYSK.
//
// Copyright 2005, 2007, 2008, Red Hat Inc.
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

import frysk.rsl.Log;
import frysk.testbed.TestLib;
import frysk.testbed.SlaveOffspring;

public class TestFindProc extends TestLib {
    private static final Log fine = Log.fine(TestFindProc.class);

    class MyFinder implements FindProc {
	private final int expectedId;
	Proc proc;
	public MyFinder(int pid) {
	    expectedId = pid;
	}
	public void procFound(Proc proc) {
	    fine.log("procFound", proc, "parent", proc.getParent());
	    assertEquals("procId", expectedId, proc.getPid());
	    this.proc = proc;
	    Manager.eventLoop.requestStop();
	}
	public void procNotFound(int pid) {
	    fail("Could not find process with ID" + pid);
	}
    }

    public void testFindProcDetached() {
	SlaveOffspring ackProc = SlaveOffspring.createChild();
	doFindProc(ackProc);
    }
  
    public void testFindProcAttached() {
	SlaveOffspring ackProc = SlaveOffspring.createAttachedChild();
	//expect no additional processes to be added to the procPool.
	doFindProc(ackProc);
    }
  
    public void testFindProcAckDaemon() {
	SlaveOffspring ackProc = SlaveOffspring.createDaemon();
	doFindProc(ackProc);
    }

    private void doFindProc(SlaveOffspring ackProc) {
	// This finds out how many processes are associated with the
	// frysk process.  For example: init->gnome
	// terminal->bash->frysk.
	Manager.host.getSelf();
	// Find out how many processes are associated with the test
	// process.  Should be just the one.
	MyFinder finder = new MyFinder(ackProc.getPid().intValue());
	Manager.host.requestProc(ackProc.getPid().intValue(), finder);
	assertRunUntilStop("testFindProc");
	assertNotNull("finder got proc", finder.proc);
    }
  
    public void testFindAndRefreshFailed() {
	FindProc finder = new FindProc() {
		public void procFound(Proc proc) {
		    fine.log(this, "procFound", proc);
		    fail("Found proc 0, should have failed.");
		}
		public void procNotFound(int pid) {
		    fine.log(this, "procNotFound", pid);
		    assertEquals("pid", 0, pid);
		    Manager.eventLoop.requestStop();
		}
	    };
	Manager.host.requestProc(0, finder);
	assertRunUntilStop("testFindFailed");
    }
}
