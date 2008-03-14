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

package frysk.proc.live;

import java.io.File;
import java.util.HashSet;
import frysk.event.EventLoop;
import java.util.List;
import java.util.LinkedList;
import java.util.HashMap;
import frysk.proc.Proc;
import frysk.sys.proc.Stat;
import frysk.sys.proc.ProcBuilder;
import java.util.Iterator;
import frysk.proc.Task;
import frysk.proc.TaskObserver.Attached;
import frysk.sys.ProcessIdentifier;
import frysk.sys.ProcessIdentifierFactory;
import frysk.proc.Manager;
import frysk.rsl.Log;
import frysk.sys.Fork;
import frysk.sys.Tid;
import frysk.sys.Pid;
import frysk.event.Event;
import frysk.proc.FindProc;
import frysk.proc.HostRefreshBuilder;
import java.util.Collection;

/**
 * A Linux Host tracked using PTRACE.
 */

public class LinuxPtraceHost extends LiveHost {
    private static final Log fine = Log.fine(LinuxPtraceHost.class);

    /**
     * Construct an instance of the LinuxPtraceHost that uses the
     * specified eventLoop.
     */
    public LinuxPtraceHost(EventLoop eventLoop) {
	eventLoop.add(new LinuxWaitBuilder(this));
    }


    /**
     * Maintain a cache of tasks indexed by ProcessIdentifier.
     */
    private final HashMap tasks = new HashMap();
    LinuxPtraceTask getTask(ProcessIdentifier pid) {
	return (LinuxPtraceTask) tasks.get(pid);
    }
    void putTask(ProcessIdentifier pid, LinuxPtraceTask task) {
	tasks.put(pid, task);
    }

    /**
     * Maintain a cache of procs indexed by ProcessIdentifier.
     */
    private final HashMap procs = new HashMap();
    LinuxPtraceProc getProc(ProcessIdentifier pid) {
	return (LinuxPtraceProc) procs.get(pid);
    }
    void addProc(ProcessIdentifier pid, LinuxPtraceProc proc) {
	procs.put(pid, proc);
    }
    void removeProc(ProcessIdentifier pid) {
	procs.remove(pid);
    }

    /**
     * Either add or update a process, however, before doing that
     * determine the parent and ensure that it has been updated.
     */
    private class ProcChanges {
	/**
	 * ADDED accumulates all the tasks added as things are
	 * updated.
	 */
	List added = new LinkedList();

	/**
	 * REMOVED starts with the full list of processes and then
	 * works backwards removing any that are processed, by the end
	 * it contains processes that no longer exist.
	 */
	HashMap removed = (HashMap) procs.clone();

	/**
	 * Update PROCID, either adding it
	 */
	Proc update(ProcessIdentifier pid) {
	    Proc proc = getProc(pid);
	    if (proc == null) {
		// New, unknown process. Try to find both the process
		// and its parent. In the case of a daemon process, a
		// second attempt may be needed.
		Stat stat = new Stat();
		Proc parent = null;
		int attempt = 0;
		while (true) {
		    // Should take no more than two attempts - one for
		    // a normal process, and one for a daemon.
		    if (attempt++ >= 2)
			return null;
		    // Scan in the process's stat file. Of course, if
		    // the stat file disappeared indicating that the
		    // process exited, return NULL.
		    if (stat.scan(pid) == null)
			return null;
		    // Find the parent, every process, except process
		    // 1, has a parent.
		    if (pid.intValue() <= 1)
			break;
		    parent = update(stat.ppid);
		    if (parent != null)
			break;
		}
		// .. and then add this process.
		proc = new LinuxPtraceProc(LinuxPtraceHost.this, parent,
					   pid, stat);
		added.add(proc);
	    } else if (removed.containsKey(pid)) {
		// Process 1 never gets a [new] parent.
		if (pid.intValue() > 1) {
		    Stat stat = ((LinuxPtraceProc) proc).getStat();
		    // An existing process that hasn't yet been
		    // updated. Still need check that its parent
		    // didn't change (assuming there is one).
		    if (stat.scan(pid) == null)
			// Oops, just disappeared.
			return null;
		    Proc oldParent = proc.getParent();
		    if (oldParent.getPid() != stat.ppid.intValue()) {
			// Transfer ownership
			Proc newParent = update(stat.ppid);
			oldParent.remove(proc);
			proc.parent = newParent;
			newParent.add(proc);
		    }
		}
		removed.remove(pid);
	    }
	    return proc;
	}
    }

