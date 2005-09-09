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
 * Test Proc's public get methods.
 */

package frysk.proc;

import java.util.Observer;
import java.util.Observable;

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
	{
	    // Store the extracted auxv here.
	    Auxv[] auxv;
	    CaptureAuxv ()
	    {
		Manager.host.procAdded.addObserver (new Observer ()
		    {
			public void update (Observable o, Object obj)
			{
			    Proc proc = (Proc) obj;
			    proc.taskDiscovered.addObserver (new Observer ()
				{
				    public void update (Observable o, Object obj)
				    {
					Task task = (Task) obj;
					auxv = task.proc.getAuxv ();
				    }
				});
			}
		    });

	    }
	}

	TmpFile tmpFile = new TmpFile ();
	CaptureAuxv captureAuxv = new CaptureAuxv ();
	new StopEventLoopOnProcDestroy ();
	Manager.host.requestCreateProc (null, tmpFile.toString (), null,
					new String[] {
					    "./prog/print/auxv"
					});

	assertRunUntilStop ("run \"auxv\" to completion");

	assertNotNull ("AUXV successfully captured", captureAuxv.auxv);
	Auxv[] auxv = captureAuxv.auxv;

	// Compare the AUXV as printed against that extracted using
	// Proc.getAuxv.
	try {
	    util.Scanner reader = new util.Scanner (tmpFile.getFile ());
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
	Manager.host.requestRefresh ();
	Manager.eventLoop.runPending ();

	assertEquals ("Child's name", "detach",
		      child.findProc ().getCommand ());
    }
}
