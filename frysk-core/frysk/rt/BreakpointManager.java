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

package frysk.rt;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Observable;
import java.util.TreeMap;
import java.io.File;
import frysk.proc.Action;
import frysk.proc.Proc;
import frysk.proc.ProcTasksAction;
import frysk.proc.ProcTasksObserver;
import frysk.proc.Task;
import frysk.proc.TaskObserver;
import frysk.rsl.Log;
import frysk.stepping.SteppingEngine;
import frysk.symtab.DwflSymbol;
import frysk.symtab.PLTEntry;
import frysk.symtab.SymbolFactory;
import frysk.util.CountDownLatch;

import lib.dwfl.DwarfDie;

/**
 * Class for managing user breakpoints. In particular it defers
 * inserting breakpoints whose addresses cannot be found in the
 * executable, trying again when shared libraries are loaded.
 */
public class BreakpointManager extends Observable {
    private static final Log fine = Log.fine(BreakpointManager.class);

    private TreeMap breakpointMap = new TreeMap();    
    private SteppingEngine steppingEngine;

    /**
     * Initialize the BreakpointManager.
     * @param steppingEngine the stepping engine the manager will use
     * to insert / delete breakpoints.
     */
    public BreakpointManager(SteppingEngine steppingEngine) {
        this.steppingEngine = steppingEngine;
    }

    // Watch a process and its tasks for events that might cause
    // breakpoints to be added or deleted in it.
    private class ProcWatcher implements ProcTasksObserver {
        Proc proc;
        ProcTasksAction ptObs;

        ProcWatcher(Proc proc) {
            this.proc = proc;
            ptObs = new ProcTasksAction(proc, this);
        }
    
        HashSet procTasks = new HashSet();

        public void existingTask(Task task) {
            procTasks.add(task);
        }

        public void taskAdded(Task task) {
            procTasks.add(task);

            Iterator bptIterator = breakpointMap.values().iterator();

            while (bptIterator.hasNext()) {
                SourceBreakpoint bpt = (SourceBreakpoint)bptIterator.next();

                if (bpt.appliesTo(proc, task)) {
                    enableBreakpoint(bpt, task);
                }
            }
        }
        public void taskRemoved(Task task) {
            procTasks.remove(task);
        }
	// Standard observer stuff
        public void addedTo(Object observable) {
        }

        public void addFailed(Object observable, Throwable w) {
        }

        public void deletedFrom(Object observable) {
        }
    
    }

    private HashMap watchers = new HashMap();

    private synchronized void addBreakpoint(SourceBreakpoint bp) {
        breakpointMap.put(new Integer(bp.getId()), bp);
        setChanged();
        notifyObservers();
    }

    /**
     * Create a line breakpoint that is not associated with any
     * process.
     * @param fileName the file
     * @param lineNumber line in the file
     * @param column column number in the file
     * @return LineBreakpoint object
     */
    public LineBreakpoint addLineBreakpoint(String fileName,
					    int lineNumber,
					    int column) {
	LineBreakpoint sourceBreakpoint =
	    new LineBreakpoint(CountManager.getNextId(),
			       fileName, lineNumber, column);
	addBreakpoint(sourceBreakpoint);
	return sourceBreakpoint;
    }

    /**
     * Create a line breakpoint that is not associated with any
     * process.
     * @param fileName the file
     * @param lineNumber line in the file
     * @param column column number in the file
     * @return LineBreakpoint object
     */
    public LineBreakpoint addLineBreakpoint(File fileName,
					    int line, int column) {
	return addLineBreakpoint(fileName.getPath(), line, column);
    }

    /**
     * Create a symbol breakpoint not associated with any process
     * @param symbol the symbol to breakpoint at
     * @return FunctionBreakpoint object
     */
    public SymbolBreakpoint addSymbolBreakpoint(DwflSymbol symbol) {
	SymbolBreakpoint sourceBreakpoint =
	    new SymbolBreakpoint(CountManager.getNextId(), symbol);
	addBreakpoint(sourceBreakpoint);
	return sourceBreakpoint;
    }

