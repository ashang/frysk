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
 * Wait for an event from either a process, task, or all processes and
 * tasks.
 */

public final class Wait
{
    /**
     * Handler notifying of each possible wait event.
     */
    public interface Observer
    {
	/**
	 * The task PID got a clone event; CLONE is the new task's ID.
	 */
 	void cloneEvent (int pid, int clone);
	/**
	 * The task PID got a fork event; CHILD is the new process ID.
	 */
 	void forkEvent (int pid, int child);
	/**
	 * The task PID got an exit event; if SIGNAL, VALUE is the +ve
	 * terminating signal, otherwize VALUE is the cardinal exit
	 * status.
	 */
 	void exitEvent (int pid, boolean signal, int value,
			boolean coreDumped);
	/**
	 * The task PID got an exec event; the process has already
	 * been overlayed.
	 */
 	void execEvent (int pid);
	/**
	 * The task PID got a syscall event; the task is either
	 * entering or exiting a system call.
	 * An integer identifier is passed:
	 *   0 = entry
	 *   1 = exit
	 *  -1 = don't know
	 *
	 * XXX: It isn't currently possible to determine which of enter
	 * or exit is occuring.  This information must be supplied by
	 * the low-level event.
	 */
 	void syscallEvent (int pid, int syscallType);
	/**
	 * The task PID stopped; if SIGNAL is non-zero, then SIGNAL is
	 * pending.
	 */
 	void stopped (int pid, int signal);
	/**
	 * The task PID terminated (WIFEXITED, WIFSIGNALED); if
	 * SIGNAL, VALUE is the +ve terminating signal, otherwize
	 * VALUE is the cardinal exit status.
	 */
 	void terminated (int pid, boolean signal, int value,
			 boolean coreDumped);
	/**
	 * The task PID disappeared.
	 *
	 * Received an event for PID but then that, by the time its
	 * status was checked, the process had vanished.
	 */
	void disappeared (int pid);
    }
    /**
     * Wait for any process or task event.  If there is no outstanding
     * event return immediatly.
     */
    public native static void waitAllNoHang (Observer observer);
    /**
     * Wait for a single process or task event.  Block if no event is
     * pending (provided that there are still potential events).
     */
    public native static void waitAll (int pid, Observer observer);

}
