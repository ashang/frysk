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
import frysk.Config;
import frysk.sys.Pid;
import frysk.junit.TestCase;
import java.util.LinkedList;
import java.util.logging.Level;
import frysk.proc.Manager;

/**
 * Create a process running funit-exec.
 *
 * The program funit-exec, when sent a signal, will exec its
 * arguments.
 */
public class ExecOffspring extends SynchronizedOffspring {
    /**
     * Create a process that, when requested, will execute an exec
     * system call.
     */
    public ExecOffspring(String[] programAndArgs) {
	super(START_ACK, getCommandLine(0, 0, null, programAndArgs));
    }
    /**
     * Create a multi-threaded process that, when requested, will exec
     * ARGS[0] passing ARGS.
     */
    public ExecOffspring(int threads, String[] programAndArgs) {
	super(START_ACK, getCommandLine(0, threads, null, programAndArgs));
    }

    /**
     * Construct a command line for invoking the WORD_SIZED version of
     * funit-exec (0 for default word size).  If THREADS is non-zero,
     * create that many additional threads.  If EXE is non-null,
     * invoke that program instead of PROGRAM from PROGRAM_ARGS.
     */
    public static String[] getCommandLine(int wordSize,
					  int threads,
					  String exe,
					  String[] programArgs) {
	LinkedList args = new LinkedList();
	switch (wordSize) {
	case 0:
	    args.add(Config.getPkgLibFile("funit-exec").getPath());
	    break;
	case 32:
	    args.add(Config.getPkgLib32File("funit-exec").getPath());
	    break;
	case 64:
	    args.add(Config.getPkgLib64File("funit-exec").getPath());
	    break;
	default:
	    throw new RuntimeException("wordSize " + wordSize + " is unknown");
	}
	if (threads > 0) {
	    args.add("-c");
	    args.add(Integer.toString(threads));
	}
	if (exe != null) {
	    args.add("-e");
	    args.add(exe);
	}
	args.add("-m");
	args.add(Integer.toString(Pid.get()));
	args.add("-s");
	args.add(Integer.toString(START_ACK.hashCode()));
	args.add("-t");
	args.add(Integer.toString(TestCase.getTimeoutSeconds()));
	for (int i = 0; i < programArgs.length; i++) {
	    args.add(programArgs[i]);
	}
	logger.log(Level.FINE, "funit-exec: {0}\n", args);
	return (String[]) args.toArray(new String[0]);
    }
    /**
     * Request that the process perform an exec.  Since the exec
     * starts a new program, this operation is not acknowledged.
     */
    public void requestExec() {
	signal(Sig.INT);
    }
    /**
     * Request that a random thread does an exec.
     */
    public void requestRandomExec() {
	signal(Sig.USR1);
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
  }
