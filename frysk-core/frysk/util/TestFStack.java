

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

  public void testSingleThreadedDetached () throws IOException
  {
    AckProcess ackProc = new DetachedAckProcess();
    singleThreaded(ackProc);
  }

  public void testSingleThreadedAckDaemon () throws IOException
  {
    AckProcess ackProc = new AckDaemonProcess();
    singleThreaded(ackProc);
  }

  public void singleThreaded (AckProcess ackProc) throws IOException
  {
    FStack stacker = new FStack();

    PipedReader input = new PipedReader();
    PipedWriter output = new PipedWriter(input);
    stacker.setWriter(new PrintWriter(output, true));

    stacker.scheduleStack(ackProc.getPid());

    assertRunUntilStop("test");

    BufferedReader br = new BufferedReader(input);

    String[] matches = new String[] {
                                     "^Task #\\d+$",
                                     "^#0 0x[\\da-f]+ in __kernel_vsyscall \\(\\)",
                                     "^#1 0x[\\da-f]+ in [_]*sigsuspend \\(\\)",
                                     "^#2 0x[\\da-f]+ in server \\(\\) from .*",
                                     "^#3 0x[\\da-f]+ in main \\(\\) from .*",
                                     "^#4 0x[\\da-f]+ in __libc_start_main \\(\\)",
                                     "^#5 0x[\\da-f]+ in _start \\(\\)" };

    for (int i = 0; i < matches.length; i++)
      {
        String line = br.readLine();
        assertTrue(line + " did not match: " + matches[i],
                   line.matches(matches[i]));
      }
    
    input.close();
    output.close();
    br.close();
  }
}
