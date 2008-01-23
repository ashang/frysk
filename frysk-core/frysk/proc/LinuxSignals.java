// This file is part of the program FRYSK.
//
// Copyright 2008, Red Hat Inc.
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

import frysk.isa.ISAMap;
import frysk.isa.ISA;

/**
 * Table of signals for for each known ISA.
 */

class LinuxSignals {
    private static class SignalEntry {
	private final String name;
	private final int[] value = new int[3];
	private final String description;
	SignalEntry(String name, int a0, int a1, int a2, String description) {
	    this.name = name;
	    this.value[0] = a0;
	    this.value[1] = a1;
	    this.value[2] = a2;
	    this.description = description;
	}
	SignalEntry(String name, int a, String description) {
	    this(name, a, a, a, description);
	}
	void put(SignalTable signalTable, int index) {
	    if (value[index] >= 0)
		signalTable.add(value[index], name, description);
	}
    }
    private static final SignalEntry[] linuxSignals
	= new SignalEntry[] {
	new SignalEntry("SIGHUP", 1, "Hangup detected on controlling terminal or death of controlling process"),
	new SignalEntry("SIGINT", 2, "Interrupt from keyboard"),
	new SignalEntry("SIGQUIT", 3, "Quit from keyboard"),
	new SignalEntry("SIGILL", 4, "Illegal Instruction"),
	new SignalEntry("SIGABRT", 6, "Abort signal from abort(3)"),
	new SignalEntry("SIGFPE", 8, "Floating point exception"),
	new SignalEntry("SIGKILL", 9, "Kill signal"),
	new SignalEntry("SIGSEGV", 11, "Invalid memory reference"),
	new SignalEntry("SIGPIPE", 13, "Broken pipe: write to pipe with no readers"),
	new SignalEntry("SIGALRM", 14, "Timer signal from alarm(2)"),
	new SignalEntry("SIGTERM", 15, "Termination signal"),
	new SignalEntry("SIGUSR1", 30,10,16, "User-defined signal 1"),
	new SignalEntry("SIGUSR2", 31,12,17, "User-defined signal 2"),
	new SignalEntry("SIGCHLD", 20,17,18, "Child stopped or terminated"),
	new SignalEntry("SIGCONT", 19,18,25, "Continue if stopped"),
	new SignalEntry("SIGSTOP", 17,19,23, "Stop process"),
	new SignalEntry("SIGTSTP", 18,20,24, "Stop typed at tty"),
	new SignalEntry("SIGTTIN", 21,21,26, "tty input for background process"),
	new SignalEntry("SIGTTOU", 22,22,27, "tty output for background process"),
	new SignalEntry("SIGBUS", 10,7,10, "Bus error (bad memory access)"),
	new SignalEntry("SIGPOLL", 23,29,22, "IO event (Sys V). Synonym of SIGIO"),
	new SignalEntry("SIGPROF", 27,27,29, "Profiling timer expired"),
	new SignalEntry("SIGSYS", 12,-1,12, "Bad argument to routine (SVr4)"),
	new SignalEntry("SIGTRAP", 5, "Trace/breakpoint trap"),
	new SignalEntry("SIGURG", 16,23,21, "Urgent condition on socket (4.2BSD)"),
	new SignalEntry("SIGVTALRM", 26,26,28, "Virtual alarm clock (4.2BSD)"),
	new SignalEntry("SIGXCPU", 24,24,30, "CPU time limit exceeded (4.2BSD)"),
	new SignalEntry("SIGXFSZ", 25,25,31, "File size limit exceeded (4.2BSD)"),
	new SignalEntry("SIGIOT", 6, "IOT trap. A synonym for SIGABRT"),
	new SignalEntry("SIGEMT", 7,-1,7, ""),
	new SignalEntry("SIGSTKFLT", -1,16,-1, "Stack fault on coprocessor (unused)"),
	new SignalEntry("SIGIO", 23,29,22, "I/O now possible (4.2BSD)"),
	new SignalEntry("SIGCLD", -1,-1,18, "A synonym for SIGCHLD"),
	new SignalEntry("SIGPWR", 29,30,19, "Power failure (System V)"),
	new SignalEntry("SIGINFO", 29,-1,-1, "synonym for SIGPWR"),
	new SignalEntry("SIGLOST", -1,-1,-1, "File lock lost"),
	new SignalEntry("SIGWINCH", 28,28,20, "Window resize signal (4.3BSD, Sun)"),
	new SignalEntry("SIGSYS", -1,31,-1, "Unused signal (will be SIGSYS)"),
    };
    public static final SignalTable ALPHA = new SignalTable();
    public static final SignalTable SPARC = ALPHA;
    public static final SignalTable IA32 = new SignalTable();
    public static final SignalTable PPC = IA32;
    public static final SignalTable SH = IA32;
    public static final SignalTable MIPS = new SignalTable();
    static {
	for (int i = 0; i < linuxSignals.length; i++) {
	    linuxSignals[i].put(ALPHA, 0);
	    linuxSignals[i].put(IA32, 1);
	    linuxSignals[i].put(MIPS, 2);
	}
    }

    private static final ISAMap isaSignals
	= new ISAMap("signals")
	.put(ISA.IA32, IA32)
	.put(ISA.X8664, IA32)
	.put(ISA.PPC32BE, IA32)
	.put(ISA.PPC64BE, IA32);

    static SignalTable getSignalTable(ISA isa) {
	return (SignalTable) isaSignals.get(isa);
    }
}
