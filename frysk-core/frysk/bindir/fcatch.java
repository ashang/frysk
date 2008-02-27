// This file is part of the program FRYSK.
//
// Copyright 2005, 2006, 2007, Red Hat Inc.
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

import java.util.logging.Logger;
import frysk.util.Util;
import frysk.util.CommandlineParser;
import frysk.util.FCatch;
import frysk.proc.Proc;
import gnu.classpath.tools.getopt.Option;
import gnu.classpath.tools.getopt.OptionException;

public class fcatch {

    FCatch catcher = new FCatch();

    protected static final Logger logger = Logger.getLogger("frysk");

    private boolean requestedPid = false;

    private static StringBuffer argString;

    private void run(String[] args) {
	CommandlineParser parser = new CommandlineParser("fcatch") {
	    protected void validate() throws OptionException {
		if (!requestedPid && argString == null)
		    throw new OptionException("no command or PID specified");
	    }

	    //@Override
	    public void parseCommand(Proc command) {
		// FIXME: This concatinatin the string is unnecessary.
		String[] line = command.getCmdLine();
		argString = new StringBuffer(line[0]);
		for (int i = 1; i < line.length; i++)
		    argString.append(" ").append(line[i]);
	    }

	};
	addOptions(parser);
	parser
		.setHeader("Usage: fcatch [OPTIONS] -- PATH ARGS || fcatch [OPTIONS] PID");

	parser.parse(args);

	if (argString != null) {
	    String[] cmd = argString.toString().split("\\s");

	    catcher.trace(cmd, requestedPid);
	}
    }

    public void addOptions(CommandlineParser p) {
	p.add(new Option('p', "pid to trace", "PID") {
	    public void parsed(String arg) throws OptionException {
		try {
		    int pid = Integer.parseInt(arg);
		    catcher.addProc(Util.getProcFromPid(pid));
		    requestedPid = true;
		    if (argString == null)
			argString = new StringBuffer(pid);
		    else
			argString.append(" " + pid);

		} catch (NumberFormatException e) {
		    OptionException oe = new OptionException(
			    "couldn't parse pid: " + arg);
		    oe.initCause(e);
		    throw oe;
		}
	    }
	});
    }

    public static void main(String[] args) {
	fcatch fc = new fcatch();
	fc.run(args);
    }
}
