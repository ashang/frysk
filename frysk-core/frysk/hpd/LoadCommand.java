// This file is part of the program FRYSK.
//
// Copyright 2007, 2008 Red Hat Inc.
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import frysk.proc.dead.LinuxExeFactory;
import frysk.debuginfo.DebugInfo;
import frysk.debuginfo.DebugInfoFrame;
import frysk.debuginfo.DebugInfoStackFactory;
import frysk.sysroot.SysRootCache;
import frysk.proc.Proc;
import frysk.proc.Task;

/**
 * LoadCommand handles the "load path-to-executable" command on the fhpd
 * commandline.
 * 
 */

public class LoadCommand extends ParameterizedCommand {

    LoadCommand() {
	super("load", 
	      "load [ path-to-executable ] [ -sysroot Path ]", 
	      "The load command lets the user examine information about"
	      + " an executable file without actually running it.  An"
	      + " executable must be loaded with this command before it"
	      + " can be run with either the 'start' or 'run' command. "
	      + " If no args are entered a list of the loaded procs (if any)"
	      + " is displayed.\nNo arguments to be passed to the proc are"
	      + " entered here.  Those arguments are passed to the proc(s)"
	      + " via the 'start' or 'run' commands.");
        add(new CommandOption("sysroot", "pathname to use as a sysroot",
			      "Pathname") {
		void parse(String args, Object options) {
		    ((Options)options).sysroot = args;
		}
	    });
        add(new CommandOption("exe", "Path to executable", "executable") {
		void parse(String args, Object options) {
		    ((Options)options).executable = args;
		}
	    });
    }

    private static class Options {
	String sysroot = "/";
	String executable = null;
    }
    Object options() {
	return new Options();
    }

    public void interpret(CLI cli, Input cmd, Object options) {
	
	Options o = (Options)options;
	if (cmd.size() < 1) {
	    if (cli.loadedProcs.isEmpty()) {
		cli.addMessage("No loaded procs currently",
			       Message.TYPE_NORMAL);
		return;
	    } else {
		// List the loaded procs if no parameters entered
		printLoop(cli, "Loaded procs", cli.loadedProcs);
		return;
	    }
	}
	Proc exeProc;
	if (o.executable != null) {
	    SysRootCache.setSysroot(o.executable, o.sysroot);
	    exeProc = LinuxExeFactory.createProc
		(new File(o.executable), cmd.stringArrayValue());
	} else {
	    SysRootCache.setSysroot(cmd.stringArrayValue()[0], o.sysroot);
	    exeProc = LinuxExeFactory.createProc(cmd.stringArrayValue(), o.sysroot);
	}

	load(exeProc, cli, o.sysroot, cmd.stringArrayValue());
    }

    public static void load(Proc exeProc, CLI cli, String[] params) {
	load(exeProc, cli, null, params);
    }

    public static void load(Proc exeProc, CLI cli, String sysroot, 
	    String[] params) {
	
	int procID;
	if (cli.taskID < 0)
	    procID = cli.idManager.reserveProcID();
	else
	    procID = cli.taskID;
	
	cli.idManager.manageProc(exeProc, procID);
	
	if (params.length == 1)
	    params = (String []) cli.ptsetParams.get(new Integer(procID));

	Iterator foo = cli.targetset.getTasks();
	while (foo.hasNext()) {
	    Task task = (Task) foo.next();
	    if (task.getTid() == exeProc.getMainTask().getTid()) {
		DebugInfoFrame frame = DebugInfoStackFactory
			.createDebugInfoStackTrace(task);
		cli.setTaskFrame(task, frame);
		cli.setTaskDebugInfo(task, new DebugInfo(frame));
	    }
	}
	if (params == null) 
	    System.out.println("LoadCommand.load: params.length = null");
	else
	    System.out.println("LoadCommand.load: params.length = " + params.length);
	synchronized (cli) {
	    cli.loadedProcs.put(new Integer(procID), 
		    exeProc.getExeFile().getSysRootedPath());
	    if (params != null)
		cli.ptsetParams.put(new Integer(procID), params);
	    else {
		String[] command = { exeProc.getExeFile().getSysRootedPath() };
		cli.ptsetParams.put(new Integer(procID), command);
	    }
	}

	cli.addMessage("[" + procID + ".0] Loaded executable file: " + 
		exeProc.getExeFile().getSysRootedPath(), Message.TYPE_NORMAL);
    }
    
    /**
     * printLoop goes through the specified set of procs/tasks and prints them out
     * 
     * @param cli is the current command line interface object
     * @param displayedName is the String used for the title of the set
     * @param hashProcs is a HashMap containing the procs to list 
     */
    
    static void printLoop(CLI cli, String displayedName, HashMap hashProcs) {
	Set procSet = hashProcs.entrySet();
	cli.outWriter.print(displayedName);
	cli.outWriter.println("\tpath-to-executable");
	ArrayList listing = new ArrayList();
	// Run through procs and put into ArrayList so we can print them out in 
	// numerical order after sorting them
	for (Iterator foo = procSet.iterator(); foo.hasNext();) {
	    Map.Entry me = (Map.Entry) foo.next();
	    Integer taskid = (Integer) me.getKey();
	    String proc = (String) me.getValue();
	    listing.add("[" + taskid.intValue() + ".0]\t\t" + proc);
	}
	String alphaListing[] = (String[]) listing.toArray(new String[listing.size()]);
	java.util.Arrays.sort(alphaListing);
	for (int foo = 0; foo < alphaListing.length; foo++) {
	    cli.outWriter.println(alphaListing[foo]);
	}
	cli.outWriter.flush();
    }
    
    int completer(CLI cli, Input input, int cursor, List completions) {
	return CompletionFactory.completeFileName(cli, input, cursor,
		completions);
    }
}
