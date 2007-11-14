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

package frysk.hpd;

import java.io.PrintWriter;
import inua.eio.ByteBuffer;
import frysk.proc.Proc;
import frysk.proc.ProcId;
import frysk.proc.Task;
import java.util.List;

/**
 * PeekCommand handles the "peek memory-location" command on the fhpd
 * commandline.  This command is only used after a "load" command has
 * been issued.
 *
 */

public class PeekCommand extends ParameterizedCommand {

    PeekCommand() {
	super("peek", "peek <memory-location>",
	      "peek at an executable file's memory");
    }

    void interpret(CLI cli, Input cmd, Object options) {
	final PrintWriter output = cli.getPrintWriter();
	if (cmd.size() > 1 ) {
	    throw new InvalidCommandException("Too many parameters");
	}
	if (cli.exeHost == null) {
	    throw new InvalidCommandException("No executable loaded");
	}
	
	Proc proc = cli.exeHost.getProc(new ProcId(0));
	Task task = proc.getMainTask();
	
	ByteBuffer buffer = task.getMemory();

	String memposition = cmd.parameter(0);
	int radix = 10;
	if (memposition.lastIndexOf("x") != -1) {
	    radix = 16;
	    memposition = memposition.substring(memposition.lastIndexOf("x") + 1);
	    if (memposition.lastIndexOf("L") != -1)
		memposition = memposition.substring(0, memposition.lastIndexOf("L"));
	}
	
	try {
	    long value = Long.parseLong(memposition.trim(), radix);
	    buffer.position(value);
	    output.println("The value at " + memposition + " = " + buffer.getUByte());
	} catch (NumberFormatException nfe) {
	    System.out.println("NumberFormatException: " + nfe.getMessage());
	}
	
    }

    int completer(CLI cli, Input input, int cursor, List completions) {
	return CompletionFactory.completeExpression(cli, input, cursor,
						    completions);
    }
}