    /**
     * Create a PLT breakpoint not associated with any process
     * @param entry the PLT entry to breakpoint at
     * @return PLTBreakpoint object
     */
    public PLTBreakpoint addPLTBreakpoint(PLTEntry entry) {
	PLTBreakpoint sourceBreakpoint =
	    new PLTBreakpoint(CountManager.getNextId(), entry);
	addBreakpoint(sourceBreakpoint);
	return sourceBreakpoint;
    }

    /**
     * Create a function breakpoint not associated with any process
     * @param name the name of the function
     * @param die a DwarfDie representing the function or inlined
     * instance. If null, a lookup against the Elf symbol name is
     * performed.
     * @return FunctionBreakpoint object
     */
    public FunctionBreakpoint addFunctionBreakpoint(String name, DwarfDie die) {
	FunctionBreakpoint sourceBreakpoint =
	    new FunctionBreakpoint(CountManager.getNextId(), name, die);
	addBreakpoint(sourceBreakpoint);
	return sourceBreakpoint;
    }

    /**
     * Try to enable a breakpoint in a task. If there is an error, the
     * state of the breakpoint will be set to DEFERRED.
     * @param breakpoint the breakpoint
     * @param task task in which breakpoint will be enabled.
     * @return the state of the breakpoint of attempting to enable it.
     */
    public SourceBreakpoint.State enableBreakpoint(SourceBreakpoint breakpoint,
                                                   Task task) {
        Proc proc = task.getProc();
        ProcWatcher watcher = (ProcWatcher)watchers.get(proc);
        if (watcher == null) {
            watcher = new ProcWatcher(proc);
            watchers.put(proc, watcher);
        }
        try {
            breakpoint.enableBreakpoint(task, this.steppingEngine);
        }
        catch (Exception e) {
            breakpoint.setState(task, SourceBreakpoint.DEFERRED);
            return SourceBreakpoint.DEFERRED;
        }
        breakpoint.setState(task, SourceBreakpoint.ENABLED);
        setChanged();
        notifyObservers(breakpoint);
        return SourceBreakpoint.ENABLED;
    }

    /**
     * Disable a breakpoint in a task.
     * @param breakpoint the breakpoint
     * @param task the task
     */
    public void disableBreakpoint(SourceBreakpoint breakpoint, Task task) {
        breakpoint.disableBreakpoint(task, this.steppingEngine);
        setChanged();
        notifyObservers(breakpoint);
    }

    public Iterator getBreakpointTableIterator() {
        return breakpointMap.values().iterator();
    }

    public SourceBreakpoint getBreakpoint(int bptId) {
        SourceBreakpoint bpt
            = (SourceBreakpoint)breakpointMap.get(new Integer(bptId));
        return bpt;
    }

    public void refreshBreakpoints(Task task) {
        Iterator iter = breakpointMap.values().iterator();
        while (iter.hasNext()) {
            SourceBreakpoint bpt = (SourceBreakpoint)iter.next();
            SourceBreakpoint.State state = bpt.getState(task);
            if (state == SourceBreakpoint.DEFERRED || state == null) {
                enableBreakpoint(bpt, task);
            }
        }
    }
    
    private HashSet managedProcs = new HashSet();
  
    public void manageProcess(final Proc proc) {
        Task task = proc.getMainTask();
        if (managedProcs.contains(proc))
            return;
        managedProcs.add(proc);
	// Assume that the Proc's main task is stopped.
        LinkedList sharedLibBptAddrs
            = SymbolFactory.getAddresses(task, "_dl_debug_state");
        if (sharedLibBptAddrs.size() == 0)
            return;
        long sharedLibBptAddr
            = ((Long)sharedLibBptAddrs.getFirst()).longValue();
        final CountDownLatch codeObserverLatch = new CountDownLatch(1);
        task.requestAddCodeObserver(new TaskObserver.Code() {
                public Action updateHit(Task task, long address) {
                    refreshBreakpoints(task);
                    return Action.CONTINUE;
                }

                public void addedTo(Object observable) {
                    codeObserverLatch.countDown();
                }

                public void addFailed(Object observable, Throwable w) {
                    fine.log("_dl_debug_state breakpoint couldn't be added:", w);
                    codeObserverLatch.countDown();
                }

                public void deletedFrom(Object observable) {
                }
            },
            sharedLibBptAddr);
        while (true) {
            try {
                codeObserverLatch.await();
                break;
            }
            catch (InterruptedException e) {
            }
        }
    }
}
