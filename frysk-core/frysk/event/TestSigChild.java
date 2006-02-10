package frysk.event;

import frysk.sys.Sig;
import junit.framework.TestCase;

/**
 * Frysk, and the GCJ runtime, when it comes to SIGCHLD, can interact
 * in bad ways.
 */

public class TestSigChild
    extends TestCase
{
    public void stackDump ()
    {
	EventLoop eventLoop = new EventLoop ();
	eventLoop.add (new SignalEvent (Sig.CHLD)
	    {
		public final void execute ()
		{
		}
	    });
	new RuntimeException ().getStackTrace ();
    }
    public void testAAA ()
    {
	stackDump ();
    }
    public void testBBB ()
    {
	stackDump ();
    }
}
