// This file is part of the program FRYSK.
//
// Copyright 2008, Red Hat Inc.
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

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import frysk.proc.Proc;

/**
 * UnloadCommand handles the unloading of processes that have been loaded
 * by the load command.
 * 
 */

public class UnloadCommand extends ParameterizedCommand {

    UnloadCommand() {
	super("unload",
		"unload [ -t id ]",
		"The unload command allows a user to unload processes that"
		+ " have been loaded via the 'load' command.  The user can"
		+ " either specify a 'path-to-executable' as a parameter if"
		+ " the name of the process is not unique or use the name"
		+ " of the process or unload the process using its target id"
		+ " which is printed out if the unload command is given"
		+ " without any parameters.");
    }
    
    public void interpret(CLI cli, Input cmd, Object options) {
	
	if (cmd.size() > 3) {
	    throw new InvalidCommandException("Too many parameters");
	} else if (cmd.size() < 1) {
	    // List the loaded procs if no parameters entered
	    LoadCommand.listLoadedProcs(cli);
	    return;
	}
	
	if (cmd.parameter(0).equals("-t")) {
	    if (cmd.size() != 2)
		throw new InvalidCommandException("Not enough parameters");
	    int id = Integer.parseInt(cmd.parameter(1));
	    Proc proc = cli.idManager.getProc(id);
	    removeFromHashMap(proc, cli.getLoadedProcs(), cli);
	    proc.getHost().remove(proc);
	    cli.idManager.removeProcID(id);
	}
    }
    
    /**
     * removeFromHashMap removes the designated process from the designated HashMap.
     */
    private void removeFromHashMap(Proc proc, HashMap procMap, CLI cli) {
	String procCommand = proc.getExe();
	Set procSet = procMap.entrySet();
	Iterator foo = procSet.iterator();
	while (foo.hasNext()) {
	    Map.Entry me = (Map.Entry) foo.next();
	    Proc newProc = (Proc) me.getKey();
	    if (procCommand.equals(newProc.getExe())) {
		synchronized (cli) {
		    foo.remove();
		}
		return;
	    }
	}
    }
    
    int completer(CLI cli, Input input, int cursor, List completions) {
	return CompletionFactory.completeFileName(cli, input, cursor,
		completions);
    }
}