    private ProcChanges executeRefresh() {
	// Iterate (build) the /proc tree, passing each found PID to
	// procChanges where it can update the /proc tree.
	final ProcChanges procChanges = new ProcChanges();
	ProcBuilder pidBuilder = new ProcBuilder() {
		public void build(ProcessIdentifier pid) {
		    procChanges.update(pid);
		}
	    };
	pidBuilder.construct();
	// Tell each process that no longer exists that it has been
	// destroyed.
	for (Iterator i = procChanges.removed.values().iterator();
	     i.hasNext();) {
	    LinuxPtraceProc proc = (LinuxPtraceProc) i.next();
	    // XXX: Should there be a ProcEvent.schedule(), instead of
	    // Manager .eventLoop .appendEvent for injecting the event
	    // into the event loop?
	    proc.performRemoval();
	}
	return procChanges;
    }
  
    public void requestRefresh(final Collection knownProcesses,
			       final HostRefreshBuilder updates) {
	fine.log(this, "requestRefresh");
	Manager.eventLoop.add(new Event() {
		public void execute() {
		    LinuxPtraceHost.this.executeRefresh(knownProcesses,
							updates);
		}
	    });
    }
    private void executeRefresh(Collection knownProcesses,
				HostRefreshBuilder builder) {
	ProcChanges procChanges = executeRefresh();
	Collection exitedProcesses = procChanges.removed.values();
	exitedProcesses.retainAll(knownProcesses);
	Collection newProcesses = new HashSet(procs.values());
	newProcesses.removeAll(knownProcesses);
	builder.construct(newProcesses, exitedProcesses);
    }

    public void requestProc(final int theProcId, final FindProc theFinder) {
	Manager.eventLoop.add(new Event() {
		private final ProcessIdentifier pid
		    = ProcessIdentifierFactory.create(theProcId);
		private final FindProc finder = theFinder;
		public void execute() {
		    // Iterate (build) the /proc tree starting with
		    // the given procId.
		    new ProcBuilder() {
			public void build(ProcessIdentifier pid) {
			    new ProcChanges().update(pid);
			}
		    }.construct(pid);
		    Proc proc = getProc(pid);
		    if (proc == null) {
			finder.procNotFound(pid.intValue());
		    } else {
			proc.sendRefresh();
			finder.procFound(proc);
		    }
		}
	    });
    }

    public void requestCreateAttachedProc(final File exe,
					  final String stdin,
					  final String stdout,
					  final String stderr,
					  final String[] args,
					  final Attached attachedObserver) {
	fine.log(this, "requestCreateAttachedProc");
	Manager.eventLoop.add(new Event() {
		public void execute() {
		    fine.log(LinuxPtraceHost.this, "executeCreateAttachedProc");
		    ProcessIdentifier pid
			= Fork.ptrace(exe, stdin, stdout, stderr, args);
		    // See if the Host knows about this task.
		    ProcessIdentifier myTid = Tid.get();
		    Task myTask = getTask(myTid);
		    if (myTask == null) {
			// If not, find this process and add this task to it.
			Proc myProc = getSelf();
			myTask = new LinuxPtraceTask(myProc, pid);
		    }
		    LinuxPtraceProc proc = new LinuxPtraceProc(myTask, pid);
		    new LinuxPtraceTask(proc, attachedObserver);
		}
	    });
    }
	    
    /**
     * Return a pointer to this <em>frysk</em> instance.
     */
    public Proc getSelf() {
	if (self == null) {
	    ProcChanges procChanges = new ProcChanges();
	    self = procChanges.update(Pid.get());
	}
	return self;
    }
    private Proc self;
}
