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

package frysk.proc;

import inua.util.PrintWriter;

/**
 * A class that holds static information about a system call.  It is
 * used in combination with {@link SyscallEventInfo} and the
 * task to get information about a particular system call event.
 */

public abstract class Syscall {
    public static final Syscall INVALID = new Syscall("<invalid>", -1) {
	    public long getArguments(Task task, int n) {
		return 0;
	    }
	    public long getReturnCode(Task task) {
		return 0;
	    }
	};

    private final int number;
    private final int numArgs;
    private final String name;
    private final String argList;
    private final boolean noReturn;

    Syscall (String name, int number, int numArgs,
	     String argList, boolean noReturn) {
	this.name = name;
	this.number = number;
	this.numArgs = numArgs;
	this.argList = argList;
	this.noReturn = noReturn;
    }

    Syscall (String name, int number, int numArgs, String argList) {
	this(name, number, numArgs, argList, false);
    }

    Syscall (String name, int number, int numArgs) {
	this(name, number, numArgs, "i:iiiiiiii");
    }

    Syscall (String name, int number) {
	this(name, number, 0, "i:");
    }

    Syscall (int number) {
	this("<" + number + ">", number, 0, "i:");
    }

    /** Return the name of the system call.  */
    public String getName() {
        return name;
    }

    /** Return the system call's number.  */
    public int getNumber() {
        return number;
    }
    /** Return the number of arguments.  */
    public int getNumArgs() {
	return numArgs;
    }
    /** Return the argument list.  */
    public String getArgList() {
	return argList;
    }
    /** Does the system call return a result?  */
    public boolean isNoReturn() {
	return noReturn;
    }

    /** Return true if this object equals the argument.  */
    public boolean equals(Object other) {
	// Syscall objects are unique.
	return this == other;
    }

    abstract public long getArguments (Task task, int n);
    abstract public long getReturnCode (Task task);

    private String extractStringArg (frysk.proc.Task task,
				     long addr) {
	if (addr == 0)
	    return "0x0";
	else {
	    StringBuffer x = new StringBuffer();
	    task.getMemory().get (addr, 20, x);
	    if (x.length () == 20)
		x.append ("...");
	    return "\"" + x.toString() + '\"';
	}
    }

    /**
     * Get call arguments as a vector of Objects.  Currently returns
     * simpy a vector of formatted strings.
     *
     * @param task the task which supplies information about the
     * arguments
     */
    public String[] extractCallArguments (frysk.proc.Task task) {
	String[] ret = new String[numArgs];

	for (int i = 0; i < numArgs; ++i) {
	    char fmt = argList.charAt (i + 2);
	    long arg = getArguments (task, i + 1);

	    switch (fmt) {
	    case 'a':
	    case 'b':
	    case 'p':
		if (arg == 0)
		    ret[i] = "NULL";
		else
		    ret[i] = "0x" + Long.toHexString (arg);
		break;

	    case 's':
	    case 'S':
		ret[i] = extractStringArg(task, arg);
		break;

	    case 'i':
	    default:
		ret[i] = "" + arg;
		break;
	    }
	}

	return ret;
    }

    /**
     * Print a textual representation of a system call.
     * @param writer where to print the representation
     * @param task the task which supplies information about the
     * arguments
     * @return writer
     */
    public PrintWriter printCall (PrintWriter writer,
				  frysk.proc.Task task) {
	String[] args = extractCallArguments(task);
	writer.print ("<SYSCALL> " + name + " (");
	for (int i = 0; i < args.length; ++i) {
	    writer.print (args[i]);
	    if (i < numArgs)
		writer.print (",");
	}
	if (noReturn)
	    writer.println (")");
	else
	    writer.print (")");
	return writer;
    }

    public String toString() {
	return (this.getClass()
		+"[name=" + getName()
		+ ",number=" + getNumber() + "]");
    }

    /**
     * Extract system call return value.  Currently returns formatted
     * string.
     */
    public String extractReturnValue(frysk.proc.Task task) {
	long retVal = getReturnCode(task);

	switch (argList.charAt (0)) {
	case 'a':
	case 'b':
	case 'p':
	    if (retVal == 0)
		return "NULL";
	    else
		return "0x" + Long.toHexString (retVal);

	case 's':
	case 'S':
	    return extractStringArg (task, retVal);

	case 'i':
	    if ((int)retVal < 0)
		return "-1 ERRNO=" + (-(int)retVal);

	    // fall-through
	default:
	    return "" + retVal;
	}
    }

    /**
     * Print a textual representation of the return result of a system
     * call.
     * @param writer where to print the representation
     * @param task the task which supplies information about the
     * return value
     * @return writer
     */
    public PrintWriter printReturn (PrintWriter writer,
				    frysk.proc.Task task) {
	writer.print (" = " + extractReturnValue(task));
	return writer;
    }

    /**
     * Given a system call's number, this will return the corresponding
     * Syscall object.  Note that system call numbers are platform
     * dependent.  This will return a Syscall object in all cases; if
     * there is no predefined system call with the given number, a unique
     * "unknown" system call with the indicated number will be created.
     *
     * @param num the number of the system call
     * @param task the current task
     * @return the Syscall object
     */
    public static Syscall syscallByNum(int num, Task task) {
	Syscall[] syscallList;

	SyscallTable syscallTable
	    = SyscallTableFactory.getSyscallTable(task.getISA());
	syscallList = syscallTable.getSyscallList();

	if (num < 0) {
	    return INVALID;
	} else if (num >= syscallList.length) {
	    return syscallTable.unknownSyscall(num);
	} else {
	    return syscallList[num];
	}
    }
}
