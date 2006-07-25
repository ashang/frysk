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

import java.util.logging.Level;

import lib.dw.DwarfDie;
import lib.dw.Dwfl;
import lib.dw.DwflDieBias;
import lib.elf.Elf;
import lib.elf.ElfData;
import lib.elf.ElfSection;
import lib.unwind.RegisterX86;
import lib.unwind.UnwindCallbacks;
import frysk.proc.Host;
import frysk.proc.Isa;
import frysk.proc.Task;

public class StackCallbacks
    implements UnwindCallbacks
{
  private Task myTask;

  private Isa isa;

  public StackCallbacks (Task myTask) throws Task.TaskException
  {
    this.myTask = myTask;
    this.myTask.toString();
    isa = myTask.getIsa();
  }

  public boolean findProcInfo (long procInfo, long addressSpace,
                               long instructionAddress, boolean needInfo)
  {
    Host.logger.log(Level.FINE, "Libunwind: findProcInfo for 0x"
                                + Long.toHexString(instructionAddress) + "\n");

    Dwfl dwfl = new Dwfl(myTask.getTid());
    DwflDieBias bias = dwfl.getDie(instructionAddress);
    DwarfDie die = bias.die;

    long adjustedAddress = instructionAddress - bias.bias;

    if (die == null)
      return false;

    DwarfDie lowest = die.getScopes(adjustedAddress)[0];
    if (lowest == null)
      return false;

    if (needInfo)
      {
        Elf elf = null;
        elf = dwfl.getModule(adjustedAddress).getElf().elf;
        // elf = new Elf(myTask.getTid(), ElfCommand.ELF_C_READ);

        ElfSection found = null;
        for (int i = 0; i < elf.getSectionCount(); i++)
          {
            ElfSection current = elf.getSection(i);
            if (current.getSectionHeader().name.equals(".debug_frame"))
              {
                found = current;
                break;
              }
          }

        if (found == null)
          return false;

        populate_procinfo(procInfo, lowest.getLowPC(), lowest.getHighPC(), 0,
                          0, 0, found.getData());
      }
    else
      populate_procinfo_nounwind(procInfo, lowest.getLowPC(),
                                 lowest.getHighPC(), 0, 0, 0);

    return true;
  }

  public void putUnwindInfo (long addressSpace, long procInfo)
  {
    free_proc_info(procInfo);
  }

  public long getDynInfoListAddr (long addressSpace)
  {
    // No such thing :)
    return 0;
  }

  public long accessMem (long addressSpace, long addr)
  {
    Host.logger.log(Level.FINE, "Libunwind: reading memory at 0x"
                                + Long.toHexString(addr) + "\n");

    // XXX: Fixme for 64
    long value = myTask.getMemory().getInt(addr);

    Host.logger.log(Level.FINE, "Libunwind: read value 0x"
                                + Long.toHexString(value) + "\n");
    return value;
  }

  public void writeMem (long as, long addr, long value)
  {
    Host.logger.log(Level.FINE, "Libunwind: writing value 0x"
                                + Long.toHexString(value)
                                + " to memory address 0x"
                                + Long.toHexString(addr) + "\n");
    throw new RuntimeException("Not implemented in core yet");
  }

  public long accessReg (long as, long regnum)
  {
    String registerName = RegisterX86.getUnwindRegister(regnum);
    Host.logger.log(Level.FINE, "Libunwind: reading from register "
                                + registerName + "\n");

    long value = isa.getRegisterByName(registerName).get(myTask);

    Host.logger.log(Level.FINE, "Libunwind: read value 0x"
                                + Long.toHexString(value) + "\n");
    return value;
  }

  public void writeReg (long as, long regnum, long value)
  {
    String registerName = RegisterX86.getUnwindRegister(regnum);
    Host.logger.log(Level.FINE, "Libunwind: writing value 0x"
                                + Long.toHexString(value) + " to register "
                                + registerName + "\n");
    // TODO Auto-generated method stub
    throw new RuntimeException("Not implemented in core yet");
  }

  public double accessFpreg (long as, long regnum)
  {
    String registerName = RegisterX86.getUnwindRegister(regnum);
    Host.logger.log(Level.FINE, "Libunwind: reading register " + registerName
                                + "\n");

    throw new RuntimeException("Not implemented in core yet");
    // TODO Auto-generated method stub
    // return 0;
  }

  public void writeFpreg (long as, long regnum, double value)
  {
    String registerName = RegisterX86.getUnwindRegister(regnum);
    Host.logger.log(Level.FINE, "Libunwind: writing value " + value
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

  public String getProcName (long as, long addr)
  {
    Host.logger.log(Level.FINE, "Libunwind: getting procedure name at 0x"
                                + Long.toHexString(addr) + "\n");

    Dwfl dwfl = new Dwfl(myTask.getTid());
    DwflDieBias bias = dwfl.getDie(addr);
    DwarfDie die = bias.die;
    if (die == null)
      return "";

    DwarfDie lowest = die.getScopes(addr - bias.bias)[0];
    if (lowest == null)
      return "";

    return lowest.getName();
  }

  public long getProcOffset (long as, long addr)
  {
    Host.logger.log(Level.FINE, "Libunwind: getting procedure offset at 0x"
                                + Long.toHexString(addr) + "\n");

    Dwfl dwfl = new Dwfl(myTask.getTid());
    DwflDieBias bias = dwfl.getDie(addr);
    DwarfDie die = bias.die;
    if (die == null)
      return 0;

    DwarfDie lowest = die.getScopes(addr - bias.bias)[0];
    if (lowest == null)
      return 0;

    return addr - lowest.getLowPC();
  }

  private native void populate_procinfo (long procInfo, long lowPC,
                                         long highPC, long lsda, long gp,
                                         long flags, ElfData debug_frame);

  private native void populate_procinfo_nounwind (long procInfo, long lowPC,
                                                  long highPC, long lsda,
                                                  long gp, long flags);

  private native void free_proc_info (long proc_info);
}
