// This file is part of the program FRYSK.
//
// Copyright 2005, 2006, 2007 Red Hat Inc.
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

package frysk.isa.syscalls;

import java.util.WeakHashMap;
import frysk.proc.Task;

/**
 * System call database.
 */

public abstract class SyscallTable {

    /**
     * @return Syscall return a system-call representing the unknown
     * NUM.
     */
    Syscall unknownSyscall(long num) {
	synchronized (unknownSyscalls) {
	    Long key = new Long(num);
	    Syscall syscall = (Syscall)unknownSyscalls.get(key);
	    if (syscall == null) {
		syscall = new Syscall("UNKNOWN SYSCALL " + num, (int)num) {
			public long getArguments (Task task, int n) {
			    return 0;
			}
			public long getReturnCode (Task task) {
			    return 0;
			}
		    };
		unknownSyscalls.put(key, syscall);
	    }
	    return syscall;
	}
    }
    private static WeakHashMap unknownSyscalls = new WeakHashMap();

    /**
     * @return Syscall return system call object if the name could be 
     * found in syscallList, otherwise return null.
     */
    public abstract Syscall getSyscall(String Name);

    /**
     * Given a system call's name, this will return the corresponding
     * Syscall object.  If no predefined system call with that name is
     * available, this will return null.
     * @param name the name of the system call
     * @param syscallList system calls list
     * @return the Syscall object, or null
     */
    Syscall iterateSyscallByName (String name, Syscall[] syscallList) {
	for (int i = 0; i < syscallList.length; ++i)
	    if (name.equals(syscallList[i].getName()))
		return syscallList[i];
	return null;
    }

    /**
     * Return the system call responding to N; or NULL.
     */
    Syscall findSyscall(Syscall[] syscalls, long num) {
	if (num < 0)
	    return Syscall.INVALID;
	if (num >= syscalls.length)
	    return unknownSyscall(num);
	return syscalls[(int)num];
    }

    /**
     * Return the system call responding to N; or UNKNOWN.
     */
    Syscall findSubcall(Syscall[] subcalls, long num, Syscall unknown) {
	if (num < 0 || num >= subcalls.length)
	    return unknown;
	return subcalls[(int)num];
    }

    /**
     * Return the NUM'th system call; implemented using findSyscall.
     */
    abstract public Syscall getSyscall(long num);

    /**
     * Return the number of syscalls.
     */
    abstract public long getNumSyscalls();

    /** 
     * Assuming that TASK is at a system-call entry, return the system
     * call.
     * @param task the task that system call occurred
     * @return the Syscall object
     */
    public abstract Syscall getSyscall(Task task);
    
}
