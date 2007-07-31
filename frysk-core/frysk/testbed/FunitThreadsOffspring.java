// This file is part of the program FRYSK.
//
// Copyright 2005, 2006, 2007, Red Hat Inc.
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

import frysk.sys.Pid;
import frysk.junit.TestCase;
import java.util.List;
import java.util.LinkedList;
import frysk.Config;

/**
 * Create a program that contains a large number of threads.
 */

public class FunitThreadsOffspring
    extends SynchronizedOffspring
{
    static public class Type {
	private Type() {}
	/**
	 * The threads in the created program should block.
	 */
	public static final Type CLONE = new Type();
	/**
	 * The threads in the created program should execute an infinite
	 * loop.
	 */
	public static final Type LOOP = new Type();
	/**
	 * The threads in the created program should repeatedly clone.
	 */
	public static final Type BLOCK = new Type();
	/**
	 * Build an funit-threads command to run.
	 */
    }
    /**
     * Construct the argument list for funit-threads with THREADS
     * thread-count and TYPE.
     */
    private static String[] funitThreadsCommand (int threads,
						 Type type) {
	List command = new LinkedList();
	
	command.add (Config.getPkgLibFile("funit-threads").getPath());
	if (type == Type.BLOCK)
	    command.add("--block");
	else if (type == Type.LOOP)
	    command.add("--loop");
	// Use getpid as this testsuite always runs the event loop
	// from the main thread (which has tid==pid).
	command.add(Integer.toString(Pid.get()));
	command.add(Integer.toString(START_ACK.hashCode()));
	command.add(Integer.toString(TestCase.getTimeoutSeconds()));
	command.add(Integer.toString(threads));
	String[] argv = new String[command.size()];
	command.toArray(argv);
	return argv;
    }

    /**
     * Create a program with a large number of threads, each thread
     * joining its predicessor, cloning creating a successor, and then
     * exiting (so its successor can join it).  At any time there is
     * somewhere between 1(main)+THREADS and 1+2*THREADS.
     */
    public FunitThreadsOffspring (int threads) {
	super(START_ACK, funitThreadsCommand(threads, Type.CLONE));
    }

    public FunitThreadsOffspring(int threads, Type type) {
	super(START_ACK, funitThreadsCommand(threads, type));
    }
}
