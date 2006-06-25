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

/**
 * Test attaching to a process with many many tasks.
 * 
 * When a the kernel is sent both a request to attach to a task and,
 * moments later, a signal, the attach may either find the task
 * stopped, or stopped-with-signal.  Since this isn't really
 * deterministic, exercise the edge case via a stress test.
 */

public class StressAttachDetachSignaledTask
    extends TestLib
{
    /**
     * An agressive observer that spends its life adding and removing
     * itself.
     */
    class AttachDetach
	extends TaskObserverBase
	implements TaskObserver.Attached
    {
	public void addedTo (Object o)
	{
	    Task task = (Task) o;
	    task.requestDeleteAttachedObserver (this);
	}
	public void deletedFrom (Object o)
	{
	    Task task = (Task) o;
	    task.requestAddAttachedObserver (this);
	}
	public Action updateAttached (Task task)
	{
	    return Action.CONTINUE;
	}
    }

    /**
     * Stress test attaching and detaching a process that is
     * constantly receiving signals.
     */
    abstract class Spawn
    {
	/**
	 * Perform arbitrary operation OP.
	 */
	abstract void op (AckDaemonProcess child, int iteration);

	Spawn ()
	{
	    if (brokenXXX ())
		return;
	    AckDaemonProcess child = new AckDaemonProcess ();
	    AttachDetach attachDetach = new AttachDetach ();
	    Task task = child.findTaskUsingRefresh (true);

	    // Ask for the observer to be attached, then run the event
	    // loop sufficiently for the attach request to be
	    // initiated but not completed.
	    task.requestAddAttachedObserver (attachDetach);
	    runPending ();

	    for (int i = 0; i < 100; i++) {
		// Execute OP (which hopefully involves signals).
		// While this op is being performed, the task will be
		// constantly attaching and detaching.
		op (child, i);
	    }
	}
    }
    /**
     * Stress attaching and detaching a task that is constantly
     * receiving signals, and simultaneously creating and deleting
     * child processes.
     */
    public void testForking ()
    {
	new Spawn ()
	{
	    void op (AckDaemonProcess child, int iteration)
	    {
		switch (iteration % 2) {
		case 0:
		    child.addFork ();
		    break;
		case 1:
		    child.delFork ();
		    break;
		}
	    }
	};
    }
    /**
     * Stress attaching and detaching a task that is constantly
     * receiving signals, and simultaneously creating and deleting new
     * tasks.
     */
    public void testCloning ()
    {
	new Spawn ()
	{
	    void op (AckDaemonProcess child, int iteration)
	    {
		switch (iteration % 2) {
		case 0:
		    child.addClone ();
		    break;
		case 1:
		    child.delClone ();
		    break;
		}
	    }
	};
    }
    /**
     * Stress attaching and detaching a task that is constantly
     * receiving signals, and simultaneously doing execs.
     */
    public void testExecing ()
    {
	new Spawn ()
	{
	    void op (AckDaemonProcess child, int iteration)
	    {
		child.exec ();
	    }
	};
    }
}
