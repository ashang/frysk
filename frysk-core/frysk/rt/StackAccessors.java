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
import java.util.Arrays;
import java.util.logging.Logger;
import java.util.logging.Level;

import frysk.proc.Isa;
import frysk.proc.Task;
import frysk.sys.Execute;
import frysk.sys.Server;
import frysk.sys.proc.MapsBuilder;

import lib.dw.SymbolBuilder;
import lib.dw.Dwfl;
import lib.dw.DwflModule;

import lib.unwind.Accessors;
import lib.unwind.AddressSpace;
import lib.unwind.Cursor;
import lib.unwind.ElfImage;
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

  // procInfo is a wrapper for a RawDataManaged object, keep a reference
  // to it for as long as needed.
  ProcInfo procInfo;

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
    logger.log(Level.FINE, "entering accessMem reading memory at 0x{0}\n",
	       Long.toHexString(addr));

    int wordSize = myTask.getIsa().getWordSize();
    switch (wordSize)
      {
      case 4:
      case 8:
	logger.log(Level.FINEST, "In wordsize case: {0} valp.length: {1}\n",
		   new Object[] { new Integer(wordSize),
				 new Integer(valp.length) });
	myTask.getMemory().get(addr, valp, 0, valp.length);
	break;
      default:
	logger.log(Level.FINEST, "In wordSize case: default\n");
	throw new RuntimeException("Not implemented for this word length yet");
      }

    logger.log(Level.FINE, "exiting accessMem: read value: 0x{0}\n",
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
    logger.log(
	       Level.FINE,
	       "Entering findProcInfo {0}, ip: {1}\n",
	       new Object[] { addressSpace.getUnwinder(), Long.toHexString(ip) });

    ElfImage elfImage = getElfImage(ip);
    logger.log(Level.FINEST, "Obtained elfImage: {0}\n", elfImage);
    procInfo = addressSpace.getUnwinder().createProcInfoFromElfImage(
								     addressSpace,
								     ip,
								     needUnwindInfo,
								     elfImage,
								     this);

    logger.log(Level.FINE, "post procInfo {0}\n", procInfo);
    return procInfo;
  }

  // @Override
  public int getDynInfoListAddr (byte[] dilap)
  {
    // Need to tell ptrace thread to perform the getDynInfoListAddr
    // operation.
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
    Server.request(executer);
    logger.log(Level.FINE, "ret: {0}\n", new Integer(executer.ret));
    if (executer.ret < 0)
      Arrays.fill(dilap, (byte) 0);
    return executer.ret;
  }

  private DwflModule getModuleFromAddress (long addr)
  {
    logger.log(Level.FINE, "Looking for addr: {0}\n", Long.toHexString(addr));
    Dwfl dwfl = null;
    dwfl = new Dwfl(myTask.getProc().getPid());

    if (dwfl == null)
      {
	logger.log(Level.FINE, "Dwfl was null");
	return null;
      }

    return dwfl.getModule(addr);

  }

  // @Override
  public ProcName getProcName (long addr, int maxNameSize)
  {
    logger.log(Level.FINE, "entering getProcName addr: {0}, maxNameSize: {1}\n",
               new Object[] {Long.toHexString(addr), new Integer(maxNameSize)});
    // Need to tell ptrace thread to perform the findProcInfo operation.
    class ExecuteGetProcName 
        implements Execute, SymbolBuilder
    {
      ProcName procName;
      long addr;
      
      ExecuteGetProcName (long addr)
      {
        this.addr = addr;
      }
      
      public void symbol (String name, long value, long size, int type,
			  int bind, int visibility)
      {
	  procName = new ProcName(addr-value, name);
      }
      
      public void execute ()
      {

	DwflModule dwflModule = getModuleFromAddress(addr);

	if (dwflModule != null)
	  {
	    dwflModule.getSymbol(addr, this);

	    logger.log(Level.FINE, "ProcName is: {0}\n", procName);
	  }

	if (procName == null)
	  procName = new ProcName(- lib.unwind.Error.UNW_EUNSPEC_);
      }
    }
    ExecuteGetProcName executer = new ExecuteGetProcName(addr);
    Server.request(executer);
    return executer.procName;
  }

  // @Override
  public void putUnwindInfo (final ProcInfo procInfo)
  {
    // No longer need to hold procInfo.
    this.procInfo = null;
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
    Server.request(executer);
    return executer.ret;
  }

  private ElfImage getElfImage (long addr)
  {
    class ProcMapsReader
	extends MapsBuilder
    {
      long addr;

      long addressLow;
      long addressHigh;

      long offset;

      String elfImageName;

      byte[] mapsLocal;

      ProcMapsReader (long addr)
      {
	super();
	this.addr = addr;
      }

      // @Override
      public void buildBuffer (byte[] maps)
      {
	// Safe a refernce to the raw maps.
	mapsLocal = maps;
	maps[maps.length - 1] = 0;
      }

      // @Override
      public void buildMap (long addressLow, long addressHigh,
			    boolean permRead, boolean permWrite,
			    boolean permExecute, boolean shared, long offset,
			    int devMajor, int devMinor, int inode,
			    int pathnameOffset, int pathnameLength)
      {
	if (addressLow <= addr && addr < addressHigh)
	  {
	    this.addressLow = addressLow;
	    this.addressHigh = addressHigh;
	    this.offset = offset;
	    byte[] filename = new byte[pathnameLength];

	    System.arraycopy(mapsLocal, pathnameOffset, filename, 0,
			     pathnameLength);
	    elfImageName = new String(filename);

	  }
      }

    }

    ProcMapsReader mapReader = new ProcMapsReader(addr);
    mapReader.construct(myTask.getProc().getPid());

    logger.log(Level.FINEST, "Elf image name: {0}, addressLow: {1}, "
			     + "offset: {2}\n",
	       new Object[] { mapReader.elfImageName,
			     Long.toHexString(mapReader.addressLow),
			     Long.toHexString(mapReader.offset) });
    
    ElfImage elfImage;
    if (mapReader.elfImageName.equals("") || mapReader.elfImageName.equals("[vdso]"))
      elfImage = addressSpace.getUnwinder().createElfImageFromVDSO(addressSpace, 
                                                                   mapReader.addressLow, 
                                                                   mapReader.addressHigh,
                                                                   mapReader.offset, this);
    else
      elfImage = ElfImage.mapElfImage(mapReader.elfImageName,
					     mapReader.addressLow,
					     mapReader.addressHigh,
					     mapReader.offset);
    return elfImage;
  }

}
