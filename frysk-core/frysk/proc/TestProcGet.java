// This file is part of the program FRYSK.
//
// Copyright 2005, 2006, 2007, 2008, Red Hat Inc.
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


package frysk.proc;

import frysk.testbed.SlaveOffspring;
import inua.util.Scanner;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import frysk.testbed.TearDownFile;
import frysk.testbed.TestLib;
import frysk.testbed.StopEventLoopWhenProcTerminated;
import frysk.testbed.DaemonBlockedAtEntry;
import frysk.testbed.ExecOffspring;
import frysk.testbed.ExecCommand;
import frysk.testbed.Offspring;

/**
 * Test Proc's public get methods.
 */

public class TestProcGet
    extends TestLib
{
  /**
   * Compare the output from a program that just prints its AUXV (to a tempoary
   * file) to that extracted from the same process using Proc.getAuxv().
   */
  public void testGetAuxv ()
  {
    TearDownFile tmpFile = TearDownFile.create();

    DaemonBlockedAtEntry child = new DaemonBlockedAtEntry(new String[]
	{
	    getExecPath ("funit-print-auxv"),
	    tmpFile.toString(),
	    "/dev/null"
	});

    new StopEventLoopWhenProcTerminated(child);
    // Grab the AUXV from the process sitting at its entry point.
    Auxv[] auxv = child.getMainTask().getProc().getAuxv();
    assertNotNull("captured AUXV", auxv);
    child.requestUnblock();
    assertRunUntilStop("run \"auxv\" to completion");

    // Compare the AUXV as printed against that extracted using
    // Proc.getAuxv.
    try
      {
        Scanner reader = new Scanner(tmpFile);
        for (int i = 0; i < auxv.length; i++)
          {
            if (auxv[i].type == 0)
              break;
            long type = reader.readDecimalLong();
            reader.skipWhitespace();
            long val = reader.readDecimalLong();
            reader.skipWhitespace();
            assertEquals("auxv[" + i + "].type", type, auxv[i].type);
            assertEquals("auxv[" + i + "].val", val, auxv[i].val);
          }
        assertTrue("reached AUXV end-of-file", reader.endOfFile());
      }
    catch (Exception e)
      {
        throw new RuntimeException(e);
      }
  }

  /**
   * Check that .getCommand returns the command in an expected format.
   */
  public void testGetCommand ()
  {
    Offspring child = SlaveOffspring.createDaemon();
    Proc childProc = child.assertRunToFindProc();
    assertEquals("value of child's getCommand()",
		 SlaveOffspring.getExecutable().getName(),
                 childProc.getCommand());
  }

  /**
   * Check that .getTasks, for a two task process returns two tasks.
   */
  public void testGetTasks ()
  {
      Offspring child = SlaveOffspring.createDaemon()
	  .assertSendAddClonesWaitForAcks(1);
    Proc proc = child.assertRunToFindProc(); // and tasks
    List tasks = proc.getTasks();

    assertEquals("number of tasks", 2, tasks.size());

    // Find the main task.
    Task mainTask = null;
    for (Iterator i = tasks.iterator(); i.hasNext();)
      {
        Task task = (Task) i.next();
        if (proc.getPid() == task.getTid())
          {
            // Only found once.
            assertNull("main task", mainTask);
            mainTask = task;
          }
      }
    assertNotNull("main task", mainTask);
  }

  /**
   * Check that .getChildren, for this process with two children returns both of
   * them.
   */
  public void testGetChildren ()
  {
    // Create two children. The refreshes have the side effect of
    // updating this processes proc list.
    Proc[] child = new Proc[] {
	SlaveOffspring.createChild().assertRunToFindProc(),
	SlaveOffspring.createChild().assertRunToFindProc() };
    Proc self = host.getSelf();

    assertEquals("number of children", 2, self.getChildren().size());
    assertNotSame("children", child[0], child[1]);
    for (int i = 0; i < child.length; i++)
      {
        assertTrue("this contains child " + i,
                   self.getChildren().contains(child[i]));
      }
  }

    /**
     * Check that getCmdLine returns the list of arguments that
     * matches what was passed to a detached process.
     */
    public void testGetCmdLine() {
	// Create a process with a known set of arguments.
	ExecCommand cmd = new ExecCommand();
	ExecOffspring child = new ExecOffspring(cmd);
	Proc proc = child.assertRunToFindProc();
	String[] cmdLine = proc.getCmdLine();
	assertEquals("cmdLine.length", cmd.argv.length, cmdLine.length);
	for (int i = 0; i < cmd.argv.length; i++) {
	    assertEquals("cmdLine[" + i + "]", cmd.argv[i], cmdLine[i]);
	}
    }

    /**
     * Check that getExe returns the fully qualified path to the
     * ack-daemon program.
     */
    public void testGetExe() throws IOException {
	// Create a process with a known set of arguments.
	ExecCommand cmd = new ExecCommand();
	ExecOffspring child = new ExecOffspring(cmd);
	String file = new File(cmd.argv[0]).getCanonicalPath();
	Proc proc = child.assertRunToFindProc();
	assertEquals("exe", proc.getExeFile().getSysRootedPath(), file);
    }
}
