// This file is part of the program FRYSK.
//
// Copyright 2005, Red Hat Inc.
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
package frysk.gui.monitor;

import java.io.File;
import java.io.IOException;
import java.util.Observable;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import frysk.gui.monitor.observers.AttachedContinueObserver;
import frysk.gui.monitor.observers.AttachedResumeObserver;
import frysk.gui.monitor.observers.AttachedStopObserver;
import frysk.gui.monitor.observers.DetachedContinueObserver;
import frysk.gui.monitor.observers.ObserverRunnable;
import frysk.gui.monitor.observers.TaskExecObserver;
import frysk.gui.monitor.observers.TaskTerminatingObserver;
import frysk.proc.Action;
import frysk.proc.Proc;
import frysk.proc.Task;
import frysk.proc.TaskObserver;

/**
 * @author pmuldoon
 *
 */
public class EventLogger implements TaskObserver.Execed, TaskObserver.Syscall,
		TaskObserver.Cloned, TaskObserver.Forked, TaskObserver.Terminating {

	private static final String FRYSK_CONFIG = System.getProperty("user.home")
			+ "/" + ".frysk" + "/";

	public static final String EVENT_LOG_ID = "frysk.gui.monitor.eventlog";

	private Logger eventLogFile = null;

	class EventFileHandler extends FileHandler {

		public EventFileHandler(String arg0, boolean arg1) throws IOException,
				SecurityException {
			super(arg0, arg1);
		}

		public synchronized void publish(LogRecord arg) {

			// As this is going to log exceptions, and as frysk might be kill -9'd
			// I've not found a way to let normal FileHandlers do an explicit flush
			// after each log event. So we found logfiles that were incomplete.
			// So will cause and explicit flush after each publish, until we figure out
			// otherwise.

			super.publish(arg);
			super.flush();
		}

	}

	/**{
	 * Local Observers
	 * */
	public AttachedContinueObserver attachedContinueObserver;

	public DetachedContinueObserver detachedContinueObserver;

	public AttachedStopObserver attachedStopObserver;

	public AttachedResumeObserver attachedResumeObserver;

	public TaskExecObserver taskExecObserver;

	public TaskTerminatingObserver taskExitingObserver;

	/** }*/

	public EventLogger() {
		this.attachedContinueObserver = new AttachedContinueObserver();
		this.attachedContinueObserver.addRunnable(new AttachedContinueRunnable());

		this.detachedContinueObserver = new DetachedContinueObserver();
		this.detachedContinueObserver.addRunnable(new DetachedContinueRunnable());

		this.attachedStopObserver = new AttachedStopObserver();
		this.attachedStopObserver.addRunnable(new AttachedStopRunnable());

		this.attachedResumeObserver = new AttachedResumeObserver();
		this.attachedResumeObserver.addRunnable(new AttachedResumeRunnable());

		eventLogFile = Logger.getLogger(EVENT_LOG_ID);
		eventLogFile.addHandler(buildHandler());
	}

	private FileHandler buildHandler() {
		FileHandler handler = null;
		File log_dir = new File(FRYSK_CONFIG + "eventlogs" + "/");

		if (!log_dir.exists())
			log_dir.mkdirs();

		try {
			handler = new EventFileHandler(log_dir.getAbsolutePath() + "/"
					+ "frysk_event_log.log", true);
		} catch (Exception e) {
			e.printStackTrace();
		}

		handler.setFormatter(new EventFormatter());
		return handler;
	}

	//XXX: soon to be removed
	class AttachedContinueRunnable implements ObserverRunnable {
		public void run(Observable o, Object obj) {
			eventLogFile.log(Level.INFO, "PID " + ((Proc) obj).getPid()
					+ " Host XXX Attached ");
		}
	}

	//XXX: soon to be removed
	class DetachedContinueRunnable implements ObserverRunnable {
		public void run(Observable o, Object obj) {
			eventLogFile.log(Level.INFO, "PID " + ((Proc) obj).getPid()
					+ " Host XXX Detached ");
		}
	}

	//XXX: soon to be removed
	class AttachedStopRunnable implements ObserverRunnable {
		public void run(Observable o, Object obj) {
			eventLogFile.log(Level.INFO, "PID " + ((Proc) obj).getPid()
					+ " Host XXX Stopped");
		}
	}

	//XXX: soon to be removed
	class AttachedResumeRunnable implements ObserverRunnable {
		public void run(Observable o, Object obj) {
			eventLogFile.log(Level.INFO, "PID " + ((Proc) obj).getPid()
					+ " Host XXX Resumed");
		}
	}

	public Action updateExeced(Task task) {
		eventLogFile.log(Level.INFO, "PID " + task.getTid()
				+ " Host XXX Execed");
		return Action.CONTINUE;
	}

	public Action updateSyscallEnter(Task task) {
		eventLogFile.log(Level.INFO, "PID " + task.getTid()
				+ " Host XXX entered syscall");
		return Action.CONTINUE;
	}

	public Action updateSyscallExit(Task task) {
		eventLogFile.log(Level.INFO, "PID " + task.getTid()
				+ " Host XXX left syscall");
		return Action.CONTINUE;
	}

	public Action updateCloned(Task task, Task clone) {
		eventLogFile.log(Level.INFO, "PID " + task.getTid()
				+ " Host XXX cloned new task: " + clone);
		return Action.CONTINUE;
	}

	public Action updateForked(Task task, Proc child) {
		eventLogFile.log(Level.INFO, "PID " + task.getTid()
				+ " Host XXX Forked a new proccess: " + child);
		return Action.CONTINUE;
	}

	public Action updateTerminating(Task task, boolean signal, int value) {
		eventLogFile.log(Level.INFO, "PID " + task.getTid()
				+ " Host XXX is exiting with signal: " + value);
		return Action.CONTINUE;
	}
	
	public void addedTo(Object o) {
		// TODO Auto-generated method stub
	}

        public void addFailed(Object o, Throwable w) {
	        throw new RuntimeException (w);
        }

	public void deletedFrom(Object o) {
		// TODO Auto-generated method stub
	}
	
}
