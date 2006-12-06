// This file is part of the program FRYSK.
//
// Copyright 2005, Red Hat Inc.
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

package frysk.util;

import inua.util.PrintWriter;

import java.util.Set;

import frysk.proc.Syscall;
import frysk.proc.SyscallEventInfo;
import frysk.proc.Task;

/**
 * This is a simple SyscallHandler which prints information about
 * syscalls.  It will print a partial line when a system call is
 * entered, and then print the return result (and a newline) when
 * the system call returns.
 */
public class StracePrinter implements SyscallHandler
{
	private Set tracedCalls;
	private PrintWriter writer;

	/**
	 * Create a new printer given the output writer and a set of
	 * system call names.  If tracedCalls is null, all system calls
	 * will be printed.  Otherwise a given system call will be printed
	 * only if its name appears in tracedCalls.
	 * @param writer the print writer
	 * @param tracedCalls a set of system call names, or null
	 */
	public StracePrinter(PrintWriter writer, Set/*<String>*/ tracedCalls)
	{
		this.writer = writer;
		this.tracedCalls = tracedCalls;
	}

	/**
	 * Called on system call enter and exit.
	 */
	public void handle(Task task, SyscallEventInfo syscallEventInfo, int when)
	{
		Syscall syscall = syscallEventInfo.getSyscall(task);

		if (tracedCalls == null || tracedCalls.contains(syscall.getName()))
		{
			writer.print(task.getProc().getPid() + "." + task.getTid() + " ");
			if (when == SyscallEventInfo.ENTER)
				syscall.printCall(writer, task, syscallEventInfo);
			else
				syscall.printReturn(writer, task, syscallEventInfo);
			writer.flush();
		}
	}
}
