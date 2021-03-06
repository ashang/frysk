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

import frysk.sys.Errno;
import frysk.sys.ProcessIdentifier;
import frysk.sys.PseudoTerminal;
import frysk.sys.Signal;
import frysk.rsl.Log;
import frysk.rsl.LogFactory;

/**
 * The child being waited on.
 */

class Child {
    static protected Log fine = LogFactory.fine(Child.class);
    static private Log finest = LogFactory.finest(Child.class);

    private final PseudoTerminal child;
    private final ProcessIdentifier pid;

    /**
     * Create an expect instance running the specified program args[0]
     * and args.
     */
    Child(String[] args) {
	child = new PseudoTerminal();
	pid = child.addChild(args);
	fine.log(this, "new", child, "pid", pid, "args", args);
    }

    public String toString() {
	return child.toString();
    }

    /**
     * Clean up.
     *
     * XXX: This drains all outstanding WAITPID events, and SIGCHLD
     * events.
     */
    public void close() {
	if (child != null) {
	    fine.log(this, "close child");
	    child.close();
	}
	if (pid != null) {
	    fine.log(this, "close pid");
	    try {
		pid.kill();
	    } catch (Errno e) {
		// Toss it, as cleanup.
	    }
	    pid.blockingDrain();
	}
	Signal.CHLD.drain();
    }

    /**
     * Finalizer, also tries to clean up.
     */
    public void finalize() {
	close ();
    }

    /**
     * Return the Process ID of the child.
     */
    ProcessIdentifier getPid() {
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
     * Send "string" to the child process.
     */
    void send(String string) {
	fine.log(this, "send", (Object)string);
	byte[] bytes = string.getBytes();
	child.write(bytes, 0, bytes.length);
    }

    /**
     * Expect one of the specified patterns; throw TimeoutException if
     * timeoutSecond expires; throw EofException if end-of-file is
     * encountered.
     * 
     * This is package visible so that Expect can use it, but no one
     * else.
     */
    void expectMilliseconds(long timeoutMilliseconds, Match[] matches) {
	final long endTime = (System.currentTimeMillis()
			      + timeoutMilliseconds);
	fine.log(this, "expect", matches,
		 "timeout [milliseconds]", (int)timeoutMilliseconds);
	while (true) {
	    for (int i = 0; i < matches.length; i++) {
		Match p = matches[i];
		finest.log(this, "find", p, "in", (Object) output);
		if (p.find(output)) {
		    fine.log(this, "match", (Object) p.group(),
			     "with", p);
		    p.execute();
		    // Remove everything up to and including what
		    // matched.
		    if (p.end() >= 0)
			output = output.substring(p.end());
		    return;
		}
	    }
	    if (eof) {
		fine.log(this, "match EOF");
		throw new EndOfFileException(matches, output);
	    }
	    long timeRemaining = endTime - System.currentTimeMillis();
	    if (timeRemaining <= 0) {
		fine.log(this, "match TIMEOUT");
		throw new TimeoutException(timeoutMilliseconds, matches,
			output);
	    }

	    finest.log(this, "poll for [milliseconds]", timeRemaining);
	    if (child.ready(timeRemaining)) {
		byte[] bytes = new byte[100];
		int nr = child.read(bytes, 0, bytes.length);
		switch (nr) {
		case -1:
		    finest.log(this, "poll -> EOF");
		    eof = true;
		    break;
		case 0:
		    finest.log(this, "poll -> no data!");
		    break;
		default:
		    String read = new String(bytes, 0, nr);
		    output = output + read;
		    finest.log(this, "poll -> ", (Object) read, "giving",
			    (Object) output);
		    break;
		}
	    }
	}
    }
}
