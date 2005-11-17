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

package frysk.proc;

/**
 * The binding between an Observer and its Observable.
 */

abstract class Observation
{
    private Observable observable;
    private Observer observer;
    /**
     * Create a new Observer binding.
     */
    public Observation (Observable observable, Observer observer)
    {
	this.observable = observable;
	this.observer = observer;
    }
    /**
     * Returns true if the Object's Observable:Observer binding is
     * identical to this one.
     */
    public boolean equals (Object o)
    {
	if (o instanceof Observation) {
	    Observation rhs = (Observation) o;
	    return (this.observable == rhs.observable
		    && this.observer == rhs.observer);
	}
	else
	    return false;
    }
    /**
     * A somewhat arbitrary hash code.
     */
    public int hashCode ()
    {
	return (observable.hashCode () + observer.hashCode ());
    }
    /**
     * Apply the add operation to obervable.
     */
    void add ()
    {
	observable.add (observer);
    }
    /**
     * Delete the Observer from the Observable.
     */
    void delete ()
    {
	observable.delete (observer);
    }
    /**
     * Tell the observer that the add is failing with w.
     */
    void fail (Throwable w)
    {
	observable.fail (observer, w);
    }
    /**
     * Request that the Observer be added to the Observable.
     */
    public abstract void requestAdd ();
}
