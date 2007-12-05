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

import frysk.sys.Signal;
import frysk.proc.Manager;
import java.util.logging.Level;

/**
 * Create a process running funit-exec as described by ExecCommand.
 *
 * The funit-exec program is created in two steps; first the
 * ExecCommand is created. and second the ExecOffspring running that
 * command.  This two step process makes it easier to construct a
 * chain of funit-execs.  For instance, to create an funit-exec proces
 * will invoke funit-exec and then invoke /bin/echo, use: new
 * ExecOffspring(new ExecCommnand(new ExecCommand(new String[] {
 * "/bin/echo", "hi"}))).
 *
 * The program funit-exec, when sent a signal, will exec its
 * arguments.
 */
public class ExecOffspring extends SynchronizedOffspring {
    /**
     * Invoke funit-exec as specified by COMMAND.
     */
    public ExecOffspring(ExecCommand command) {
	super(START_ACK, command.argv);
    }
    /**
     * Invoke funit-exec in a way that allows it to repeatedly re-exec
     * itself.
     */
    public ExecOffspring() {
	this(new ExecCommand());
    }

    /**
     * Request that the process perform an exec.  Since the exec
     * starts a new program, this operation is not acknowledged.
     */
    public void requestExec() {
	signal(Signal.INT);
    }
    /**
     * Request that a random thread does an exec.
     */
    public void requestThreadExec() {
	signal(Signal.USR1);
    }
    /**
     * Request an exec and then wait for the new program to signal
     * back that it is running.  This assumes that the new program is
     * set up to send the START_ACK to this process.
     */
    public void assertRunExec(String why) {
	logger.log(Level.FINE, why + "\n");
	SignalWaiter ack
	    = new SignalWaiter(Manager.eventLoop, START_ACK, why);
	requestExec();
	ack.assertRunUntilSignaled();
    }
    /**
     * Request that a random non-main thread do an exec and then wait
     * for the new program to signal back that it is running.  This
     * assumes that the new program is set up to send the START_ACK to
     * this process.
     */
    public void assertRunThreadExec(String why) {
	logger.log(Level.FINE, why + "\n");
	SignalWaiter ack
	    = new SignalWaiter(Manager.eventLoop, START_ACK, why);
	requestThreadExec();
	ack.assertRunUntilSignaled();
    }
}
