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

import java.util.Iterator;
import frysk.rsl.Log;
import frysk.proc.Action;
import frysk.proc.Observable;
import frysk.proc.Task;
import frysk.proc.TaskObserver;
import frysk.stepping.SteppingEngine;

public class Breakpoint implements TaskObserver.Code {
    private static final Log fine = Log.fine(Breakpoint.class);

    protected long address;

    protected int triggered;

    protected boolean added;

    protected boolean removed;

    protected Object monitor = new Object();

    protected SteppingEngine steppingEngine;

    public Breakpoint (SteppingEngine steppingEngine) {
        this.steppingEngine = steppingEngine;
    }

    public Breakpoint (SteppingEngine steppingEngine, long address) {
        //    System.out.println("Setting address to 0x" + Long.toHexString(address));
        this.steppingEngine = steppingEngine;
        this.address = address;
    }

    public Action updateHit (Task task, long address) {

	fine.log(this, "updateHit task", task, "address", address);
        if (address != this.address) {
            fine.log(this, "updateHit task", task, "address", address,
		     "wrong address!");
            return Action.CONTINUE;
        }
        else {
	    fine.log(this, "updateHit adding instruction observer", task,
		     "address", address);
            task.requestAddInstructionObserver(this.steppingEngine.getSteppingObserver());
            this.steppingEngine.addBlocker(task, this);
        }

        ++triggered;
        return Action.BLOCK;
    }

    public int getTriggered () {
        return triggered;
    }

    public void addFailed (Object observable, Throwable w) {
        w.printStackTrace();
    }

    public void addedTo (Object observable) {
        synchronized (monitor) {
            added = true;
            removed = false;
            monitor.notifyAll();
        }
        //    System.err.println("BreakPoint.addedTo");
        ((Task) observable).requestDeleteInstructionObserver(this.steppingEngine.getSteppingObserver());
    }

    public boolean isAdded () {
        return added;
    }

    public void deletedFrom (Object observable) {
        synchronized (monitor) {
            removed = true;
            added = false;
            monitor.notifyAll();
        }
    }

    public boolean isRemoved () {
        return removed;
    }

    public long getAddress() {
        return address;
    }

    static public class PersistentBreakpoint extends Breakpoint {

        /*
         * A breakpoint added by a high-level action e.g., set by the
         * user. It is not meant to be transient and applies only to one task.
         */
        private Observable observable;
        private final Task targetTask;

        public Task getTargetTask() {
            return targetTask;
        }

        public PersistentBreakpoint(Task targetTask, long address,
                                    SteppingEngine steppingEngine) {
            super(steppingEngine, address);
            observable = new Observable(this);
            this.targetTask = targetTask;
        }

        // These operations synchronize on the breakpoint, not the
        // observable object, so that other users of PersistentBreakpoint
        // can synchronize too without having to make the observable public.
        public synchronized void addObserver(BreakpointObserver observer) {
            observable.add(observer);
        }

        public synchronized void deleteObserver(BreakpointObserver observer) {
            observable.delete(observer);
        }

        public Iterator observersIterator() {
            return observable.iterator();
        }

        public synchronized int numberOfObservers() {
            return observable.numberOfObservers();
        }

        public synchronized void removeAllObservers() {
            observable.removeAllObservers();
        }

        public Action updateHit(Task task, long address) {
            if (task != targetTask)
                return Action.CONTINUE;
	    fine.log(this, "updateHit task", task, "address", address);
            Action action = super.updateHit(task, address);

            synchronized (SteppingEngine.class) {
                steppingEngine.getRunningTasks().remove(task);
            }

            synchronized (this) {
                Iterator iterator = observable.iterator();
                while (iterator.hasNext()) {
                    BreakpointObserver observer
                        = (BreakpointObserver)iterator.next();
                    observer.updateHit(this, task, address);
                }
            }
            return action;
        }

        public void addedTo (Object observable) {
            synchronized (monitor) {
                added = true;
                removed = false;
                monitor.notifyAll();
            }
            // Don't remove the current insturction observer.
        }
    }

    //    public PersistentBreakpoint getTaskPersistentBreakpoint(Task task)
    //    {
    //      return (PersistentBreakpoint) SteppingEngine.getTaskBreakpoint(task);
    //    }

    public void addPersistentBreakpoint(Task task, PersistentBreakpoint bp) {
        task.requestAddCodeObserver(bp, bp.getAddress());
    }

    public void deletePersistentBreakpoint(Task task, PersistentBreakpoint bp) {
        task.requestDeleteCodeObserver(bp, bp.getAddress());
    }
}
