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
import java.io.InputStream;
import java.io.OutputStream;
import java.util.WeakHashMap;
import java.util.Map;
import java.util.Iterator;

/**
 * Minimal testing for the FileDescriptor.
 */

public class TestFileDescriptor
    extends TestCase
{
    private Pipe pipe;
    public void setUp ()
    {
	pipe = new Pipe ();
    }
    public void tearDown ()
    {
	try {
	    pipe.close ();
	}
	catch (Errno e) {
	    // Toss it, tear down.
	}
    }

    private void assertBecomesReady (FileDescriptor fd)
    {
	final int maxDelay = 100;
	int delay;
	for (delay = 0; delay < maxDelay; delay++) {
	    if (fd.ready ())
		break;
	    try {
		Thread.sleep (1);
	    }
	    catch (InterruptedException e) {
		fail ("wait interrupted");
	    }
	}
	assertTrue ("FileDescriptor became ready", delay < maxDelay);
    }

    /**
     * Test the ready method, a pipe is only ready if something was
     * written to it.*/
    public void testReady ()
    {
	assertFalse ("empty pipe not ready", pipe.in.ready ());
	pipe.out.write ((byte) 1);
	assertTrue ("non-empty pipe ready", pipe.in.ready ());
    }

    /**
     * Test passing individual bytes through a pipe.
     */
    public void testByteIO ()
    {
	byte[] values = "hello".getBytes ();
	for (int i = 0; i < values.length; i++ ) {
	    pipe.out.write (values[i]);
	}
	// This is risky, could hang.
	for (int i = 0; i < values.length; i++) {
	    assertTrue ("ready", pipe.in.ready ());
	    assertEquals ("value " + i, pipe.in.read (), values[i]);
	}
    }

    private final String hello = "hello";
    private final String xxxhelloyyy = "xxxhelloyyy";

    /**
     * Test passing arrays of bytes through a pipe.
     */
    public void testArrayIO ()
    {
	//  Simple read/write.
	pipe.out.write (hello.getBytes (), 0, hello.length ());
	// This is risky, could hang.
	assertTrue ("ready", pipe.in.ready ());
	byte[] bytesIn = new byte[100];
	int bytesRead = pipe.in.read (bytesIn, 0, bytesIn.length);
	assertEquals ("bytes transfered", hello.length (), bytesRead);
	assertEquals ("contents", hello, new String (bytesIn, 0, bytesRead));
    }
    /**
     * Test a sub-buffer write.
     */
    public void testArraySubBufferWrite ()
    {
	pipe.out.write (xxxhelloyyy.getBytes (), 3, hello.length ());
	// This is risky, could hang.
	assertTrue ("ready", pipe.in.ready ());
	byte[] bytesIn = new byte[100];
	int bytesRead = pipe.in.read (bytesIn, 0, bytesIn.length);
	assertEquals ("bytes transfered", hello.length (), bytesRead);
	assertEquals ("contents", hello, new String (bytesIn, 0, bytesRead));
    }

    /**
     * Test a sub-buffer read.
     */
    public void testArraySubBufferRead ()
    {
	pipe.out.write (hello.getBytes (), 0, hello.length ());
	// This is risky, could hang.
	assertTrue ("ready", pipe.in.ready ());
	byte[] bytesIn = "xxxHELLOyyy".getBytes ();
	int bytesRead = pipe.in.read (bytesIn, 3, bytesIn.length);
	assertEquals ("bytes transfered", hello.length (), bytesRead);
	assertEquals ("contents", xxxhelloyyy, new String (bytesIn));
    }

    /**
     * Test closing a pipe.
     */
    public void testByteEOF ()
    {
	pipe.out.close ();
	assertBecomesReady (pipe.in);
	int b = pipe.in.read ();
	assertEquals ("eof", -1, b);
    }

    /**
     * Test closing a pipe.
     */
    public void testArrayEOF ()
    {
	pipe.out.close ();
	assertBecomesReady (pipe.in);
	int b = pipe.in.read (new byte[10], 0, 10);
	assertEquals ("eof", -1, b);
    }

    /**
     * Test input and output streams.
     */
    public void testInputOutputStreams ()
	throws java.io.IOException
    {
	InputStream ins = pipe.in.getInputStream ();
	OutputStream outs = pipe.out.getOutputStream ();
	assertEquals ("input stream available", ins.available (), 0);
	outs.write (1);
	assertEquals ("input stream available", ins.available (), 1);
	assertEquals ("read back", ins.read (), 1);
    }

    /**
     * Try duping the output of a Pipe back to its input.
     */
    public void testDupPipeOutToIn ()
	throws java.io.IOException
    {
	assertFalse ("input available on pipe.in", pipe.in.ready());
	pipe.out.getOutputStream ().write ((byte) 1);
	assertTrue ("input available on pipe.in", pipe.in.ready());
	pipe.out.dup (pipe.in);
	assertTrue ("input available on pipe.out", pipe.out.ready());
	assertEquals ("read worked", 1, pipe.out.read ());
    }

    /**
     * Try opening a random file and reading it.  Looking for an error
     * to be thrown.
     */
    public void testOpenEtcPasswd ()
    {
	FileDescriptor f = new FileDescriptor ("/etc/passwd",
					       FileDescriptor.RDONLY);
	f.read ();
    }

    /**
     * Allocate, but loose, lots and lots of pipe file desciptors,
     * checks that a garbage collect eventually occures.
     *
     * This test relies on the underlying code managing to trigger a
     * garbage collect, something that in java, isn't really reliable.
     * However, with a requested garbage collect, and a yield, the gc
     * likes to run.
     */
    public void testLeakyPipes ()
    {
	// Keep a table of all file file descriptors created; weak so
	// that a garbage collect can empty it.
	Map fds = new WeakHashMap ();
	
	for (int i = 0; i < 2000; i++) {
	    setUp ();
	    fds.put (pipe.in, null);
	    fds.put (pipe.out, null);
	}
	// Close out any FileDescriptors not yet garbage collected.
	for (Iterator i = fds.keySet ().iterator (); i.hasNext (); ) {
	    FileDescriptor fd = (FileDescriptor) i.next ();
	    fd.close ();
	}
    }
}
