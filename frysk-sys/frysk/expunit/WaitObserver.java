// This file is part of the program FRYSK.
//
// Copyright 2007, 2008, Red Hat Inc.
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

package frysk.expunit;

import frysk.sys.ProcessIdentifier;
import frysk.sys.Signal;
import frysk.sys.WaitBuilder;

class WaitObserver implements WaitBuilder {
    private final Signal expectedSignal;
    private final int expectedStatus;
    /**
     * Observe wait status, expecting a specific termination value.
     */
    WaitObserver(int expectedStatus) {
	this.expectedSignal = null;
	this.expectedStatus = expectedStatus;
    }
    WaitObserver(Signal expectedSignal) {
	this.expectedSignal = expectedSignal;
	this.expectedStatus = -1;
    }
    private RuntimeException terminationException(String msg) {
	return new TerminationException(expectedSignal, expectedStatus, msg);
    }
    public void disappeared(ProcessIdentifier pid, Throwable t) {
	throw terminationException("Process disappeared");
    }
    public void terminated(ProcessIdentifier pid, Signal signal, int status,
			   boolean coreDumped) {
	if (signal != null) {
	    if (signal != expectedSignal)
		throw terminationException("Killed with signal " + signal);
	} else {
	    if (status != expectedStatus)
		throw terminationException("Exited with status " + status);
	}
    }
    public void stopped(ProcessIdentifier pid, Signal signal) {
	throw terminationException("Stopped with signal " + signal);
    }
    public void syscallEvent(ProcessIdentifier pid) {
	throw terminationException("Stopped with syscall event");
    }
    public void execEvent(ProcessIdentifier pid) {
	throw terminationException("Stopped with exec event");
    }
    public void exitEvent(ProcessIdentifier pid, Signal signal,
			  int status, boolean coreDumped) {
	throw terminationException("Stopped with exit event");
    }
    public void forkEvent(ProcessIdentifier pid, ProcessIdentifier offspring) {
	throw terminationException("Stopped with fork event");
    }
    public void cloneEvent(ProcessIdentifier pid, ProcessIdentifier offspring) {
	throw terminationException("Stopped with clone event");
    }
}
