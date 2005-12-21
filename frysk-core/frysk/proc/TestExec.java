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
 * Test the exec event.
 *
 * The exec needs to completly replace the existing (possibly
 * multi-threaded) process with an entirely new one.
 */

public class TestExec
    extends TestLib
{
    /**
     * A simple (single threaded) program performs an exec, check it
     * is correctly tracked.
     */
    public void testProcExec ()
    {
	ExecCounter execCounter = new ExecCounter ();
	new StopEventLoopWhenChildProcRemoved ();

	// Create a temp file, the exec will remove.  That way it's
	// possible to confirm that the exec did work.
	TmpFile tmpFile = new TmpFile ();
	Manager.host.requestCreateAttachedProc
	    (null, "/dev/null", null,
	     new String[] {
		"./prog/syscall/exec",
		"/bin/rm", tmpFile.toString (),
	    });

	assertRunUntilStop ("run \"exec\" until exit");

	assertEquals ("number of execs", 1, execCounter.numberExecs);
	assertFalse ("tmp file exists", tmpFile.stillExists ());
    }

    /**
     * A multi-tasked program's non main task performs an exec, check
     * that it is correctly tracked.
     *
     * This case is messy, the exec blows away all but the exec task,
     * making the exec task the new main task.
     */
    public void testTaskExec ()
    {
	TaskCounter taskCounter = new TaskCounter (true);
	ExecCounter execCounter = new ExecCounter ();
	new StopEventLoopWhenChildProcRemoved ();

	// Create a temp file, the exec will remove.  That way it's
	// possible to confirm that the exec did work.
	TmpFile tmpFile = new TmpFile ();
	Manager.host.requestCreateAttachedProc
	    (null, "/dev/null", null,
	     new String[] {
		"./prog/syscall/threadexec",
		"/bin/rm", tmpFile.toString (),
	    });

	assertRunUntilStop ("run \"threadexec\" to exit");

	assertEquals ("number of child exec's", 1, execCounter.numberExecs);
	assertEquals ("number of child tasks created", 2,
		      taskCounter.added.size ());
	// The exec makes one task disappear.
	assertEquals ("number of tasks destroyed", 1,
		      taskCounter.removed.size ());
	assertFalse ("tmp file exists", tmpFile.stillExists ());
    }
}
