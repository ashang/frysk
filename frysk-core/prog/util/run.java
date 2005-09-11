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

package prog.util;

/** Runs the program, along with any sub-programs.
 *
 */

import java.util.Observable;
import java.util.Observer;

import frysk.proc.Manager;
import frysk.proc.Proc;
import frysk.proc.Task;

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
	    proc.taskDiscovered.addObserver (new TaskCreatedObserver ());
	    proc.taskRemoved.addObserver (new TaskDestroyedObserver ());
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
	    if (process.getParent () == null) {
		System.out.println ("Top process destroyed");
		Manager.eventLoop.requestStop ();
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

	Manager.procDiscovered.addObserver (new ProcCreatedObserver ());
	Manager.procRemoved.addObserver (new ProcDestroyedObserver ());
	Manager.host.requestCreateProc (args);
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
