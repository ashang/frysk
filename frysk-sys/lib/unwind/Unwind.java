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

public abstract class Unwind {
    static final Log fine = LogFactory.fine(Unwind.class);
    static final Log finest = LogFactory.finest(Unwind.class);
  
    abstract long createCursor(AddressSpace addressSpace,
			       long unwAddressSpace);
    abstract void destroyCursor(long unwCursor);
     
    abstract long createAddressSpace (ByteOrder byteOrder);
    abstract void destroyAddressSpace (long unwAddressSpace);
  
    abstract void setCachingPolicy(long unwAddressSpace, 
				   CachingPolicy cachingPolicy);
  
    abstract int isSignalFrame (long unwCursor);
  
    abstract int step (long unwCursor);
  
    abstract void getRegister(long unwCursor, Number regNum,
			      long offset, int length, byte[] word, int start);
    abstract void setRegister(long unwCursor, Number regNum,
			      long offset, int length, byte[] word, int start);
  
    abstract long getIP(long unwCursor);
    abstract long getSP(long unwCursor);
    abstract long getCFA(long unwCursor);
  
    abstract long copyCursor(long unwCursor);  
    abstract int getContext(long context);
 
    // FIXME: shouldn't be public.
    public abstract int createProcInfoFromElfImage(AddressSpace addressSpace,
						   long ip, 
						   boolean needUnwindInfo,
						   ElfImage elfImage,
						   ProcInfo procInfo);
 
    // FIXME: shouldn't be public.
    public abstract ElfImage createElfImageFromVDSO(AddressSpace addressSpace, 
						    long segbase, long hi, 
						    long mapoff);


    abstract long getProcInfo(long unwCursor);
    abstract void destroyProcInfo(long unwProcInfo);

    abstract long getStartIP(long unwProcInfo);
    abstract long getEndIP(long unwProcInfo);
    abstract long getLSDA(long unwProcInfo);
    abstract long getHandler(long unwProcInfo);
    abstract long getGP(long unwProcInfo);
    abstract long getFlags(long unwProcInfo);
    abstract int getFormat(long unwProcInfo);
    abstract int getUnwindInfoSize(long unwProcInfo);
    abstract long getUnwindInfo(long unwProcInfo);
}
