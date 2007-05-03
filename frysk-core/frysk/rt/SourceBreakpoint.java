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
import java.util.HashMap;
import java.util.LinkedList;

import frysk.proc.Task;
import frysk.proc.Proc;

/**
 * Class representing a breakpoint at at location in a program. It
 * might correspond to multiple breakpoints in the executable due to
 * inlining. The class manages a collection of "raw" breakpoints that
 * should be added or deleted as a group by the RunState class. The
 * subclasses must define a method, getRawAddress, for calculating the
 * address of a raw breakpoint.
 */
/**
 * 
 */
public abstract class SourceBreakpoint implements BreakpointObserver
{
  private HashMap procMap;
  private BreakpointObserver observerDelegate;

  private class ProcEntry
  {
    LinkedList addrs = null; // The "raw" addresses
    LinkedList breakpoints = null; // RunState breakpoints
  }

  public SourceBreakpoint(BreakpointObserver observerDelegate)
  {
    procMap = new HashMap();
    this.observerDelegate = observerDelegate;
  }

  public SourceBreakpoint()
  {
    this(null);
  }
  
  /**
   * Get the list of raw address objects for a process
   *
   * @param The process
   * @return the list
   */
  public LinkedList getAddrs(Proc proc)
  {
    ProcEntry procEntry = (ProcEntry)procMap.get(proc);
    if (procEntry != null) 
      return procEntry.addrs;
    else
      return null;
  }

  /**
   * Set the list of raw address objects.
   * @param addrs the address objects
   */
  public void setAddrs(Proc proc, LinkedList addrs)
  {
    ProcEntry procEntry = (ProcEntry)procMap.get(proc);
    if (procEntry == null)
      {
	procEntry = new ProcEntry();
	procMap.put(proc, procEntry);
  }
    procEntry.addrs = addrs;
  }

  /**
   * Return the address to use as a breakpoint from the object stored
   * in the list of breakpoints.
   * @param addr the object stored in the addrs list
   * @return the raw address at which a breakpoint will be set
   */
  abstract long getRawAddress(Object addr);
  
   /**
    * Add this object's raw breakpoints to the process via the RunState object.
    * @param runState the RunState object
    * @param task task to which breakpoints are added, although they are in
    * 	fact added to the entire process.
    */
  public void addBreakpoint(Task task)
  {
    Proc proc = task.getProc();
    ProcEntry procEntry = (ProcEntry)procMap.get(proc);
    if (procEntry == null)
      return; 			// Exception?
    Iterator bpts = procEntry.addrs.iterator();
    procEntry.breakpoints = new LinkedList();
    while (bpts.hasNext())
      {
	Object bpt = bpts.next();
	long address = getRawAddress(bpt);
	Breakpoint.PersistentBreakpoint breakpoint
	  = new Breakpoint.PersistentBreakpoint(address);
	breakpoint.addObserver(this);
	procEntry.breakpoints.add(breakpoint);
	SteppingEngine.addBreakpoint(task, breakpoint);
      }
  }

  /**
   * Delete the object's raw breakpoints from a process via the RunState.
   * @param runState the RunState object
   * @param task task in the process
   */
  public void deleteBreakpoint(Task task)
  {
    Proc proc = task.getProc();
    ProcEntry procEntry = (ProcEntry)procMap.get(proc);
    if (procEntry == null)
      return; 			// Exception?
    Iterator iterator = procEntry.breakpoints.iterator();

    while (iterator.hasNext())
      {
	Breakpoint.PersistentBreakpoint bpt
	  = (Breakpoint.PersistentBreakpoint) iterator.next();
        SteppingEngine.deleteBreakpoint(task, bpt);
      }
    procEntry.breakpoints.clear();
  }

  /**
   * Test if RunState breakpoint is contained in this object. This is only
   * valid if the source breakpoint has been added to (and not deleted from)
   * the process.
   * @param bpt
   * @return
   */
  public boolean
  containsPersistantBreakpoint(Proc proc, Breakpoint.PersistentBreakpoint bpt)
  {
    ProcEntry procEntry = (ProcEntry)procMap.get(proc);
    if (procEntry == null)
      return false;
    return procEntry.breakpoints.contains(bpt);
  }

  public void updateHit(Breakpoint.PersistentBreakpoint breakpoint,
			Task task,
			long address)
  {
    if (observerDelegate != null)
      observerDelegate.updateHit(breakpoint, task, address);
  }

  public BreakpointObserver getObserverDelegate()
  {
    return observerDelegate;
  }

  public void setObserverDelegate(BreakpointObserver observerDelegate)
  {
    this.observerDelegate = observerDelegate;
  }

  public void addedTo (Object observable)
  {
  }

  public void addFailed (Object observable, Throwable w)
  {
  }

  public void deletedFrom (Object observable)
  {
  }
}
