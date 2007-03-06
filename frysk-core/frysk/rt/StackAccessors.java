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

package frysk.rt;

import java.math.BigInteger;
import java.util.logging.Logger;
import java.util.logging.Level;

import frysk.proc.Isa;
import frysk.proc.Task;

import lib.unwind.PtraceAccessors;
import lib.unwind.ByteOrder;

public class StackAccessors
    extends PtraceAccessors
{

  Task myTask;
  
  Logger logger = Logger.getLogger("frysk");
  StackAccessors(Task task, ByteOrder byteOrder)
  {
    super(task.getProc().getPid(), byteOrder);
    myTask = task;
  }

  //@Override
  protected int accessMem (long addr, byte[] valp, boolean write)
  {
    logger.log(Level.FINE, "Libunwind: reading memory at 0x{0}\n",
               Long.toHexString(addr));

    int wordSize = myTask.getIsa().getWordSize();
    switch (wordSize)
      {
      case 4:
        logger.log(Level.FINEST, "In wordSize case: 4\n");
       myTask.getMemory().get(addr, valp, 0, valp.length);
        break;
      case 8:
        logger.log(Level.FINEST, "In wordsize case: 8\n");
       myTask.getMemory().get(addr, valp, 0, valp.length);
        break;
      default:
        logger.log(Level.FINEST, "In wordSize case: default\n");
        throw new RuntimeException("Not implemented for this word length yet");
      }
    
    logger.log(Level.FINE, "accessMem: read value: 0x{0}\n",  
               Long.toHexString(new BigInteger(valp).longValue()));
    
    return 0;
  }

  //@Override
  protected int accessFPReg (int regnum, byte[] fpvalp, boolean write)
  {
    return super.accessFPReg(regnum, fpvalp, write);
  }

  //@Override
  protected int accessReg (int regnum, byte[] valp, boolean write)
  {
    Isa isa = myTask.getIsa();
    String registerName = isa.getRegisterNameByUnwindRegnum(regnum);
    logger.log(Level.FINE, "Libunwind: reading from register {0}, regnum: {1}\n",
               new Object[] {registerName, new Long(regnum)});
    
    byte[] tmp = isa.getRegisterByName(registerName).getBytes(myTask);
   
    for (int i = 0; i < tmp.length; i++)
      valp[i] = tmp[i];
    
    logger.log(Level.FINE, "accessReg: read value: 0x{0}\n",  
               Long.toHexString(new BigInteger(valp).longValue()));
    
    return 0;
  }
  
}
