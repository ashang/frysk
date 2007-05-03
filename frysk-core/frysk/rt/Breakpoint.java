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

import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import frysk.proc.Action;
import frysk.proc.Observable;
import frysk.proc.Task;
import frysk.proc.TaskObserver;

public class Breakpoint implements TaskObserver.Code 
{
  protected static Logger logger = Logger.getLogger ("frysk");
  
  protected long address;

  protected int triggered;

  protected boolean added;

  protected boolean removed;
  
  protected Object monitor = new Object();
  
  public Breakpoint () {}

  public Breakpoint (long address)
  {
//    System.out.println("Setting address to 0x" + Long.toHexString(address));
    this.address = address;
  }

  protected void logHit (Task task, long address, String message)
  {
    if (logger.isLoggable(Level.FINEST))
      {
        Object[] logArgs = { task, Long.toHexString(address),
                            Long.toHexString(task.getIsa().pc(task)),
                            Long.toHexString(this.address) };
        logger.logp(Level.FINEST, "RunState.Breakpoint", "updateHit",
                    message, logArgs);
      }
  }
  
  public Action updateHit (Task task, long address)
  {
//    System.err.println("SteppingBreakpoint.updateHIt " + task);
    logHit(task, address, "task {0} at 0x{1}\n");
    if (address != this.address)
      {
        logger.logp(Level.WARNING, "RunState.Breakpoint", "updateHit",
                    "Hit wrong address!");
        return Action.CONTINUE;
      }
    else
      {
        logHit(task, address, "adding instructionobserver {0} 0x{2}");
        task.requestAddInstructionObserver(SteppingEngine.getSteppingObserver());
      }

    ++triggered;
    return Action.BLOCK;
  }

  int getTriggered ()
  {
    return triggered;
  }

  public void addFailed (Object observable, Throwable w)
  {
    w.printStackTrace();
  }

  public void addedTo (Object observable)
  {
    synchronized (monitor)
      {
        added = true;
        removed = false;
        monitor.notifyAll();
      }
//    System.err.println("BreakPoint.addedTo");
    ((Task) observable).requestDeleteInstructionObserver(SteppingEngine.getSteppingObserver());
  }

  public boolean isAdded ()
  {
    return added;
  }

  public void deletedFrom (Object observable)
  {
    synchronized (monitor)
      {
        removed = true;
        added = false;
        monitor.notifyAll();
      }
  }

  public boolean isRemoved ()
  {
    return removed;
  }

  public long getAddress()
  {
    return address;
  }
  
  static public class PersistentBreakpoint extends Breakpoint
  {
    
    private Observable observable;
    
    /*
     * A breakpoint added by a high-level action e.g., set by the
     * user. It is not meant to be transient.
     */
    public PersistentBreakpoint(long address) 
    {
      super(address);
      observable = new Observable(this);
    }

    // These operations synchronize on the breakpoint, not the
    // observable object, so that other users of PersistentBreakpoint
    // can synchronize too without having to make the observable public.
    public synchronized void addObserver(BreakpointObserver observer)
    {
      observable.add(observer);
    }

    public synchronized void deleteObserver(BreakpointObserver observer)
    {
      observable.delete(observer);
    }

    public Iterator observersIterator()
    {
      return observable.iterator();
    }

    public synchronized int numberOfObservers()
    {
      return observable.numberOfObservers();
    }

    public synchronized void removeAllObservers()
    {
      observable.removeAllObservers();
    }
      
    public Action updateHit(Task task, long address)
    {
      //logger.entering("RunState.PersistentBreakpoint",
      //"updateHit");
      logHit(task, address, "Persistent.Breakpoint.updateHit at 0x{1}");
      Action action = super.updateHit(task, address);
      synchronized (SteppingEngine.class)
	{
	  SteppingEngine.runningTasks.remove(task);
	}
      synchronized (this)
	{
	  Iterator iterator = observable.iterator();
	  while (iterator.hasNext())
	    {
	      BreakpointObserver observer
		= (BreakpointObserver)iterator.next();
	      observer.updateHit(this, task, address);
	    }
	}
      return action;
    }

    public void addedTo (Object observable)
    {
      synchronized (monitor)
	{
	  added = true;
	  removed = false;
	  monitor.notifyAll();
	}
      // Don't remove the current insturction observer.
    }
  }



    public PersistentBreakpoint getTaskPersistentBreakpoint(Task task)
    {
      return (PersistentBreakpoint) SteppingEngine.getTaskBreakpoint(task);
    }
    
    public void addPersistentBreakpoint(Task task, PersistentBreakpoint bp)
    {
      task.requestAddCodeObserver(bp, bp.getAddress());
    }

    public void deletePersistentBreakpoint(Task task, PersistentBreakpoint bp)
    {
      task.requestDeleteCodeObserver(bp, bp.getAddress());
    }

  }
  