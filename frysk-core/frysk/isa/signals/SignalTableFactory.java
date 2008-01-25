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

package frysk.isa.signals;

import frysk.isa.ISAMap;
import frysk.isa.ISA;

/**
 * Table of signals for for each known ISA.
 */

public class SignalTableFactory {
    private static class SignalEntry {
	private final StandardSignal signal;
	private final int[] value = new int[3];
	SignalEntry(int a0, int a1, int a2, StandardSignal signal) {
	    this.signal = signal;
	    this.value[0] = a0;
	    this.value[1] = a1;
	    this.value[2] = a2;
	}
	SignalEntry(int a, StandardSignal signal) {
	    this(a, a, a, signal);
	}
	void put(SignalTable signalTable, int index) {
	    if (value[index] >= 0) {
		signalTable.add(value[index], signal);
	    }
	}
    }
    private static final SignalEntry[] linuxSignals
	= new SignalEntry[] {
	new SignalEntry(1, StandardSignal.HUP),
	new SignalEntry(2, StandardSignal.INT),
	new SignalEntry(3, StandardSignal.QUIT),
	new SignalEntry(4, StandardSignal.ILL),
	new SignalEntry(6, StandardSignal.ABRT),
	new SignalEntry(8, StandardSignal.FPE),
	new SignalEntry(9, StandardSignal.KILL),
	new SignalEntry(11, StandardSignal.SEGV),
	new SignalEntry(13, StandardSignal.PIPE),
	new SignalEntry(14, StandardSignal.ALRM),
	new SignalEntry(15, StandardSignal.TERM),
	new SignalEntry(30,10,16, StandardSignal.USR1),
	new SignalEntry(31,12,17, StandardSignal.USR2),
	new SignalEntry(20,17,18, StandardSignal.CHLD),
	new SignalEntry(19,18,25, StandardSignal.CONT),
	new SignalEntry(17,19,23, StandardSignal.STOP),
	new SignalEntry(18,20,24, StandardSignal.TSTP),
	new SignalEntry(21,21,26, StandardSignal.TTIN),
	new SignalEntry(22,22,27, StandardSignal.TTOU),
	new SignalEntry(10,7,10, StandardSignal.BUS),
	new SignalEntry(27,27,29, StandardSignal.PROF),
	new SignalEntry(12,-1,12, StandardSignal.SYS),
	new SignalEntry(5, StandardSignal.TRAP),
	new SignalEntry(16,23,21, StandardSignal.URG),
	new SignalEntry(26,26,28, StandardSignal.VTALRM),
	new SignalEntry(24,24,30, StandardSignal.XCPU),
	new SignalEntry(25,25,31, StandardSignal.XFSZ),
	new SignalEntry(7,-1,7, StandardSignal.EMT),
	new SignalEntry(-1,16,-1, StandardSignal.STKFLT),
	new SignalEntry(23,29,22, StandardSignal.IO),
	new SignalEntry(29,30,19, StandardSignal.PWR),
	new SignalEntry(-1,-1,-1, StandardSignal.LOST),
	new SignalEntry(28,28,20, StandardSignal.WINCH),
	new SignalEntry(-1,31,-1, StandardSignal.SYS),
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
	ALPHA.add("SIGCLD", StandardSignal.CHLD)
	    .add("SIGINFO", StandardSignal.PWR)
	    .add("SIGPOLL", StandardSignal.IO)
	    .add("SIGIOT", StandardSignal.ABRT);
	IA32.add("SIGCLD", StandardSignal.CHLD)
	    .add("SIGINFO", StandardSignal.PWR)
	    .add("SIGPOLL", StandardSignal.IO)
	    .add("SIGIOT", StandardSignal.ABRT);
	MIPS.add("SIGCLD", StandardSignal.CHLD)
	    .add("SIGINFO", StandardSignal.PWR)
	    .add("SIGPOLL", StandardSignal.IO)
	    .add("SIGIOT", StandardSignal.ABRT);
    }

    private static final ISAMap isaSignals
	= new ISAMap("signals")
	.put(ISA.IA32, IA32)
	.put(ISA.X8664, IA32)
	.put(ISA.PPC32BE, IA32)
	.put(ISA.PPC64BE, IA32);

    public static SignalTable getSignalTable(ISA isa) {
	return (SignalTable) isaSignals.get(isa);
    }
}
