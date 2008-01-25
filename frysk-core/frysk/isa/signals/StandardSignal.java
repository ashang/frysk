// This file is part of the program FRYSK.
//
// Copyright 2008 Red Hat Inc.
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

package frysk.isa.signals;

/**
 * A standard or well known.  Consult the SignalTable to get the
 * corresponding ISA specific Signal value.
 */
public class StandardSignal {
    private final String name;
    private final String description;
    private StandardSignal(String name, String description) {
	this.name = name;
	this.description = description;
    }

    public String getName() {
	return name;
    }

    public String getDescription() {
	return description;
    }

    public boolean equals(Signal signal) {
	return description == signal.getDescription();
    }

    public static final StandardSignal ABRT
	= new StandardSignal("SIGABRT", "Abort signal from abort(3)");
    public static final StandardSignal ALRM
	= new StandardSignal("SIGALRM", "Timer signal from alarm(2)");
    public static final StandardSignal BUS
	= new StandardSignal("SIGBUS", "Bus error (misaligned memory access)");
    public static final StandardSignal CHLD
	= new StandardSignal("SIGCHLD", "Child stopped or terminated");
    public static final StandardSignal CONT
	= new StandardSignal("SIGCONT", "Continue if stopped");
    public static final StandardSignal EMT
	= new StandardSignal("SIGEMT", "Emulation Trap");
    public static final StandardSignal FPE
	= new StandardSignal("SIGFPE", "Floating point exception");
    public static final StandardSignal HUP
	= new StandardSignal("SIGHUP", "Hangup detected on controlling terminal or death of controlling process");
    public static final StandardSignal ILL
	= new StandardSignal("SIGILL", "Illegal Instruction");
    public static final StandardSignal INT
	= new StandardSignal("SIGINT", "Interrupt from keyboard");
    public static final StandardSignal IO
	= new StandardSignal("SIGIO", "I/O now possible");
    public static final StandardSignal KILL
	= new StandardSignal("SIGKILL", "Kill signal");
    public static final StandardSignal LOST
	= new StandardSignal("SIGLOST", "File lock lost");
    public static final StandardSignal PIPE
	= new StandardSignal("SIGPIPE", "Broken pipe");
    public static final StandardSignal PROF
	= new StandardSignal("SIGPROF", "Profiling timer expired");
    public static final StandardSignal PWR
	= new StandardSignal("SIGPWR", "Power failure");
    public static final StandardSignal QUIT
	= new StandardSignal("SIGQUIT", "Quit from keyboard");
    public static final StandardSignal SEGV
	= new StandardSignal("SIGSEGV", "Segmentation fault (invalid memory location)");
    public static final StandardSignal STKFLT
	= new StandardSignal("SIGSTKFLT", "Stack fault on coprocessor");
    public static final StandardSignal STOP
	= new StandardSignal("SIGSTOP", "Stop process");
    public static final StandardSignal SYS
	= new StandardSignal("SIGSYS", "Bad argument to routine");
    public static final StandardSignal TERM
	= new StandardSignal("SIGTERM", "Termination signal");
    public static final StandardSignal TRAP
	= new StandardSignal("SIGTRAP", "Trace/breakpoint trap");
    public static final StandardSignal TSTP
	= new StandardSignal("SIGTSTP", "Stop typed at TTY");
    public static final StandardSignal TTIN
	= new StandardSignal("SIGTTIN", "TTY input for background process");
    public static final StandardSignal TTOU
	= new StandardSignal("SIGTTOU", "TTY output for background process");
    public static final StandardSignal URG
	= new StandardSignal("SIGURG", "Urgent condition on socket");
    public static final StandardSignal USR1
	= new StandardSignal("SIGUSR1", "User-defined signal 1");
    public static final StandardSignal USR2
	= new StandardSignal("SIGUSR2", "User-defined signal 2");
    public static final StandardSignal VTALRM
	= new StandardSignal("SIGVTALRM", "Virtual alarm clock");
    public static final StandardSignal WINCH
	= new StandardSignal("SIGWINCH", "Window size changed");
    public static final StandardSignal XCPU
	= new StandardSignal("SIGXCPU", "CPU time limit exceeded");
    public static final StandardSignal XFSZ
	= new StandardSignal("SIGXFSZ", "File size limit exceeded");
    
}
