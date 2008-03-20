// This file is part of the program FRYSK.
//
// Copyright 2005, 2006, 2007, 2008, Red Hat Inc.
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

import java.io.PrintWriter;
import java.io.StringWriter;
import gnu.classpath.tools.getopt.OptionGroup;
import java.util.HashMap;
import frysk.debuginfo.PrintStackOptions;
import frysk.isa.signals.Signal;
import frysk.proc.Action;
import frysk.proc.Task;
import frysk.proc.TaskObserver;
import frysk.rsl.Log;

public class FCatch {
    private static final Log fine = Log.fine(FCatch.class);

    private final PrintStackOptions stackPrintOptions = new PrintStackOptions();

    private PrintWriter printWriter = new PrintWriter(System.out);
    HashMap signaledTasks = new HashMap();

    private static class SignalStack {
	Signal signal;
	String stack;
    }

    /**
     * Builds a stack trace from the incoming blocked task, and appends the
     * output to this class' StringBuffer. Decrements the numTasks variable to
     * let FCatch know when to unblock the signaled thread.
     * 
     * @param task
     *                The Task to be StackTraced
     */
    private void printStackTrace(Task task, SignalStack signalStack) {
	printWriter.println("\n" + task.getProc().getPid() + "."
		+ task.getTid() + " terminated with signal "
		+ signalStack.signal);
	printWriter.println(signalStack.stack);
	printWriter.flush();
    }

    /**
     * An observer that sets up things once frysk has set up the requested proc
     * and attached to it.
     */
    class CatchObserver implements TaskObserver.Terminated, TaskObserver.Signaled{

	public Action updateTerminated(Task task, Signal signal, int value) {
	    SignalStack signalStack = (SignalStack) signaledTasks.remove(task);
	    if (signalStack != null && signal != null && signal.equals(signalStack.signal)) {
		printStackTrace(task, signalStack);
	    }else{
		// if the main thread exited print out a message that the process
		// terminated safely 
		if(task.getTid() == task.getProc().getPid()){
		    printWriter.println(task.getProc().getPid() + "."
				+ task.getTid() + " terminated normally");
		    printWriter.flush();
		}
	    }
	    return Action.CONTINUE;
	}

	public Action updateSignaled(Task task, Signal signal) {
	    fine.log(this, "updateSignaled", task, "signal", signal);
	    
	    StringWriter stringWriter = new StringWriter();
	    StackPrintUtil.print(task, stackPrintOptions,
				 new PrintWriter(stringWriter));
	    
	    SignalStack signalStack = new SignalStack();
	    signalStack.signal = signal;
	    signalStack.stack = stringWriter.getBuffer().toString();
	    signaledTasks.remove(task);
	    signaledTasks.put(task, signalStack);
	    
	    return Action.CONTINUE;
	}

	public void addedTo(Object observable) {
	    fine.log(this, "CatchObserver.addedTo", observable);
	}

	public void addFailed(Object observable, Throwable w) {
	    throw new RuntimeException("Failed to attach to created proc", w);
	}

	public void deletedFrom(Object observable) {
	    fine.log(this, "deletedFrom", observable);
	}
    }

    private OptionGroup[] options() {
	return new OptionGroup[] {
	    StackPrintUtil.options(stackPrintOptions)
	};
    }

    public void run(String[] args) {
	CatchObserver catchObserver = new CatchObserver();
	ProcRunUtil procRunUtil
	    = new ProcRunUtil("fcatch",
			      "Usage: fcatch [OPTIONS] -- PATH ARGS || fcatch [OPTIONS] PID",
			      args, new TaskObserver[] { catchObserver},
			      options(), ProcRunUtil.DEFAULT);
	procRunUtil.start();
    }
}
