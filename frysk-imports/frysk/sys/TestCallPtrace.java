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

package frysk.sys;

import frysk.junit.TestCase;
import frysk.testbed.TearDownProcess;

public class TestCallPtrace
    extends TestCase
{
    /**
     * Rip down everything related to PID.
     */
    public void tearDown ()
    {
	TearDownProcess.tearDown ();
    }
 
    public void testChildContinue ()
    {
	final int pid = PtraceServer.child(null, null, null,
					   new String[] { "/bin/true" });
	assertTrue ("pid", pid > 0);
	TearDownProcess.add (pid);
	
	// The initial stop.
	Wait.waitAll (pid, new UnhandledWaitBuilder ()
	    {
		private final int id = pid;
		protected void unhandled (String why)
		{
		    fail (why);
		}
		public void stopped (int pid, int signal)
		{
		    assertEquals ("stopped pid", id, pid);
		    assertEquals ("stopped sig", Sig.TRAP_, signal);
		}
	    });

	PtraceServer.singleStep(pid, 0);
	Wait.waitAll (pid, new UnhandledWaitBuilder ()
	    {
		private final int id = pid;
		protected void unhandled (String why)
		{
		    fail (why);
		}
		public void stopped (int pid, int signal)
		{
		    assertEquals ("stopped pid", id, pid);
		    assertEquals ("stopped sig", Sig.TRAP_, signal);
		}
	    });

	PtraceServer.cont (pid, Sig.TERM_);
	Wait.waitAll (pid, new UnhandledWaitBuilder ()
	    {
		private final int id = pid;
		protected void unhandled (String why)
		{
		    fail (why);
		}
		public void terminated (int pid, boolean signal, int value,
					boolean coreDumped)
		{
		    assertEquals ("terminated pid", id, pid);
		    assertEquals ("terminated signal", true, signal);
		    assertEquals ("terminated value", Sig.TERM_, value);
		}
	    });
    }
	
    public void testAttach ()
    {
	final int pid = new Daemon (new Execute ()
	    {
		public void execute ()
		{
		    Itimer.sleep (TestCase.getTimeoutSeconds());
		}
	    }).hashCode ();
	TearDownProcess.add (pid);
	assertTrue ("pid", pid > 0);

	PtraceServer.attach(pid);
	Wait.waitAll (pid, new UnhandledWaitBuilder ()
	    {
		private final int id = pid;
		protected void unhandled (String why)
		{
		    fail (why);
		}
		public void stopped (int pid, int signal)
		{
		    assertEquals ("stopped pid", id, pid);
		    assertEquals ("stopped sig", Sig.STOP_, signal);
		}
	    });

	PtraceServer.detach (pid, 0);
	Errno errno = null;
	try {
	    Wait.waitAll (pid, new UnhandledWaitBuilder ()
		{
		    protected void unhandled (String why)
		    {
			fail (why);
		    }
		});
	}
	catch (Errno e) {
	    errno = e;
	}
	assertEquals ("Errno", Errno.Echild.class, errno.getClass());
    }
}
