

package frysk.sys;

import junit.framework.TestCase;
import frysk.sys.proc.MapsBuilder;

public class TestMapRead
    extends TestCase
{
  /**
   * A variable that has the value true. Used by code trying to stop the
   * optimizer realise that there's dead code around.
   */
  static boolean trueXXX = true;

  /**
   * A function that returns true, and prints skip. Used by test cases that want
   * to be skipped (vis: if(broken()) return) while trying to avoid the
   * compiler's optimizer realizing that the rest of the function is dead.
   */
  protected static boolean brokenXXX (int bug)
  {
    System.out.print("<<BROKEN http://sourceware.org/bugzilla/show_bug.cgi?id="
                     + bug + " >>");
    return trueXXX;
  }

  private int pid;

  protected void setUp ()
  {
    pid = TestLib.forkIt();
    Ptrace.attach(pid);
    TestLib.waitIt(pid);
  }

  protected void tearDown ()
  {
    Ptrace.detach(pid, 15);
  }

  /*
   * MapsBuilder that will try to read each int in the address space (this is
   * where we expect it to break)
   */
  class MapIntBuilder
      extends MapsBuilder
  {
    PtraceByteBuffer buffer;

    public MapIntBuilder (PtraceByteBuffer buffer)
    {
      this.buffer = buffer;
    }

    public void buildBuffer (byte[] maps)
    {
      maps[maps.length - 1] = 0;
    }

    public void buildMap (long addressLow, long addressHigh, boolean permRead,
                          boolean permWrite, boolean permExecute,
                          boolean permPrivate, long offset, int devMajor,
                          int devMinor, int inode, int pathnameOffset,
                          int pathnameLength)
    {

      for (long i = addressLow; i < addressHigh; i++)
        {
          // System.err.println(Long.toHexString(i) + " is in the Mmap!");
          // assertEquals(0, i & 0x3);
          buffer.getInt(i);
        }
    }

  }

  public void testMapRead ()
  {
    if(brokenXXX(2928))
      return;
    
    PtraceByteBuffer buffer = new PtraceByteBuffer(pid,
                                                   PtraceByteBuffer.Area.DATA);

    MapIntBuilder builder2 = new MapIntBuilder(buffer);
    builder2.construct(pid);
  }
}
