//This file is part of the program FRYSK.
//
//Copyright 2005, Red Hat Inc.
//
//FRYSK is free software; you can redistribute it and/or modify it
//under the terms of the GNU General Public License as published by
//the Free Software Foundation; version 2 of the License.
//
//FRYSK is distributed in the hope that it will be useful, but
//WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
//General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with FRYSK; if not, write to the Free Software Foundation,
//Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
//
//In addition, as a special exception, Red Hat, Inc. gives You the
//additional right to link the code of FRYSK with code not covered
//under the GNU General Public License ("Non-GPL Code") and to
//distribute linked combinations including the two, subject to the
//limitations in this paragraph. Non-GPL Code permitted under this
//exception must only link to the code of FRYSK through those well
//defined interfaces identified in the file named EXCEPTION found in
//the source code files (the "Approved Interfaces"). The files of
//Non-GPL Code may instantiate templates or use macros or inline
//functions from the Approved Interfaces without causing the
//resulting work to be covered by the GNU General Public
//License. Only Red Hat, Inc. may make changes or additions to the
//list of Approved Interfaces. You must obey the GNU General Public
//License in all respects for all of the FRYSK code and other code
//used in conjunction with FRYSK except the Non-GPL Code covered by
//this exception. If you modify this file, you may extend this
//exception to your version of the file, but you are not obligated to
//do so. If you do not wish to provide this exception without
//modification, you must delete this exception statement from your
//version and license this file solely under the GPL without
//exception.

package frysk.sys;
/**
 * Trace a process.
 */

public class Ptrace
{
	/* The thread explicitly used for ptrace calls */
	static volatile PtraceThread pt;
	
	/**
	 * Return our PtraceThread
	 */
	static synchronized PtraceThread getPt ()
	{
		if (pt == null) {
			pt = new PtraceThread();
			pt.startThread();
		}
		return pt;
	}
	
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
    public static native int peek(int peekRequest, int pid, String paddr);
    /**
     * Copy the word in data to child's addr.
     */
    public static native void poke(int pokeRequest, int pid, String paddr,
                    int data);
	/**
	 * Create an attached child process.  Uses PT_TRACEME.
	 */
	public static native int child (String in, String out, String err,
			String[] args);
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
	
	
	/**************************************************************************
	 * The new ptrace thread class                                            *
	 *************************************************************************/
	public static class PtraceThread extends Thread {
		
		volatile int request;	/* The ptrace operation request */
		volatile int pid;		/* The pid of the target process */
		volatile int error;		/* Error returned from the ptrace call */
		volatile long addr;		/* Address used by ptrace (Not yet used) */
		volatile long data;		/* Data or signals passed to the target */
		volatile long result;	/* Result of the ptrace call */
		volatile String in;		/* Standard in */
		volatile String out;	/* Standard out */
		volatile String err;	/* Standard error */
		volatile String[] args;	/* Arguments to be executed */
		
		Integer lock = new Integer (0); 	/* Used as a locking object */
		
		
		/**
		 * RUN - Main thread execution method
		 * Loop and wait for notification events, signalling that the core
		 * thread and relevant data are ready to have a ptrace call 
		 * executed and data returned.
		 */
		public void run() 
		{
			/* Critical section */
			synchronized (this) 
			{
				notify ();
				/* Continue serving requests */
				while (true) {
					try {
						wait ();
					}
					catch (InterruptedException ie)
					{
						System.out.println(ie.getMessage());
						System.exit(1);
					}
					/* The data is ready; call ptrace and let Core continue
					 * when ptrace returns. */
					callPtrace();
					notify();
				}
			}
		}
		
		/**
		 * Setup the data variables and when ready, signal the ptrace thread
		 * to continue while the core thread waits here for its completion.
		 */
		public long notifyPtraceThread(int request, int pid, long addr,
				long data) {
			
			/* Critical section */
			synchronized (lock) {
				
				this.request = request;
				this.pid = pid;
				this.addr = addr;
				this.data = data;
				this.error = 0;
				
				/* Critical section */
				synchronized (this) {
					
					/* All the data has been set, let the ptrace thread
					 * do its thing and Core will wait until its done. */
					notify();
					try {
						wait();
					} catch (InterruptedException ie) {
						throw new RuntimeException (ie);
					}
				}
				
				if (error != 0)
					Errno.throwErrno (error, "ptrace");
				return result;
			}
		}
		
		/**
		 * Assign variables representing standard in, out, and error
		 */
		public void assignFileDescriptors(String in, String out,
				String err, String[] args) {
			this.in = in;
			this.out = out;
			this.err = err;
			this.args = args;
		}
		
		/**
		 * Hack-job thread starting synchronization method 
		 */
		public synchronized void startThread () {
			pt.start();
			try {
				pt.wait();
			} catch (InterruptedException ie) {
				System.exit(1);
			}
		}
		
		/**
		 * Make the actual call to ptrace 
		 */
		public native void callPtrace();
		
	}
}
