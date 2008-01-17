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

package frysk.testbed;

import frysk.proc.FindProc;
import frysk.proc.Proc;
import frysk.proc.Task;
import frysk.sys.Errno;
import frysk.proc.Manager;
import frysk.proc.ProcId;
import java.util.Iterator;
import frysk.sys.Signal;
import frysk.junit.TestCase;
import java.util.logging.Logger;

/**
 * A generic test process created by this testbed.
 */

public abstract class Offspring {
    protected final static Logger logger = Logger.getLogger("frysk");
    /**
     * Return the process's system identifier.
     */
    public abstract int getPid();
    /**
     * Package private.
     */
    Offspring() {
    }
    /**
     * Send the child the sig.
     */
    public void signal(Signal sig) {
	sig.tkill(getPid());
    }
    /**
     * Attempt to kill the child. Return false if the child doesn't
     * appear to exist.
     */
    public boolean kill () {
	try {
	    signal(Signal.KILL);
	    return true;
	} catch (Errno.Esrch e) {
	    return false;
	}
    }
    /**
     * Find/return the child's Proc, polling /proc if necessary.
     */
    public Proc assertFindProcAndTasks () {
	class ProcFinder implements FindProc {
	    Proc proc;
	    public void procFound(Proc p) {
		proc = p;
		Manager.eventLoop.requestStop();
	    }
	    public void procNotFound(ProcId procId) {
		TestCase.fail("Couldn't find the given proc");
	    }
	}
	ProcFinder findProc = new ProcFinder();
	Manager.host.requestProc(new ProcId(getPid()), findProc);
	Manager.eventLoop.run();
	return findProc.proc;
    }
    
    /**
     * Find the child's Proc's main or non-main Task, polling /proc if
     * necessary.
     */
    public Task findTaskUsingRefresh (boolean mainTask) {
	Proc proc = assertFindProcAndTasks();
	for (Iterator i = proc.getTasks().iterator(); i.hasNext();) {
	    Task task = (Task) i.next();
	    if (task.getTid() == proc.getPid()) {
		if (mainTask)
		    return task;
	    } else {
		if (! mainTask)
		    return task;
	    }
	}
	return null;
    }

    /**
     * The Offspring's main thread is in (or transitions to) the
     * specified state.
     */
    public void assertIs(StatState state) {
	state.assertIs(getPid());
    }
    /**
     * Run the event-loop until the offspring's main thread is in the
     * specified state.
     */
    public void assertRunUntil(StatState state) {
	state.assertRunUntil(getPid());
    }

    /**
     * Stop a Task with a SIGSTOP and then wait until it has stopped..
     */
    public void assertSendStop () {
	signal(Signal.STOP);
	assertIs(StatState.TRACED_OR_STOPPED);
    }
}
