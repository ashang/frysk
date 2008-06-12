// This file is part of the program FRYSK.
//
// Copyright 2007, 2008, Red Hat Inc.
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

package frysk.expunit;

import frysk.sys.ProcessIdentifier;
import frysk.sys.Signal;
import frysk.rsl.Log;
import java.io.File;

/**
 * Simple expect like framework, that works within JUnit.
 *
 * This works creating a pty with a daemon process attached.
 *
 * XXX: At present, since it is a daemon, it isn't possible to expect
 * an exit status.
 */

public class Expect {
    private static final Log fine = Log.fine(Expect.class);

    private final Child child;
    private final long timeoutMilliseconds;
    private Expect(Child child, long timeoutMilliseconds) {
	this.child = child;
	this.timeoutMilliseconds = timeoutMilliseconds;
    }

    /**
     * Create an expect instance running the specified program args[0]
     * and args.
     */
    public Expect(String[] args) {
	this(new Child(args), defaultTimeoutSeconds * 1000);
	fine.log(this, "new", child, "args", args);
    }
    /**
     * Create an expect instance running PROGRAM with no arguments.
     * 
     * Turns out that doing this is common and it saves the hassle of creating
     * an argument list.
     */
    public Expect(File program) {
	this(new String[] { program.getAbsolutePath() });
    }
    /**
     * Using <tt>bash</tt, create an expect instance running the
     * specified command.  Since the command is invoked using
     * <tt>bash</tt> it will be expanded using that shells variable
     * and globbing rules.
     */
    public Expect(String command) {
	this(new String[] { "/bin/bash", "-c", command });
    }

    /**
     * Create a bash shell sitting at a prompt.
     */
    public Expect() {
	this(new String[] {
		"/bin/bash", "-c",
		"export PS1=$\\ ; export PS2=>\\ ; exec /bin/bash --norc --noprofile"
	    });
	expect("\\$ ");
    }

    /**
     * Clean up.
     *
     * XXX: This drains all outstanding WAITPID events, and SIGCHLD
     * events.
     */
    public void close() {
	if (child != null) {
	    child.close();
	}
    }
    /**
     * Finalizer, also tries to clean up.
     */
    public void finalize() {
	close();
    }

    /**
     * The global default timeout (in seconds).
     */
    static private int defaultTimeoutSeconds = 1; // 1 seconds.
    /**
     * Set the global default timeout (in seconds).  Any expect
     * classes inherit this value.
     */
    static public void setDefaultTimeoutSeconds(int defaultTimeoutSeconds) {
	Expect.defaultTimeoutSeconds = defaultTimeoutSeconds;
    }

    /**
     * Create a new expect (that shares everything with the old one)
     * only with a different timeout.
     */
    public Expect timeout(int timeoutSeconds) {
	return timeoutMilliseconds(timeoutSeconds * 1000);
    }
    /**
     * Create a new expect (that shares everything with the old one)
     * only with a different timeout.
     */
    Expect timeoutMilliseconds(long timeoutMilliseconds) {
	return new Expect(child, timeoutMilliseconds);
    }

    /**
     * Return the Process ID of the child.
     */
    public ProcessIdentifier getPid() {
	return child.getPid();
    }

    /**
     * Send "string" to the child process.
     */
    public void send(String string) {
	child.send(string);
    }

    /**
     * Expect a specified pattern, throw a TimeoutException if the
     * default timeout expires or EofException if end-of-file is
     * reached.
     */
    public void expect(Match[] matches) {
	child.expectMilliseconds(timeoutMilliseconds, matches);
    }

    /**
     * Expect the specified pattern, throw a TimeoutException if the
     * default timeout expires or EofException if end-of-file is
     * reached.
     */
    public void expect(Match match) {
	expect(new Match[] { match });
    }

    /**
     * Expect the specified regular expression, throw a
     * TimeoutException if the default timeout expires or EofException
     * if end-of-file is reached.
     */
    public void expect(String regex) {
	expect(new Regex (regex));
    }

    /**
     * Expect an EOF.
     */
    public void expectEOF() {
	try {
	    expect(new Match[0]);
	} catch (EndOfFileException e) {
	    // Just what the doctor ordered.
	}
    }

    /**
     * Expect the child process to exit with status.
     */
    public void expectTermination(int status) {
	try {
	    expect(new Match[0]);
	} catch (EndOfFileException e) {
	    // This is blocking; which probably isn't good.
	    getPid().blockingWait(new WaitObserver(status));
	}
    }

    /**
     * Expect the child process to be terminated by SIGNAL.
     */
    public void expectTermination(Signal signal) {
	try {
	    expect(new Match[0]);
	} catch (EndOfFileException e) {
	    // This is blocking; which probably isn't good.
	    getPid().blockingWait(new WaitObserver(signal));
	}
    }
}
