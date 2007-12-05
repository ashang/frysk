// This file is part of the program FRYSK.
//
// Copyright 2007, Red Hat Inc.
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

package frysk.sys;

/**
 * Identifies a process.
 */
public class ProcessIdentifier
    implements Comparable
{
    private int pid;
    public ProcessIdentifier (int pid)
    {
	this.pid = pid;
    }
    public int hashCode ()
    {
	return pid;
    }
    public int compareTo (Object o)
    {
	return ((ProcessIdentifier)o).pid - this.pid;
    }
    public boolean equals (Object o)
    {
	if (o instanceof ProcessIdentifier)
	    return ((ProcessIdentifier)o).pid == this.pid;
	else
	    return false;
    }
    /**
     * Represent the ProcessIdentifier textually.  Return the PID as a
     * number so that it can be used directly.
     */
    public String toString ()
    {
	return Integer.toString (pid);
    }

    /**
     * Send a fatal signal (SIGKILL) to this process.
     */
    public void kill() {
	Signal.KILL.kill(pid);
    }

    /**
     * Send a signal to THIS pid.
     */
    public void tkill(Signal signal) {
	signal.tkill(pid);
    }

    /**
     * Perform a blocking drain of all wait events from this process.
     * Only returns when the process has disappeared.
     */
    public void blockingDrain ()
    {
	Wait.drain (pid);
    }

    /**
     * Perform a blocking wait for a single event from this process.
     */
    public void blockingWait (WaitBuilder o)
    {
	Wait.waitAll (pid, o);
    }
}
