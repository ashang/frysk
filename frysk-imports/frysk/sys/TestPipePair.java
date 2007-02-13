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

package frysk.sys;

import frysk.junit.TestCase;

/**
 * Test creation of a process wired up to a pipe.
 */

public class TestPipePair
    extends TestCase
{
    private PipePair pipe;
    public void tearDown ()
    {
	if (pipe != null) {
	    try {
		pipe.close ();
	    }
	    catch (Errno e) {
		// discard - tearDown
	    }
	    try {
		pipe.pid.kill ();
	    }
	    catch (Errno e) {
		// discard - tearDown
	    }
	    pipe.pid.drainWait ();
	    Signal.drain (Sig.CHLD);
	    pipe = null;
	}
    }

    final String[] tee = new String[] { "/usr/bin/tee" };

    /**
     * Verify that what goes in comes out.
     */
    public void verifyIO ()
    {
	assertFalse ("pipe.in.ready at start", pipe.in.ready ());
	pipe.out.write ('a');
	assertTrue ("pipe.in.ready with data",
		    pipe.in.ready (getTimeoutMilliseconds ()));
	assertEquals ("pipe.in.read", 'a', pipe.in.read ());
	pipe.out.write ('b');
	assertTrue ("pipe.in.ready with data",
		    pipe.in.ready (getTimeoutMilliseconds ()));
	assertEquals ("pipe.in.read", 'b', pipe.in.read ());
	pipe.out.close ();
	assertTrue ("pipe.in.ready with EOF", 
		    pipe.in.ready (getTimeoutMilliseconds ()));
	assertEquals ("pipe.in.read", -1, pipe.in.read ());
    }

    /**
     * Test a daemon Pipe pair.
     */
    public void testDaemonTee ()
    {
	pipe = new DaemonPipePair (tee);
	verifyIO ();
    }
    public void testChildTee ()
    {
	pipe = new ChildPipePair (tee);
	verifyIO ();
    }

    /**
     * Test a daemon assembly file.
     */
    public void testDaemonExecute ()
    {
	pipe = new DaemonPipePair (new Tee ());
	verifyIO ();
    }

    /**
     * Test a child assembly file.
     */
    public void testChildExecute ()
    {
	pipe = new ChildPipePair (new Tee ());
	verifyIO ();
    }

    /**
     * XXX: Instead of having TestPipePair implement Execute, this
     * static nested class is used.  This is to avoid having a native
     * class that extends junit.framework.TestCase - a header file for
     * which which isn't available.
     */
    static class Tee
	implements Execute
    {
	public native void execute ();
    }
}
