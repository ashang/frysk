// This file is part of the program FRYSK.
//
// Copyright 2007, Red Hat Inc.
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

import java.util.Observable;
import java.util.TreeMap;
import frysk.dwfl.DwflFactory;
import frysk.proc.Proc;
import frysk.proc.ProcObserver;
import frysk.proc.ProcTasksObserver;
import frysk.proc.Task;
//import frysk.proc.TaskObserver;
import lib.dw.DwarfDie;

public class BreakpointManager
  extends Observable
{
  private int breakpointID = 0;
  private TreeMap breakpointMap = new TreeMap();
  private SteppingEngine steppingEngine;
  
  public BreakpointManager(SteppingEngine steppingEngine)
  {
    this.steppingEngine = steppingEngine;
  }

  // Watch a process and its tasks for events that might cause
  // breakpoints to be added or deleted in it.
  private class ProcWatcher
    implements ProcObserver.ProcTasks
  {
    Proc proc;
    ProcTasksObserver ptObs;

    ProcWatcher(Proc proc)
    {
      this.proc = proc;
      ptObs = new ProcTasksObserver(proc, this);
    }
    
    HashSet procTasks = new HashSet();

    public void existingTask(Task task)
    {
      procTasks.add(task);
    }

    public void taskAdded(Task task)
    {
      procTasks.add(task);

      Iterator bptIterator = breakpointMap.values().iterator();

      while (bptIterator.hasNext()) {
	SourceBreakpoint bpt = (SourceBreakpoint)bptIterator.next();

	if (bpt.appliesTo(proc, task)) {
	  enableBreakpoint(bpt, task);
	}
      }
    }

    public void taskRemoved(Task task)
    {
      procTasks.remove(task);
    }

    public void addedTo(Object observable)
    {
    }

    public void addFailed(Object observable, Throwable w)
    {
    }

    public void deletedFrom(Object observable)
    {
    }
    
  }

  private HashMap watchers = new HashMap();
  
  public synchronized LineBreakpoint addLineBreakpoint(String fileName,
						       int lineNumber,
						       int column)
  {
    int bptId = breakpointID++;
    LineBreakpoint sourceBreakpoint
      = new LineBreakpoint(bptId, fileName, lineNumber, column);
    breakpointMap.put(new Integer(bptId), sourceBreakpoint);
    setChanged();
    notifyObservers();
    return sourceBreakpoint;
  }

  public FunctionBreakpoint addFunctionBreakpoint(String name, DwarfDie die)
  {
    int bptId = breakpointID++;
    FunctionBreakpoint sourceBreakpoint
      = new FunctionBreakpoint(bptId, name, die);
    breakpointMap.put(new Integer(bptId), sourceBreakpoint);
    setChanged();
    notifyObservers();
    return sourceBreakpoint;
  }

  public void enableBreakpoint(SourceBreakpoint breakpoint, Task task)
  {
    Proc proc = task.getProc();
    ProcWatcher watcher = (ProcWatcher)watchers.get(proc);
    if (watcher == null) {
      watcher = new ProcWatcher(proc);
      watchers.put(proc, watcher);
    }
        
    breakpoint.enableBreakpoint(task, this.steppingEngine);
    setChanged();
    notifyObservers();
  }

  public void disableBreakpoint(SourceBreakpoint breakpoint, Task task)
  {
    breakpoint.disableBreakpoint(task, this.steppingEngine);
    setChanged();
    notifyObservers();
  }

  public Iterator getBreakpointTableIterator()
  {
    return breakpointMap.values().iterator();
  }

  public SourceBreakpoint getBreakpoint(int bptId)
  {
    SourceBreakpoint bpt
      = (SourceBreakpoint)breakpointMap.get(new Integer(bptId));
    return bpt;
  }

  private HashSet managedProcs = new HashSet();
  
  public void manageProcess(Proc proc)
  {
    if (managedProcs.contains(proc))
      return;
    FunctionBreakpoint sharedLibBpt
      = new FunctionBreakpoint(-1, "_dl_debug_state", null);
    sharedLibBpt.addObserver(new SourceBreakpointObserver() {
	public void updateHit(SourceBreakpoint bpt, Task task, long address)
	{
	  Proc proc = task.getProc();
	  DwflFactory.clearDwfl(proc);
	  new Exception().printStackTrace();
	  steppingEngine.continueExecution(proc.getTasks());
	}

	public void addedTo(Object observable)
	{
	}

	public void addFailed(Object observable, Throwable w)
	{
	}

	public void deletedFrom(Object observable)
	{
	}
      });
    managedProcs.add(proc);
    enableBreakpoint(sharedLibBpt, proc.getMainTask());
  }
}
