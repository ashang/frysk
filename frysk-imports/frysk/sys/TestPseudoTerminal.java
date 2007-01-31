// This file is part of the program FRYSK.
//
// Copyright 2005, 2006, 2007, Red Hat Inc.
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

import frysk.junit.TestCase;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Test manipulation of the pseudo-terminal stuff.
 */

public class TestPseudoTerminal extends TestCase
{
    /**
     * Some tests have a child process, keep it in this PID.
     */
    int pid;

    /**
     * During setup, clear the daemon process.
     */
    public void setUp ()
    {
	pid = -1;
    }
    /**
     * During teardown, kill any daemon process, if present.
     */
    public void tearDown ()
    {
	if (pid > 0) {
	    try {
		Signal.tkill (pid, Sig.KILL);
	    }
	    catch (Errno e) {
		// toss it; don't care
	    }
	}
    }

    /**
     * Check that a PseudoTerminal opens successfully.
     */
    public void testOpen ()
    {
	PseudoTerminal pty = new PseudoTerminal();
	final int b = 0x3f;
	
	int master = pty.getFd ();
	assertFalse ("master is invalid", master == -1);
	String name = pty.getName ();
	assertNotNull ("name is null", name);
	pty.write ((byte) b);
    }

    /**
     * Wait a short period of time for something to become available.
     */
    private void assertBecomesAvailable (InputStream in)
	throws java.io.IOException, InterruptedException
    {
	int delay;
	final int maxDelay = 100;
	for (delay = 0; delay < maxDelay; delay++) {
	    if (in.available () > 0)
		break;
	    Thread.sleep (1);
	}
	assertTrue ("something available before timeout", delay < maxDelay);
    }

    /**
     * Create a pseudo-terminal with an attached daemon process.
     */
    private PseudoTerminal getPseudoTerminalDaemon (String[] args)
    {
	PseudoTerminal pty = new PseudoTerminal ();
	pid = pty.addDaemon (args);
	return pty;
    }

    /**
     * Wire a PseudoTerminal up to /bin/echo, check that the expected
     * output string is returned.
     */
    public void testEchoHi ()
	throws java.io.IOException, InterruptedException
    {
	String hi = "hello";
	PseudoTerminal echo = getPseudoTerminalDaemon (new String[] { "/bin/echo", hi });
	InputStream in = echo.getInputStream ();
	assertBecomesAvailable (in);
	// Read back the message
	byte[] bytes = new byte[100];
	int bytesRead  = in.read (bytes);
	assertEquals ("read hi", new String (bytes, 0, bytesRead),
		      hi + "\r\n");
    }

    /**
     * Wire a PseudoTerminal up to tee, which agressively copies its
     * input-to-output, check what is fed in comes back.
     */
    public void testTeeHi ()
	throws java.io.IOException, InterruptedException
    {
	String hi = "hello";
	PseudoTerminal tee = getPseudoTerminalDaemon (new String[] { "/usr/bin/tee" });
	InputStream in = tee.getInputStream ();
	OutputStream out = tee.getOutputStream ();
	out.write (hi.getBytes ());
	assertBecomesAvailable (in);
	byte[] bytes = new byte[100];
	int bytesRead = in.read (bytes);
	assertEquals ("read hi", new String (bytes, 0, bytesRead), hi);
    }

    /**
     * Wire a PseudoTerminal to a program that just exits, creating
     * EOF. Check that is correctly detected.
     */
    public void testEOF ()
	throws java.io.IOException, InterruptedException
    {
	PseudoTerminal exit = getPseudoTerminalDaemon (new String [] { "/bin/true" });
	InputStream in = exit.getInputStream ();
	assertBecomesAvailable (in);
	byte[] bytes = new byte[100];
	int bytesRead = in.read (bytes);
	assertEquals ("eof read", -1, bytesRead);
    }
  }
