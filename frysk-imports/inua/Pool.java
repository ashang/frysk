// This file is part of INUA.  Copyright 2004, 2005, Andrew Cagney
//
// INUA is free software; you can redistribute it and/or modify it
// under the terms of the GNU General Public License as published by
// the Free Software Foundation; version 2 of the License.
//
// INUA is distributed in the hope that it will be useful, but
// WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
// General Public License for more details.
// 
// You should have received a copy of the GNU General Public License
// along with INUA; if not, write to the Free Software Foundation,
// Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
// 
// In addition, as a special exception, Andrew Cagney. gives You the
// additional right to link the code of INUA with code not covered
// under the GNU General Public License ("Non-GPL Code") and to
// distribute linked combinations including the two, subject to the
// limitations in this paragraph. Non-GPL Code permitted under this
// exception must only link to the code of INUA through those well
// defined interfaces identified in the file named EXCEPTION found in
// the source code files (the "Approved Interfaces"). The files of
// Non-GPL Code may instantiate templates or use macros or inline
// functions from the Approved Interfaces without causing the
// resulting work to be covered by the GNU General Public
// License. Only Andrew Cagney may make changes or additions to the
// list of Approved Interfaces. You must obey the GNU General Public
// License in all respects for all of the INUA code and other code
// used in conjunction with INUA except the Non-GPL Code covered by
// this exception. If you modify this file, you may extend this
// exception to your version of the file, but you are not obligated to
// do so. If you do not wish to provide this exception without
// modification, you must delete this exception statement from your
// version and license this file solely under the GPL without
// exception.

package inua;

import java.util.List;
import java.util.ArrayList;

/**
 * Object pool, call recycle to start reusing them.
 */

public class Pool
{
    private java.lang.reflect.Constructor constructor;
    private Object[] constructorArgs;

    /**
     * A simple pool.
     */
    public Pool (Class sample)
    {
	try {
	    // Find the "Object()" constructor.
	    Class[] constructorArgsClass = new Class[0];
	    constructor = sample.getConstructor (constructorArgsClass);
	    constructorArgs = new Object[0];
	}
	catch (Exception e) {
	    throw new RuntimeException (e);
	}
    }

    /**
     * A pool where each object's constructor is parameterized with
     * PARAM (It does an exact match of PARAM's class, is that a good
     * idea?).
     */
    public Pool (Class sample, Object param)
    {
	try {
	    // Find the "Object()" constructor.
	    Class[] constructorArgsClass = new Class[] { param.getClass () };
	    constructor = sample.getConstructor (constructorArgsClass);
	    constructorArgs = new Object[] { param };
	}
	catch (Exception e) {
	    throw new RuntimeException (e);
	}
    }

    private List pool = new ArrayList ();
    private int nextEvent = 0;

    /**
     * Return an object from the pool.
     */
    public Object get ()
    {
	if (nextEvent >= pool.size ()) {
	    try {
		pool.add (constructor.newInstance (constructorArgs));
	    }
	    catch (Exception e) {
		throw new RuntimeException (e);
	    }
	}
	return pool.get (nextEvent++);
    }
    /**
     * Recycle all objects from the pool.
     */
    public void recycle ()
    {
	nextEvent = 0;
    }
}
