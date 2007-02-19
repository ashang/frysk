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
import java.io.File;
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
    private ProcessIdentifier pid;

    /**
     * Some tests use a pty that needs closing.
     */
    private PseudoTerminal pty;

    /**
     * During setup, clear the daemon process.
     */
    public void setUp ()
    {
	pid = null;
	pty = null;
    }
    /**
     * During teardown, kill any daemon process, if present.
     */
    public void tearDown ()
    {
	if (pid != null) {
	    try {
		pid.kill ();
	    }
	    catch (Errno e) {
		// toss it; don't care
	    }
	}
	if (pty != null) {
	    try {
		pty.close ();
	    }
	    catch (Errno e) {
		// toss it; don't care
	    }
	}
	Signal.drain (Sig.CHLD);
    }

    /**
     * Check that a PseudoTerminal opens successfully.
     */
    public void testOpen ()
    {
	pty = new PseudoTerminal();
	final int b = 0x3f;
	
	int master = pty.getFd ();
	assertFalse ("master is invalid", master == -1);
	File file = pty.getFile ();
	assertNotNull ("file", file);
	pty.write ((byte) b);
    }

    /**
     * Wait a short period of time for something to become available.
     */
    private void assertBecomesAvailable (InputStream in)
	throws java.io.IOException, InterruptedException
    {
	int delay;
	final long maxDelay = getTimeoutMilliseconds ();
	for (delay = 0; delay < maxDelay; delay++) {
	    if (in.available () > 0)
		return;
	    Thread.sleep (10);
	}
	fail ("assertBecomesAvailable timeout");
    }

    /**
     * Create a pseudo-terminal with an attached daemon process.
     */
    private void createPseudoTerminalDaemon (String[] args)
    {
	pty = new PseudoTerminal ();
	pid = pty.addDaemon (args);
    }

    /**
     * Wire a PseudoTerminal up to /bin/echo, check that the expected
     * output string is returned.
     */
    public void testEchoHi ()
	throws java.io.IOException, InterruptedException
    {
	String hi = "hello";
	createPseudoTerminalDaemon (new String[] { "/bin/echo", hi });
	InputStream in = pty.getInputStream ();
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
	createPseudoTerminalDaemon (new String[] { "/usr/bin/tee" });
	InputStream in = pty.getInputStream ();
	OutputStream out = pty.getOutputStream ();
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
	createPseudoTerminalDaemon (new String [] { "/bin/true" });
	InputStream in = pty.getInputStream ();
	assertBecomesAvailable (in);
	byte[] bytes = new byte[100];
	int bytesRead = in.read (bytes);
	assertEquals ("eof read", -1, bytesRead);
    }
}
