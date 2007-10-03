// This file is part of the program FRYSK.
//
// Copyright 2005, 2006, Red Hat Inc.
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

import gnu.gcj.RawDataManaged;

/**
 * A Signal Set, corresponds to <tt>sigset_t</tt>.
 */

public final class SignalSet
{
    private RawDataManaged rawSet;
    private native RawDataManaged newRawSet ();

    /**
     * Return a pointer to the underlying sigset_t buffer.
     */
    RawDataManaged getRawSet ()
    {
	return rawSet;
    }
    /**
     * Create an empty signal set.
     */
    public SignalSet ()
    {
	rawSet = newRawSet ();
    }
    /**
     * Create a SigSet containing the signals in the array.
     */
    public SignalSet (Sig[] sigs)
    {
	this ();
	for (int i = 0; i < sigs.length; i++) {
	    add (sigs[i]);
	}
    }
    /**
     * Empty the signal set; return this.
     */
    public native SignalSet empty ();
    /**
     * Fill the signal set; return this.
     */
    public native SignalSet fill ();
    /**
     * Add sigNum to the SignalSet; return this.
     */
    public native SignalSet add (Sig sig);
    /**
     * Remove Sig from the SignalSet (the underlying code uses
     * <tt>sigdelset</tt>, the name remove is more consistent with
     * java); return this.
     */
    public native SignalSet remove (Sig sig);
    /** 
     * Does this SignalSet contain sigNum (the underlying code uses
     * <tt>sigismember</tt>, the name contains is more consistent with
     * java.
     */
    public native boolean contains (Sig sig);

    /**
     * Get the pending set of signals; return this
     */
    public native SignalSet getPending ();
    /**
     * Suspend the thread, unblocking signals in SignalSet; return this.
     */
    public native SignalSet suspend ();

    /**
     * Block this SignalSet's signals; if oldSet is non-null, return the
     * previous signal set; return this.
     */
    public native SignalSet blockProcMask (SignalSet oldSet);
    /**
     * Block this SignalSet's signals; return this.
     */
    public SignalSet blockProcMask ()
    {
	return blockProcMask (null);
    }
    /**
     * Unblock this SignalSet's signals; if oldSet is non-null, return
     * the previous signal set; return this.
     */
    public native SignalSet unblockProcMask (SignalSet oldSet);
    /**
     * Unblock this SignalSet's signals; return this.
     */
    public SignalSet unblockProcMask ()
    {
	return unblockProcMask (null);
    }
    /**
     * Set the signal mask to this SignalSet's signals; if oldSet is
     * non-null, return the previous signal set; return this.
     */
    public native SignalSet setProcMask (SignalSet oldSet);
    /**
     * Set the process signal mask to this SignalSet's signals; return
     * this.
     */
    public SignalSet setProcMask ()
    {
	return setProcMask (null);
    }
    /**
     * Get the current process signal mask; return this.
     */
    public native SignalSet getProcMask ();
}
