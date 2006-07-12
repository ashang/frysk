

package frysk.rt;

import frysk.proc.Task;
import lib.unwind.UnwindCallbacks;

public class StackCallbacks
    implements UnwindCallbacks
{
  private Task myTask;

  public StackCallbacks (Task myTask)
  {
    this.myTask = myTask;
    this.myTask.toString();
  }

  public long findProcInfo (long addressSpace, long instructionAddress,
                            boolean needInfo)
  {
    throw new RuntimeException("Not implemented in core yet");
//    return get_proc_info(instructionAddress, needInfo);
  }

  public void putUnwindInfo (long addressSpace, long procInfo)
  {
    throw new RuntimeException("Not implemented in core yet");
//    free_proc_info(procInfo);
  }

  public long getDynInfoListAddr (long addressSpace)
  {
    throw new RuntimeException("Not implemented in core yet");
    // TODO Auto-generated method stub
//    return 0;
  }

  public long accessMem (long addressSpace, long addr)
  {
    // XXX: Fixme for 64
    return myTask.getMemory().getInt(addr);
  }

  public void writeMem (long as, long addr, long value)
  {
    throw new RuntimeException("Not implemented in core yet");
  }

  public long accessReg (long as, long regnum)
  {
    throw new RuntimeException("Not implemented in core yet");
//    return 0;
  }

  public void writeReg (long as, long regnum, long value)
  {
    // TODO Auto-generated method stub
    throw new RuntimeException("Not implemented in core yet");
  }

  public double accessFpreg (long as, long regnum)
  {
    throw new RuntimeException("Not implemented in core yet");
    // TODO Auto-generated method stub
//    return 0;
  }

  public void writeFpreg (long as, long regnum, double value)
  {
    throw new RuntimeException("Not implemented in core yet");
    // TODO Auto-generated method stub

  }

  public int resume (long as, long cp)
  {
    throw new RuntimeException("Not implemented in core yet");
    // TODO Auto-generated method stub
//    return 0;
  }

  public String getProcName (long as, long addr)
  {
    throw new RuntimeException("Not implemented in core yet");
    // TODO Auto-generated method stub
//    return null;
  }

  public long getProcOffset (long as, long addr)
  {
    throw new RuntimeException("Not implemented in core yet");
    // TODO Auto-generated method stub
//    return 0;
  }

//  private native long get_proc_info (long address, boolean need_info);
//  private native long free_proc_info(long proc_info);
}
