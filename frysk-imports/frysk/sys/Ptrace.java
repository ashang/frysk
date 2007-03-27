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

package frysk.sys;

import gnu.gcj.RawData;

/**
 * Trace a process.
 */

public class Ptrace
{
  /**
   * Constants describing register sets that can be read and written by ptrace.
   */
  public static final int REGS = 0;
  public static final int FPREGS = 1;
  public static final int FPXREGS = 2;

  /**
   * Attach to the process specified by PID.
   */
  public static native void attach (int pid);
  /**
   * Detach from the process specified by PID.
   */
  public static native void detach(int pid, int sig);
  /**
   * Detach from the process specified by PID.
   */
  public static void detach (int pid, Sig sig)
  {
    detach (pid, sig.hashCode ());
  }
  /**
   * Single-step (instruction step) the process specified by PID, if
   * SIG is non-zero, deliver the signal.
   */
  public static native void singleStep(int pid, int sig);
  /**
   * Continue the process specified by PID, if SIG is non-zero,
   * deliver the signal.
   */
  public static native void cont(int pid, int sig);
  /**
   * Continue the process specified by PID, stopping when there is a
   * system-call; if SIG is non-zero deliver the signal.
   */
  public static native void sysCall(int pid, int sig);
  /**
   * Fetch the auxilary information associated with PID's last WAIT
   * event.
   */ 
  public static native long getEventMsg(int pid);
    /**
     * Fetch the word located at paddr.
     */
    static long peek(int peekRequest, int pid, RawData paddr)
    {
	return ptrace.request (peekRequest, pid, paddr, 0);
    }
    /**
     * Copy the word in data to child's addr.
     */
    static void poke(int pokeRequest, int pid, RawData paddr, long data)
    {
	ptrace.request (pokeRequest, pid, paddr, data);
    }
  /**
   * Return the size (in bytes) of a register set.
   *
   * @param set the register set
   * @returns required size of buffer
   */
  public static native int registerSetSize(int set);
  
  /**
   * Read the entire contents of a register set.
   *
   * @param registerSet the register set constant.
   * @param pid pid of the task
   * @param data buffer for the register set
   */
  public static native void peekRegisters(int registerSet, int pid, 
					  byte[] data);

  /**
   * Write the entire contents of a register set.
   *
   * @param registerSet the register set constant.
   * @param pid pid of the task
   * @param data buffer for the register set
   */
  public static native void pokeRegisters(int registerSet, int pid, 
					  byte[] data); 
  
  /**
   * Set PID's trace options.  OPTIONS is formed by or'ing the
   * values returned by the option* methods below.
   */
  public static native void setOptions (int pid, long options);
  /**
   * Return the bitmask for enabling clone tracing.
   */
  public static native long optionTraceClone ();
  /**
   * Return the bitmask for enabling fork tracing.
   */
  public static native long optionTraceFork ();
  /**
   * Return the bitmask for enabling exit tracing.
   */
  public static native long optionTraceExit ();
  /**
   * Return the bitmask for enabling SYSGOOD(?} tracing.
   */ 
  public static native long optionTraceSysgood ();
  /**
   * Return the bitmask for enabling exec tracing.
   */
  public static native long optionTraceExec ();
	
	
    /**
     * Class to encapsulate a PTRACE requests sent to the server.
     */
    protected static class PtraceRequest
	implements Execute
    {
	int op;	/* The ptrace operation request */
	int pid;	/* The pid of the target process */
	RawData addr;	/* Address used by ptrace (Not yet used) */
	long data;	/* Data or signals passed to the target */
	long result;	/* Result of the ptrace call */

	/**
	 * Perform the PTRACE call.
	 */
	public native void execute ();

	/**
	 * Request a ptrace call.
	 */
	public synchronized long request (int op, int pid,
					  RawData addr, long data)
	{
	    this.op = op;
	    this.pid = pid;
	    this.addr = addr;
	    this.data = data;
	    Server.request (this);
	    return result;
	}
    }
    private static final PtraceRequest ptrace = new PtraceRequest ();
    protected static void request (int op, int pid, RawData addr, long data)
    {
	ptrace.request (op, pid, addr, data);
    }
    
    /**
     * Pass a fork request through to the server.
     */
    protected static class ForkRequest
	implements Execute
    {
	private String in;	/* Standard in */
	private String out;	/* Standard out */
	private String err;	/* Standard error */
	private String[] args;	/* Arguments to be executed */
	private int result;

	public void execute ()
	{
	    result = Fork.ptrace (in, out, err, args);
	}

	/**
	 * Request a fork using the server thread.
	 */
	public synchronized int request (String in, String out, String err,
					 String[] args)
	{
	    this.in = in;
	    this.out = out;
	    this.err = err;
	    this.args = args;
	    Server.request (this);
	    return result;
	}	
    }
    private static final ForkRequest fork = new ForkRequest ();
    /**
     * Create an attached child process.
     *
     * A linux kernel bug means that a PT_TRACEME child must be
     * created by the same thread that will do later do the attach.
     */
    public static int child (String in, String out, String err,
			     String[] args)
    {
	return fork.request (in, out, err, args);
    }

}
