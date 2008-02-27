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

package frysk.proc;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import frysk.sys.ProcessIdentifier;
import frysk.sys.ProcessIdentifierFactory;
import frysk.util.CountDownLatch;
import frysk.event.Event;
import frysk.sys.Signal;

/**
 * A UNIX Process, containing tasks, memory, ...
 */

public abstract class Proc implements Comparable {
    protected static final Logger logger = Logger.getLogger(ProcLogger.LOGGER_ID);

    private CountDownLatch quitLatch;
  
    /**
     * If known, due to the tracing of a fork, the Task that created
     * this process.
     */
    final Task creator;

    /**
     * XXX: This should not be public.
     */
    public Proc parent;

    public Proc getParent() {
	// XXX: This needs to be made on-demand.
	return this.parent;
    }

    /**
     * Return the Proc's Host.
     */
    public Host getHost() {
	return host;
    }
    private final Host host;

    public int getPid() {
	return pid;
    }
    private final int pid;

    /**
     * Return the basename of the program that this process is
     * running.
     */
    public abstract String getCommand();

    /**
     * Return the full path of the program that this process is
     * running.
     */
    public abstract String getExe();

    /**
     * Return the UID of the Proc.
     */
    public abstract int getUID();

    /**
     * Return the GID of the Proc.
     */
    public abstract int getGID();

    /**
     * @return The main task for this process
     */
    public Task getMainTask() {
	return this.host.get(new TaskId(this.getPid()));
    }
  
    /**
     * Return the Proc's command line argument list
     */
    public abstract String[] getCmdLine();

    public abstract MemoryMap[] getMaps();
  
    public MemoryMap getMap(long address) {
	MemoryMap maps[] = getMaps();
	for (int i = 0; i < maps.length; i++)
	    if (maps[i].addressLow <= address && maps[i].addressHigh > address)
		return maps[i];
      
	return null;
    }

    /**
     * Create a new Proc skeleton. Since PARENT could be NULL,
     * explicitly specify the HOST.
     */
    private Proc(int pid, Proc parent, Host host, Task creator) {
	this.host = host;
	this.pid = pid;
	this.parent = parent;
	this.creator = creator;
	// Keep parent informed.
	if (parent != null)
	    parent.add(this);
    }

    /**
     * Create a new, unattached, running, Proc. Since PARENT could be
     * NULL, explicitly specify the HOST.
     */
    protected Proc(Host host, Proc parent, int pid) {
	this(pid, parent, host, null);
	logger.log(Level.FINEST, "{0} new - create unattached running proc\n", this);
    }

    /**
     * Create a new, attached, running, process forked by Task. For
     * the moment assume that the process will be immediately
     * detached; if this isn't the case the task, once it has been
     * created, will ram through an attached observer.  Note the
     * chicken-egg problem here: to add the initial observation the
     * Proc needs the Task (which has the Observable). Conversely, for
     * a Task, while it has the Observable, it doesn't have the
     * containing proc.
     */
    protected Proc(Task task, int fork) {
	this(fork, task.getProc(), task.getProc().getHost(), task);
	logger.log(Level.FINE, "{0} new - create attached running proc\n", this);
    }

    /** XXX: Should not be public.  */
    public abstract void sendRefresh();

    /**
     * Return the current state as a string.
     */
    protected abstract String getStateFIXME();

    /**
     * killRequest handles killing off processes that either the
     * commandline or GUI have designated need to be removed from the
     * CPU queue.
     */
  
    public void requestKill() {
	// FIXME: Should be handled by lower-level code.
	ProcessIdentifier pid = ProcessIdentifierFactory.create(this.getPid());
	Signal.KILL.kill(pid);
	// Throw the countDown on the queue so that the command
	// thread will wait until events provoked by Signal.kill()
	// are handled.
	this.quitLatch = new CountDownLatch(1);
	Manager.eventLoop.add(new Event() {
		public void execute() {
		    quitLatch.countDown();
		}
	    });
    }

    /**
     * Request that the Proc be forcefully detached. Quickly.
     */
    public void requestAbandon() {
	logger.log(Level.FINE, "{0} abandon\n", this);
	performDetach();
	observations.clear();
    }

