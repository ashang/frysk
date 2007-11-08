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
import frysk.proc.dead.CorefileStatus;
import frysk.proc.dead.LinuxHost;
import frysk.proc.Manager;
import frysk.proc.Proc;
import frysk.proc.Task;

public class CoreCommand extends ParameterizedCommand {

    CoreCommand() {
	super("core", "core <core-file> [ <executable> ]",
	      "open a core file");
    }

    void interpret(CLI cli, Input cmd, Object options) {
    	
	Proc coreProc;
	File exeFile = null;
	LinuxHost coreHost = null;
    
	// If > 2 parameter, then too many parameters.
	if (cmd.size() > 2) {
	    throw new InvalidCommandException("Too many parameters");
	}
	
	// If < 1 parameter, then not enough parameters.
	if (cmd.size() < 1) {
	    throw new InvalidCommandException("Please specify a corefile with the core command");		
	}

	File coreFile = new File(cmd.parameter(0));

	// Build corefile host, report errors and quit if any.
	if (cmd.size() == 1)
		try {
			coreHost = new LinuxHost(Manager.eventLoop, coreFile);
		} catch (Exception e) {
			cli.addMessage("An error has occured while loading corefile: '" + coreFile.getAbsolutePath() 
					+ "'. Error message is: " + e.getMessage(), Message.TYPE_ERROR);
			return;
		}
	else {
	    exeFile = new File(cmd.parameter(1));
	    try {
	    	coreHost = new LinuxHost(Manager.eventLoop, coreFile, exeFile);
	    } catch (Exception e) {
			cli.addMessage("An error has occured while loading corefile: '" + coreFile.getAbsolutePath() 
					+ "'. Error message is: " + e.getMessage(), Message.TYPE_ERROR);
			return;
	    	
	    }
	}
	
	// Get an iterator to the one process
	Iterator i = coreHost.getProcIterator(); 
	
	// Find process, if not error out and return.
	if (i.hasNext())
		coreProc = (Proc) i.next(); 
	else {
		cli.addMessage("Cannot find a process in corefile: '" + coreFile.getAbsolutePath()+"'. This may not be a valid ELF corefile.", Message.TYPE_ERROR);
		return;
	}
	
	// If > 1 process, this is a very odd corefile. Abort, can't handle multiple process corefiles.
	if (i.hasNext()) {
	    cli.addMessage("There appears to be two or more processes in corefile: '" + coreFile.getAbsolutePath()+"'. This is not valid for an ELF corefile", Message.TYPE_ERROR);
	    return;
	}
	
	// Check status, and report whether we managed to load an executable. If not build a message
	// to the effect of why we have not.
	CorefileStatus status = coreHost.getStatus();
	if (status.hasExe == false)
	{
		String message = "The corefile: '"+coreFile.getAbsolutePath()+"' has no executable associated with it. The executable name ";
		String exeName = "";
		if (exeFile != null) 
		{
			exeName = exeFile.getAbsolutePath();
			message += "specified by the user on the Core command was: '" + exeName;
		}
		else
		{
			exeName = coreProc.getExe();
			message += "automatically read from the corefile was: '" + exeName;
		}
		message+="'. This executable could not be read. Please specifiy an executable on the 'core' command (eg core core.1234 exenamedFile) for richer metadata.";
		
		cli.addMessage(message,Message.TYPE_WARNING);
	}
	
	// All checks are done. Host is built. Now start reserving space in the sets
	int procID = cli.idManager.reserveProcID();
	cli.idManager.manageProc(coreProc, procID);
		
	// Build debug info for each task and frame.
	Iterator foo = cli.targetset.getTasks();
	while (foo.hasNext()) {
	    Task task = (Task) foo.next();
	    DebugInfoFrame frame = DebugInfoStackFactory
		.createVirtualStackTrace(task);
	    cli.setTaskFrame(task, frame);
	    cli.setTaskDebugInfo(task, new DebugInfo(
						     frame));
	}

	// Finally, done.
	cli.addMessage("\n* Attached to core file: " + cmd.parameter(0),
		Message.TYPE_NORMAL);

	
    }

}
