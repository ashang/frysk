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

import java.util.Iterator;
import java.util.List;

import frysk.expr.Expression;
import frysk.isa.watchpoints.WatchpointFunctionFactory;
import frysk.proc.Action;
import frysk.proc.Task;
import frysk.proc.TaskObserver;
import frysk.value.Format;

class WatchCommand extends ParameterizedCommand {

    private boolean writeOnly = true;
    
    WatchCommand() {
	// XXX: Add details on thread-handling when implemented
	super("Define a watchpoint",
	      "watch <expression> [-a | -w]",
	      ("The watch command defines a watchpoint that stops the"
		+ " execution of a program when the memory location" 
	        + " associated with an expression is read or written to"));
		
	
	add(new CommandOption("w", "Trigger only on a write. (default)") {
		void parse(String arg, Object options) {
		    writeOnly = true;
		}
	    });
	add(new CommandOption("a", "Trigger on read and write.") {
		void parse(String arg, Object options) {
		    writeOnly = false;
		}
	    });
    }
    
    void interpret(CLI cli, Input cmd, Object arguments) {
        if (cmd.size() < 1) {
            throw new InvalidCommandException
                ("missing argument");
        }
        
        String expressionStr = cmd.stringValue();

	PTSet ptset = cli.getCommandPTSet(cmd);
	Iterator taskIter = ptset.getTasks();

	while (taskIter.hasNext()) {	   
	    Task task = (Task) taskIter.next();
	    
	    Expression expr = null;
	    try {
		// Construct an Expression object from the expression string.
		expr = cli.parseExpression(task, expressionStr);
	    } catch (RuntimeException e) {
		cli.outWriter.println();
		cli.printError(e);
		continue;
	    }	    

	    // XXX: getValue may modify inferior.
	    String oldValueStr = expr.getValue().toPrint
	                         (Format.NATURAL, task.getMemory());

	    // Get the number of hardware watchpoints - architecture dependent
	    int watchpointCount = WatchpointFunctionFactory.getWatchpointFunctions
	                          (task.getISA()).getWatchpointCount();
	    // Get the max length a hardware watchpoint can watch - architecture dependent
	    int watchLength = WatchpointFunctionFactory.getWatchpointFunctions
                              (task.getISA()).getWatchpointMaxLength();
	 
	    // XXX: May fail for non-contiguos memory and registers 
	    long variableAddress = expr.getLocation().getAddress();
	    int variableLength = expr.getType().getSize();
	    
	    if (variableLength > watchpointCount * watchLength )
		throw new RuntimeException ("Watchpoint set error: Variable size too large.");

	    // Calculate number of watch observers needed to completely
	    // watch the variable.
	    int numberOfObservers = (int)Math.ceil((double)variableLength/
		                                   (double)watchLength);

	    // Add watchpoint observers to task. 
	    for (int i=0; i< numberOfObservers-1; i++) {
		WatchpointObserver wpo = new WatchpointObserver
		                         (expr, cli, expressionStr, oldValueStr);    
		task.requestAddWatchObserver
		     (wpo, variableAddress + i*watchLength, watchLength, writeOnly);
	    }	
	    // Last observer may not need to watch all watchLength bytes. 
	    WatchpointObserver wpo = new WatchpointObserver
	                            (expr, cli, expressionStr, oldValueStr);
	    task.requestAddWatchObserver
	         (wpo, variableAddress + (numberOfObservers-1)*watchLength, 
	          variableLength-(numberOfObservers-1)*watchLength, writeOnly);
	}       
    }
    
    static class WatchpointObserver 
    implements TaskObserver.Watch
    {
        Expression expr;
	CLI cli;
	String exprStr;
	String oldValueStr;

	WatchpointObserver(Expression expr, CLI cli, String exprStr, String oldValueStr) {
	    this.expr = expr;
	    this.cli = cli;
	    this.exprStr = exprStr;
	    this.oldValueStr = oldValueStr;

	}
	public Action updateHit(Task task, long address, int length) {
	    
	    String newValueStr = expr.getValue().toPrint
	                         (Format.NATURAL, task.getMemory());
	    
	    String watchMessage = "Watchpoint hit: " + exprStr + "\n" +
	                          "   Value before hit = " + oldValueStr + "\n" +
	                          "   Value after  hit = " + newValueStr + "\n";
	    // Remember the previous value
	    oldValueStr = newValueStr;

	    cli.getSteppingEngine().blockedByActionPoint(task, this, watchMessage, cli.outWriter);
	    task.requestUnblock(this);
	    return Action.BLOCK;
	}

	public void addFailed(Object observable, Throwable w) {
	    cli.outWriter.println ("Watchpoint Error:" + w.getMessage());
	}

	public void addedTo(Object observable) {
	    cli.outWriter.println("Watchpoint set: " + exprStr); 
	}

	public void deletedFrom(Object observable) {
	}

    }
    
    int completer(CLI cli, Input input, int cursor, List completions) {
	return CompletionFactory.completeExpression(cli, input, cursor,
						    completions);
    }
}