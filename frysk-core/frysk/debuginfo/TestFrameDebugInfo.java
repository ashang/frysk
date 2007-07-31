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

package frysk.debuginfo;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.logging.Logger;

import javax.naming.NameNotFoundException;

import lib.dwfl.DwTagEncodings;
import lib.dwfl.DwarfDie;
import lib.dwfl.Dwfl;
import lib.dwfl.DwflDieBias;
import frysk.dwfl.DwflCache;
import frysk.proc.Action;
import frysk.proc.Manager;
import frysk.proc.Task;
import frysk.proc.TaskObserver;
import frysk.stack.Frame;
import frysk.stack.StackFactory;
import frysk.testbed.DaemonBlockedAtEntry;
import frysk.testbed.TestLib;

public class TestFrameDebugInfo
    extends TestLib
{

  Logger logger = Logger.getLogger("frysk");

  public void testFrameDebugInfoStackTrace ()
  {
    Task task = getStoppedTask();

    StringWriter stringWriter = new StringWriter();
    DebugInfoFrame frame = DebugInfoStackFactory.createDebugInfoStackTrace(task);
    DebugInfoStackFactory.printStackTrace(new PrintWriter(stringWriter),frame, true, true, true);
      
    String string = stringWriter.getBuffer().toString();
    assertTrue("first", string.contains("first"));
    assertTrue("second", string.contains("second"));
    assertTrue("third",string.contains("third"));
    assertTrue("fourth",string.contains("fourth"));
    
  }

  public void testFrameAdjustedAddress ()
  {
    if(unresolved(4676))
        return;

    Task task = getStoppedTask("funit-stacks-exit");

    Frame frame = StackFactory.createFrame(task);
    StringWriter stringWriter = new StringWriter();
    StackFactory.printStackTrace(new PrintWriter(stringWriter),frame, true);
      
    String string = stringWriter.getBuffer().toString();
    
    assertTrue("first", string.contains("first"));
    assertTrue("second", string.contains("second"));
    assertTrue("third",string.contains("third"));
    assertTrue("fourth",string.contains("fourth"));
    
  }
  
  public void testFrameScopes ()
  {
    if(unresolved(4677))
        return;

    Task task = getStoppedTask("funit-scopes");
    Frame frame = StackFactory.createFrame(task);
    
    Dwfl dwfl = DwflCache.getDwfl(task);
    DwflDieBias bias = dwfl.getDie(frame.getAdjustedAddress());
    DwarfDie[] scopes = bias.die.getScopes(frame.getAdjustedAddress() - bias.bias);
    
    assertEquals("number of scopes", 3, scopes.length);
    
    assertEquals("inlined die" , DwTagEncodings.DW_TAG_inlined_subroutine_,scopes[0].getTag());
    assertEquals("function die", DwTagEncodings.DW_TAG_subprogram_, scopes[1].getTag());
    assertEquals("compliation unit die", DwTagEncodings.DW_TAG_compile_unit_, scopes[0].getTag());
    
  }
  
  public void testFrameScopesWorkAround ()
  {
    
    Task task = getStoppedTask("funit-scopes-workaround");
    Frame frame = StackFactory.createFrame(task);
    
    Dwfl dwfl = DwflCache.getDwfl(task);
    DwflDieBias bias = dwfl.getDie(frame.getAdjustedAddress());
    DwarfDie[] scopes = bias.die.getScopes(frame.getAdjustedAddress() - bias.bias);
    scopes = scopes[0].getScopesDie();
    
    assertEquals("number of scopes", 4, scopes.length);
    
    assertEquals("inlined die" , DwTagEncodings.DW_TAG_inlined_subroutine_,scopes[1].getTag());
    assertEquals("function die", DwTagEncodings.DW_TAG_subprogram_, scopes[2].getTag());
    assertEquals("compliation unit die", DwTagEncodings.DW_TAG_compile_unit_, scopes[3].getTag());
    
  }
  
  public void testGetInlinedSubroutines ()
  {
    
    Task task = getStoppedTask("funit-inlined");
    DebugInfoFrame frame = DebugInfoStackFactory.createDebugInfoStackTrace(task);
    
    LinkedList inlinedSubprograms =  frame.getInlnedSubprograms();
    
    assertEquals("Number of inline functions",3,inlinedSubprograms.size());
    
  }
  
  public void testValues() throws NameNotFoundException
  {
    Task task = getStoppedTask("funit-stacks-values");
    Subprogram subprogram;
    DebugInfoFrame frame;
    Variable variable;
    
    // inner most frame
    frame = DebugInfoStackFactory.createDebugInfoStackTrace(task);
    
    subprogram = frame.getSubprogram();
    assertEquals("Subprogram name", subprogram.getName(), "third");
    
    variable = (Variable) subprogram.getParameters().iterator().next();
    assertNotNull(variable);
    assertEquals("Name", variable.getVariable().getText(), "param3");
    assertEquals("Value", variable.getValue(frame).intValue(), 3);
    
    variable = (Variable) subprogram.getVariables().getFirst();
    assertNotNull(variable);
    assertEquals("Name", variable.getVariable().getText(), "var4");
    assertEquals("Value", variable.getValue(frame).intValue(), 4);
    
    // outer frame
    frame = frame.getOuterDebugInfoFrame();
    
    subprogram = frame.getSubprogram();
    assertEquals("Subprogram name", subprogram.getName(), "second");
    
    variable = (Variable) subprogram.getParameters().iterator().next();
    assertNotNull(variable);
    assertEquals("Name", variable.getVariable().getText(), "param2");
    assertEquals("Value", variable.getValue(frame).intValue(), 2);
    
    variable = (Variable) subprogram.getVariables().getFirst();
    assertNotNull(variable);
    assertEquals("Name", variable.getVariable().getText(), "var3");
    assertEquals("Value", variable.getValue(frame).intValue(), 3);
    
    // outer outer frame
    frame = frame.getOuterDebugInfoFrame();
    
    subprogram = frame.getSubprogram();
    assertEquals("Subprogram name", subprogram.getName(), "first");
    
    variable = (Variable) subprogram.getParameters().iterator().next();
    assertNotNull(variable);
    assertEquals("Name", variable.getVariable().getText(), "param1");
    assertEquals("Value", variable.getValue(frame).intValue(), 1);
    
    variable = (Variable) subprogram.getVariables().getFirst();
    assertNotNull(variable);
    assertEquals("Name", variable.getVariable().getText(), "var2");
    assertEquals("Value", variable.getValue(frame).intValue(), 2);
    
  }

  public void testColNumbers(){
      
  }
  
  public void testLineNumbers(){
      Task task = getStoppedTask("funit-stacks-linenum");
      
      Subprogram subprogram;
      DebugInfoFrame frame;
      Variable variable;
      
      // inner most frame
      frame = DebugInfoStackFactory.createDebugInfoStackTrace(task);
      
      subprogram = frame.getSubprogram();
      assertEquals("Subprogram name", subprogram.getName(), "first");
      Iterator iterator = subprogram.getParameters().iterator();
      
      variable = (Variable) iterator.next();
      assertNotNull(variable);
      assertEquals("Name", variable.getVariable().getText(), "param1");
      assertEquals("line number", variable.getLineNumber(), 3);
      
      variable = (Variable) iterator.next();
      assertNotNull(variable);
      assertEquals("Name", variable.getVariable().getText(), "param2");
      assertEquals("line number", variable.getLineNumber(), 3);
      
      frame = frame.getOuterDebugInfoFrame();
      
      subprogram = frame.getSubprogram();
      assertEquals("Subprogram name", subprogram.getName(), "main");
      iterator = subprogram.getVariables().iterator();
      
      variable = (Variable) iterator.next();
      assertNotNull(variable);
      assertEquals("Name", variable.getVariable().getText(), "var1");
      assertEquals("line number", variable.getLineNumber(), 9);
      
      variable = (Variable) iterator.next();
      assertNotNull(variable);
      assertEquals("Name", variable.getVariable().getText(), "var2");
      assertEquals("line number", variable.getLineNumber(), 9);
      
      variable = (Variable) iterator.next();
      assertNotNull(variable);
      assertEquals("Name", variable.getVariable().getText(), "var3");
      assertEquals("line number", variable.getLineNumber(), 9);
      
      variable = (Variable) iterator.next();
      assertNotNull(variable);
      assertEquals("Name", variable.getVariable().getText(), "var4");
      assertEquals("line number", variable.getLineNumber(), 10);
  }
  
  public Task getStoppedTask(){
    return this.getStoppedTask("funit-stacks");
  }
  
  public Task getStoppedTask (String process)
  {

    DaemonBlockedAtEntry ackProc = new DaemonBlockedAtEntry(new String[] { getExecPath(process) });

    Task task = ackProc.getMainTask();

    task.requestAddSignaledObserver(new TerminatingSignaledObserver());
    task.requestAddTerminatingObserver(new TerminatingSignaledObserver());
    
    ackProc.requestRemoveBlock();
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
