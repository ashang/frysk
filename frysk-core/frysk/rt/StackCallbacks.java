// This file is part of the program FRYSK.
//
// Copyright 2005, Red Hat Inc.
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

import frysk.proc.Isa;
import frysk.proc.Task;
import frysk.proc.TaskException;
import java.util.logging.Level;
import java.util.logging.Logger;
import lib.unwind.UnwindCallbacks;

public class StackCallbacks
  implements UnwindCallbacks
{
    static final Logger logger = Logger.getLogger ("frysk");//.rt");
  private Task myTask;

  private Isa isa;

  public StackCallbacks (Task myTask) throws TaskException
  {
    this.myTask = myTask;
    this.myTask.toString();
    isa = myTask.getIsa();
  }

  public long accessMem (long addressSpace, long addr)
  {
    logger.log(Level.FINE, "Libunwind: reading memory at 0x"
                                + Long.toHexString(addr) + "\n");

    long value;

    switch (isa.getWordSize())
      {
      case 4:
	value = myTask.getMemory().getInt(addr);
	break;
      case 8:
	value = myTask.getMemory().getLong(addr);
	break;
      default:
	throw new RuntimeException("Not implemented for this word length yet");
      }

    logger.log(Level.FINE, "Libunwind: read value 0x"
                                + Long.toHexString(value) + "\n");
    return value;
  }

  public void writeMem (long as, long addr, long value)
  {
    logger.log(Level.FINE, "Libunwind: writing value 0x"
                                + Long.toHexString(value)
                                + " to memory address 0x"
                                + Long.toHexString(addr) + "\n");
    throw new RuntimeException("Not implemented in core yet");
  }

  public long accessReg (long as, long regnum)
  {
    String registerName = isa.getRegisterNameByUnwindRegnum(regnum);
    logger.log(Level.FINE, "Libunwind: reading from register "
                                + registerName + "\n");

    long value = isa.getRegisterByName(registerName).get(myTask);

    logger.log(Level.FINE, "Libunwind: read value 0x"
                                + Long.toHexString(value) + "\n");
    return value;
  }

  public void writeReg (long as, long regnum, long value)
  {
    String registerName = isa.getRegisterNameByUnwindRegnum(regnum);
    logger.log(Level.FINE, "Libunwind: writing value 0x"
                                + Long.toHexString(value) + " to register "
                                + registerName + "\n");
    // TODO Auto-generated method stub
    throw new RuntimeException("Not implemented in core yet");
  }

  public double accessFpreg (long as, long regnum)
  {
    /* This is probably broken, since the numbering for FP regs ought
     * to be different from that for non-FP reg.  */
    String registerName = isa.getRegisterNameByUnwindRegnum(regnum);
    logger.log(Level.FINE, "Libunwind: reading register " + registerName
                                + "\n");

    throw new RuntimeException("Not implemented in core yet");
    // TODO Auto-generated method stub
    // return 0;
  }

  public void writeFpreg (long as, long regnum, double value)
  {
    /* This is probably broken, since the numbering for FP regs ought
     * to be different from that for non-FP reg.  */
    String registerName = isa.getRegisterNameByUnwindRegnum(regnum);
    logger.log(Level.FINE, "Libunwind: writing value " + value
                                + " to register " + registerName + "\n");
    throw new RuntimeException("Not implemented in core yet");
    // TODO Auto-generated method stub

  }

  public int resume (long as, long cp)
  {
    throw new RuntimeException("Not implemented in core yet");
    // TODO Auto-generated method stub
    // return 0;
  }

  public int getPid ()
  {
    /* FIXME: this relies on the hashCode of a ProcId returning the
     * actual id.  */
    return myTask.getProc().getId().hashCode();
  }
}
