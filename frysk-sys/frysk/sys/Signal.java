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

package frysk.sys;

/**
 * Host signal handling.
 *
 * Need to use native calls to find out the current host's signal
 * values.
 */

public class Signal implements Comparable {
    private final int sig;
    private final String name;

    private Signal(int sig, String name) {
	this.sig = sig;
	this.name = name;
    }

    private static native int alrm();
    public static final Signal ALRM = new Signal(alrm(), "SIGALRM");

    private static native int bus();
    public static final Signal BUS = new Signal(bus(), "SIGBUS");

    private static native int chld();
    public static final Signal CHLD = new Signal(chld(), "SIGCHLD");

    private static native int cont();
    public static final Signal CONT = new Signal(cont(), "SIGCONT");

    private static native int fpe();
    public static final Signal FPE = new Signal(fpe(), "SIGFPE");

    private static native int hup();
    public static final Signal HUP = new Signal(hup(), "SIGHUP");

    private static native int ill();
    public static final Signal ILL = new Signal(ill(), "SIGILL");

    private static native int int_();
    public static final Signal INT = new Signal(int_(), "SIGINT");

    private static native int io();
    public static final Signal IO = new Signal(io(), "SIGIO");

    private static native int kill();
    public static final Signal KILL = new Signal(kill(), "SIGKILL");

    private static native int none();
    public static final Signal NONE = new Signal(none(), "SIGNONE");

    private static native int prof();
    public static final Signal PROF = new Signal(prof(), "SIGPROF");

    private static native int pwr();
    public static final Signal PWR = new Signal(pwr(), "SIGPWR");

    private static native int segv();
    public static final Signal SEGV = new Signal(segv(), "SIGSEGV");

    private static native int stop();
    public static final Signal STOP = new Signal(stop(), "SIGSTOP");

    private static native int term();
    public static final Signal TERM = new Signal(term(), "SIGTERM");

    private static native int trap();
    public static final Signal TRAP = new Signal(trap(), "SIGTRAP");

    private static native int usr1();
    public static final Signal USR1 = new Signal(usr1(), "SIGUSR1");

    private static native int usr2();
    public static final Signal USR2 = new Signal(usr2(), "SIGUSR2");

    private static native int urg();
    public static final Signal URG = new Signal(urg(), "SIGURG");

    private static native int winch();
    public static final Signal WINCH = new Signal(winch(), "SIGWINCH");

    public String toString() {
	return "" + sig;
    }

    public String toPrint() {
	return name;
    }

    /**
     * Comparison operations.
     */
    public boolean equals(Object o) {
	return (o instanceof Signal
		&& ((Signal)o).sig == this.sig);
    }
    public boolean equals(int signum) {
	return sig == signum;
    }
    public int intValue() {
	return sig;
    }
    public int hashCode() {
	return sig;
    }
    public int compareTo(Object o) {
	Signal rhs = (Signal)o; // exception ok
	return rhs.sig - this.sig;
    }

    /**
     * Deliver SIG to process PID.
     */
    public final void kill(int pid) {
	kill(pid, sig);
    }
    private static native void kill(int pid, int signum);
    /**
     * Deliver SIGNAL to task (or thread) LWP.
     */
    public final void tkill(int lwp) {
	tkill(lwp, sig);
    }
    private static native void tkill(int lwp, int signum);

    /**
     * Momentarialy sets the signal handler for Sig to SIGIGN so that
     * any pending signals are discarded.
     */
    public final void drain() {
	drain(sig);
    }
    private static native void drain(int signum);

    public static Signal valueOf(int signum) {
	if (ALRM.equals(signum)) return ALRM;
	if (BUS.equals(signum)) return BUS;
	if (CONT.equals(signum)) return CONT;
	if (CHLD.equals(signum)) return CHLD;
	if (FPE.equals(signum)) return FPE;
	if (HUP.equals(signum)) return HUP;
	if (ILL.equals(signum)) return ILL;
	if (INT.equals(signum)) return INT;
	if (IO.equals(signum)) return IO;
	if (KILL.equals(signum)) return KILL;
	if (NONE.equals(signum)) return NONE;
	if (PROF.equals(signum)) return PROF;
	if (PWR.equals(signum)) return PWR;
	if (SEGV.equals(signum)) return SEGV;
	if (STOP.equals(signum)) return STOP;
	if (TERM.equals(signum)) return TERM;
	if (TRAP.equals(signum)) return TRAP;
	if (USR1.equals(signum)) return USR1;
	if (USR2.equals(signum)) return USR2;
	if (URG.equals(signum)) return URG;
	if (WINCH.equals(signum)) return WINCH;
	throw new NullPointerException("unknown signal: " + signum);
    }
}
