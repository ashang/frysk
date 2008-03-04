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

package frysk.util;

import frysk.event.Event;
import frysk.event.ProcEvent;
import frysk.proc.Manager;
import frysk.proc.Proc;
import frysk.proc.ProcBlockAction;
import frysk.proc.ProcObserver;
import frysk.proc.Task;
import frysk.util.CommandlineParser;
import gnu.classpath.tools.getopt.Option;
import frysk.rsl.Log;

/**
 * Framework to be used for frysk utilities that,
 * a) Accept pids, executable paths, core files &
 * b) Require tasks to be stopped,
 * 
 * Utilities must define a event.ProcEvent to execute.
 */
public class ProcStopUtil {
    private static final Log fine = Log.fine(ProcStopUtil.class);

    private String[] args;
    private CommandlineParser parser;
	
    public ProcStopUtil (String utilName, String[] args, 
			 final ProcEvent procEvent) {
	this.args = args;
	parser = new CommandlineParser(utilName) {
		//@Override
		public void parsePids(Proc[] procs) { 
		    for (int i= 0; i < procs.length; i++)  {                  
			Proc proc = procs[i];
			UtilEvent utilEvent = new UtilEvent(proc, procEvent);
			new ProcBlockAction(proc, new UtilAction(proc, utilEvent));
			Manager.eventLoop.run();
		    }
		}
	    
		//@Override 
		public void parseCores(Proc[] cores) {
		    for (int i = 0; i < cores.length; i++) {       
			Proc core = cores[i];
			fine.log("execute dead core", core);
			procEvent.executeDead(core);
		    }
		}  
	    
		//@Override
		public void parseCommand(Proc command) {
		    fine.log("execute dead command", command);
		    procEvent.executeDead(command);
		}
	    };
    }    
    
    /**
     * Use to set the usage of utility.
     * 
     * @param usage - String that describes the usage of
     *                tool.
     */
    public void setUsage (String usage) {
	parser.setHeader(usage);
    }
    
    /**
     * Use to add options to the utility.
     * 
     * @param option - Option object that defines the
     *                 option.
     */
    public void addOption (Option option) {
	parser.add (option);
    }
    
    public void execute () {
	parser.parse(args);

	// If we got here and args is null, we didn't find a pid.
	if (args.length < 1) {
	    System.err.println("ERROR: No argument provided.");
	    parser.printHelp();
	}
    }
    
    private static class UtilAction 
    implements ProcObserver.ProcAction 
    {
	protected Proc proc;
	private Event event;

	public UtilAction(Proc proc, Event event) {
	    this.proc = proc;
	    this.event = event;
	}

	public void allExistingTasksCompleted() {
	    Manager.eventLoop.add(event);
	}
	public void taskAddFailed(Object task, Throwable w) {
	}
	public void existingTask(Task task) {
	}
	public void addFailed(Object observable, Throwable w) {
	}
	public void addedTo(Object observable) {
	}
	public void deletedFrom(Object observable) {
	}
    }
    
    private static class UtilEvent
    implements Event
    {
	private Proc proc = null;
	private ProcEvent event = null;
	
	public UtilEvent(Proc proc, ProcEvent event) {
	    this.proc = proc;
	    this.event = event;
	}
	
	public void execute() {
	    fine.log("execute live", proc);
	    event.executeLive(proc);
	    
	    // FIXME: Should request eventloop to stop
	    // instead of exit.
	    System.exit(0);
	}
    }
}