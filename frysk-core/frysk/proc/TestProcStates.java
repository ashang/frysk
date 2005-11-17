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

/**
 * Test that a Proc correctly transitions between the states: unattached,
 * attachedContinue attachedStop; and destroyed.
 */

public class TestProcStates
    extends TestLib
{
    /**
     * Transition a process to attached / continue.
     */
    private void requestAttachedContinue (Proc proc)
    {
	// Request that the child be attached; wait for it to ack.
	proc.observableAttached.addObserver (new StopEventLoopObserver ());
	proc.requestAttachedContinue ();
	assertRunUntilStop ("attached continue");
	// XXX: Prove that it is attached and continued?
    }

    /**
     * Transition a process to detached / continue.
     */
    private void requestDetachedContinue (Proc proc)
    {
	// Request that the child be detached; wait for it to ack.
	proc.observableDetached.addObserver (new StopEventLoopObserver ());
	proc.requestDetachedContinue ();
	assertRunUntilStop ("detached continue");
	// XXX: Prove that it is detached and continued?
    }

    /**
     * Transition a single tasked process from detached/continue to
     * attached/continue.
     */
    public void testSingleTaskDetachedContinueToAttachedContinue ()
    {
	Child child = new CloneDaemon ();
	Proc proc = child.findProcUsingRefresh ();
	requestAttachedContinue (proc);
    }

    /**
     * Transition a multi-tasked process from detached/continue to
     * attached/continue.
     *
     * The single- and multi-task cases involve different code paths.
     * For the latter, a two stage process is involved: first attach
     * to the main task; and second attach to the remaining tasks.
     */
    public void testMultiTaskDetachedContinueToAttachedContinue ()
    {
	Child child = new CloneDaemon (2);
	Proc proc = child.findProcUsingRefresh ();
	requestAttachedContinue (proc);
    }

    /**
     * Transition a single-tasked process from attached/continue to
     * detach/continue (the process being created using a fork and
     * then exec).
     */
    public void testSingleTaskAttachedContinueToDetachedContinue ()
    {
	Child child = new AttachedChild ();
	Proc proc = child.findProcUsingRefresh ();
	requestDetachedContinue (proc);
    }

    /**
     * Transition a multi-tasked process from attached/continue to
     * detached/continue (the process being created using a fork and
     * then exec).
     */
    public void testMultiTaskAttachedContinueToDetachedContinue ()
    {
	Child child = new AttachedChild (2);
	Proc proc = child.findProcUsingRefresh ();
	requestDetachedContinue (proc);
    }

    /**
     * Transition a multi-tasked process from continued to continued.
     */
    public void testMultiTaskAttachedContinueToAttachedContinue ()
    {
	Child child = new AttachedChild (2);
	Proc proc = child.findProcUsingRefresh ();
	requestAttachedContinue (proc);
	requestAttachedContinue (proc);
    }
}
