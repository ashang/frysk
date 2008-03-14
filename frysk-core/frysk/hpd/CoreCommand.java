// This file is part of the program FRYSK.
//
// Copyright 2007, 2008, Red Hat Inc.
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
import frysk.sysroot.SysRootCache;
import frysk.proc.Task;
import frysk.proc.dead.LinuxCoreFactory;
import java.io.IOException;
import frysk.proc.Proc;

public class CoreCommand extends ParameterizedCommand {

    private static class Options {
	boolean loadMetaData = true;
	String sysroot = "/";
    }
    Object options() {
	return new Options();
    }

    CoreCommand() {
	super("Load a Corefile.",
	      "core <core-file> [ <executable> ] [ -noexe ]"
	      + "[ -sysroot Path ]", "Opens, loads and models corefile.");

	add(new CommandOption("noexe", "Do not attempt to load executable ") {
		void parse(String argument, Object options) {
		    ((Options)options).loadMetaData = false;
		}
	    });
	add(new CommandOption("sysroot", "pathname to use as a sysroot",
			      "Pathname") {
		void parse(String args, Object options) {
		    ((Options)options).sysroot = args;
		}
	    });
    }

    void interpret(CLI cli, Input cmd, Object optionsObject) {
	Options options = (Options)optionsObject;
	File coreFile;
	File exeFile;

	switch (cmd.size()) {
	case 0:
	    throw new InvalidCommandException
		("Please specify a corefile with the core command");
	case 1:
	    // <core>
	    coreFile = new File(cmd.parameter(0));
	    exeFile = null;
	    break;
	case 2:
	    coreFile = new File(cmd.parameter(0));
	    exeFile = new File(cmd.parameter(1));
	    break;
	default:
	    throw new InvalidCommandException
		("Too many parameters, a maximum of two should be specified.");
	}

	// Make paths canonical (keeps elfutils working).
	try {
	    coreFile = coreFile.getCanonicalFile();
	    if (exeFile != null)
		exeFile = exeFile.getCanonicalFile();
	} catch (IOException e) {
	    throw new RuntimeException(e);
	}

	// Build Core. Move any exceptions up to cli and print to user.
	Proc coreProc = LinuxCoreFactory.createProc(coreFile, exeFile,
						    options.loadMetaData);

	load(coreProc, cli, options.sysroot);
    }

    public static void load(Proc coreProc, CLI cli) {
    load(coreProc, cli, null);
    }

    public static void load(Proc coreProc, CLI cli, String sysroot) {
	// All checks are done. Host is built. Now start reserving
	// space in the sets.
	int procID = cli.idManager.reserveProcID();
	cli.idManager.manageProc(coreProc, procID);

	// Build debug info for each task and frame.
	for (Iterator i = cli.targetset.getTasks(); i.hasNext(); ) {
	    Task task = (Task) i.next();
	    DebugInfoFrame frame = DebugInfoStackFactory
		.createVirtualStackTrace(task);
	    cli.setTaskFrame(task, frame);
	    cli.setTaskDebugInfo(task, new DebugInfo(frame));
	    SysRootCache.setSysroot(task, sysroot);
	}
	// Finally, done.
	synchronized (cli) {
	    cli.getCoreProcs().put(coreProc, new Integer(procID));
	}
	cli.outWriter.println("Attached to core file: "
			      + coreProc.getHost().getName());
    }

    int completer(CLI cli, Input input, int cursor, List completions) {
	return CompletionFactory.completeFileName(cli, input, cursor,
						  completions);
    }
}
