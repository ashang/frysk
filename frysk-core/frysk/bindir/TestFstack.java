// This file is part of the program FRYSK.
//
// Copyright 2006, 2007, Red Hat Inc.
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

package frysk.bindir;

import frysk.Config;
import frysk.expunit.Expect;
import frysk.proc.Proc;
import frysk.testbed.CoreFileAtSignal;
import frysk.testbed.TestLib;

/**
 * This performs a "sniff" test of Fstack, confirming basic
 * functionality.
 */

public class TestFstack
    extends TestLib
{
    Expect e;
    
    public void tearDown ()
    {
	if (e != null)
	    e.close ();
	e = null;
	super.tearDown();
    }

    public void testBackTrace () {
	
	Proc proc = CoreFileAtSignal.constructCore("funit-stacks");
	e = new Expect (new String[] {
			    Config.getBinFile("fstack").getAbsolutePath (),
			    "core." + proc.getPid()
			});
	// Just look for main.
	e.expect ("main");
    }
    
    public void testBackTraceWithParams () {
	Proc proc = CoreFileAtSignal.constructCore("funit-stacks");
        e = new Expect (new String[] {
			    Config.getBinFile("fstack").getAbsolutePath (),
			    "core." + proc.getPid(),
			    "--print","params"
			});
        e.expect ("int param1");
    }

    public void testBackTraceWithScopes () {
	Proc proc = CoreFileAtSignal.constructCore("funit-stacks");
        e = new Expect (new String[] {
			    Config.getBinFile("fstack").getAbsolutePath (),
			    "core." + proc.getPid(),
			    "--print", "scopes"
			});
        e.expect ("int one.*int two");
    }

    public void testBackTraceWithFullpath () {
	Proc proc = CoreFileAtSignal.constructCore("funit-stacks");
        e = new Expect (new String[] {
			    Config.getBinFile("fstack").getAbsolutePath (),
			    "core." + proc.getPid(),
			    "--print", "fullpath"
			});
        e.expect (Config.getRootSrcDir()
		  + ".*"
		  + proc.getCommand()
		  + ".c#");
    }

    public void testBackTraceWithDashA () {
	Proc proc = CoreFileAtSignal.constructCore("funit-stacks");
        e = new Expect (new String[] {
			    Config.getBinFile("fstack").getAbsolutePath (),
			    "core." + proc.getPid(),
			    "-a"
			});
        e.expect ("fourth.*int param1.*int param2.*int param3.*"
		  + Config.getRootSrcDir()
		  + ".*"
		  + proc.getCommand() + "\\.c#" 
		  + ".*char var1.*"
		  + "");
    }

    public void testBackTraceWithDashC () {
	Proc proc = CoreFileAtSignal.constructCore("funit-stacks");
        e = new Expect (new String[] {
			    Config.getBinFile("fstack").getAbsolutePath (),
			    "core." + proc.getPid(),
			    "-c"
			});
        
        e.expect ("fourth.*int param1.*int param2.*int param3.*"
		  + Config.getRootSrcDir()
		  + ".*"
		  + proc.getCommand() + "\\.c#" 
		  + "");
  }

    public void testBackTraceWithDashV () {
	Proc proc = CoreFileAtSignal.constructCore("funit-inlined");
        e = new Expect (new String[] {
			    Config.getBinFile("fstack").getAbsolutePath (),
			    "-v",
			    "-a",
			    "core." + proc.getPid()
			});
        e.expect("0.1.*third[^\r\n]*\\[inline\\]"
		 + ".*0.2.*second[^\r\n]*\\[inline\\]"
		 + ".*0.3.*first[^\r\n]*\\[inline\\]"
		 + ".*main");
    }

}
