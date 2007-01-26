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

/**
 * Minimal testing for the FileDescriptor.
 */

public class TestFileDescriptor
    extends TestCase
{
    FileDescriptor in;
    FileDescriptor out;
    public void setUp ()
    {
	FileDescriptor[] pipe = FileDescriptor.pipe ();
	in = pipe[0];
	out = pipe[1];
    }

    /**
     * Test the ready method, a pipe is only ready if something was
     * written to it.*/
    public void testReady ()
    {
	assertFalse ("empty pipe not ready", in.ready ());
	out.write ((byte) 1);
	assertTrue ("non-empty pipe ready", in.ready ());
    }

    /**
     * Test passing individual bytes through a pipe.
     */
    public void testByteIO ()
    {
	byte[] values = "hello".getBytes ();
	for (int i = 0; i < values.length; i++ ) {
	    out.write (values[i]);
	}
	// This is risky, could hang.
	for (int i = 0; i < values.length; i++) {
	    assertTrue ("ready", in.ready ());
	    assertEquals ("value " + i, in.read (), values[i]);
	}
    }

    /**
     * Test passing arrays of bytes through a pipe.
     */
    public void testArrayIO ()
    {
	String hello = "hello";
	String xxxhelloyyy = "xxxhelloyyy";

	//  Simple read/write.
	{
	    out.write (hello.getBytes (), 0, hello.length ());
	    // This is risky, could hang.
	    assertTrue ("ready", in.ready ());
	    byte[] bytesIn = new byte[100];
	    int bytesRead = in.read (bytesIn, 0, bytesIn.length);
	    assertEquals ("bytes transfered", hello.length (), bytesRead);
	    assertEquals ("contents", hello, new String (bytesIn, 0, bytesRead));
	}

	// Write a sub-buffer
	{
	    out.write (xxxhelloyyy.getBytes (), 3, hello.length ());
	    // This is risky, could hang.
	    assertTrue ("ready", in.ready ());
	    byte[] bytesIn = new byte[100];
	    int bytesRead = in.read (bytesIn, 0, bytesIn.length);
	    assertEquals ("bytes transfered", hello.length (), bytesRead);
	    assertEquals ("contents", hello, new String (bytesIn, 0, bytesRead));
	}

	// Read a sub-buffer
	{
	    out.write (hello.getBytes (), 0, hello.length ());
	    // This is risky, could hang.
	    assertTrue ("ready", in.ready ());
	    byte[] bytesIn = "xxxHELLOyyy".getBytes ();
	    int bytesRead = in.read (bytesIn, 3, bytesIn.length);
	    assertEquals ("bytes transfered", hello.length (), bytesRead);
	    assertEquals ("contents", xxxhelloyyy, new String (bytesIn));
	}
    }

    /**
     * Test closing a pipe.
     */
    public void testByteEOF ()
    {
	out.close ();
	int b = in.read ();
	assertEquals ("eof", -1, b);
    }

    /**
     * Test closing a pipe.
     */
    public void testArrayEOF ()
    {
	out.close ();
	int b = in.read (new byte[10], 0, 10);
	assertEquals ("eof", -1, b);
    }

    /**
     * Test input and output streams.
     */
    public void testInputOutputStreams ()
	throws java.io.IOException
    {
	InputStream ins = in.getInputStream ();
	OutputStream outs = out.getOutputStream ();
	assertEquals ("input stream available", ins.available (), 0);
	outs.write (1);
	assertEquals ("input stream available", ins.available (), 1);
	assertEquals ("read back", ins.read (), 1);
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
	for (int i = 0; i < 2000; i++)
	    setUp ();
    }
}
