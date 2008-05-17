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

package frysk.sys;

/**
 * A Signal Set, corresponds to <tt>sigset_t</tt>.
 */

public final class SignalSet {
    private long rawSet;
    private native static long malloc();
    private native static void free(long rawSet);

    /**
     * Return a pointer to the underlying sigset_t buffer.
     */
    long getRawSet() {
	return rawSet;
    }
    /**
     * Create an empty signal set.
     */
    public SignalSet() {
	rawSet = malloc();
    }
    /**
     * Create a SigSet containing the signals in the array.
     */
    public SignalSet(Signal[] sigs) {
	this();
        add(sigs);
    }
    /**
     * Create a SigSet containing the single signal.
     */
    public SignalSet (Signal sig) {
	this();
	add(sig);
    }

    protected void finalize() {
	if (rawSet != 0) {
	    free(rawSet);
	    rawSet = 0;
	}
    }

    /**
     * As a string.
     */
    public String toString() {
	Signal[] sigs = toArray();
	StringBuffer s = new StringBuffer("{");
	for (int i = 0; i < sigs.length; i++) {
	    if (i > 0)
		s.append(",");
	    s.append(sigs[i].toString());
	}
	s.append("}");
	return s.toString();
    }

    /**
     * As an array.
     */
    public Signal[] toArray() {
	Signal[] signals = new Signal[this.size()];
	int sig = 0;
	for (int i = 0; i < signals.length; i++) {
	    Signal signal;
	    do {
		sig++;
		signal = Signal.valueOf(sig);
	    } while (! this.contains(signal));
	    signals[i] = signal;
	}
	return signals;
    }

    /**
     * Empty the signal set; return this.
     */
    public SignalSet empty() {
	empty(rawSet);
	return this;
    }
    private static native void empty(long rawSet);
    /**
     * Fill the signal set; return this.
     */
    public SignalSet fill() {
	fill(rawSet);
	return this;
    }
    private static native void fill(long rawSet);
    /**
     * Add sigNum to the SignalSet; return this.
     */
    public SignalSet add(Signal sig) {
	add(rawSet, sig.intValue());
	return this;
    }
    private static native void add(long rawSet, int sig);
    /**
     * Add the array of signals to the SignalSet; return this.
     */
    public SignalSet add(Signal[] sigs) {
	for (int i = 0; i < sigs.length; i++) {
            if (sigs[i] != null)
		add(rawSet, sigs[i].intValue());
	}
	return this;
    }
    /**
     * The number of elements in the set.
     */
    public int size() {
	return size(rawSet);
    }
    private static native int size(long rawSet);
    /**
     * Remove Signal from the SignalSet (the underlying code uses
     * <tt>sigdelset</tt>, the name remove is more consistent with
     * java); return this.
     */
    public SignalSet remove(Signal sig) {
	remove(rawSet, sig.intValue());
	return this;
    }
    private static native void remove(long rawSet, int sig);
    /** 
     * Does this SignalSet contain sigNum (the underlying code uses
     * <tt>sigismember</tt>, the name contains is more consistent with
     * java.
     */
    public boolean contains(Signal sig) {
	return contains(rawSet, sig.intValue());
    }
    private static native boolean contains(long rawSet, int sig);

    /**
     * Set to the set of pending signals; return this
     */
    public SignalSet getPending() {
	getPending(rawSet);
	return this;
    }
    private static native void getPending(long rawSet);
    /**
     * Suspend the thread, unblocking signals in SignalSet; return this.
     */
    public SignalSet suspend() {
	suspend(rawSet);
	return this;
    }
    private static native void suspend(long rawSet);

    /**
     * Block this SignalSet's signals; return the previous signal set
     * in oldSet; return this.
     */
    public SignalSet blockProcMask(SignalSet oldSet) {
	blockProcMask(rawSet, oldSet.rawSet);
	return this;
    }
    private static native void blockProcMask(long rawSet, long oldSet);
    /**
     * Block this SignalSet's signals; return this.
     */
    public SignalSet blockProcMask() {
	blockProcMask(rawSet, 0);
	return this;
    }
    /**
     * Unblock this SignalSet's signals; return the previous signal
     * set in oldSet; return this.
     */
    public SignalSet unblockProcMask(SignalSet oldSet) {
	unblockProcMask(rawSet, oldSet.rawSet);
	return this;
    }
    private static native void unblockProcMask(long rawSet, long oldSet);
    /**
     * Unblock this SignalSet's signals; return this.
     */
    public SignalSet unblockProcMask() {
	unblockProcMask(rawSet, 0);
	return this;
    }
    /**
     * Set the thread's signal mask to this SignalSet's signals;
     * return the previous signal set in oldSet.
     */
    public SignalSet setProcMask(SignalSet oldSet) {
	setProcMask(rawSet, oldSet.rawSet);
	return this;
    }
    private static native void setProcMask(long rawSet, long oldSet);
    /**
     * Set the process signal mask to this SignalSet's signals; return
     * this.
     */
    public SignalSet setProcMask() {
	setProcMask(rawSet, 0);
	return this;
    }
    /**
     * Get the current process signal mask; return this.
     */
    public SignalSet getProcMask() {
	getProcMask(rawSet);
	return this;
    }
    private static native void getProcMask(long rawSet);
}
