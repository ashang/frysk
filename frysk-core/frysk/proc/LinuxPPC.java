// This file is part of the program FRYSK.

package frysk.proc;

import java.util.HashMap;

import java.util.logging.Level;
import java.util.logging.Logger;

class LinuxPPC
  extends IsaPPC implements SyscallEventDecoder
{
  private static Logger logger = Logger.getLogger ("frysk");//.proc");
  private static LinuxPPC isa;

  static LinuxPPC isaSingleton ()
  {
    if (isa == null)
      isa = new LinuxPPC ();
    return isa;
  }

  // This is used to keep track of syscalls whose number we do not
  // know.
  static HashMap unknownSyscalls = new HashMap();

  private SyscallEventInfo info;

  public SyscallEventInfo getSyscallEventInfo ()
  {
    if (info == null)
      info = new SyscallEventInfo ()
      {
	public int number (Task task)
        {
          logger.log (Level.FINE, "Get GPR0 {0}\n", getRegisterByName("gpr0"));
          return (int)getRegisterByName("gpr0").get(task);
        }
	public Syscall getSyscall(Task task)
	{
	  int number = this.number(task);
	  return LinuxPowerPCSyscall.syscallByNum (task, number);
	}
        };
    return info;
  }

}
