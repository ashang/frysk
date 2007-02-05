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

package frysk.expunit;

import frysk.sys.PseudoTerminal;
import frysk.sys.Sig;
import frysk.sys.Signal;
import frysk.sys.Errno;
import frysk.sys.Wait;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Simple expect like framework, that works within JUnit.
 *
 * This works creating a pty with a daemon process attached.
 *
 * XXX: At present, since it is a daemon, it isn't possible to expect
 * an exit status.
 */

public class Expect
{
    static protected Logger logger = Logger.getLogger("frysk");

    private final PseudoTerminal child = new PseudoTerminal ();
    private int pid = -1;

    /**
     * Create an expect instance running the specified program and
     * args.
     */
    public Expect (String[] args)
    {
	pid = child.addChild (args);
	logger.log (Level.FINE, "{0} new {1} pid {2,number,integer}\n",
		    new Object[] {
			this,
			child,
			new Integer (pid)
		    });
    }

    /**
     * Using <tt>bash</tt, create an expect instance running the
     * specified command.  Since the command is invoked using
     * <tt>bash</tt> it will be expanded using that shells variable
     * and globbing rules.
     */
    public Expect (String command)
    {
	this (new String[] { "/bin/bash", "-c", command });
    }

    /**
     * Clean up.
     */
    public void close ()
    {
	if (pid >= 0) {
	    logger.log (Level.FINE, "{0} close\n", this);
	    child.close ();
	    try {
		Signal.tkill (pid, Sig.KILL);
	    }
	    catch (Errno e) {
		// Toss it, as cleanup.
	    }
	    try {
		Wait.waitAll (pid, new WaitObserver (0));
	    }
	    catch (TerminationException e) {
		// Toss it, as cleanup.
	    }
	    catch (Errno e) {
		// Toss it, as cleanup.
	    }
	}
    }

    /**
     * The default timeout, in milli-seconds.
     */
    private long millisecondTimeout = 1000; // 1 second
    /**
     * Set the default timeout (in milliseconds).
     */
    public void setTimeoutMillis (long millisecondTimeout)
    {
	this.millisecondTimeout = millisecondTimeout;
    }
    /**
     * Get the default timeout (in milliseconds).
     */
    public long getTimeoutMillis ()
    {
	return millisecondTimeout;
    }

    /**
     * Finalizer, also tries to clean up.
     */
    public void finalize ()
    {
	close ();
    }

    /**
     * Return the Process ID of the child.
     */
    public int getPid ()
    {
	return pid;
    }

    /**
     * Buffer containing all the unmatched output from the child.
     * It's a String and not a StringBuffer as after every change the
     * buffer gets re-converted to a String anyway.
     */
    private String output = new String ();
    /**
     * Detected end-of-file, or hang-up.
     */
    private boolean eof = false;
    
    /**
     * Block waiting on the inferior for more output, appending it to
     * the buffer IN.  When the child exits this sees an EOF
     * indication.
     */
    private void pollChild (long millisecondTimeout)
    {
	if (eof) {
	    logger.log (Level.FINE, "{0} pollChild -> already EOF\n", this);
	    return;
	}
	if (child.ready (millisecondTimeout)) {
	    byte[] bytes = new byte[100];
	    int nr = child.read (bytes, 0, bytes.length);
	    switch (nr) {
	    case -1:
		logger.log (Level.FINE, "{0} pollChild -> EOF\n", this);
		eof = true;
		break;
	    case 0:
		logger.log (Level.FINE, "{0} pollChild -> no data!\n", this);
		break;
	    default:
		String read = new String (bytes, 0, nr);
		output = output + read;
		logger.log (Level.FINE,
			    "{0} pollChild -> read <<{1}>> giving <<{2}>>\n",
			    new Object[] { this, read, output });
		break;
	    }
	}
    }

    /**
     * Sends "string" to the child process.
     */
    void send (String string)
    {
	logger.log (Level.FINE,
		    "{0} send <<{1}>>\n",
		    new Object[] { this, string });
	byte[] bytes = string.getBytes ();
	child.write (bytes, 0, bytes.length);
    }

    /**
     * Expect one of the specified patterns; throw TimeoutException if
     * timeoutMillis expires; throw EofException if end-of-file is
     * encountered.
     */
    public void expect (long millisecondTimeout, Match[] matches)
    {
	final long endTime = System.currentTimeMillis () + millisecondTimeout;
	while (true) {
	    if (matches != null) {
		for (int i = 0; i < matches.length; i++) {
		    Match p = matches[i];
		    if (p != null) {
			if (p.find (output)) {
			    logger.log (Level.FINE,
					"{0} match <<{1}>>\n",
					new Object[] { this, p.group () });
			    p.execute ();
			    // Remove everying up to and including what
			    // matched.
			    if (p.end () >= 0)
				output = output.substring (p.end ());
			    return;
			}
		    }
		}
	    }
	    if (eof) {
		logger.log (Level.FINE, "{0} match EOF\n", this);
		throw new EofException ();
	    }
	    long timeRemaining = endTime - System.currentTimeMillis ();
	    if (timeRemaining <= 0) {
		logger.log (Level.FINE, "{0} match TIMEOUT\n", this);
		throw new TimeoutException (millisecondTimeout);
	    }
	    pollChild (timeRemaining);
	}
    }

    /**
     * Expect a specified pattern, throw a TimeoutException if the
     * default timeout expires or EofException if end-of-file is
     * reached.
     */
    public void expect (Match[] matches)
    {
	expect (millisecondTimeout, matches);
    }

    /**
     * Expect the specified pattern, throw a TimeoutException if the
     * specified timeout expires or EofException if end-of-file is
     * reached.
     */
    public void expect (long timeoutMillis, Match match)
    {
	expect (timeoutMillis, new Match[] { match });
    }

    /**
     * Expect the specified pattern, throw a TimeoutException if the
     * default timeout expires or EofException if end-of-file is
     * reached.
     */
    public void expect (Match match)
    {
	expect (millisecondTimeout, match);
    }

    /**
     * Expect the specified regular expression, throw a
     * TimeoutException if the specified timeout expires or
     * EofException if end-of-file is reached.
     */
    public void expect (long millisecondTimeout, String regex)
    {
	expect (millisecondTimeout, new Regex (regex));
    }

    /**
     * Expect the specified regular expression, throw a
     * TimeoutException if the default timeout expires or EofException
     * if end-of-file is reached.
     */
    public void expect (String regex)
    {
	expect (millisecondTimeout, regex);
    }

    /**
     * Expect a TimeoutException after millisecondTimeout.
     */
    public void expect (long millisecondTimeout)
    {
	expect (millisecondTimeout, (Match[]) null);
    }

    /**
     * Expect a TimeoutException to be thrown after waiting the
     * default time period.
     */
    public void expect ()
    {
	expect (millisecondTimeout);
    }

    /**
     * Expect an EOF.
     */
    public void expectEOF ()
    {
	try {
	    expect ();
	}
	catch (EofException e) {
	    // Just what the doctor ordered.
	}
    }

    /**
     * Expect the child process to have terminated.  A +ve or zero
     * value indicates an exit status, a -ve value indicates
     * termination with signal.
     */
    public void expectTermination (final int status)
    {
	try {
	    expect ();
	}
	catch (EofException e) {
	    // This is blocking; which probably isn't good.
	    Wait.waitAll (pid, new WaitObserver (status));
	}
    }
}
