package prog.util;

/** Runs the program, along with any sub-programs.
 *
 */

import java.util.*;
import com.redhat.fedora.frysk.proc.*;

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
	Manager.procFactory.createProc (args);
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
