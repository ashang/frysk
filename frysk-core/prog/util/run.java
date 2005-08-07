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
package prog.util;

/** Runs the program, along with any sub-programs.
 *
 */

import java.util.Observable;
import java.util.Observer;

import com.redhat.fedora.frysk.proc.Manager;
import com.redhat.fedora.frysk.proc.Proc;
import com.redhat.fedora.frysk.proc.Task;

class run
{
    static class TaskCreatedObserver
	implements Observer
    {
	static long count;
	public void update (Observable o, Object obj)
	{
	    count++;
	    Task task = (Task) obj;
	    task.traceFork = true;
	    task.traceClone = true;
	}
    }

    static class TaskDestroyedObserver
	implements Observer
    {
	static long count;
	public void update (Observable o, Object obj)
	{
	    count++;
	}
    }

    static class ProcCreatedObserver
	implements Observer
    {
	static long count;
	public void update (Observable o, Object obj)
	{
	    count++;
	    Proc proc = (Proc) obj;
	    proc.taskCreated.addObserver (new TaskCreatedObserver ());
	    proc.taskDestroyed.addObserver (new TaskDestroyedObserver ());
	}
    }

    static class ProcDestroyedObserver
	implements Observer
    {
	static long count;
	public void update (Observable o, Object obj)
	{
	    count++;
	    Proc process = (Proc) obj;
	    if (process.parent == null) {
		System.out.println ("Top process destroyed");
		Manager.eventLoop.stop ();
	    }
	}
    }

    public static void main (String[] args)
    {
	int n;

	if (args.length == 0) {
	    System.out.println ("Usage: program args ...");
	    return;
	}

	Manager.procCreated.addObserver (new ProcCreatedObserver ());
	Manager.procDestroyed.addObserver (new ProcDestroyedObserver ());
	Manager.host.createProc (args);
	Manager.eventLoop.run ();
	System.out.println ("Tasks Created " +
			    Long.toString (TaskCreatedObserver.count));
	System.out.println ("Tasks Destroyed " +
			    Long.toString (TaskDestroyedObserver.count));
	System.out.println ("Processes Created " +
			    Long.toString (ProcCreatedObserver.count));
	System.out.println ("Processes Destroyed " +
			    Long.toString (ProcDestroyedObserver.count));
    }
}
