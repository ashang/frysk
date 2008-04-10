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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import frysk.proc.Proc;
import frysk.proc.Task;
import frysk.sysroot.SysRootFile;

import java.util.List;

/**
 * KillCommand kills the processes in the current target set.
 */

public class KillCommand extends ParameterizedCommand {
    private static String full = "kill [PID]\n" + 
	"kill the processes that are currently in " +
    	"the current target set or kill a specific PID.  If kill is run " +
    	"without parameters, all processes in the current target set are " +
    	"killed and then reloaded and are then ready to be run again.";
    
    Map saveProcs = new HashMap();
    
    PTSet userSet;

    KillCommand() {
	super("kill the current targetset or kill a specific PID", "kill", full);
    }

    public void interpret(CLI cli, Input cmd, Object options) {
	
	userSet = cli.targetset;
	    
	switch (cmd.size()) {
	
	default:
	    throw new InvalidCommandException("Too many parameters");
	
	case 0:
	    // See if user specified an HPD notation on the command line
	    if (cmd.getFullCommand().startsWith("[")) {
		userSet = cli.getCommandPTSet(cmd);
		killProc(-1, cli, cmd);
	    }
	    // If no HPD notation, kill all currently running procs
	    else killRunningProcs(cli, cmd);
	    break;
	    
	   /* synchronized (cli) {
		// Clear the stepping engine structures
		cli.steppingEngine.clear();
		// Add back in the stepping observer for cli
		cli.steppingEngine.addObserver(cli.steppingObserver);
	    } */
	
	// This is the case where a PID was entered
	case 1:
	    int pid;
	    try {
		pid = Integer.parseInt(cmd.parameter(0));
	    } catch (NumberFormatException e) {
		cli.addMessage("PID entered is not an integer", Message.TYPE_ERROR);
		return;
	    }
	    if (!killProc(pid, cli, cmd))
		cli.addMessage("PID " + pid + " could not be found", Message.TYPE_ERROR);
	}
	
	// Now loop through and re-load all of the killed procs
	Iterator bar = saveProcs.keySet().iterator();
	while (bar.hasNext()) {
	    Integer procId = (Integer) bar.next();
	    SysRootFile sysRootFile = (SysRootFile) saveProcs.get(procId);
	    String cmdline = sysRootFile.getFile().getAbsolutePath();
	    if (sysRootFile.getSysRoot().getAbsolutePath().compareTo("/") != 0) {
		cmdline += " -sysroot " + sysRootFile.getSysRoot().getAbsolutePath();
	    }
	    cli.taskID = procId.intValue();
	    System.out.println("XXX " + cmdline);
	    cli.execCommand("load " + cmdline + "\n");
	}
	saveProcs.clear();
	cli.taskID = -1;
    }
	
    /**
     * killProc will kill all Procs in the current target set or, if a PID
     *     was passed as a parameter, kill just the Proc specified by the PID
     *     passed to it.
     * 
     * @param pid
     *                is an int containing the PID that should be killed, if
     *                pid < 0, kill all the PIDs frysk in targetset, else just
     *                kill the process of the specified PID.
     * @param cli
     *                is the current command line interface object
     * @param cmd
     * 		      is the current Input object
     * @return 	      
     * 		      true if proc(s) were killed, false if none were
     */
	
    boolean killProc(int pid, CLI cli, Input cmd) {
	int procPID = -1;
	Iterator foo = userSet.getTaskData();
	while (foo.hasNext()) {
	    TaskData taskData = (TaskData) foo.next();
	    Task task = taskData.getTask();
	    Proc proc = task.getProc();
	    if ((proc.getPid() != procPID && pid < 0) ||
		    proc.getPid() == pid) {
		cli.addMessage("Killing process " + proc.getPid()
			+ " that was created from " + proc.getExeFile().getSysRootedPath(),
			Message.TYPE_NORMAL);
		// Save the procs we are killing so we can re-load them later
		saveProcs.put(new Integer(taskData.getParentID()), proc.getExeFile());
		procPID = proc.getPid();
		// Now, call the Proc object to kill off the executable(s)
		proc.requestKill();
		if ((pid > 0))
		    return true;
	    }
	}
	// If we got to here and pid > 0 then we did not find that PID
	if (pid > 0)
	    return false;
	return true;
    }
    
    /**
     * killRunningProcs is called when the kill command is issued with no
     *     parameters and no HPD notation in front of it.  This implies that
     *     all of the processes currently running are to be killed and
     *     reloaded.
     *     
     * @param cli
     * 			is the current command line interface object
     * @param cmd
     * 			is the current command object
     * @return
     * 			true if indeed procs were killed, false if not
     */
    
    boolean killRunningProcs(CLI cli, Input cmd) {
	int tempId = -1;
	int procId = 0;
	boolean returnProc = false;
    	Iterator foo = cli.runningProcs.iterator();
    	if (!foo.hasNext()) {
    	    cli.addMessage("No processes to kill", Message.TYPE_ERROR);
    	    return false;
    	}
	while (foo.hasNext()) {
	    Proc proc = (Proc) foo.next();
	    Iterator bar = cli.targetset.getTaskData();
	    while (bar.hasNext()) {
		TaskData taskData = (TaskData) bar.next();
		Proc taskProc = taskData.getTask().getProc();
		procId = taskProc.getPid();
		if (taskProc == proc && tempId != procId) {
		    saveProcs.put(new Integer(taskData.getParentID()), proc.getExeFile());
		    cli.addMessage("Killing process " + proc.getPid()
			+ " that was created from " + proc.getExeFile().getSysRootedPath(),
			Message.TYPE_NORMAL);
		    proc.requestKill();
		    tempId = procId;
		    returnProc = true;
		}
	    }
	}
	if (returnProc) {
	    synchronized (cli) {
		// Clear the running procs set
		cli.runningProcs.clear();
	    }
	}
	return returnProc;
    }

    int completer(CLI cli, Input input, int cursor, List completions) {
	return CompletionFactory.completeFileName(cli, input, cursor,
						  completions);
    }
}
