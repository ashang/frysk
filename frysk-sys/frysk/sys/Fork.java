// This file is part of the program FRYSK.
//
// Copyright 2005, 2007, 2008, Red Hat Inc.
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

import java.io.File;

/**
 * Create a child process (using fork) that immediately performs some
 * sort of exec.
 */

public final class Fork {
    private static native int spawn(File exe,
				    String in, String out, String err,
				    String[] args, long environ);
    private static native int ptrace(File exe,
				     String in, String out, String err,
				     String[] args, long environ);
    private static native int utrace(File exe,
				     String in, String out, String err,
				     String[] args, long environ);
    private static native int daemon(File exe,
				     String in, String out, String err,
				     String[] args, long environ);

    /**
     * Create a child process running EXE with arguments ARGS[0..].
     *
     * Also wire up IN, OUT, and ERR.
     */
    public static ProcessIdentifier exec(File exe,
					 String in, String out,
					 String err, String[] args) {
	return ProcessIdentifierFactory.create
	    (spawn(exe, in, out, err, args, 0));
    }
    /**
     * Create a child process running EXE with arguments ARGS[0..].
     *
     * Also wire up IN, OUT, and ERR.
     */
    public static ProcessIdentifier exec(String in, String out,
					 String err, String[] args) {
	return ProcessIdentifierFactory.create
	    (spawn(new File(args[0]), in, out, err, args, 0));
    }
    /**
     * Create a child process running ARGS[0] with arguments
     * ARGS[0..].
     */
    public static ProcessIdentifier exec(String[] args) {
	return ProcessIdentifierFactory.create
	    (spawn(new File(args[0]), null, null, null, args, 0));
    }

    /**
     * Create a child process running EXE with arguments ARGV[0...];
     * mark the process for tracing.
     *
     * Also wire up IN, OUT, and ERR.
     */
    public static ProcessIdentifier ptrace(File exe,
					   String in, String out, String err,
					   String[] args,
					   String libs) {
	Environ environ = new Environ();
	environ.setEnv("LD_LIBRARY_PATH", libs);
	long env = environ.putEnviron();
	return ProcessIdentifierFactory.create
	    (ptrace(exe, in, out, err, args, env));
    }
    /**
     * Create a child process running ARGS[0] with arguments
     * ARGS[0...]; mark the process for tracing.
     */
    public static ProcessIdentifier ptrace(String[] args) {
	return ProcessIdentifierFactory.create
	    (ptrace(new File(args[0]), null, null, null, args, 0));
    }

    /**
     * Create a child process running EXE with arguments ARGS[0...];
     * mark the process for utracing.
     *
     * Also wire up IN, OUT, and ERR.
     */
    public static ProcessIdentifier utrace(File exe,
					   String in, String out,
					   String err, String[] args) {
	return ProcessIdentifierFactory.create
	    (utrace(exe, in, out, err, args, 0));
    }
    /**
     * Create a child process running ARGS[0] with arguments
     * ARGV[0...]; mark the process for utracing.
     */
    public static ProcessIdentifier utrace(String[] args) {
	return ProcessIdentifierFactory.create
	    (utrace(new File(args[0]), null, null, null, args, 0));
    }

    /**
     * Create a "daemon" process running ARGV[0] with arguments
     * ARGV[1...]; a daemon has process ID 1 as its parent.
     */
    public static ProcessIdentifier daemon(File exe,
					   String in, String out, String err,
					   String[] argv) {
	return ProcessIdentifierFactory.create
	    (daemon(exe, in, out, err, argv, 0));
    }
    /**
     * Create a "daemon" process running ARGV[0] with arguments
     * ARGV[1...]; a daemon has process ID 1 as its parent.
     */
    public static ProcessIdentifier daemon(String[] argv) {
	return ProcessIdentifierFactory.create
	    (daemon(new File(argv[0]), null, null, null, argv, 0));
    }
}
