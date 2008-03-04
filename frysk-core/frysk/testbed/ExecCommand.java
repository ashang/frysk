// This file is part of the program FRYSK.
//
// Copyright 2007, 2008, Red Hat Inc.
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

import frysk.config.Config;
import frysk.sys.Pid;
import frysk.junit.TestCase;
import java.util.LinkedList;
import java.io.File;

/**
 * Create a command line for invoking funit-exec.
 *
 * By chaining ExecCommands it is possible to construct a complext
 * sequence of exec calls.  For instance, new ExecCommand(new
 * ExecCommand()) will create an funit-exec command line that will
 * invoke funit-exec.
 */
public class ExecCommand {
    public final String[] argv;
    public final File exe;
    /**
     * Construct a command line for invoking the WORD_SIZED version of
     * funit-exec.  If THREADS is non-zero, create that many
     * additional threads when starting.  If EXE is non-null, invoke
     * that program instead of PROGRAM from PROGRAM_ARGS.
     */
    private ExecCommand(Executable funitExec, int threadCount,
			File exe, String[] programArgs) {
	this.exe = funitExec.getFile();
	LinkedList args = new LinkedList();
	args.add(funitExec.getFile().getAbsolutePath());
	if (threadCount > 0) {
	    args.add("-c");
	    args.add(Integer.toString(threadCount));
	}
	if (exe != null) {
	    args.add("-e");
	    args.add(exe.getAbsolutePath());
	    args.add("-b"); // Also brand exec
	}
	args.add("-m");
	args.add(Integer.toString(Pid.get().intValue()));
	args.add("-s");
	args.add(Integer.toString(SynchronizedOffspring.START_ACK.hashCode()));
	args.add("-t");
	args.add(Integer.toString(TestCase.getTimeoutSeconds()));
	args.add("--");
	for (int i = 0; i < programArgs.length; i++) {
	    args.add(programArgs[i]);
	}
	argv = new String[args.size()];
	args.toArray(argv);
    }

    /**
     * Create an funit-exec command that will repeatedly invoke
     * itself.
     */
    public ExecCommand() {
	this(Executable.DEFAULT);
    }
    /**
     * Create an funit-exec command that will repeatedly invoke
     * itself.
     */
    public ExecCommand(Executable funitExec) {
	this(funitExec, 0, funitExec.getFile(), new String[0]);
    }

    /**
     * Create an funit-exec command that is running COUNT threads, and
     * exec-s the specified ExecCommand.
     */
    public ExecCommand(int threadCount, ExecCommand command) {
	this(Executable.DEFAULT, threadCount, command.exe, command.argv);
    }
    /**
     * Create an WORD_SIZED funit-exec command that will invoke
     * COMMAND when requested.
     */
    public ExecCommand(Executable funitExec, ExecCommand command) {
	this(funitExec, 0, command.exe, command.argv);
    }

    /**
     * Create an funit-exec command that will exec the specified
     * program and argments.
     */
    public ExecCommand (String[] programAndArgs) {
	this(Executable.DEFAULT, 0, null, programAndArgs);
    }
    /**
     * Create an funit-exec command that is running THREAD_COUNT extra
     * threads, and invokes the specified PROGRAM_AND_ARGS.
     */
    public ExecCommand(int threadCount, String[] programAndArgs) {
	this(Executable.DEFAULT, threadCount, null, programAndArgs);
    }

    public static abstract class Executable {
	/**
	 * Return the executable
	 */
	abstract File getFile();
	public static final Executable BIT32 = new Executable() {
		File getFile() {
		    return Config.getPkgLib32File("funit-exec");
		}
	    };
	public static final Executable BIT64 = new Executable() {
		File getFile() {
		    return Config.getPkgLib64File("funit-exec");
		}
	    };
	public static final Executable DEFAULT = new Executable() {
		File getFile() {
		    return Config.getPkgLibFile("funit-exec");
		}
	    };
	public static final Executable ALIAS = new Executable() {
		File getFile() {
		    return Config.getPkgLibFile("funit-exec-alias");
		}
	    };
    }
}
