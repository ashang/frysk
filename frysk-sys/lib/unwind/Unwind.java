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

import frysk.rsl.Log;
import frysk.rsl.LogFactory;
import gnu.gcj.RawDataManaged;
import gnu.gcj.RawData;

public abstract class Unwind {
    static final Log fine = LogFactory.fine(Unwind.class);
    static final Log finest = LogFactory.finest(Unwind.class);
  
    abstract long createCursor(AddressSpace addressSpace,
			       RawData unwAddrSpace);
    abstract void destroyCursor(long unwCursor);
     
    abstract RawData createAddressSpace (ByteOrder byteOrder);
  
    abstract void destroyAddressSpace (RawData addressSpace);
  
    abstract void setCachingPolicy(RawData addressSpace, 
				   CachingPolicy cachingPolicy);
  
    abstract int isSignalFrame (long unwCursor);
  
    abstract int step (long unwCursor);
  
    abstract ProcInfo getProcInfo (long unwCursor);
  
    abstract void getRegister(long unwCursor, Number regNum,
			      long offset, int length, byte[] word, int start);
    abstract void setRegister(long unwCursor, Number regNum,
			      long offset, int length, byte[] word, int start);
  
    abstract long getIP(long unwCursor);
    abstract long getSP(long unwCursor);
    abstract long getCFA(long unwCursor);
  
    abstract long copyCursor(long unwCursor);  
    abstract int getContext(RawDataManaged context);
 
    // FIXME: shouldn't be public.
    public abstract ProcInfo createProcInfoFromElfImage(AddressSpace addressSpace,
							long ip, 
							boolean needUnwindInfo,
							ElfImage elfImage);
 
    // FIXME: shouldn't be public.
    public abstract ElfImage createElfImageFromVDSO(AddressSpace addressSpace, 
						    long segbase, long hi, 
						    long mapoff);

    abstract long getStartIP(RawDataManaged procInfo);
    abstract long getEndIP(RawDataManaged procInfo);
    abstract long getLSDA(RawDataManaged procInfo);
    abstract long getHandler(RawDataManaged procInfo);
    abstract long getGP(RawDataManaged procInfo);
    abstract long getFlags(RawDataManaged procInfo);
 
    abstract int getFormat(RawDataManaged procInfo);
    abstract int getUnwindInfoSize(RawDataManaged procInfo);
    abstract RawData getUnwindInfo(RawDataManaged procInfo);
}
