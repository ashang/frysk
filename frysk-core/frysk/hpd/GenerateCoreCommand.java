// This file is part of the program FRYSK.
//
// Copyright 2008 Red Hat Inc.
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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import frysk.proc.Proc;
import frysk.proc.Task;
import frysk.proc.dead.DeadProc;
import frysk.isa.corefiles.LinuxElfCorefile;
import frysk.isa.corefiles.LinuxElfCorefileFactory;

public class GenerateCoreCommand extends ParameterizedCommand {

    	String corefileName = "core";
	LinuxElfCorefile coreFile = null;

	File exeFile = null;

	boolean writeAllMaps = false;

	GenerateCoreCommand() {
		super("Generate a Corefile.", "dump [ -a ]",
				"Generates and writes a corefile");

		add(new CommandOption("a", "Write all segments, do not elide.") {
			void parse(String argument, Object options) {
				writeAllMaps = true;
			}
		});

		add(new CommandOption("o", "Optional corefile name", "name") {
			void parse(String argument, Object options) {
				corefileName = argument;
			}
		});
	}

	void interpret(CLI cli, Input cmd, Object options) {
	    
	    int processCount = 0;
	    HashMap procList = new HashMap();
	    PTSet ptset = cli.getCommandPTSet(cmd);
	    Iterator taskDataIterator = ptset.getTaskData();
	    if (taskDataIterator.hasNext() == false) {
		cli.addMessage("Cannot find main task. Cannot create core file.", 
				Message.TYPE_ERROR);
		return;
	    }

	    
	    while (taskDataIterator.hasNext()) {
		
		Proc proc = ((TaskData) taskDataIterator.next()).getTask().getProc();
		if (!procList.containsValue(proc)) {
		    procList.put(proc,proc);
		    processCount++;
		}
	    }

	    Iterator procIterator = procList.values().iterator();

	    while (procIterator.hasNext()) {

		Proc mainProc = ((Proc)procIterator.next());
		if (mainProc instanceof DeadProc) {
		    cli.addMessage("Process has to be alive (not core or not started) to dump core.", Message.TYPE_WARNING);
		    continue;
		}
		cli.addMessage("Generating corefile '"+corefileName+"."+
				mainProc.getPid()+"' for process: " + 
				mainProc.getPid(), Message.TYPE_NORMAL);
		Task[] tasks = (Task[]) mainProc.getTasks().toArray(new Task[mainProc.getTasks().size()]);
		coreFile = LinuxElfCorefileFactory.getCorefile(mainProc, tasks);
		
		if (writeAllMaps)
		    coreFile.setWriteAllMaps(true);
		
		coreFile.setName(corefileName);
		
		coreFile.constructCorefile();
	    }
	}

    int completer(CLI cli, Input input, int cursor, List completions) {
	return CompletionFactory.completeFileName(cli, input, cursor,
						  completions);
    }
}
