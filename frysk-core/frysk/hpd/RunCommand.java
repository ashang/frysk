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

package frysk.hpd;

class RunCommand extends StartRun {
    // Used to synchronize with updateAttached method
    RunCommand() {
	super("run program and immediately attach",
	      "run <arguments*> || <--noargs>",
	      "The run command alllows the debugger to run a(any) program(s)"
	      + " that has(have) been previously loaded via a load or"
	      + " core command.  The run command can accept arguments to"
	      + " be passed to the executable or if arguments have been"
	      + " previously given and no arguments are desired for the"
	      + " next run, use '--noargs' and the executable(s) will not be"
	      + " passed any arguments.  The run command causes the executable"
	      + " to be run until the first breakpoint or until the process"
	      + " terminates either normally or abnormally.  Issuing the run"
	      + " command at any point in the debugging sequence cause the"
	      + " process(es) being worked on to tbe killed and a fresh copy"
	      + " to be reloaded and run until breakpoint or termination.");
    }

    public void interpret(CLI cli, Input cmd, Object options) {
	interpretRun(cli, cmd, options);
    }
}
