// This file is part of FRYSK.
//
// Copyright 2005, Red Hat Inc.
//
// FRYSK is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
//
// FRYSK is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with FRYSK; if not, write to the Free Software
// Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA

/**
 * Wait for an event from either a process, task, or all processes and
 * tasks.
 */

package frysk.sys;

public final class Wait
{
    /**
     * Handler notifying of each possible wait event.
     */
    public interface Observer
    {
	/**
	 * PID cloned, creating the new task CLONE.
	 */
 	void cloneEvent (int pid, int clone);
	/**
	 * PID forked, creating the new child process CHILD.
	 */
 	void forkEvent (int pid, int child);
	/**
	 * PID is exiting with STATUS.
	 */
 	void exitEvent (int pid, int status);
	/**
	 * PID execed, starting a new program.
	 */
 	void execEvent (int pid);
	/**
	 * PID is either entering or exiting a system call.
	 *
	 * XXX: It isn't directly possible to determine which of enter
	 * or exit is occuring.  */
 	void syscallEvent (int pid);
	/**
	 * PID stopped (possibly with SIGNAL).
	 */
 	void stopped (int pid, int signal);
	/**
	 * PID exited cleanly with STATUS.
	 */
 	void exited (int pid, int status, boolean coreDumped);
	/**
	 * PID was destroyed by SIGNAL (the corresponding wait event
	 * is WSIGNALED but that name just gets confusing).
	 */
 	void terminated (int pid, int signal, boolean coreDumped);
	/**
	 * PID disappeared.
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
