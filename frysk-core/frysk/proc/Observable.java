// This file is part of the program FRYSK.
//
// Copyright 2005, 2007, Red Hat Inc.
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

package frysk.proc;

import java.util.HashMap;
import java.util.Iterator;

/**
 * Observable element of the proc model.
 */

public class Observable
{
    final protected Object observable;
    /**
     * Create an observable bound to Object.
     */
    public Observable (Object observable)
    {
	this.observable = observable;
    }
    /**
     * Set of Observer's for this Observable.  Since an observable can
     * contain an observer multiple times we keep a count.
     */
    private HashMap observers = new HashMap();
    /**
     * Add Observer to this Observable.
     */
    public void add (Observer observer)
    {
      Integer count = (Integer) observers.get(observer);
	if (count == null)
	  count = Integer.valueOf(1);
	else
	  count = Integer.valueOf(count.intValue() + 1);
	observers.put(observer, count);
	observer.addedTo (observable); // Success
    }
    /**
     * Delete Observer from this Observable.
     * Does nothing when observer isn't part of this observable.
     */
    public void delete (Observer observer)
    {
        Integer count = (Integer) observers.get(observer);
        if (count == null)
	  return;
        int c = count.intValue();
        if (c == 1)
	  observers.remove(observer);
        else
	  observers.put(observer, Integer.valueOf(c--));
        observer.deletedFrom (observable); // Success.
    }

    /**
     * Whether or not the given Observer is contained in this
     * set of Observables.
     */
    public boolean contains(Observer observer)
    {
      return observers.get(observer) != null;
    }

    /**
     * Fail to add the observer.
     */
    public void fail (Observer observer, Throwable w)
    {
	observer.addFailed (observable, w);
    }
    /**
     * Return an iterator for all this Observable's Observers.
     */
    public Iterator iterator ()
    {
      return observers.keySet().iterator ();
    }
    /**
     * Return the current number of observers
     */
    public int numberOfObservers(){
      return this.observers.size();
    }
    
    /**
     * Clear the set of observers.
     */
    public void removeAllObservers()
    {
      Iterator iter = observers.keySet().iterator();
      while (iter.hasNext())
        {
          Observer observer = (Observer) iter.next();
          observer.deletedFrom(observers);
        }
      this.observers.clear();
    }
}
