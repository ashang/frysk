// This file is part of the program FRYSK.
//
// Copyright 2007, 2008 Red Hat Inc.
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

package frysk.hpd;

import frysk.junit.TestCase;
import frysk.Config;
import frysk.testbed.CorefileFactory;
import frysk.expunit.Expect;
import frysk.expunit.Match;
import frysk.expunit.Regex;
import frysk.expunit.EndOfFileException;
import frysk.expunit.TimeoutException;
import java.io.File;
import frysk.testbed.TearDownExpect;
import frysk.testbed.TearDownProcess;
import frysk.sys.ProcessIdentifier;

/**
 * Variation on frysk.expunit.Expect that drives the HPD.
 */

public class HpdTestbed
    extends Expect
{
    /**
     * The prompt string.
     */
    private final String prompt = "\\(fhpd\\) ";

    private HpdTestbed(String[] command) {
	super(command);
	TearDownExpect.add(this);
	TearDownProcess.add(getPid());
    }

    /**
     * Create an FHPD process managed by Expect.
     */
    public HpdTestbed() {
	this(new String[] {
		Config.getBinFile("fhpd").getPath()
	     });
	expectPrompt();
    }

    /**
     * Create an FHPD process, with PARAM, managed by expect; wait for
     * the STARTUP message followed immediately by the prompt.
     */
    public HpdTestbed(String param, String startup) {
	this(new String[] {
		  Config.getBinFile("fhpd").getPath (),
		  param
	      });
	expectPrompt(startup);
    }

    /**
     * Expect; OUTPUT followed by PROMPT; report WHY.
     */
    private HpdTestbed expectPrompt(final String why, final String output) {
	try {
	    expect(new Match[] {
		       new Regex(output + prompt),
		       new Regex(".*\r\n" + prompt) {
			   public void execute() {
			       TestCase.fail(why + " got: <" + group() + ">");
			   }
		       }
		   });
	} catch (EndOfFileException e) {
	    TestCase.fail(why + " got: <EOF>");
	} catch (TimeoutException t) {
	    TestCase.fail(why + " got: <TIMEOUT>");
	}
	return this;
    }

    /**
     * Expect; after skipping garbage, a command prompt.
     */
    public HpdTestbed expectPrompt() {
	try {
	    expect(prompt);
	} catch (EndOfFileException e) {
	    TestCase.fail("expecting: <" + prompt + "> got: EOF");
	} catch (TimeoutException t) {
	    TestCase.fail("expecting: <" + prompt + "> got: TIMEOUT");
	}
	return this;
    }

    /**
     * Expect OUTPUT immediately followed by the prompt.
     */
    public HpdTestbed expectPrompt(String output) {
	return expectPrompt("expecting: <" + output + ">", output);
    }

    /**
     * Send COMMAND ("\n" will be appended); expect OUTPUT along with
     * the prompt (OUTPUT must match everything up to the prompt
     * including newlines); if other output, or a timeout, fail.
     */
    public HpdTestbed sendCommandExpectPrompt(final String command,
					      final String output) {
	send(command);
	send("\n");
	return expectPrompt("sent: <" + command
			    + "> expecting: <" + output + ">",
			    output);
    }


    /**
     * Start the specified program as a separate process and then
     * attach to it.
     *
     * XXX: This implementation of broken as there is a race between
     * the process and the hpd starting, leading to the started
     * processes state not being well defined when HPD attaches to it.
     */
    static HpdTestbed attachXXX(String program) {
	Expect child = new Expect(Config.getPkgLibFile(program));
	TearDownExpect.add(child);
	ProcessIdentifier pid = child.getPid();
	TearDownProcess.add(pid);
	return new HpdTestbed(pid.toString(),
			      "Attached to process "
			      + pid
			      + "\r\n");
    }

    /**
     * Run the specified program from under HPD.
     */
    static HpdTestbed run(String program, String args) {
	HpdTestbed h = new HpdTestbed();
	File exe = Config.getPkgLibFile(program);
	h.send("run ");
	h.send(exe.getAbsolutePath());
	if (args != null) {
	    h.send(" ");
	    h.send(args);
	}
	h.send("\n");
	try {
	    h.expect(new Match[] {
			 new Regex("Attached to process ([0-9]+)\r\n"
				   + h.prompt) {
			     public void execute() {
				 int pid = Integer.parseInt(group(1));
				 TearDownProcess.add(pid);
			     }
			 },
			 new Regex(".*\r\n" + h.prompt) {
			     public void execute() {
				 TestCase.fail("Expecting <run> got: <"
					       + group() + ">");
			     }
			 }
		     });
	} catch (EndOfFileException e) {
	    TestCase.fail("Expecting <run " + program + "> got: <EOF>");
	} catch (TimeoutException t) {
	    TestCase.fail("Expecting <run " + program + "> got: <TIMEOUT>");
	}
	return h;
    }

    static HpdTestbed run(String program) {
	return run(program, null);
    }

    /**
     * Start the specified program from under HPD.
     */
    static HpdTestbed start(String program, String args) {
	HpdTestbed h = new HpdTestbed();
	File exe = Config.getPkgLibFile(program);
	h.send("start ");
	h.send(exe.getAbsolutePath());
	if (args != null) {
	    h.send(" ");
	    h.send(args);
	}
	h.send("\n");
	try {
	    h.expect(new Match[] {
			 new Regex("Attached to process ([0-9]+)\r\n"
				   + h.prompt) {
			     public void execute() {
				 int pid = Integer.parseInt(group(1));
				 TearDownProcess.add(pid);
			     }
			 },
			 new Regex(".*\r\n" + h.prompt) {
			     public void execute() {
				 TestCase.fail("Expecting <start> got: <"
					       + group() + ">");
			     }
			 }
		     });
	} catch (EndOfFileException e) {
	    TestCase.fail("Expecting <start " + program + "> got: <EOF>");
	} catch (TimeoutException t) {
	    TestCase.fail("Expecting <start " + program + "> got: <TIMEOUT>");
	}
	return h;
    }

    static HpdTestbed start(String program) {
	return start(program, null);
    }
    /**
     * Start HPD attached to PROGRAM that is crashing (due to a
     * signal).
     *
     * XXX: The current implementation runs the program until it
     * crashes and then pulls a core file from it; the code then loads
     * the core-file into the HPD.  A future implementation may just
     * run the program to the terminating event.
     */
    static HpdTestbed hpdTerminatingProgram(String program) {
	File exeFile = Config.getPkgLibFile(program);
	File coreFile = CorefileFactory.constructCoreAtSignal(exeFile);
	HpdTestbed hpd
	    = new HpdTestbed(new String[] {
				 Config.getBinFile("fhpd").getPath (),
				 coreFile.toString(),
				 exeFile.toString()
			     });
	hpd.expectPrompt("Attached to core file.*");
	return hpd;
    }
}
