// This file is part of the program FRYSK.
// 
// Copyright 2005, 2006, 2007, 2008, Red Hat Inc.
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

package frysk.sys.ptrace;

import frysk.rsl.Log;
import frysk.sys.ProcessIdentifier;
import frysk.sys.Signal;

/**
 * Trace a process.
 */

public class Ptrace {
    private static final Log fine = Log.fine(Ptrace.class);

    /**
     * Attach to the process specified by PID.
     */
    public static void attach(ProcessIdentifier pid) {
	fine.log("attach", pid);
	attach(pid.intValue());
    }
    private static native void attach(int pid);

    /**
     * Detach from the process specified by PID.
     */
    public static void detach(ProcessIdentifier pid, Signal signal) {
	fine.log("detach", pid, "signal", signal);
	detach(pid.intValue(), signal.intValue());
    }
    private static native void detach(int pid, int sig);

    /**
     * Single-step (instruction step) the process specified by PID, if
     * SIG is non-zero, deliver the signal.
     */
    public static void singleStep(ProcessIdentifier pid, Signal signal) {
	fine.log("signleStep", pid, "signal", signal);
	singleStep(pid.intValue(), signal.intValue());
    }
    private static native void singleStep(int pid, int sig);

    /**
     * Continue the process specified by PID, if SIG is non-zero,
     * deliver the signal.
     */
    public static void cont(ProcessIdentifier pid, Signal signal) {
	fine.log("cont", pid, "signal", signal);
	cont(pid.intValue(), signal.intValue());
    }
    private static native void cont(int pid, int signal);

    /**
     * Continue the process specified by PID, stopping when there is a
     * system-call; if SIG is non-zero deliver the signal.
     */
    public static void sysCall(ProcessIdentifier pid, Signal signal) {
	fine.log("sysCall", pid, "signal", signal);
	sysCall(pid.intValue(), signal.intValue());
    }
    private static native void sysCall(int pid, int sig);

    /**
     * Fetch the auxilary information associated with PID's last WAIT
     * event.
     */ 
    public static long getEventMsg(ProcessIdentifier pid) {
	fine.log("getEventMsg", pid, "...");
	long ret = getEventMsg(pid.intValue());
	fine.log("... getEventMsg", pid, "returns", ret);
	return ret;
    }
    private static native long getEventMsg(int pid);

    /**
     * Set PID's trace options.  OPTIONS is formed by or'ing the
     * values returned by the option* methods below.
     */
    public static void setOptions(ProcessIdentifier pid, long options) {
	fine.log("setOptions", pid, "options", options);
	setOptions(pid.intValue(), options);
    }
    private static native void setOptions (int pid, long options);

    /**
     * Return the bitmask for enabling clone tracing.
     */
    public static final long OPTION_CLONE = optionTraceClone();
    private static native long optionTraceClone();
    /**
     * Return the bitmask for enabling fork tracing.
     */
    public static final long OPTION_FORK = optionTraceFork();
    private static native long optionTraceFork();
    /**
     * Return the bitmask for enabling exit tracing.
     */
    public static final long OPTION_EXIT = optionTraceExit();
    private static native long optionTraceExit();
    /**
     * Return the bitmask for enabling SYSGOOD(?} tracing.
     */ 
    public static final long OPTION_SYSGOOD = optionTraceSysgood();
    private static native long optionTraceSysgood();
    /**
     * Return the bitmask for enabling exec tracing.
     */
    public static final long OPTION_EXEC = optionTraceExec();
    private static native long optionTraceExec();
}
