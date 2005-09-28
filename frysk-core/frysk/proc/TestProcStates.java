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

package frysk.proc;

import java.util.Observer;
import java.util.Observable;

/**
 * Test that a Proc correctly transitions between the states: unattached,
 * attachedContinue attachedStop; and destroyed.
 */

public class TestProcStates
    extends TestLib
{
    /**
     * Check that it is possible to attach / continue a single tasked
     * process.
     */
    public void testOneTaskDetachedContinueToAttachedContinue ()
    {
	Child child = new DaemonChild ();
	Proc proc = child.findProcUsingRefresh ();

	// Request that the child be attached; wait for it to ack.
	proc.observableAttachedContinue.addObserver (new Observer ()
	    {
		public void update (Observable o, Object obj)
		{
		    Manager.eventLoop.requestStop ();
		}
	    });
	proc.requestAttachedContinue ();
	assertRunUntilStop ("attaching to process");
    }

    /**
     * Check that it is possible to attach / continue a multi-tasked
     * process.
     *
     * The single and multi-task cases exercise different paths - for
     * the former there is no need to also attach to the non-main
     * tasks.
     */
    public void testMultiTaskDetachedContinueToAttachedContinue ()
    {
	Child child = new DaemonChild (2);
	Proc proc = child.findProcUsingRefresh ();

	// Request that the child be attached; wait for it to ack.
	proc.observableAttachedContinue.addObserver (new Observer ()
	    {
		public void update (Observable o, Object obj)
		{
		    Manager.eventLoop.requestStop ();
		}
	    });
	proc.requestAttachedContinue ();
	assertRunUntilStop ("attaching to process");
    }

    /**
     * Check that it is possible to detach / continue an attached
     * single tasked child process started via fork / exec.
     */
    public void testOneTaskAttachedContinueToDetachedContinue ()
    {
	Child child = new AttachedChild ();
	Proc proc = child.findProcUsingRefresh ();

	// Request that the child be detached; wait for it to ack.
	proc.observableDetachedContinue.addObserver (new Observer ()
	    {
		public void update (Observable o, Object obj)
		{
		    Manager.eventLoop.requestStop ();
		}
	    });
	proc.requestDetachedContinue ();
	assertRunUntilStop ("detaching from a process");
    }

    /**
     * Check that it is possible to detach / continue a multi-tasked
     * child process started via fork / exec.
     */
    public void testMultiTaskAttachedContinueToDetachedContinue ()
    {
	Child child = new AttachedChild (2);
	Proc proc = child.findProcUsingRefresh ();

	// Request that the child be detached; wait for it to ack.
	proc.observableDetachedContinue.addObserver (new Observer ()
	    {
		public void update (Observable o, Object obj)
		{
		    Manager.eventLoop.requestStop ();
		}
	    });
	proc.requestDetachedContinue ();
	assertRunUntilStop ("detaching from a process");
    }

//     /**
//      * Check that it is possible to attach / detach a single tasked
//      * child process.
//      */
//     public void testReattachProcess ()
//     {
//     }

//     /**
//      * Check that it is possible to attach / detach a multi-tasked
//      * child process.
//      */
//     public void testReattachTasks ()
//     {
//     }
}
