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

import java.io.File;
import java.text.ParseException;
import java.util.ArrayList;

import frysk.debuginfo.DebugInfo;
import frysk.proc.Proc;
import frysk.stack.StackFactory;

public class CoreCommand extends CLIHandler {

    private static String name = "core";
    private static String desc = "open a core file";
    
    CoreCommand(CLI cli) {
	super(name, cli, new CommandHelp(name, desc, "core core.file", desc));
    }

    public void handle(Command cmd) throws ParseException {
	ArrayList params = cmd.getParameters();
	if (params.size() == 1 && params.get(0).equals("-help")) {
	    cli.printUsage(cmd);
	    return;
	}
	
	if (params.size() != 1) {
	    cli.printUsage(cmd);
	    return;
	}
	
	Proc coreProc = frysk.util.Util.getProcFromCoreFile(new File((String) params.get(0)));
	
	
	cli.proc = coreProc;
	cli.task = coreProc.getMainTask();
	cli.frame = StackFactory.createFrame(cli.task);
	cli.debugInfo = new DebugInfo(cli.frame);
	cli.attached = true;
	cli.addMessage("Attached to core file: " + params.get(0), Message.TYPE_NORMAL);
    }

}