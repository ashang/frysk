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
package frysk.proc;

import java.util.HashMap;
import inua.util.PrintWriter;

/**
 * A class that holds static information about a system call.  It is
 * used in combination with {@link SyscallEventInfo} and the
 * task to get information about a particular system call event.
 */
public abstract class Syscall
{
    int number;
    public final int numArgs;
    String name;
    public final String argList;
    boolean noreturn;

    Syscall (String name, int number, int numArgs, 
	     String argList, boolean noreturn)
    {
	this.name = name;
	this.number = number;
	this.numArgs = numArgs;
	this.argList = argList;
	this.noreturn = noreturn;
    }
    Syscall (String name, int number, int numArgs, String argList)
    {
	this.name = name;
	this.number = number;
	this.numArgs = numArgs;
	this.argList = argList;
    }        

    Syscall (String name, int number, int numArgs)
    {
	this (name, number, numArgs, "i:iiiiiiii");
    }
    
    Syscall (String name, int number)
    {
	this (name, number, 0, "i:");
    }

    Syscall (int number)
    {
	this ("<" + number + ">", number, 0, "i:");
    }

    /** Return the name of the system call.  */
    public String getName()
    {
        return name;
    }

    /** Return the system call's number.  */
    public int getNumber()
    {
        return number;
    }
    /** Return true if this object equals the argument.  */
    public boolean equals(Object other)
    {
      // Syscall objects are unique.
      return this == other;
    }

  abstract public long getArguments (Task task, int n);
  abstract public long getReturnCode (Task task);

  private void printStringArg (PrintWriter writer,
			       frysk.proc.Task task,
			       long addr)
  {
    if (addr == 0)
      writer.print ("0x0");
    else {
      writer.print ("\"");
      StringBuffer x = new StringBuffer ();
      task.memory.get (addr, 20, x);
      if (x.length () == 20)
	x.append ("...");
      x.append ("\"");
      writer.print (x);
    }
  }

    /**
     * Print a textual representation of a system call.
     * @param writer where to print the representation
     * @param task the task which supplies information about the
     * arguments
     * @param syscall the system call event info
     * @return writer
     */
    public PrintWriter printCall (PrintWriter writer,
			   frysk.proc.Task task,
			   SyscallEventInfo syscall)
    {
	long addr = 0;
	long arg = 0;
	writer.print ("<SYSCALL> " + name + " (");
	for (int i = 1; i <= numArgs; ++i) {
	    char fmt = argList.charAt (i + 1);
	    switch (fmt) {
	    case 'a':
	    case 'b':
	    case 'p':
		arg = syscall.arg (task, i);
		if (arg == 0)
		    writer.print ("NULL");
		else
		    writer.print ("0x" + Long.toHexString (arg));
		break;
	    case 's':
	    case 'S':
		addr = syscall.arg (task, i);
		printStringArg (writer, task, addr);
		break;
	    case 'i':
	    default:
		arg = (int)syscall.arg (task, i);
		writer.print (arg);
		break;
	    }
	    if (i < numArgs)
		writer.print (",");
	}
	if (noreturn)
	    writer.println (")");
	else
	    writer.print (")");
	return writer;
    }
    
    /**
     * Print a textual representation of the return result of a system
     * call.
     * @param writer where to print the representation
     * @param task the task which supplies information about the
     * return value
     * @param syscall the system call event info
     * @return writer
     */
    public PrintWriter printReturn (PrintWriter writer,
			     frysk.proc.Task task,
			     SyscallEventInfo syscallEventInfo)
    {
	long addr = 0;
	long arg = 0;
	
	writer.print (" = ");
	
	switch (argList.charAt (0)) {
	case 'a':
	case 'b':
	case 'p':
	    arg = syscallEventInfo.returnCode (task);
	    if (arg == 0)
		writer.println ("NULL");
	    else
		writer.println ("0x" + Long.toHexString (arg));
	    break;
	case 's':
	case 'S':
	    addr = syscallEventInfo.returnCode (task);
	    printStringArg (writer, task, addr);
	    writer.println ("");
	    break;
	case 'i':
	    arg = (int)syscallEventInfo.returnCode (task);
	    if (arg < 0) {
		writer.print ("-1");
		writer.println (" ERRNO=" + (-arg));
	    }
	    else
		writer.println (syscallEventInfo.returnCode (task));
	    break;
	default:
	    writer.println (syscallEventInfo.returnCode (task));
	    break;
	}
	return writer;
    }

   /**
   * Given a system call's name, this will return the corresponding
   * Syscall object.  If no predefined system call with that name
   * is available, this will return null.
   * @param name the name of the system call
   * @param syscallList system calls list
   * @return the Syscall object, or null
   */
  public static Syscall iterateSyscallByName (String name, Syscall[] syscallList)
  {
    for (int i = 0; i < syscallList.length; ++i)
      if (name.equals(syscallList[i].name))
	return syscallList[i];
    return null;
  }

  /**
   * Given a system call's number, this will return the corresponding
   * Syscall object.  Note that system call numbers are platform
   * dependent.  This will return a Syscall object in all cases; if
   * there is no predefined system call with the given number, a unique
   * "unknown" system call with the indicated number will be saved in
   * unknownSyscalls.
   * @param num the number of the system call
   * @param task the current task
   * @return the Syscall object
   */
  public static Syscall syscallByNum (int num, Task task)
  {
    Syscall[] syscallList;
    HashMap unknownSyscalls;

    try
      {
	syscallList = task.getIsa().getSyscallList ();
	unknownSyscalls = task.getIsa().getUnknownSyscalls ();
      }
    catch (Exception e)
      {
	throw new RuntimeException ("Could not get the isa");
      }

    if (num < 0)
      {
	throw new RuntimeException ("Negative Syscall Number:" + 
				    Integer.toString(num));
      }
    else if (num >= syscallList.length)
      {
	synchronized (Syscall.class)
	  {
	    Integer key = new Integer(num);
	    if (unknownSyscalls == null)
	      unknownSyscalls = new HashMap();
	    else if (unknownSyscalls.containsKey(key))
	      return (Syscall) unknownSyscalls.get(key);
	    
	    class UnknownSyscall
	      extends Syscall
	    {
	      UnknownSyscall (String name, int number)
	      {
		super (name, number);
	      }
	      
	      public long getArguments (Task task, int n)
	      {
		return 0;
	      }
	      public long getReturnCode (Task task)
	      {
		return 0;
	      }
	    }
	    Syscall result = new UnknownSyscall("UNKNOWN SYSCALL " 
						+ Integer.toString(num), num);

	    unknownSyscalls.put(key, result);
	    
	    return result;
	  }
      }
    else
      {
	return syscallList[num];
      }
  }

  /**
   * Given a system call's name, this will return the corresponding
   * Syscall object.  If no predefined system call with that name
   * is available, this will return null.
   * @param name the name of the system call
   * @param task the cuurent task
   * @return the Syscall object, or null
   * @throws NullPointerException if name is null
   */
  public static Syscall syscallByName (String name, Task task)
  {
    Syscall syscall;

    try
      {
	syscall = task.getIsa().syscallByName(name);
      }
    catch (Exception e)
      {
	throw new RuntimeException ("Could not get the name of isa");
      }

    return syscall;
  }
}
