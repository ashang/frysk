// This file is part of the program FRYSK.
//
// Copyright 2005, 2007, Red Hat Inc.
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
 * Implementation of WaitBuilder that, passes any unhandled wait event
 * onto the method unhandled().  Extenders can then either throw an
 * exception, or discard the event.
 */
public abstract class UnhandledWaitBuilder
    implements WaitBuilder
{
    /**
     * An unhandled waitpid event was encountered.
     */
    protected abstract void unhandled (String why);
    /**
     * An unhandled waitpid event was encountered, describe why and
     * call unhandled.
     */
    private void unhandled (String what, int pid)
    {
	unhandled ("unhandled " + what + " (pid " + pid + ")");
    }
    /**
     * An unhandled waitpid event was encountered, describe why and
     * call unhandled.
     */
    private void unhandled (String what, int pid, String also, int value)
    {
	unhandled ("unhandled " + what
		   + " (pid " + pid + ", " + also + " " + value + ")");
    }
    /**
     * An unhandled waitpid event was encountered, describe why and
     * call unhandled.
     */
    private void unhandled (String what, int pid, String also, String value)
    {
	unhandled ("unhandled " + what
		   + " (pid " + pid + ", " + also + " " + value + ")");
    }
    /**
     * The task PID got a clone event; CLONE is the new task's ID.
     */
    public void cloneEvent (int pid, int clone)
    {
	unhandled ("cloneEvent", pid, "clone", clone);
    }
    /**
     * The task PID got a fork event; CHILD is the new process ID.
     */
    public void forkEvent (int pid, int child)
    {
	unhandled ("forkEvent", pid, "child", child);
    }
    /**
     * The task PID got an exit event; if SIGNAL, VALUE is the +ve
     * terminating signal, otherwize VALUE is the cardinal exit
     * status.
     */
    public void exitEvent (int pid, boolean signal, int value,
			   boolean coreDumped)
    {
	if (signal)
	    unhandled ("exitEvent", pid, "signal", value);
	else
	    unhandled ("exitEvent", pid, "exit", value);
    }
    /**
     * The task PID got an exec event; the process has already
     * been overlayed.
     */
    public void execEvent (int pid)
    {
	unhandled ("execEvent", pid);
    }
    /**
     * XXX: It isn't currently possible to determine from the
     * syscall event whether it is entry or exit.  We must
     * do state transitioning in the upper-level and figure it out.
     */
    public void syscallEvent (int pid)
    {
	unhandled ("syscallEvent", pid);
    }
    /**
     * The task PID stopped; if SIGNAL is non-zero, then SIGNAL is
     * pending.
     */
    public void stopped (int pid, int signal)
    {
	unhandled ("stopped", pid, "signal", signal);
    }
    /**
     * The task PID terminated (WIFEXITED, WIFSIGNALED); if
     * SIGNAL, VALUE is the +ve terminating signal, otherwize
     * VALUE is the cardinal exit status.
     */
    public void terminated (int pid, boolean signal, int value,
			    boolean coreDumped)
    {
	if (signal)
	    unhandled ("terminated", pid, "signal", value);
	else
	    unhandled ("terminated", pid, "exit", value);
    }
    /**
     * The task PID disappeared.
     *
     * Received an event for PID but then that, by the time its
     * status was checked, the process had vanished.
     */
    public void disappeared (int pid, Throwable w)
    {
	unhandled ("disappeared", pid, "why", w.toString ());
    }
}
