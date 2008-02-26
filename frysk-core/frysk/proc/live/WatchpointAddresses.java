// This file is part of the program FRYSK.
//
// Copyright 2006, 2007, 2008 Red Hat Inc.
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


package frysk.proc.live;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.TreeSet;

import frysk.proc.Task;
import frysk.proc.TaskObserver;

/**
 * Keeps track of address watchppoints for a Proc (all Tasks of a Proc
 * share the same breakpoints).  Watchpoints absolute
 * addresses with a length Proc text/data area.  
 * 
 * This class is used to construct
 * higher level watchpoint observers.  The class keeps track of the
 * number of observers interested in an address/length for the Proc. It adds
 * or deletes the actual watchpoints depending on the number of active
 * observers.  
 */
public class WatchpointAddresses
{
  /**
   * Proc used to set watchpoints and which sents us notifications
   * when watchpoints are hit.
   */
  private final Task task;

  /**
   * Maps watchpoints addresses/length to a list of observers.  We assume the
   * number of observers for each address is small, so an ArrayList
   * will do.
   */
  private final HashMap map;

  /**
   * A sorted set (on address) of Watchpoints, used for getWatchpoints().
   */
  private final TreeSet watchpoints;

  /**
   * Package private constructor used by the Proc when created.
   */
  public WatchpointAddresses(Task task)
  {
    this.task = task;
    map = new HashMap();
    watchpoints = new TreeSet();
  }

  /**
   * Adds a watchpoint observer to an address. If there is not yet a
   * watchpoint at the given address the given Task is asked to add
   * one (the method will return true). Otherwise the observer is
   * added to the list of objects to notify when the watchpoint is
   * hit (and the method returns false).
   */
  public boolean addWatchpoint(TaskObserver.Watch observer, long address, int length)
  {
    Watchpoint watchpoint = Watchpoint.create(address, length, task);

    ArrayList list = (ArrayList) map.get(watchpoint);
    if (list == null)
      {
	  watchpoints.add(watchpoint);
	list = new ArrayList();
	map.put(watchpoint, list);
	list.add(observer);
	return true;
      }
    else
      {
	list.add(observer);
	return false;
      }
  }

  /**
   * Removes an observer from a watchpoint. If this is the last
   * observer interested in this particular address then the
   * watchpoint is really removed by requestion the given task to do
   * so (the method will return true). Otherwise just this observer
   * will be removed from the list of observers for the watchpoint
   * address (and the method will return false).
   *
   * @throws IllegalArgumentException if the observer was never added.
   */
  public boolean removeWatchpoint(TaskObserver.Watch observer, long address, int length)
  {
    Watchpoint watchpoint = Watchpoint.create(address, length, task);
    ArrayList list = (ArrayList) map.get(watchpoint);
    if (list == null || ! list.remove(observer))
      throw new IllegalArgumentException("No breakpoint installed: "
					 + watchpoint);
    
    if (list.isEmpty())
      {
	watchpoints.remove(watchpoint);
	map.remove(watchpoint);
	return true;
      }
    else
      return false;
  }

    /**
     * Called by the Proc when it has trapped a watchpoint.  Returns a
     * Collection of TaskObserver.Watch observers interested in the given
     * address or null when no Watch observer was installed on this address.
     */
    public Collection getWatchObservers(long address, int length) {
	ArrayList observers;
	Watchpoint watchpoint = Watchpoint.create(address, length, task);
	ArrayList list = (ArrayList) map.get(watchpoint);
	if (list == null)
	    return null;
	// Return the cloned list of observers in case the Code
	// observer wants to add or remove itself or a new observer to
	// that same breakpoint.
	observers = (ArrayList) list.clone();
	return observers;
    }

  public Watchpoint getWatchpoint(long address, int length)
  {
    Watchpoint breakpoint = Watchpoint.create(address, length, task);
    Object observer = map.get(breakpoint);
    if (observer == null)
      return null;
    else
      return breakpoint;
  }

  /**
   * Called from TaskState when the Task gets an execed event which
   * clears the whole address space.
   *
   * XXX: Should not be public.
   */
  public void removeAllWatchObservers()
  {
    map.clear();
    watchpoints.clear();
  }
}
