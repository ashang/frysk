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

import inua.util.Scanner;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

/**
 * Test Proc's public get methods.
 */

public class TestProcGet
    extends TestLib
{
    /**
     * Compare the output from a program that just prints its AUXV (to
     * a tempoary file) to that extracted from the same process using
     * Proc.getAuxv().
     */
    public void testGetAuxv ()
    {
	TmpFile tmpFile = new TmpFile ();
	new StopEventLoopWhenChildProcRemoved ();
	AttachedDaemonProcess child = new AttachedDaemonProcess (new String[]
	    {
		getExecPrefix () + "funit-print-auxv",
		tmpFile.toString (),
		"/dev/null"
	    });
	// Grab the AUXV from the process sitting at its entry point.
	Auxv[] auxv = child.mainTask.proc.getAuxv ();
	assertNotNull ("captured AUXV", auxv);
	child.resume ();
	assertRunUntilStop ("run \"auxv\" to completion");

	// Compare the AUXV as printed against that extracted using
	// Proc.getAuxv.
	try {
	    Scanner reader = new Scanner (tmpFile.getFile ());
	    for (int i = 0; i < auxv.length; i++) {
		if (auxv[i].type == 0)
		    break;
		long type = reader.readDecimalLong ();
		reader.skipWhitespace ();
		long val = reader.readDecimalLong ();
		reader.skipWhitespace ();
		assertEquals ("auxv[" + i + "].type", type, auxv[i].type);
		assertEquals ("auxv[" + i + "].val", val, auxv[i].val);
	    }
	    assertTrue ("reached AUXV end-of-file", reader.endOfFile ());
	}
	catch (Exception e) {
	    throw new RuntimeException (e);
	}
    }

    /**
     * Check that .getCommand returns the command in an expected
     * format.
     */
    public void testGetCommand ()
    {
	Child child = new AckDaemonProcess ();
	Proc childProc = child.findProcUsingRefresh ();
	assertEquals ("value of child's getCommand()", "funit-child",
		      childProc.getCommand ());
    }

    /**
     * Check that .getTasks, for a two task process returns two tasks.
     */
    public void testGetTasks ()
    {
	Child child = new AckDaemonProcess (1);
	Proc proc = child.findProcUsingRefresh (true); // and tasks
	List tasks = proc.getTasks ();

	assertEquals ("number of tasks", 2, tasks.size ());

	// Find the main task.
	Task mainTask = null;
	for (Iterator i = tasks.iterator  (); i.hasNext (); ) {
	    Task task = (Task) i.next ();
	    if (proc.getPid () == task.getTid ()) {
		// Only found once.
		assertNull ("main task", mainTask);
		mainTask = task;
	    }
	}
	assertNotNull ("main task", mainTask);
    }

    /**
     * Check that .getChildren, for this process with two children
     * returns both of them.
     */
    public void testGetChildren ()
    {
	// Create two children.  The refreshes have the side effect of
	// updating this processes proc list.
	Proc[] child = new Proc[] {
	    new DetachedAckProcess ().findProcUsingRefresh (),
	    new DetachedAckProcess ().findProcUsingRefresh ()
	};
	Proc self = host.getSelf ();

	assertEquals ("number of children", 2, self.getChildren ().size ());
	assertNotSame ("children", child[0], child[1]);
	for (int i = 0; i < child.length; i++) {
	    assertTrue ("this contains child " + i,
			self.getChildren ().contains (child[i]));
	}
    }

    /**
     * Check that getCmdLine returns the list of arguments that
     * matches what was passed to a detached child.
     */
    public void testGetCmdLine ()
    {
	Child child = new AckDaemonProcess ();
	String[] argv = child.getArgv ();
	Proc proc = child.findProcUsingRefresh ();
	String[] cmdLine = proc.getCmdLine ();
	assertEquals ("cmdLine.length", argv.length, cmdLine.length);
	for (int i = 0; i < argv.length; i++) {
	    assertEquals ("cmdLine[" + i + "]", argv[i], cmdLine[i]);
	}
    }

    /**
     * Check that getExe returns the fully qualified path to the
     * ack-daemon program.
     */
    public void testGetExe ()
    {
	Child child = new AckDaemonProcess ();
	String[] argv = child.getArgv ();
	String file;
	try {
	    file = new File (argv[0]).getCanonicalPath ();
	}
	catch (IOException e) {
	    throw new RuntimeException (e);
	}
	Proc proc = child.findProcUsingRefresh ();
	assertEquals ("exe", proc.getExe (), file);
    }
}
