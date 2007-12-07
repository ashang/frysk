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

import java.util.List;
import frysk.debuginfo.DebugInfo;
import frysk.debuginfo.DebugInfoFrame;
import frysk.debuginfo.DebugInfoStackFactory;
import frysk.proc.Manager;
import frysk.proc.Proc;
import frysk.proc.Task;
import frysk.proc.dead.LinuxHost;

public class CoreCommand extends ParameterizedCommand {

	File coreFile = null;

	File exeFile = null;

	boolean noExeOption = false;

	CoreCommand() {
		super("Load a Corefile.", "core <core-file> [ <executable> ] [ -noexe ]",
				"Opens, loads and models corefile.");

		add(new CommandOption("noexe", "Do not attempt to load executable ") {
			void parse(String argument, Object options) {
				noExeOption = true;
			}
		});

	}

	void interpret(CLI cli, Input cmd, Object options) {

		Proc coreProc;
		LinuxHost coreHost = null;

		// If > 2 parameter, then too many parameters.
		if (cmd.size() > 2) {
			throw new InvalidCommandException(
					"Too many parameters, a maximum of two should be specified.");
		}

		// If < 1 parameter, then not enough parameters.
		if (cmd.size() < 1) {
			throw new InvalidCommandException(
					"Please specify a corefile with the core command");
		}

		// Command line seems, sane parse.
		parseCommandLine(cmd);

		// Does the corefile exist?
		if ((!coreFile.exists()) || (!coreFile.canRead()
			|| coreFile.isDirectory()))
			throw new InvalidCommandException(
					"No core file found, or cannot read corefile");

		// Build Core. Move any exceptions up to cli and print to user.
		coreHost = getHost(coreFile, exeFile, noExeOption);

		// Get the core proc.
		coreProc = getProc(coreHost);

		// Error out if no exe found, and -noexe option specified
		if ((noExeOption == false) && (coreHost.getStatus().hasExe == false)) {
			cli.addMessage(
					"Could not find executable: '"
					+ coreProc.getExe()+  "' specified for corefile. "
					+ "You can specify one with the core command. E.g: core core.file yourexefile. Alternatively "
					+ "you can tell fhpd to ignore the executable with -noexe. E.g core core.file -noexe. No "
					+ "corefile has been loaded at this time.",
					Message.TYPE_ERROR);
			return;
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
			cli.setTaskDebugInfo(task, new DebugInfo(frame));
		}

		// Finally, done.
		cli.addMessage("Attached to core file: " + cmd.parameter(0),
				Message.TYPE_NORMAL);
		// See if there was an executable specified
		if (coreHost.getStatus().hasExe == false)
		    return;
		synchronized (cli) {
		    cli.getCoreProcs().put(coreProc, new Integer(procID));
		}

	}

	// Build Correct Host on options.
	private LinuxHost getHost(File coreFile, File executable, boolean loadExe) {
		LinuxHost coreHost = null;
		if (executable == null)
			if (!loadExe)
				coreHost = new LinuxHost(Manager.eventLoop, coreFile);
			else
				coreHost = new LinuxHost(Manager.eventLoop, coreFile, null);
		else
			coreHost = new LinuxHost(Manager.eventLoop, coreFile, executable);

		return coreHost;
	}

	// From a Host, get a Proc
	private Proc getProc(LinuxHost coreHost) {
		// Get an iterator to the one process
		Iterator i = coreHost.getProcIterator();

		// Find process, if not error out and return.
		if (i.hasNext())
			return (Proc) i.next();
		else
			return null;
	}

	// Parse the option commandline
	private void parseCommandLine(Input cli) {
		coreFile = new File(cli.parameter(0));
		if (cli.size() == 1)
			return;
		else
			exeFile = new File(cli.parameter(1));
	}

    int completer(CLI cli, Input input, int cursor, List completions) {
	return CompletionFactory.completeFileName(cli, input, cursor,
						  completions);
    }
}
