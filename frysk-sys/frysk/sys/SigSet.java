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

public final class SigSet
{
    private RawDataManaged sigSet;
    private native RawDataManaged newSigSet (int[] sigs);
    RawDataManaged getSigSet ()
    {
	return sigSet;
    }
    /**
     * Create an empty signal set.
     */
    public SigSet ()
    {
	sigSet = newSigSet (null);
    }
    /**
     * Create a SigSet containing the signals in the array.
     */
    public SigSet (int[] sigs)
    {
	sigSet = newSigSet (sigs);
    }
    /**
     * Empty the signal set; return this.
     */
    public native SigSet empty ();
    /**
     * Fill the signal set; return this.
     */
    public native SigSet fill ();
    /**
     * Add sigNum to the SigSet; return this.
     */
    public native SigSet add (int sigNum);
    /**
     * Remove Sig from the SigSet (the underlying code uses
     * <tt>sigdelset</tt>, the name remove is more consistent with
     * java); return this.
     */
    public native SigSet remove (int sigNum);
    /**
     * Does this SigSet contain sigNum (the underlying code uses
     * <tt>sigismember</tt>, the name contains is more consistent with
     * java.
     */
    public native boolean contains (int sigNum);

    /**
     * Get the pending set of signals; return this
     */
    public native SigSet getPending ();
    /**
     * Suspend the thread, unblocking signals in SigSet; return this.
     */
    public native SigSet suspend ();

    /**
     * Block this SigSet's signals; if oldSet is non-null, return the
     * previous signal set; return this.
     */
    public native SigSet blockProcMask (SigSet oldSet);
    /**
     * Block this SigSet's signals; return this.
     */
    public SigSet blockProcMask ()
    {
	return blockProcMask (null);
    }
    /**
     * Unblock this SigSet's signals; if oldSet is non-null, return
     * the previous signal set; return this.
     */
    public native SigSet unblockProcMask (SigSet oldSet);
    /**
     * Unblock this SigSet's signals; return this.
     */
    public SigSet unblockProcMask ()
    {
	return unblockProcMask (null);
    }
    /**
     * Set the signal mask to this SigSet's signals; if oldSet is
     * non-null, return the previous signal set; return this.
     */
    public native SigSet setProcMask (SigSet oldSet);
    /**
     * Set the process signal mask to this SigSet's signals; return
     * this.
     */
    public SigSet setProcMask ()
    {
	return setProcMask (null);
    }
    /**
     * Get the current process signal mask; return this.
     */
    public native SigSet getProcMask ();
}
