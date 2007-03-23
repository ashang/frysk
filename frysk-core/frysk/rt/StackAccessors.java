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
import frysk.sys.Execute;

import lib.unwind.Accessors;
import lib.unwind.AddressSpace;
import lib.unwind.Cursor;
import lib.unwind.ProcInfo;
import lib.unwind.ProcName;
import lib.unwind.PtraceAccessors;
import lib.unwind.ByteOrder;

public class StackAccessors
    extends Accessors
{

  PtraceAccessors ptraceAccessors;

  Task myTask;
  
  AddressSpace addressSpace;

  Logger logger = Logger.getLogger("frysk");

  StackAccessors (AddressSpace addressSpace, Task task, ByteOrder byteOrder)
  {
    this.addressSpace = addressSpace;
    ptraceAccessors = new PtraceAccessors(task.getProc().getPid(), byteOrder);
    myTask = task;
  }

  // @Override
  public int accessMem (long addr, byte[] valp, boolean write)
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

  // @Override
  public int accessFPReg (int regnum, byte[] fpvalp, boolean write)
  {
    // XXX: TODO.
    return 0;
  }

  // @Override
  public int accessReg (int regnum, byte[] valp, boolean write)
  {
    Isa isa = myTask.getIsa();
    String registerName = isa.getRegisterNameByUnwindRegnum(regnum);
    logger.log(Level.FINE,
               "Libunwind: reading from register {0}, regnum: {1}\n",
               new Object[] { registerName, new Long(regnum) });

    byte[] tmp = isa.getRegisterByName(registerName).getBytes(myTask);

    if (tmp.length != valp.length)
      {
        throw new RuntimeException("Register sizes don't match");
      }
    
    for (int i = 0; i < valp.length; i++)
      valp[i] = tmp[i];

    logger.log(Level.FINE, "accessReg: read value: 0x{0}\n",
               Long.toHexString(new BigInteger(valp).longValue()));

    return 0;
  }

  // @Override
  public ProcInfo findProcInfo (long ip, boolean needUnwindInfo)
  {
    // Need to tell ptrace thread to perform the findProcInfo operation.
    class ExecuteFindProc
        implements Execute
    {
      ProcInfo procInfo;
      long ip;
      boolean needUnwindInfo;
      
      ExecuteFindProc (long ip, boolean needUnwindInfo)
      {
        this.ip = ip;
        this.needUnwindInfo = needUnwindInfo;
      }
      
      public void execute ()
      {
        procInfo = ptraceAccessors.findProcInfo(ip, needUnwindInfo);
      }
    }
    ExecuteFindProc executer = new ExecuteFindProc(ip, needUnwindInfo);
    frysk.sys.Ptrace.requestExecute(executer);
    return executer.procInfo;
  }

  // @Override
  public int getDynInfoListAddr (byte[] dilap)
  {
    // Need to tell ptrace thread to perform the findProcInfo operation.
    class ExecuterGetDyn
        implements Execute
    {
      int ret;

      byte[] dilap;

      public void execute ()
      {
        ret = ptraceAccessors.getDynInfoListAddr(dilap);
      }

      public ExecuterGetDyn (byte[] dilap)
      {
        this.dilap = dilap;
      }
    }
    ExecuterGetDyn executer = new ExecuterGetDyn(dilap);
    frysk.sys.Ptrace.requestExecute(executer);
    return executer.ret;
  }

  // @Override
  public ProcName getProcName (long addr, int maxNameSize)
  {
    // Need to tell ptrace thread to perform the findProcInfo operation.
    class ExecuteGetProcName
        implements Execute
    {
      ProcName procName;
      long addr;
      int maxNameSize;
      
      ExecuteGetProcName (long addr, int maxNameSize)
      {
        this.addr = addr;
        this.maxNameSize = maxNameSize;
      }
      
      public void execute ()
      {
        procName = ptraceAccessors.getProcName(addr, maxNameSize);
      }
    }
    ExecuteGetProcName executer = new ExecuteGetProcName(addr, maxNameSize);
    frysk.sys.Ptrace.requestExecute(executer);
    return executer.procName;
  }

  // @Override
  public void putUnwindInfo (final ProcInfo procInfo)
  {
    // Need to tell ptrace thread to perform the findProcInfo operation.
    class ExecutePutUnwind
        implements Execute
    {
      public void execute ()
      {
        ptraceAccessors.putUnwindInfo(procInfo);
      }
    }
    frysk.sys.Ptrace.requestExecute(new ExecutePutUnwind());
  }

  // @Override
  public int resume (final Cursor cursor)
  {
    // Need to tell ptrace thread to perform the findProcInfo operation.
    class ExecuteResume
        implements Execute
    {
      int ret;

      public void execute ()
      {
        ret = ptraceAccessors.resume(cursor);
      }
    }
    ExecuteResume executer = new ExecuteResume();
    frysk.sys.Ptrace.requestExecute(executer);
    return executer.ret;
  }

}