    /**
     * Request that the Proc be forcefully detached. Upon detach run
     * the given event.
     * 
     * @param e The event to run upon successful detach.
     */
    public void requestAbandonAndRunEvent(final Event e) {
	logger.log(Level.FINE, "{0} abandonAndRunEvent\n", this);
	requestAbandon();
	observableDetached.addObserver(new Observer() {
		public void update(Observable o, Object arg) {
		    Manager.eventLoop.add(e);
		}
	    });
    }

    /**
     * Request that the Proc's task list be refreshed using system
     * tables.
     */
    public abstract void requestRefresh();

    protected abstract void performDetach();

    /**
     * The set of observations that currently apply to this task.
     * Note that this is a Collection that may contain the same
     * Observer object multiple times (for possible different
     * observations).
     */
    private Collection observations = new LinkedList();

    public boolean addObservation(Object o) {
	return observations.add(o);
    }

    public boolean removeObservation(Object o) {
	return observations.remove(o);
    }

    public int observationsSize() {
	return observations.size();
    }

    public Iterator observationsIterator() {
	return observations.iterator();
    }

    public void requestUnblock(TaskObserver observerArg) {
	Iterator iter = getTasks().iterator();
	while (iter.hasNext()) {
	    Task task =(Task) iter.next();
	    task.requestUnblock(observerArg);
	}
    }

    /**
     * Table of this processes child processes.
     */
    private Set childPool = new HashSet();

    /**
     * Add Proc as a new child
     *
     * XXX: This should not be public.
     */
    public void add(Proc child) {
	logger.log(Level.FINEST, "{0} add(Proc) -- a child process\n", this);
	childPool.add(child);
    }

    /**
     * Remove Proc from this processes children.
     *
     * XXX: This should not be public.
     */
    public void remove(Proc child) {
	logger.log(Level.FINEST, "{0} remove(Proc) -- a child process\n", this);
	childPool.remove(child);
    }

    /**
     * Get the children as an array.
     */
    public LinkedList getChildren() {
	return new LinkedList(childPool);
    }

    /**
     * XXX: Temporary until .observable's are converted to
     * .requestAddObserver.
     */
    public class ObservableXXX extends Observable {
	/** XXX: Should not be public.  */
	public void notify(Object o) {
	    logger.log(Level.FINE, "{0} notify -- all observers\n", o);
	    setChanged();
	    notifyObservers(o);
	}
    }

    /**
     * Pool of tasks belonging to this Proc.
     *
     * XXX: Should not be public.
     */
    public Map taskPool = new HashMap();

    /**
     * Add the Task to this Proc.
     */
    void add(Task task) {
	taskPool.put(task.getTaskId(), task);
	host.observableTaskAddedXXX.notify(task);
    }

    /**
     * Remove Task from this Proc.
     */
    protected void remove(Task task) {
	logger.log(Level.FINEST, "{0} remove(Task) -- within this Proc\n", this);
	host.observableTaskRemovedXXX.notify(task);
	taskPool.remove(task.getTaskId());
	host.remove(task);
    }

    /**
     * Remove all but Task from this Proc.
     *
     * XXX: Should not be public.
     */
    public void retain(Task task) {
	logger.log(Level.FINE, "{0} retain(Task) -- remove all but task\n", this);
	HashMap new_tasks = new HashMap();
	new_tasks = (HashMap) ((HashMap) taskPool).clone();
	new_tasks.values().remove(task);
	taskPool.values().removeAll(new_tasks.values());
	host.removeTasks(new_tasks.values());
    }

    /**
     * Return this Proc's Task's as a list.
     */
    public LinkedList getTasks() {
	return new LinkedList(taskPool.values());
    }

    /**
     * The Process Auxiliary Vector.
     */
    public abstract Auxv[] getAuxv();

    /**
     * The process has transitioned to the attached state. XXX: Should
     * be made private and instead accessor methods added. Should more
     * formally define the observable and the event.
     */
    public ObservableXXX observableAttached = new ObservableXXX();

    /**
     * The process has transitioned to the detached. XXX: Should be
     * made private and instead accessor methods added. Should more
     * formally define the observable and the event.
     */
    public ObservableXXX observableDetached = new ObservableXXX();

    public String toString() {
	return ("{" + super.toString()
		+ ",pid=" + getPid()
		+ ",state=" + getStateFIXME()
		+ "}");
    }

    public int compareTo(Object o) {
	Proc other = (Proc)o;
	int comp = getHost().compareTo(other.getHost());
	if (comp == 0)
	    comp = getPid() - other.getPid();
	return comp;
    }
}
