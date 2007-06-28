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

import java.util.logging.Logger;
import javax.naming.NameNotFoundException;

import lib.dw.DwTagEncodings;
import lib.dw.DwarfDie;
import lib.dw.Dwfl;
import lib.dw.DwflDieBias;
import frysk.dwfl.DwflFactory;
import frysk.proc.Action;
import frysk.proc.Manager;
import frysk.proc.Task;
import frysk.proc.TaskObserver;
import frysk.proc.TestLib;
import frysk.stack.Frame;
import frysk.stack.StackFactory;

public class TestFrameDebugInfo
    extends TestLib
{

  Logger logger = Logger.getLogger("frysk");

  public void testFrameDebugInfoStackTrace ()
  {
    Task task = getStoppedTask();

    Frame frame = StackFactory.createFrame(task);
    String string = StackFactory.printRichStackTrace(frame, true, true, true);
      
//    System.out.println("TestRichFrame.testRichFrame()");
//    System.out.println(string);
//    System.out.println();
    
    assertTrue("first", string.contains("first"));
    assertTrue("second", string.contains("second"));
    assertTrue("third",string.contains("third"));
    assertTrue("fourth",string.contains("fourth"));
    
  }

  public void testFrameAdjustedAddress ()
  {
    if(brokenXXX(4676))
        return;

    Task task = getStoppedTask("funit-stacks-exit");

    Frame frame = StackFactory.createFrame(task);
    String string = StackFactory.printStackTrace(frame);
      
//    System.out.println("TestRichFrame.testRichFrame()");
//    System.out.println(string);
//    System.out.println();
//    
    assertTrue("first", string.contains("first"));
    assertTrue("second", string.contains("second"));
    assertTrue("third",string.contains("third"));
    assertTrue("fourth",string.contains("fourth"));
    
  }
  
  public void testFrameScopes ()
  {
    if(brokenXXX(4677))
        return;

    Task task = getStoppedTask("funit-scopes");
    Frame frame = StackFactory.createFrame(task);
    frame = frame.getOuter();
    
    Dwfl dwfl = DwflFactory.createDwfl(task);
    DwflDieBias bias = dwfl.getDie(frame.getAdjustedAddress());
    DwarfDie[] scopes = bias.die.getScopes(frame.getAdjustedAddress() - bias.bias);
    
    assertEquals("number of scopes", 3, scopes.length);
    
    assertEquals("inlined die" , DwTagEncodings.DW_TAG_inlined_subroutine_,scopes[0].getTag());
    assertEquals("function die", DwTagEncodings.DW_TAG_subprogram_, scopes[1].getTag());
    assertEquals("compliation unit die", DwTagEncodings.DW_TAG_compile_unit_, scopes[0].getTag());
    
  }
  
  public void testValues() throws NameNotFoundException
  {
      
      if(brokenX86XXX(4699)){
	  return;
      }
      
    Task task = getStoppedTask("funit-stacks-values");
    Subprogram subprogram;
    Frame frame;
    Variable variable;
    
    frame = StackFactory.createFrame(task);
    
    subprogram = frame.getSubprogram();
    assertEquals("Subprogram name", subprogram.getName(), "third");
    variable = (Variable) subprogram.getParameters().iterator().next();
    assertNotNull(variable);
    assertEquals("Name", variable.getVariable().getText(), "param3");
    assertEquals("Value", variable.getValue(frame).intValue(), 3);
        
  }

  public Task getStoppedTask(){
    return this.getStoppedTask("funit-stacks");
  }
  
  public Task getStoppedTask (String process)
  {

    AttachedDaemonProcess ackProc = new AttachedDaemonProcess(
							      new String[] { getExecPath(process) });

    Task task = ackProc.getMainTask();

    task.requestAddSignaledObserver(new TerminatingSignaledObserver());
    task.requestAddTerminatingObserver(new TerminatingSignaledObserver());
    
    ackProc.resume();
    assertRunUntilStop("Add TerminatingSignaledObserver");

    return task;
  }

  class TerminatingSignaledObserver implements TaskObserver.Signaled, TaskObserver.Terminating{
    public void deletedFrom (Object observable)
    {
    }

    public void addedTo (Object observable)
    {
    }

    public void addFailed (Object observable, Throwable w)
    {
        throw new RuntimeException(w);
    }

    public Action updateSignaled (Task task, int signal)
    {
      Manager.eventLoop.requestStop();
      return Action.BLOCK;
    }

    public Action updateTerminating (Task task, boolean signal, int value)
    {
      Manager.eventLoop.requestStop();
      return Action.BLOCK;
    }
  }
}
