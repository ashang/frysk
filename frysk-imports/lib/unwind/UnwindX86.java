// This file is part of the program FRYSK.
//
// Copyright 2007, Red Hat Inc.
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

import gnu.gcj.RawData;
import gnu.gcj.RawDataManaged;

public class UnwindX86
    extends Unwind
{

  //@Override
  native RawDataManaged copyCursor (RawDataManaged cursor);

  //@Override
  native void destroyAddressSpace (RawData addressSpace);

  //@Override
  native int getContext (RawDataManaged context);

  //@Override
  native ProcName getProcName (RawDataManaged cursor, int maxNameSize);

  //@Override
  native int getRegister (RawDataManaged cursor, int regNum, 
                          byte[] word);

  //@Override
  native int setRegister(RawDataManaged cursor, int regNum, long word);
  
  //@Override
  native RawDataManaged initRemote (RawData addressSpace, Accessors accessors);

  //@Override
  native int isSignalFrame (RawDataManaged cursor);

  //@Override
  native int step (RawDataManaged cursor);
  
  //@Override
  native void setCachingPolicy (RawData addressSpace, 
                                CachingPolicy cachingPolicy);
  
  //@Override
  native RawData createAddressSpace (ByteOrder byteOrder);

  native ProcInfo getProcInfo (RawDataManaged cursor);

  native public ProcInfo createProcInfoFromElfImage (AddressSpace addressSpace, 
                                                     long ip, 
                                                     boolean needUnwindInfo, 
                                                     ElfImage elfImage, 
                                                     Accessors accessors);
  
  native public ElfImage createElfImageFromVDSO(AddressSpace addressSpace, 
                                                  long segbase, long hi, 
                                                  long mapoff, Accessors accessors);
  
  native public int getSP(RawDataManaged cursor, byte[] word);
  
  native long getStartIP(RawDataManaged procInfo);
  native long getEndIP(RawDataManaged procInfo);
  native long getLSDA(RawDataManaged procInfo);
  native long getHandler(RawDataManaged procInfo);
  native long getGP(RawDataManaged procInfo);
  native long getFlags(RawDataManaged procInfo);
  
  native int getFormat(RawDataManaged procInfo);
  native int getUnwindInfoSize(RawDataManaged procInfo);
  native RawData getUnwindInfo(RawDataManaged procInfo);
}
