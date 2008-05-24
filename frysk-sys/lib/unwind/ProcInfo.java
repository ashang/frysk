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

public class ProcInfo {
    private final Unwind unwinder;
    final long unwProcInfo;
  
    ProcInfo(Unwind unwinder, long unwProcInfo) {
	this.unwinder = unwinder;
	this.unwProcInfo = unwProcInfo; 
    }
    protected void finalize() {
	unwinder.destroyProcInfo(unwProcInfo);
    }
  
    /**
     * Oops, there was no unwind info; for instance, the address is
     * invalid.  Fill-in and return the applicable failure indication.
     */
    public int fillNotAvailable() {
	return unwinder.fillProcInfoNotAvailable(unwProcInfo);
    }

    public int fillFromVDSO(AddressSpace addressSpace,
			    long addressLow, long addressHigh,
			    long offset, long ip,
			    boolean needUnwindInfo) {
	return unwinder.fillProcInfoFromVDSO(unwProcInfo, ip, needUnwindInfo,
					     addressSpace,
					     addressLow, addressHigh, offset);
    }

    public int fillFromElfImage(AddressSpace addressSpace,
				String name,
				long addressLow, long addressHigh,
				long offset, long ip,
				boolean needUnwindInfo) {
	return unwinder.fillProcInfoFromElfImage(unwProcInfo, ip,
						 needUnwindInfo,
						 addressSpace,
						 name, addressLow, addressHigh,
						 offset);
    }

    public long getStartIP() {
	return unwinder.getStartIP(unwProcInfo);
    }
  
    long getEndIP() {
	return unwinder.getEndIP(unwProcInfo);
    }
  
    long getLSDA() {
	return unwinder.getLSDA(unwProcInfo);
    }
  
    long getHandler() {
	return unwinder.getHandler(unwProcInfo);
    }
  
    long getGP() {
	return unwinder.getGP(unwProcInfo);
    }
  
    long getFlags() {
	return unwinder.getFlags(unwProcInfo);
    }
  
    int getFormat() {
	return unwinder.getFormat(unwProcInfo);
    }
  
    int getUnwindInfoSize() {
	return unwinder.getUnwindInfoSize(unwProcInfo);
    }
  
    long getUnwindInfo() {
	return unwinder.getUnwindInfo(unwProcInfo);
    }
  
    public String toString() {
	return "ProcInfo startIP: 0x" + Long.toHexString(getStartIP()) 
	    + " endIP: 0x" + Long.toHexString(getEndIP())
	    + " lsda: " + getLSDA()
	    + " handler: " + getHandler()
	    + " gp: " + getGP()
	    + " flags: " + getFlags()
	    + " format: " + getFormat()
	    + " unwindInfoSize: " + getUnwindInfoSize() 
	    + " unwindInfo: " + getUnwindInfo();
    }
}
