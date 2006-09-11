// This file is part of the program FRYSK.

package frysk.proc;

import java.util.logging.Level;
import java.util.logging.Logger;

class LinuxPPC64
  extends IsaPPC64 implements SyscallEventDecoder
{
    private static Logger logger = Logger.getLogger ("frysk");//.proc");
  private static LinuxPPC64 isa;

  static LinuxPPC64 isaSingleton ()
  {
    if (isa == null)
      isa = new LinuxPPC64 ();
    return isa;
  }

  private SyscallEventInfo info;
  public SyscallEventInfo getSyscallEventInfo ()
  {
    if (info == null)
      info = new SyscallEventInfo ()
      {
        public int number (Task task)
	{
          logger.log (Level.FINE, "Get GPR0 {0}\n",getRegisterByName("gpr0"));
          return (int)getRegisterByName("gpr0").get(task);
	}
	public long returnCode (Task task)
        {
          int flag = (int)getRegisterByName("ccr").get(task);
          if ((flag & 0x10000000) != 0)
            return -getRegisterByName("gpr3").get(task);
          else
            return getRegisterByName("gpr3").get(task);
	}
	public long arg (Task task, int n)
	{
          switch (n)
	    {
	    case 0:
	      return (long)number (task);
	    case 1:
	      return getRegisterByName("orig_r3").get(task);
	    case 2:
	      return getRegisterByName("gpr4").get(task);
	    case 3:
	      return getRegisterByName("gpr5").get(task);
	    case 4:
	      return getRegisterByName("gpr6").get(task);
	    case 5:
	      return getRegisterByName("gpr7").get(task);
	    case 6:
	      return getRegisterByName("gpr8").get(task);
	    default:
	      throw new RuntimeException ("unknown syscall arg");
	    }
        }
	};
    return info;
  }
}
