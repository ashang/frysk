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

package frysk.testbed;

import frysk.sys.Sig;
import frysk.proc.Manager;
import java.util.logging.Level;

/**
 * Create an offspring that has synchronized with this test-framework
 * using a signal.  The synchronization ensures that the test process
 * is ready before the test proceeds.
 */

public class SynchronizedOffspring
    extends Offspring
{
    // NOTE: Use a different signal to thread add/del. Within this
    // process the signal is masked and Linux appears to propogate the
    // mask all the way down to the exec'ed child.
    public static final Sig START_ACK = Sig.HUP;

    private final int pid;
    /**
     * Return the ProcessID of the child.
     */
    public int getPid () {
	return pid;
    }

    /**
     * Create a child process (using startChild), return once the
     * process is running. Wait for acknowledge SIG.
     */
    protected SynchronizedOffspring (OffspringType type,
				     Sig sig, String[] argv) {
	logger.log(Level.FINE, "{0} new ...\n", this);
	SignalWaiter ack = new SignalWaiter(Manager.eventLoop, sig,
					    "startOffspring");
	pid = type.startOffspring(null, (logger.isLoggable(Level.FINE)
					 ? null
					 : "/dev/null"),
				  null, argv);
	TearDownProcess.add(pid);
	ack.assertRunUntilSignaled();
	logger.log(Level.FINE, "{0} ... new pid {1,number,integer}\n",
		   new Object[] {this, new Integer(pid) });
    }

    public SynchronizedOffspring(Sig sig, String[] argv) {
	this(OffspringType.DAEMON, sig, argv);
    }
}