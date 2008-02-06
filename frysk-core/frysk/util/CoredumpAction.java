// This file is part of the program FRYSK.
// 
// Copyright 2006, 2007, 2008, Red Hat Inc.
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

import java.util.LinkedList;
import java.util.Observable;
import java.util.Observer;
import java.util.logging.Level;
import java.util.logging.Logger;
import frysk.event.Event;
import frysk.event.RequestStopEvent;
import frysk.event.SignalEvent;
import frysk.proc.Manager;
import frysk.proc.Proc;
import frysk.proc.ProcObserver;
import frysk.proc.Task;
import frysk.sys.Signal;
import frysk.isa.corefiles.LinuxElfCorefile;
import frysk.isa.corefiles.LinuxElfCorefileFactory;

/**
 * CoredumpAction  
 * 
 * Utility class to take a pid. Derive from that a Proc that is modelled
 * in the core, stop all the tasks, get the main tasks's maps, and each threads
 * register/status. Finally, Construct an elf core-file. 
 * 
 */

public class CoredumpAction implements ProcObserver.ProcAction {

    protected static final Logger logger = Logger.getLogger("frysk");

    private String filename = "core";

    private boolean writeAllMaps = false;

    private LinuxElfCorefile coreFile;

    int taskArraySize = 1;

    Task[] taskArray;

    private LinkedList taskList;

    Proc proc = null;

    private Event event;

    /**
     * CoredumpAction - Generate a core file
     * @param proc - Process extract and  dump core
     * @param theEvent - Event
     * @param writeAllMaps - Should all maps be written.
     */
    public CoredumpAction(Proc proc, Event theEvent, boolean writeAllMaps) {
	this.proc = proc;
	this.event = theEvent;
	taskList = proc.getTasks();
	taskArray = new Task[taskList.size()];
	this.writeAllMaps = writeAllMaps;
	Manager.eventLoop.add(new InterruptEvent(proc));
    }

    /**
     * CoredumpAction - Generate a core file
     * @param proc - Process extract and  dump core
     * @parma filename - Name of core file. (ie name.pid)
     * @param theEvent - Event
     * @param writeAllMaps - Should all maps be written.
     */
    public CoredumpAction(Proc proc, String filename, Event theEvent,
	    boolean writeAllMaps) {
	
	this(proc, theEvent, writeAllMaps);
	this.filename = filename;

    }

    /* (non-Javadoc)
     * @see frysk.proc.ProcObserver#existingTask(frysk.proc.Task)
     */
    public void existingTask(Task task) {

	// Add task to our list. Special case for
	// main task.
	if (proc.getMainTask() == task)
	    taskArray[0] = task;
	else {
	    taskArray[taskArraySize] = task;
	    taskArraySize++;
	}

	// Remove this task from the list of tasks that
	// the Proc object told us about.
	if (taskList.contains(task))
	    taskList.remove(task);
    }

    /* (non-Javadoc)
     * @see frysk.proc.Observer#addFailed(java.lang.Object, java.lang.Throwable)
     */
    public void addFailed(final Object observable, final Throwable w) {
	abandonCoreDump((Exception) w);
    }

    /* (non-Javadoc)
     * @see frysk.proc.ProcObserver.ProcAction#allExistingTasksCompleted()
     */
    public void allExistingTasksCompleted() {
	coreFile = LinuxElfCorefileFactory.getCorefile(proc, taskArray);

	if (coreFile == null) {
	    Exception e = new RuntimeException("Architecture not supported or "
		    + "LinuxElfCorefileFactory returned null");
	    abandonCoreDump(e);

	} else {
	    coreFile.setName(this.filename);
	    coreFile.setWriteAllMaps(writeAllMaps);

	    try {
		coreFile.constructCorefile();
	    } catch (RuntimeException e) {
		abandonCoreDump(e);
	    }
	    Manager.eventLoop.add(event);
	}
    }

    /* (non-Javadoc)
     * @see frysk.proc.Observer#addedTo(java.lang.Object)
     */
    public void addedTo(Object observable) {
    }

    /* (non-Javadoc)
     * @see frysk.proc.ProcObserver.ProcAction#taskAddFailed(java.lang.Object, java.lang.Throwable)
     */
    public void taskAddFailed(Object task, Throwable w) {
    }

    /* (non-Javadoc)
     * @see frysk.proc.Observer#deletedFrom(java.lang.Object)
     */
    public void deletedFrom(final Object observable) {
    }

    /**
     * Abandon the core dump. Print out error message, then as quickly as possible
     * abandon the Proc object and exit.
     * 
     * @param e - Exception that caused the abandon
     */
    private void abandonCoreDump(Exception e) {
	proc.requestAbandon();
	proc.observableDetached.addObserver(new Observer() {

	    public void update(Observable o, Object arg) {
		Manager.eventLoop.add(new RequestStopEvent(Manager.eventLoop));
	    }
	});
	throw new RuntimeException("Core file abandoned because of: ", e);
    }

    /**
     * getConstructedFileName() return the constructed filename of the
     * corefile.
     *  
     * @return String - name of corefile
     * 
     */
    public String getConstructedFileName() {
	return this.coreFile.getConstructedFileName();
    }

    /**
     * Static class to define the behaviour of CoredumpAction
     * on interrupt
     */
    static class InterruptEvent extends SignalEvent {
	Proc proc;

	public InterruptEvent(Proc theProc) {

	    super(Signal.INT);
	    proc = theProc;
	    logger.log(Level.FINE, "{0} InterruptEvent\n", this);
	}

	public final void execute() {
	    logger.log(Level.FINE, "{0} execute\n", this);
	    proc.requestAbandonAndRunEvent(new RequestStopEvent(
		    Manager.eventLoop));
	    try {
		Manager.eventLoop.join(5);
	    } catch (InterruptedException e) {
		e.printStackTrace();
	    }
	    System.exit(1);

	}
    }

}
