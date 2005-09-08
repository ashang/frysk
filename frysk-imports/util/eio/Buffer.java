// From the VENUS project.  Copyright 2004, 2005, Andrew Cagney
// Licenced under the terms of the Eclipse Public Licence.
// Licenced under the terms of the GNU CLASSPATH Licence.

package util.eio;

public abstract class Buffer
{
    // 0 <= lowWater <= caret <= bound <= highWater
    protected long lowWater;
    protected long mark;
    protected long cursor;
    protected long bound;
    protected long highWater;

    protected Buffer (long theLowWater, long theHighWater)
    {
	lowWater = theLowWater;
	highWater = theHighWater;
	cursor = theLowWater;
	mark = -1;
	bound = highWater;
    }

    // public final long tare ();
    public final long capacity ()
    {
	return highWater - lowWater;
    }
    public final long position ()
    {
	return cursor - lowWater;
    }
    public final Buffer position (long position)
    {
	cursor = position + lowWater;
	return this;
    }
    public final long limit ()
    {
	return bound - lowWater;
    }
    public final Buffer limit (long limit)
    {
	bound = limit + lowWater;
	return this;
    }
    public final Buffer mark ()
    {
	mark = cursor;
	return this;
    }
    public final Buffer reset ()
    {
	cursor = mark;
	return this;
    }
    public final Buffer clear ()
    {
	cursor = lowWater;
	bound = highWater;
	mark = -1;
	return this;
    }
    public final Buffer flip ()
    {
	bound = cursor;
	cursor = lowWater;
	return this;
    }
    public final Buffer rewind ()
    {
	cursor = lowWater;
	mark = -1;
	return this;
    }
    public final long remaining ()
    {
	return bound - cursor;
    }
    public final boolean hasRemaining ()
    {
	return remaining () > 0;
    }
}
