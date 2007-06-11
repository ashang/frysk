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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.logging.Logger;

import frysk.proc.Action;
import frysk.proc.Manager;
import frysk.proc.Task;
import frysk.proc.TestLib;
import frysk.proc.TaskObserver.Terminating;
import frysk.stack.Frame;
import frysk.stack.StackFactory;
import frysk.value.Value;

public class TestFrameDebugInfo
    extends TestLib
{

  Logger logger = Logger.getLogger("frysk");

  public void testFrameDebugInfoStackTrace ()
  {
    Task task = getStoppedTask();

    Frame frame = StackFactory.createFrame(task, 0);
    String string = StackFactory.printRichStackTrace(frame, true, true, true);
      
//    System.out.println("TestRichFrame.testRichFrame()");
//    System.out.println(string);
//    System.out.println();
    
    assertTrue("first", string.contains("first"));
    assertTrue("second", string.contains("second"));
    assertTrue("third",string.contains("third"));
    
  }

  public void testParameters(){
    Task task = getStoppedTask();

    Frame frame = StackFactory.createFrame(task, 0);
    
    while(!(frame.getSubprogram() != null && frame.getSubprogram().getName().contains("third"))){
      frame = frame.getOuter();
    }
    
    Subprogram subprogram = frame.getSubprogram();
    assertEquals("Subprogram name", subprogram.getName(), "third");
    
    LinkedList parameters = subprogram.getParameters();
    assertEquals("Number of parameters", parameters.size(), 3);
    
    Iterator iterator = parameters.iterator();
    Value value = (Value) iterator.next();
    assertEquals("Parameter name", value.getText(), "param1");
//    System.out.println("TestFrameDebugInfo.testParameters() param1 " + value);
//    assertEquals("Parameter value", value.getInt(), 1);
    
    value = (Value) (Value) iterator.next();
    assertEquals("Parameter name", value.getText(), "param2");
//    System.out.println("TestFrameDebugInfo.testParameters() param2 " + value);
//    assertEquals("Parameter value", value.getInt(), 2);
    
    value = (Value) (Value) iterator.next();
    assertEquals("Parameter name", value.getText(), "param3");
//    System.out.println("TestFrameDebugInfo.testParameters() param3 " + value);
//    assertEquals("Parameter value", value.getInt(), 3);
    
  }

  public Task getStoppedTask ()
  {

    AttachedDaemonProcess ackProc = new AttachedDaemonProcess(
							      new String[] { getExecPath("funit-stacks") });

    Task task = ackProc.getMainTask();

    task.requestAddTerminatingObserver(new Terminating()
    {

      public void deletedFrom (Object observable)
      {
      }

      public void addedTo (Object observable)
      {
      }

      public Action updateTerminating (Task task, boolean signal, int value)
      {
	Manager.eventLoop.requestStop();
	return Action.BLOCK;
      }

      public void addFailed (Object observable, Throwable w)
      {
	throw new RuntimeException(w);
      }
    });
    ackProc.resume();
    assertRunUntilStop("Add TerminatingObserver");

    return task;
  }

}
