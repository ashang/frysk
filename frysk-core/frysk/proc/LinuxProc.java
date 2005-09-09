// This file is part of FRYSK.
//
// Copyright 2005, Red Hat Inc.
//
// FRYSK is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
//
// FRYSK is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with FRYSK; if not, write to the Free Software
// Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA

/**
 * Linux implementation of Proc.
 */

package frysk.proc;

import frysk.sys.Ptrace;
import java.io.File;
import java.io.FilenameFilter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import util.Scanner;
import util.eio.ArrayByteBuffer;
import util.eio.ByteBuffer;

public class LinuxProc
    extends Proc
{
    /**
     * The contents of a Linux /proc/PID/stat file.
     */
    static class Stat
    {
	/**
	 * Read in the stat file for procId (well at least part of
	 * it).
	 */
	Stat (ProcId procId)
	{
	    Scanner scanner;
	    try {
		String statName = "/proc/" + procId.id + "/stat";
		scanner = new Scanner (statName);
	    }
	    catch (FileNotFoundException e) {
		// Fine, the stat buffer (and probably process) just
		// diappeared.
		return;
	    }
	    catch (IOException e) {
		// Botched initialization, just skip.
		return;
	    }
	    try {
		pid = (int) scanner.readDecimalLong ();
		scanner.skipWhitespace ();
		scanner.skipByte ('(');
		StringBuffer b = new StringBuffer ();
		// XXX: Need to implement this by scanning from the
		// end of the line back towards the ')'.
		while (true) {
		    char c = scanner.readByte ();
		    if (c == ')') break;
		    b.append (c);
		}
		comm = b.toString ();
		scanner.skipWhitespace ();
		state = scanner.readByte ();
		scanner.skipWhitespace ();
		ppid = (int) scanner.readDecimalLong ();
	    }
	    catch (IOException e) {
		// Oops, some sort of scanning error.
		throw new RuntimeException (e);
	    }
	}
	int pid;
	String comm;
	char state;
	int ppid;
    }
    /**
     * If it hasn't already been read, read the stat structure.
     */
    protected Stat getStat ()
    {
	if (stat == null)
	    stat = new Stat (id);
	return stat;
    }
    private Stat stat;

    public String getCommand ()
    {
	return getStat ().comm;
    }

    private Auxv[] auxv;
    LinuxProc (Host host, ProcId pid, boolean attached)
    {
	super (host, pid, attached);
    }
    LinuxProc (Proc parent, ProcId pid, boolean attached)
    {
	super (parent, pid, attached);
    }
    LinuxProc (Host host, Proc parent, ProcId procId, Stat stat)
    {
	super (host, parent, procId);
	this.stat = stat;
    }
    void sendRefresh ()
    {
	// Create an array of task IDs.
	File tasks = new File ("/proc/" + id.id, "task");
	String[] pids = tasks.list (new FilenameFilter ()
	    {
		public boolean accept(File dir, String name)
		{
		    // Assume that only valid PID's start with a
		    // digit.
		    return (name.length () > 0
			    && Character.isDigit (name.charAt (0)));
		}
	    });
	if (pids == null)
	    pids = new String[] { };
	// Compare this against the existing taskPool.  ADDED
	// accumulates any tasks added to the taskPool.  REMOVED,
	// starting with all known tasks has any existing tasks
	// removed, so that by the end it contains a set of
	// removed tasks.
	Map added = new HashMap ();
	HashMap removed = (HashMap) ((HashMap)taskPool).clone ();
	TaskId searchId = new TaskId ();
	for (int i = 0; i < pids.length; i++) {
	    int pid = Integer.parseInt (pids[i]);
	    searchId.id = pid;
	    if (removed.containsKey (searchId)) {
		removed.remove (searchId);
	    }
	    else {
		// Add the process (it currently isn't attached).
		Task newTask = new LinuxTask (this, new TaskId (pid));
		added.put (newTask.id, newTask);
	    }
	}
	// Tell each task that no longer exists that it has been
	// removed.
	for (Iterator i = removed.values().iterator(); i.hasNext();) {
	    Task task = (Task) i.next ();
	    // XXX: Should there be a TaskEvent.schedule(), instead of
	    // Manager .eventLoop .appendEvent for injecting the event
	    // into the event loop?
	    task.requestRemoval ();
	    remove (task);
	}
    }
    void sendAttach ()
    {
	Ptrace.attach (id.id);
	File tasks = new File ("/proc/" + id.id, "task");
	String[] pids = tasks.list (new FilenameFilter ()
	    {
		public boolean accept(File dir, String name)
		{
		    // Assume that only valid PID's start with a
		    // digit.
		    return (name.length () > 0
			    && Character.isDigit (name.charAt (0)));
		}
	    });
	// Iterate over the pid's adding them as tasks.
	for (int i = 0; i < pids.length; i++) {
	    int tid = Integer.parseInt (pids[i]);
	    if (tid != id.id)
		Ptrace.attach (tid);
	    TaskId newTid = new TaskId (tid);
	    LinuxTask t = (LinuxTask) newTask (newTid, true);
	    t.ptraceAttached = true;
	}
    }
    void sendNewAttachedChild (ProcId childId)
    {
	// A forked child starts out attached.
	new LinuxProc (this, childId, true);
    }

    public void detach ()
    {
	Collection c = taskPool.values ();
	Iterator i = c.iterator();
	while (i.hasNext ()) {
	    LinuxTask task = (LinuxTask)i.next ();
	    if (task.ptraceAttached || task.id.id == id.id) {
		Ptrace.detach (task.id.hashCode (), 0);
		task.ptraceAttached = false;
	    }
	}
	super.detach ();
    }
    Task newTask (TaskId id, boolean runnable)
    {
	// XXX: Should be abstracted.
	return new I386Linux.Task (this, id, runnable);
    }
    public Auxv[] getAuxv ()
    {
	if (auxv == null) {
	    // XXX: Pull a random pid off the list, use that to
	    // construct the AUXV path; shouldn't be needed, instead
	    // use getPid() and a missing getIsa().
	    ByteBuffer b;
	    LinuxTask pid = (LinuxTask) taskPool.values().iterator().next ();
	    String auxvName = "/proc/" + pid.pid + "/auxv";
	    // Due to Linux kernel tackyness, need to slurp in the
	    // AUXV and then create an ByteBuffer from that.
	    try {
		java.io.FileInputStream auxvFile
		    = new java.io.FileInputStream (auxvName);
		byte[] buf = new byte[1024];
		int len = auxvFile.read (buf);
		b = new ArrayByteBuffer (buf, 0, len);
	    }
	    catch (Exception e) {
		throw new RuntimeException (e);
	    }
	    // Adjust the AuxV buffer parameters based on the
	    // current ISA.
	    b.order (pid.getIsa ().byteOrder);
	    b.wordSize (pid.getIsa ().wordSize);
	    // Finally parse the AuxV
	    auxv = Auxv.parse (b);
	}
	return auxv;
    }
}
