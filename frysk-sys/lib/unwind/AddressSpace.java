// This file is part of the program FRYSK.
//
// Copyright 2007, 2008, Red Hat Inc.
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

package lib.unwind;

public abstract class AddressSpace {
    /**
     * Marker so that it is possible to confirm that this is an
     * AddressSpace.
     */
    static final int MAGIC = 0xacce550a;
    final int magic = MAGIC;

    public final long unwAddressSpace;
    public final Unwind unwinder;
  
    public AddressSpace(Unwind unwinder, ByteOrder byteOrder) {
	this.unwinder = unwinder;
	unwAddressSpace = unwinder.createAddressSpace(byteOrder);
    }
    protected void finalize() {
	unwinder.destroyAddressSpace(unwAddressSpace);
    }

    public Cursor createCursor() {
	return new Cursor(this, unwinder.createCursor(this, unwAddressSpace),
			  unwinder); 
    }

    public void setCachingPolicy (CachingPolicy cachingPolicy) {
	unwinder.setCachingPolicy(unwAddressSpace, cachingPolicy);
    }
  
    /**
     * Locate the information needed to unwind a particular procedure.
     * @param ip the instruction-address inside the procedure whose
     * information is needed.
     * @param needUnwindInfo whether the format, unwind_info_size and unwind_info
     *  fields of the returned ProcInfo should be set.
     * @return A ProcInfo object holding the processes info.
     */
    public abstract int findProcInfo(long ip, boolean needUnwindInfo,
				     ProcInfo procInfo);

    /**
     * Used to free a ProcInfo object created with needUnwindInfo as
     * true.
     * @param procInfo the procInfo object to be freed.
     */
    public abstract void putUnwindInfo (ProcInfo procInfo);

    public abstract int accessMem (long addr, byte[] valp, boolean write);

    /**
     * See UnwindH.hxx:access_reg(write=false); only used for small
     * integer registers.
     */
    public abstract long getReg(Number regnum);
    /**
     * See UnwindH.hxx:access_reg(write=true); only used for small
     * integer registers.
     */
    public abstract void setReg(Number regnum, long regval);

    /**
     * Access LIBUNWIND Register REGNUM.
     */
    public abstract int accessReg(Number regnum, byte[] val, boolean write);
}
