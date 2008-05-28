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

package frysk.rt;

import java.io.PrintWriter;

import frysk.expr.Expression;
import frysk.isa.watchpoints.WatchpointFunctionFactory;
import frysk.proc.Action;
import frysk.proc.Task;
import frysk.proc.TaskObserver;
import frysk.stepping.SteppingEngine;
import frysk.value.Format;

/**
 * 
 * Class that installs watchpoint observers for watching
 * read/write of variables or expressions. If the maximum 
 * size that can be watched by a hardware debug register 
 * is smaller than the size of the expression being watched, 
 * then mutiple watch observers are used.
 */
public class WatchObserverInstaller {
    
    private static int watchpointsInUse = 0;
    
    // Maintain oldValue statically so that changes made to a 
    // a value being watched by multiple watchpoints are
    // are reflected in all watch observers.
    private static String oldValue = "";
    
    Expression expr;
    String exprString;
    SteppingEngine ste;
    PrintWriter writer;
    
    /**
     * 
     * @param expr Expression to install watchpoint on
     * @param ste  Stepping engine in use
     * @param writer Writer to print watch messages to
     * @param exprString Text string of expression
     */
    public WatchObserverInstaller(Expression expr, SteppingEngine ste,
	    		          PrintWriter writer, String exprString) {
	this.expr = expr;
	this.ste = ste;
	this.writer = writer;
	this.exprString = exprString;
    }
    
    public void install (Task task, boolean writeOnly) {

	// Get the number of hardware watchpoints - architecture dependent
	int watchpointCount = WatchpointFunctionFactory.getWatchpointFunctions
	                       (task.getISA()).getWatchpointCount();
	// Get the max length a hardware watchpoint can watch - architecture dependent
	int maxWatchLength = WatchpointFunctionFactory.getWatchpointFunctions
	                      (task.getISA()).getWatchpointMaxLength();
	
	long variableAddress = expr.getLocation().getAddress();
	int variableLength = expr.getType().getSize();

	if (variableLength > (watchpointCount-watchpointsInUse) * maxWatchLength ) {
	    throw new RuntimeException ("Watch error: Available watchpoints not " +
	    		                "sufficient to watch complete value.");
	}

	// Calculate number of watch observers needed 
	// to completely watch the variable.
	int numberOfObservers = (int)Math.ceil((double)variableLength/
	                           	       (double)maxWatchLength);
	
	// Add watchpoint observers to task. 
	for (int i=0; i< numberOfObservers-1; i++) {
	    WatchpointObserver wpo = new WatchpointObserver
	                               (expr, exprString, task, ste, writer);    
	    task.requestAddWatchObserver
	         (wpo, variableAddress + i*maxWatchLength, 
	          maxWatchLength, writeOnly);
	}	
	// Last observer may not need to watch all watchLength bytes. 
	WatchpointObserver wpo = new WatchpointObserver
	                           (expr, exprString, task, ste, writer); 
	task.requestAddWatchObserver
	       (wpo, variableAddress + (numberOfObservers-1)*maxWatchLength, 
		variableLength-(numberOfObservers-1)*maxWatchLength, writeOnly);
    }

    /**
     * 
     * Watch observer that gets triggered when the
     * contents of the address being watched is written to
     * or read from.
     */
    static class WatchpointObserver 
    implements TaskObserver.Watch
    {
	Expression expr;
	String exprString;
	SteppingEngine ste;
	PrintWriter writer;
	Task task;
	
	WatchpointObserver(Expression expr, String exprStr, Task task,
		           SteppingEngine ste, PrintWriter writer) {
	    this.expr = expr;
	    this.exprString = exprStr;
	    this.ste = ste;
	    this.writer = writer;
	    this.task = task;
	}
	
	public Action updateHit(Task task, long address, int length) {

	    String newValue = expr.getValue().toPrint
	    (Format.NATURAL, task.getMemory());

	    String watchMessage = "Watchpoint hit: " + exprString + "\n" +
	                          "   Value before hit = " + oldValue + "\n" +
	                          "   Value after  hit = " + newValue + "\n";
	    // Remember the previous value
	    oldValue = newValue;

	    ste.blockedByActionPoint(task, this, watchMessage, writer);
	    task.requestUnblock(this);
	    return Action.BLOCK;
	}

	public void addFailed(Object observable, Throwable w) {
	    writer.println ("Watchpoint Error:" + w.getMessage());
	}

	public void addedTo(Object observable) {
	    writer.println("Watchpoint set: " + exprString); 
	    watchpointsInUse++;
	    // XXX: getValue may modify inferior.
	    oldValue = expr.getValue().toPrint
	               (Format.NATURAL, task.getMemory());	    
	}

	public void deletedFrom(Object observable) {
	    watchpointsInUse--;
	}

    }    
}