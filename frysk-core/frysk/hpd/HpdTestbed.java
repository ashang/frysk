// This file is part of the program FRYSK.
//
// Copyright 2007 Red Hat Inc.
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
import frysk.expunit.Expect;
import frysk.expunit.Match;
import frysk.expunit.Regex;
import frysk.expunit.EofException;
import frysk.expunit.TimeoutException;
import java.io.File;

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

    /**
     * Create an FHPD process managed by Expect.
     */
    public HpdTestbed() {
	super(Config.getBinFile("fhpd"));
	expectPrompt();
    }

    /**
     * Create an FHPD process, with PARAM, managed by expect; wait for
     * the prompt.
     */
    public HpdTestbed(File param) {
	super(new String[] {
		  Config.getBinFile("fhpd").getPath (),
		  param.getAbsolutePath()
	      });
	expectPrompt();
    }

    /**
     * Create an FHPD process, with PARAM, managed by expect; wait for
     * the STARTUP message followed immediatly by the prompt.
     */
    public HpdTestbed(String param, String startup) {
	super(new String[] {
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
	} catch (EofException e) {
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
	} catch (EofException e) {
	    TestCase.fail("expecting: <" + prompt + "> got: EOF");
	} catch (TimeoutException t) {
	    TestCase.fail("expecting: <" + prompt + "> got: TIMEOUT");
	}
	return this;
    }

    /**
     * Expect OUTPUT immediatly followed by the prompt.
     */
    public HpdTestbed expectPrompt(String output) {
	return expectPrompt("expecting: <" + output + ">", output);
    }

    /**
     * Send COMMAND ("\n" will be appended); expect OUTPUT along with
     * the prompt (OUTPUT must match everything up to the prompt
     * including newlines); if other ooutput, or a timeout, fail.
     */
    public HpdTestbed sendCommandExpectPrompt(final String command,
					      final String output) {
	send(command);
	send("\n");
	return expectPrompt("sent: <" + command
			    + "> expecting: <" + output + ">",
			    output);
    }
}
