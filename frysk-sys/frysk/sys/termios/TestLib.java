// This file is part of the program FRYSK.
//
// Copyright 2007, Red Hat Inc.
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

package frysk.sys.termios;

import java.util.logging.Logger;
import java.util.logging.Level;
import frysk.junit.TestCase;
import frysk.sys.PseudoTerminal;
import frysk.sys.FileDescriptor;
import frysk.sys.ProcessIdentifier;
import frysk.sys.Signal;
import frysk.sys.Errno;
import frysk.sys.Sig;

/**
 * Manipulates a terminal bound to FileDescriptor.
 */
public class TestLib
    extends TestCase
{
  protected final static Logger logger = Logger.getLogger("frysk.sys.termios");
    protected PseudoTerminal pty;
    private FileDescriptor fd;
    protected Termios termios;
    protected ProcessIdentifier pid;

    public void setUp ()
    {
	pty = new PseudoTerminal ();
	fd = new FileDescriptor (pty.getFile (), FileDescriptor.RDONLY);
	termios = new Termios (fd);
	// Don't keep FD, open, prevents seeing EOF when reading PTY.
	fd.close ();
	fd = null;
	pid = null;
    }

    public void tearDown ()
    {
	if (pty != null) {
	    pty.close ();
	}
	if (fd != null) {
	    fd.close ();
	}
	if (pid != null) {
	    try {
		pid.kill ();
	    }
	    catch (Errno e) {
		// Don't care.
	    }
	    pid.blockingDrain ();
	}
	Signal.drain (Sig.CHLD);
    }

    /**
     * Update the pty's child with termios.
     */
    protected void setPseudoTerminal (Termios termios)
    {
	fd = new FileDescriptor (pty.getFile (), FileDescriptor.RDONLY);
	termios.set (fd);
	fd.close ();
	fd = null;
    }

    /**
     * Update the Termios from the pty's child.
     */
    protected void getPseudoTerminal (Termios termios)
    {
	fd = new FileDescriptor (pty.getFile (), FileDescriptor.RDONLY);
	termios.get (fd);
	fd.close ();
	fd = null;
    }

    /**
     * Run stty on the test PTY, verify that the output contains the
     * EXPECT string.
     */
    protected void verifySttyOutputContains (String expected)
    {
	logger.log (Level.FINE, "{0} verifySttyOutputContains <<{1}>>\n",
		    new Object[] { this, expected });
	// Checking for EOF won't work if there's an open file
	// descriptor on the PTY slave.
	assertNull ("file descriptor closed so EOF works", fd);
	StringBuffer output = new StringBuffer ();
	// Create a child process, ensure that it has exited and hence
	// that it finished binding to the pty before trying to read
	// any output.  This assumes that there is sufficient space
	// for all the stty output in the pty.
	pid = pty.addChild (new String[] { "/bin/stty", "-a" });
	pid.blockingDrain ();
	while (true) {
	    assertTrue (pty.ready (getTimeoutMilliseconds ()));
	    int ch = pty.read ();
	    if (ch < 0)
		break;
	    output.append ((char) ch);
	}
	int index = output.indexOf (expected);
	assertTrue ("output <<" + output + ">> contains <<" + expected + ">>",
		    index >= 0);
	// Now check around it.
	int[] checks = new int[] { index - 1, index + expected.length () };
	for (int i = 0; i < checks.length; i++) {
	    int check = checks[i];
	    // OK of outside buffer.
	    if (check < 0 || check >= output.length ())
		continue;
	    char ch = output.charAt (check);
	    if (Character.isWhitespace (ch) || ch == ';')
		continue;
	    fail ("whitespace around expected <<" + expected + ">>");
	}
    }
}
