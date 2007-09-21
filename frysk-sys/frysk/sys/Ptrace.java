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

/**
 * Trace a process.
 */

public class Ptrace
{
    /**
     * Attach to the process specified by PID.
     */
    public static native void attach (int pid);
    /**
     * Attach to the process specified by PID.
     */
    public static void attach (ProcessIdentifier pid)
    {
	attach(pid.hashCode());
    }
    /**
     * Detach from the process specified by PID.
     */
    public static native void detach(int pid, int sig);
    /**
     * Detach from the process specified by PID.
     */
    public static void detach (ProcessIdentifier pid, int sig)
    {
	detach(pid.hashCode(), sig);
    }
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
     * A ptrace register set that is transfered to/from PID in bulk.
     */
    public static class RegisterSet
    {
	protected final int ptLength;
	protected final int ptGet;
	protected final int ptSet;
	RegisterSet (int ptLength, int ptGet, int ptSet)
	{
	    this.ptLength = ptLength;
	    this.ptGet = ptGet;
	    this.ptSet = ptSet;
	}
	/**
	 * Return the size of the register set in bytes.
	 */
	public int length ()
	{
	    return ptLength;
	}
	/**
	 * Fetch PID's register set into DATA.
	 */
	public native void get (int pid, byte[] data);
	/**
	 * Store PID's registers from DATA.
	 */
	public native void set (int pid, byte[] data);
	private static native RegisterSet regs ();
	private static native RegisterSet fpregs ();
	private static native RegisterSet fpxregs ();
	public static final RegisterSet REGS = regs ();
	public static final RegisterSet FPREGS = fpregs ();
	public static final RegisterSet FPXREGS = fpxregs ();
    }

    /**
     * A ptrace address space, that can be peeked or poked a "word" at
     * a time.
     */
    public static class AddressSpace
    {
	protected final String name;
	protected final int ptPeek;
	protected final int ptPoke;
	AddressSpace (String name, int ptPeek, int ptPoke)
	{
	    this.name = super.toString() + ":" + name;
	    this.ptPeek = ptPeek;
	    this.ptPoke = ptPoke;
	}
	public String toString ()
	{
	    return name;
	}
	public native long length ();
	/**
	 * Fetch a byte at ADDR of process PID.
	 */
	public native int peek (int pid, long addr);
	/**
	 * Store the byte at ADDR of process PID.
	 */
	public native void poke (int pid, long addr, int data);
	/**
	 * Fetch up-to LENGTH bytes starting at ADDR of process PID,
	 * store them in BYTES, starting at OFFSET.
	 */
	public int peek (int pid, long addr, int length,
			 byte[] bytes, int offset) {
	    return transfer(pid, addr, length, bytes, offset, ptPeek);
	}
	/**
	 * Store up-to LENGTH bytes starting at ADDR of process PID,
	 * get values from BYTES, starting at OFFSET.
	 */
	public int poke (int pid, long addr, int length,
			 byte[] bytes, int offset) {
	    return transfer(pid, addr, length, bytes, offset, ptPoke);
	}
	private native final int transfer(int pid, long addr, int length,
					  byte[] bytes, int offset, int op);
	private static native AddressSpace text ();
	private static native AddressSpace data ();
	private static native AddressSpace usr ();
	public static final AddressSpace TEXT = text ();
	public static final AddressSpace DATA = data ();
	public static final AddressSpace USR = usr ();
    }
}
