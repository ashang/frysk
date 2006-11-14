// This file is part of the program FRYSK.
//
// Copyright 2005, Red Hat Inc.
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

package frysk.sys.proc;

import frysk.junit.TestCase;

/**
 * Test the CmdLineBuilder against a predefined set of
 * <tt>cmdline</tt> buffers.
 *
 * The table of test functions was generated by the program
 * <tt>frysk-imports/tests/cmdline/print</tt>.
 */
public class TestCmdLine
    extends TestCase
{
    /**
     * Verify that the unpacked auxv matches expected.
     */
    private void check (String[] argv, byte[] cmdline)
    {
	class Builder
	    extends CmdLineBuilder
	{
	    String[] argv;
	    public void buildBuffer (byte[] auxv)
	    {
	    }
	    public void buildArgv (String[] argv)
	    {
		this.argv = argv;
	    }
	}
	Builder builder = new Builder ();
	builder.construct (cmdline);
	assertNotNull ("argv", builder.argv);
	assertEquals ("argv.length", argv.length, builder.argv.length);
	for (int i = 0; i < argv.length; i++) {
	    assertEquals ("argv[" + i + "]", argv[i], builder.argv[i]);
	}
    }

    // Start generated code.
    public void test ()
    {
	check (new String[] { },
	       new byte[] {/*0*/});
    }
    public void test_0 ()
    {
	check (new String[] { "" },
	       new byte[] {/*1*/ 0});
    }
    public void test_a0 ()
    {
	check (new String[] { "a" },
	       new byte[] {/*2*/ 97, 0});
    }
    public void test_0_0 ()
    {
	check (new String[] { "", "" },
	       new byte[] {/*2*/ 0, 0});
    }
    public void test_a0_0 ()
    {
	check (new String[] { "a", "" },
	       new byte[] {/*3*/ 97, 0, 0});
    }
    public void test_0_b0 ()
    {
	check (new String[] { "", "b" },
	       new byte[] {/*3*/ 0, 98, 0});
    }
    public void test_a0_b0 ()
    {
	check (new String[] { "a", "b" },
	       new byte[] {/*4*/ 97, 0, 98, 0});
    }
    public void test_a0_b0_c0 ()
    {
	check (new String[] { "a", "b", "c" },
	       new byte[] {/*6*/ 97, 0, 98, 0, 99, 0});
    }
    // End generated code.
}
