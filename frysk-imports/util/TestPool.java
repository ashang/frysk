// From the VENUS project.  Copyright 2004, 2005, Andrew Cagney
// Licenced under the terms of the Eclipse Public Licence.
// Licenced under the terms of the GNU CLASSPATH Licence.

package util;

import junit.framework.TestCase;

public class TestPool
    extends TestCase
{
    static class Counter
    {
	int counter;
    }

    static class ParameterlessObject
	extends Counter
    {
	static int count = 0;
	public ParameterlessObject ()
	{
	    counter = count++;
	}
    }

    static class ParameteredObject
	extends Counter
    {
	public ParameteredObject (Counter c)
	{
	    this.counter = c.counter++;
	}
    }

    void usePool (String what, Pool pool, Counter[] counters)
    {
	for (int i = 0; i < counters.length; i++) {
	    counters[i] = (Counter) pool.get ();
	    assertEquals (what, i, counters[i].counter);
	}
    }

    public void testParamaterlessPool ()
    {
	Counter[] counters = new Counter[10];
	Pool parameterlessPool = new Pool (ParameterlessObject.class);
	usePool ("Allocate parameterless", parameterlessPool, counters);
	parameterlessPool.recycle ();
	usePool ("Recycle parameterless", parameterlessPool, counters);
    }

    public void testParameteredPool ()
    {
	Counter[] counters = new Counter[10];
	Pool parameteredPool = new Pool (ParameteredObject.class,
					 new Counter ());
	usePool ("Allocate parametered", parameteredPool, counters);
	parameteredPool.recycle ();
	usePool ("Recycle parametered", parameteredPool, counters);
    }
}
