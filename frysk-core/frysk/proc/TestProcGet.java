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
	class CaptureAuxv
	    extends AutoAddTaskObserverBase
	    implements TaskObserver.Attached
	{
	    // Store the extracted auxv here.
	    Auxv[] auxv;
	    void updateTaskAdded (Task task)
	    {
		task.requestAddAttachedObserver (this);
	    }
	    public Action updateAttached (Task task)
	    {
		auxv = task.proc.getAuxv ();
		return Action.CONTINUE;
	    }
	}
	CaptureAuxv captureAuxv = new CaptureAuxv ();

	TmpFile tmpFile = new TmpFile ();
	new StopEventLoopWhenChildProcRemoved ();
	Manager.host.requestCreateAttachedContinuedProc
	    (null, tmpFile.toString (), null, new String[] {
		"./prog/print/auxv"
	    });

	assertRunUntilStop ("run \"auxv\" to completion");

	assertNotNull ("AUXV successfully captured", captureAuxv.auxv);
	Auxv[] auxv = captureAuxv.auxv;

	// Compare the AUXV as printed against that extracted using
	// Proc.getAuxv.
	try {
	    inua.Scanner reader = new inua.Scanner (tmpFile.getFile ());
	    for (int i = 0; i < auxv.length; i++) {
		if (auxv[i].type == 0)
		    break;
		long type = reader.readDecimalLong ();
		reader.skipWhitespace ();
		long val = reader.readDecimalLong ();
		reader.skipWhitespace ();
		assertEquals ("auxv[" + i + "].type",
			      type, auxv[i].type);
		assertEquals ("auxv[" + i + "].type",
			      val, auxv[i].val);
	    }
	    assertTrue ("Read the entire AUXV file", reader.endOfFile ());
	}
	catch (Exception e) {
	    throw new RuntimeException (e);
	}
    }

    public void testGetCommand ()
    {
	Child child = new DaemonChild ();
	Proc childProc = child.findProcUsingRefresh ();

	assertEquals ("Child's name", "detach",
		      childProc.getCommand ());
    }
}
