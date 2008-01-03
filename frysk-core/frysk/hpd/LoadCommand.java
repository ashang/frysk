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
import java.util.Iterator;
import frysk.debuginfo.DebugInfo;
import frysk.debuginfo.DebugInfoFrame;
import frysk.debuginfo.DebugInfoStackFactory;
import frysk.proc.Host;
import frysk.proc.dead.LinuxExeHost;
import frysk.proc.Manager;
import frysk.proc.Proc;
import frysk.proc.Task;
import java.util.List;

/**
 * LoadCommand handles the "load path-to-executable" command on the fhpd
 * commandline.
 *
 */

public class LoadCommand extends ParameterizedCommand {

    LoadCommand() {
	super("load", "load path-to-executable", "load an executable file");
    }

    public void interpret(CLI cli, Input cmd, Object options) {
	if (cmd.size() > 2) {
	    throw new InvalidCommandException("Too many parameters");
	} else if (cmd.size() < 1) {
            throw new InvalidCommandException("missing arguments");
        }
        
	File executableFile = new File(cmd.parameter(0));

	if (!executableFile.exists() || !executableFile.canRead()
		|| !executableFile.isFile()) {
	    throw new InvalidCommandException
		("File does not exist or is not readable or is not a file.");
	}

	Host exeHost = new LinuxExeHost(Manager.eventLoop, executableFile);
	Proc exeProc = frysk.util.Util.getProcFromExeFile(exeHost);
	
	int procID = cli.idManager.reserveProcID();
	cli.idManager.manageProc(exeProc, procID);
	
	Iterator foo = cli.targetset.getTasks();
	while (foo.hasNext()) {
	    Task task = (Task) foo.next();
	    if (task.getTid() == exeProc.getMainTask().getTid()) {
		DebugInfoFrame frame = DebugInfoStackFactory
			.createDebugInfoStackTrace(task);
		cli.setTaskFrame(task, frame);
		cli.setTaskDebugInfo(task, new DebugInfo(
			frame));
	    }
	}
	synchronized (cli) {
	    cli.getLoadedProcs().put(exeProc, new Integer(procID));
	}
    
    cli.addMessage("Loaded executable file: " + cmd.parameter(0),
		Message.TYPE_NORMAL);
    }

    int completer(CLI cli, Input input, int cursor, List completions) {
	return CompletionFactory.completeFileName(cli, input, cursor,
						  completions);
    }
}
