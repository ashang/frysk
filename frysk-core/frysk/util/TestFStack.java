

package frysk.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PipedReader;
import java.io.PipedWriter;

import inua.util.PrintWriter;
import frysk.util.FStack;

public class TestFStack
    extends TestLib
{

  String[] mainThread = new String[] {
                                      "^Task #\\d+$",
                                      "^#0 0x[\\da-f]+ in __kernel_vsyscall \\(\\)$",
                                      "^#1 0x[\\da-f]+ in (__)?sigsuspend \\(\\)$",
                                      "^#2 0x[\\da-f]+ in server \\(\\) from .*",
                                      "^#3 0x[\\da-f]+ in main \\(\\) from .*",
                                      "^#4 0x[\\da-f]+ in __libc_start_main \\(\\)$",
                                      "^#5 0x[\\da-f]+ in _start \\(\\)$" };

  String[] secondaryThread = new String[] {
                                           "^Task #\\d+$",
                                           "^#0 0x[\\da-f]+ in __kernel_vsyscall \\(\\)$",
                                           "^#1 0x[\\da-f]+ in (__)?sigsuspend \\(\\)$",
                                           "^#2 0x[\\da-f]+ in server \\(\\) from .*",
                                           "^#3 0x[\\da-f]+ in start_thread \\(\\)$",
                                           "^#4 0x[\\da-f]+ in (__)?clone \\(\\)$" };

  public void testSingleThreadedDetached () throws IOException
  {
    if (brokenXXX(3420))
      return;
    AckProcess ackProc = new DetachedAckProcess();
    multiThreaded(ackProc, 1);
  }

  public void testSingleThreadedAckDaemon () throws IOException
  {
    if (brokenXXX(3420))
      return;
    AckProcess ackProc = new AckDaemonProcess();
    multiThreaded(ackProc, 1);
  }

  public void testMultiThreadedDetached () throws IOException
  {
    if (brokenXXX(3420))
      return;
    AckProcess ackProc = new DetachedAckProcess(2);
    multiThreaded(ackProc, 3);
  }

  public void testMultiThreadedAckDaemon () throws IOException
  {
    if (brokenXXX(3420))
      return;
    AckProcess ackProc = new AckDaemonProcess(2);
    multiThreaded(ackProc, 3);
  }

  public void testStressMultiThreadedDetach () throws IOException
  {
    if (brokenXXX(3420))
      return;
    AckProcess ackProc = new DetachedAckProcess(7);
    multiThreaded(ackProc, 8);
  }

  public void multiThreaded (AckProcess ackProc, int threads)
      throws IOException
  {
    FStack stacker = new FStack();

    PipedReader input = new PipedReader();
    PipedWriter output = new PipedWriter(input);
    stacker.setWriter(new PrintWriter(output, true));

    stacker.scheduleStack(ackProc.getPid());

    assertRunUntilStop("test");

    BufferedReader br = new BufferedReader(input);

    for (int i = 0; i < mainThread.length; i++)
      {
        String line = br.readLine();
        assertTrue(line + " did not match: " + mainThread[i],
                   line.matches(mainThread[i]));
      }
    for (int j = 1; j < threads; j++)
      {
        for (int i = 0; i < secondaryThread.length; i++)
          {
            String line = br.readLine();
            assertTrue(line + " did not match: " + secondaryThread[i],
                       line.matches(secondaryThread[i]));
          }
      }
    input.close();
    output.close();
    br.close();
  }
}
