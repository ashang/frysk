// From the VENUS project.  Copyright 2004, 2005, Andrew Cagney
// Licenced under the terms of the Eclipse Public Licence.
// Licenced under the terms of the GNU CLASSPATH Licence.

package util;

/**
 * Pool of objects, call recycle to start reusing them.
 *
 */

import java.util.*;

public class Pool
{
    java.lang.reflect.Constructor constructor;
    Object[] constructorArgs;

    // A simple pool.
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

    // A pool where each object's constructor is parameterized with
    // PARAM (It does an exact match of PARAM's class, is that a good
    // idea?).
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

    List pool = new ArrayList ();
    int nextEvent = 0;
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
    public void recycle ()
    {
	nextEvent = 0;
    }
}
