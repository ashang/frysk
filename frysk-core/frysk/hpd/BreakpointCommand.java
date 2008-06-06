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

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import frysk.debuginfo.ObjectDeclarationSearchEngine;
import frysk.proc.Task;
import frysk.rt.BreakpointManager;
import frysk.rt.FunctionBreakpoint;
import frysk.rt.LineBreakpoint;
import frysk.rt.SourceBreakpoint;
import frysk.rt.SourceBreakpointObserver;
import frysk.stepping.SteppingEngine;
import frysk.value.ObjectDeclaration;

class BreakpointCommand extends ParameterizedCommand {

    BreakpointCommand() {
	super("Define a breakpoint",
	      "break {proc | line | #file#line}",
	      ("The break command defines a breakpoint that will be"
	       + " triggered when some thread(s) in the trigger set"
	       + " arrives at the specified location during program"
	       + " execution.  When that occurs, the process(es) containing"
	       + " the triggering thread(s) plus all processes in the"
	       + " stop set will be forcibly stopped so the user can"
	       + " examine program state information."));
    }

    static private abstract class CLIBreakpointObserver implements
	    SourceBreakpointObserver {

	public void addedTo(Object observable) {
	}

	public void addFailed(Object observable, Throwable w) {
	}

	public void deletedFrom(Object observable) {
	}
 
	public abstract void updateHit(SourceBreakpoint bpt, Task task,
                                       long address);
    }

    void interpret(CLI cli, Input cmd, Object arguments) {
        if (cmd.size() < 1) {
            throw new InvalidCommandException
                ("missing argument");
        }
	PTSet ptset = cli.getCommandPTSet(cmd);
	String breakpt = cmd.parameter(0);
	String fileName;
	int lineNumber;
	SourceBreakpoint actionpoint;
	BreakpointManager bpManager = cli.getSteppingEngine()
		.getBreakpointManager();
	final PrintWriter outWriter = cli.getPrintWriter();
	final SteppingEngine ste = cli.getSteppingEngine();
	
	Iterator taskIter = ptset.getTasks();
	// Map between tasks and breakpoints to enable.
	HashMap bptMap = new HashMap();
	if (breakpt.charAt(0) == '#') {
	    String[] bptParams = breakpt.split("#");
	    if (bptParams.length != 3) {
		// XXX should use notion of "current" source file
		throw new InvalidCommandException
		    ("bad syntax for breakpoint:" + breakpt);
	    }
	    fileName = bptParams[1];
	    lineNumber = Integer.parseInt((String) bptParams[2]);
	    actionpoint = bpManager.addLineBreakpoint(fileName, lineNumber, 0);
	    actionpoint.addObserver(new CLIBreakpointObserver() {
		public void updateHit(final SourceBreakpoint bpt, Task task,
			final long address) {
                    // Output the message in an Event in order to
                    // allow all actions, fired by events currently in
                    // the loop, to run.		    
                    LineBreakpoint lbpt = (LineBreakpoint) bpt;
                    String message = "Breakpoint " + lbpt.getId() +
                                     " #" + lbpt.getFileName() + "#" + 
                                     lbpt.getLineNumber() + " 0x" +
                                     Long.toHexString(address) + "\n";
                    ste.updateActionpointDoneEvent(message, outWriter);

                    
		}
	    });
	    while (taskIter.hasNext()) {
		bptMap.put(taskIter.next(), actionpoint);
	    }
	} else {
	    while (taskIter.hasNext()) {
		Task task = (Task) taskIter.next();
		ObjectDeclarationSearchEngine declarationSearchEngine = 
		    new ObjectDeclarationSearchEngine(task);
		
		if (declarationSearchEngine != null) {
		    
		    LinkedList objects = declarationSearchEngine.getObject(breakpt);
		    
		    if(objects.size() > 0){
		    Iterator iterator = objects.iterator();
		    
		    while (iterator.hasNext()) {			
			ObjectDeclaration function = (ObjectDeclaration) iterator.next();
			actionpoint = bpManager.addFunctionBreakpoint(breakpt, function);
			actionpoint.addObserver(new CLIBreakpointObserver() {
			    public void updateHit(final SourceBreakpoint bpt,
				    Task task, final long address) {
				// Output the message in an Event in order to
				// allow all actions, fired by events currently in
				// the loop, to run.				
				FunctionBreakpoint fbpt = (FunctionBreakpoint) bpt;				
				String message = "Breakpoint " + fbpt.getId() +
				                 " " + fbpt.getName() + " 0x" + 
				                 Long.toHexString(address) + "\n";
				ste.updateActionpointDoneEvent(message, outWriter);
			    }
			});
			bptMap.put(task, actionpoint);
		    }
		    }else{
		    actionpoint = bpManager.addFunctionBreakpoint(breakpt, null);
			actionpoint.addObserver(new CLIBreakpointObserver() {
			    public void updateHit(final SourceBreakpoint bpt,
				    Task task, final long address) {
				// Output the message in an Event in order to
				// allow all actions, fired by events currently in
				// the loop, to run.					
				FunctionBreakpoint fbpt = (FunctionBreakpoint) bpt;
				String message = "Breakpoint " + fbpt.getId() +
				                 " " + fbpt.getName() + " 0x" + 
				                 Long.toHexString(address) + "\n";
				ste.updateActionpointDoneEvent(message, outWriter);                            
			    }
			});
			bptMap.put(task, actionpoint);
		}
		}
	    }
	}
	if (bptMap.isEmpty()) {
	    outWriter.print("No matching breakpoint found.\n");
	    return;
	}
	Iterator bptIterator = bptMap.entrySet().iterator();
	while (bptIterator.hasNext()) {
	    Map.Entry entry = (Map.Entry) bptIterator.next();
	    Task task = (Task) entry.getKey();
	    actionpoint = (SourceBreakpoint) entry.getValue();
	    SourceBreakpoint.State result = bpManager.enableBreakpoint(
		    actionpoint, task);
	    outWriter.print("breakpoint " + actionpoint.getId());
	    if (result != SourceBreakpoint.ENABLED) {
		outWriter.print(" " + result.toString());
	    }
	}
	outWriter.println();
    }

    int completer(CLI cli, Input input, int cursor, List completions) {
	return CompletionFactory.completeExpression(cli, input, cursor,
						    completions);
    }
}
