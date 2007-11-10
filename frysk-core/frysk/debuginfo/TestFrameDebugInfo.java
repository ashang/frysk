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

import lib.dwfl.DwTag;
import lib.dwfl.DwarfDie;
import lib.dwfl.Dwfl;
import lib.dwfl.DwflDieBias;
import frysk.dwfl.DwflCache;
import frysk.proc.Task;
import frysk.scopes.LexicalBlock;
import frysk.scopes.Scope;
import frysk.scopes.Subprogram;
import frysk.scopes.Subroutine;
import frysk.stack.Frame;
import frysk.stack.StackFactory;
import frysk.testbed.DaemonBlockedAtSignal;
import frysk.testbed.TestLib;
import frysk.value.Variable;

public class TestFrameDebugInfo
    extends TestLib
{
    /// I am not looking at what you are typing. I just have to see ya type
  Logger logger = Logger.getLogger("frysk");

  public void testFrameDebugInfoStackTrace ()
  {
      
    Task task = (new DaemonBlockedAtSignal("funit-stacks")).getMainTask();
    
    StringWriter stringWriter = new StringWriter();
    DebugInfoFrame frame = DebugInfoStackFactory.createDebugInfoStackTrace(task);
    DebugInfoStackFactory.printStackTrace(new PrintWriter(stringWriter),frame, 20, true, true, true);
      
    String string = stringWriter.getBuffer().toString();
    assertTrue("first", string.contains("first"));
    assertTrue("second", string.contains("second"));
    assertTrue("third",string.contains("third"));
    assertTrue("fourth",string.contains("fourth"));
    
  }
  
  public void testFrameCompilerIlinedFucntions ()
  {

    Task task = (new DaemonBlockedAtSignal("funit-empty-functions")).getMainTask();
    
    DebugInfoFrame frame = DebugInfoStackFactory.createVirtualStackTrace(task);
    Subprogram subprogram = frame.getSubprogram();
    assertNotNull(subprogram);
  }
  
  public void testFrameAdjustedAddress ()
  {
    if(unresolved(4676))
        return;

    Task task = (new DaemonBlockedAtSignal("funit-stacks-exit")).getMainTask();
    
    Frame frame = StackFactory.createFrame(task);
    StringWriter stringWriter = new StringWriter();
    StackFactory.printStack(new PrintWriter(stringWriter),frame);
      
    String string = stringWriter.getBuffer().toString();
    
    assertTrue("first", string.contains("first"));
    assertTrue("second", string.contains("second"));
    assertTrue("third",string.contains("third"));
    assertTrue("fourth",string.contains("fourth"));
    
  }
  
  public void testFrameScopes ()
  {

      Task task = (new DaemonBlockedAtSignal("funit-scopes")).getMainTask();
    
    Frame frame = StackFactory.createFrame(task);
    
    Dwfl dwfl = DwflCache.getDwfl(task);
    DwflDieBias bias = dwfl.getCompilationUnit(frame.getAdjustedAddress());
    DwarfDie[] scopes = bias.die.getScopes(frame.getAdjustedAddress() - bias.bias);
    
    assertEquals("number of scopes", 3, scopes.length);
    
    assertEquals("lexical block die" , DwTag.LEXICAL_BLOCK,scopes[0].getTag());
    assertEquals("inlined function die", DwTag.INLINED_SUBROUTINE, scopes[1].getTag());
    assertEquals("compliation unit die", DwTag.COMPILE_UNIT, scopes[2].getTag());
    
  }
  
  public void testDebugInfoFrameScopes ()
  {

      Task task = (new DaemonBlockedAtSignal("funit-scopes")).getMainTask();
    DebugInfoFrame frame = DebugInfoStackFactory.createDebugInfoStackTrace(task);
    
    Scope scope1 = frame.getScopes();
    Scope scope2 = scope1.getOuter();
    Scope scope3 = scope2.getOuter();
    
    assertTrue("lexical block scope" , scope1 instanceof LexicalBlock);
    assertTrue("InlinedSubroutine scope" , scope2 instanceof Subroutine && ((Subroutine)scope2).isInlined());
    assertTrue("lexical block scope" , scope3 instanceof Scope);
    
  }
  
  public void testFrameScopesWorkAround ()
  {
    
      Task task = (new DaemonBlockedAtSignal("funit-scopes-workaround")).getMainTask();
    Frame frame = StackFactory.createFrame(task);
    
    Dwfl dwfl = DwflCache.getDwfl(task);
    DwflDieBias bias = dwfl.getCompilationUnit(frame.getAdjustedAddress());
    DwarfDie[] scopes = bias.die.getScopes(frame.getAdjustedAddress() - bias.bias);
    scopes = scopes[0].getScopesDie();
    
    assertEquals("number of scopes", 4, scopes.length);
    
    assertEquals("inlined die" , DwTag.INLINED_SUBROUTINE,scopes[1].getTag());
    assertEquals("function die", DwTag.SUBPROGRAM, scopes[2].getTag());
    assertEquals("compliation unit die", DwTag.COMPILE_UNIT, scopes[3].getTag());
    
  }
  
  public void testGetInlinedSubroutines ()
  {
    
      Task task = (new DaemonBlockedAtSignal("funit-inlined")).getMainTask();
    DebugInfoFrame frame = DebugInfoStackFactory.createDebugInfoStackTrace(task);
    
    LinkedList inlinedSubprograms =  frame.getInlinedSubprograms();
    
    assertEquals("Number of inline functions",3,inlinedSubprograms.size());
    
  }
  
  public void testVirtualStackTrace ()
  {
    
      Task task = (new DaemonBlockedAtSignal("funit-inlined")).getMainTask();
    StringWriter stringWriter = new StringWriter();
    
    DebugInfoStackFactory.printVirtualTaskStackTrace(new PrintWriter(stringWriter), task, true, true, true);
    
    assertTrue("contains inline", stringWriter.getBuffer().toString().contains("inline"));
    assertTrue("contains first", stringWriter.getBuffer().toString().contains("first"));
    assertTrue("contains second", stringWriter.getBuffer().toString().contains("second"));
    assertTrue("contains third", stringWriter.getBuffer().toString().contains("third"));
    assertTrue("contains main", stringWriter.getBuffer().toString().contains("main"));
  }
  
  // test that a Subprogram can be retrieved for a function even
  // if the call stack contains calls to inlined functions inner
  // to it.
  public void testInlinedFunctionDerailment ()
  {
  
      Task task = (new DaemonBlockedAtSignal("funit-inlined")).getMainTask();
    
    DebugInfoFrame frame = DebugInfoStackFactory.createVirtualStackTrace(task);
    Subprogram subprogram = null;
    
    while(frame.getOuterDebugInfoFrame() != null){
	subprogram = frame.getSubprogram();
	if(subprogram != null && subprogram.getName().equals("main")){
	    break;
	}
	frame = frame.getOuterDebugInfoFrame();
    }
    
    assertNotNull(subprogram);
    assertTrue("found main", subprogram.getName().equals("main"));
  }
  
  
  public void testValues() throws NameNotFoundException
  {
      Task task = (new DaemonBlockedAtSignal("funit-stacks-values")).getMainTask();
    Subprogram subprogram;
    DebugInfoFrame frame;
    Variable variable;
    
    // inner most frame
    frame = DebugInfoStackFactory.createDebugInfoStackTrace(task);
    
    subprogram = frame.getSubprogram();
    assertEquals("Subprogram name", subprogram.getName(), "third");
    
    variable = (Variable) subprogram.getParameters().iterator().next();
    assertNotNull(variable);
    assertEquals("Name", variable.getName(), "param3");
    assertEquals("Value", variable.getValue(frame).asLong(), 3);
    
    variable = (Variable) subprogram.getVariables().getFirst();
    assertNotNull(variable);
    assertEquals("Name", variable.getName(), "var4");
    assertEquals("Value", variable.getValue(frame).asLong(), 4);
    
    // outer frame
    frame = frame.getOuterDebugInfoFrame();
    
    subprogram = frame.getSubprogram();
    assertEquals("Subprogram name", subprogram.getName(), "second");
    
    variable = (Variable) subprogram.getParameters().iterator().next();
    assertNotNull(variable);
    assertEquals("Name", variable.getName(), "param2");
    assertEquals("Value", variable.getValue(frame).asLong(), 2);
    
    variable = (Variable) subprogram.getVariables().getFirst();
    assertNotNull(variable);
    assertEquals("Name", variable.getName(), "var3");
    assertEquals("Value", variable.getValue(frame).asLong(), 3);
    
    // outer outer frame
    frame = frame.getOuterDebugInfoFrame();
    
    subprogram = frame.getSubprogram();
    assertEquals("Subprogram name", subprogram.getName(), "first");
    
    variable = (Variable) subprogram.getParameters().iterator().next();
    assertNotNull(variable);
    assertEquals("Name", variable.getName(), "param1");
    assertEquals("Value", variable.getValue(frame).asLong(), 1);
    
    variable = (Variable) subprogram.getVariables().getFirst();
    assertNotNull(variable);
    assertEquals("Name", variable.getName(), "var2");
    assertEquals("Value", variable.getValue(frame).asLong(), 2);
    
  }

  public void testColNumbers(){
      
  }
  
  public void testLineNumbers(){
      Task task = (new DaemonBlockedAtSignal("funit-stacks-linenum")).getMainTask();
      
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
      assertEquals("Name", variable.getName(), "param1");
      assertEquals("line number", variable.getLineNumber(), 3);
      
      variable = (Variable) iterator.next();
      assertNotNull(variable);
      assertEquals("Name", variable.getName(), "param2");
      assertEquals("line number", variable.getLineNumber(), 3);
      
      frame = frame.getOuterDebugInfoFrame();
      
      subprogram = frame.getSubprogram();
      assertEquals("Subprogram name", subprogram.getName(), "main");
      iterator = subprogram.getVariables().iterator();
      
      variable = (Variable) iterator.next();
      assertNotNull(variable);
      assertEquals("Name", variable.getName(), "var1");
      assertEquals("line number", variable.getLineNumber(), 9);
      
      variable = (Variable) iterator.next();
      assertNotNull(variable);
      assertEquals("Name", variable.getName(), "var2");
      assertEquals("line number", variable.getLineNumber(), 9);
      
      variable = (Variable) iterator.next();
      assertNotNull(variable);
      assertEquals("Name", variable.getName(), "var3");
      assertEquals("line number", variable.getLineNumber(), 9);
      
      variable = (Variable) iterator.next();
      assertNotNull(variable);
      assertEquals("Name", variable.getName(), "var4");
      assertEquals("line number", variable.getLineNumber(), 10);
  }

  public void testThatArtificialParametersAreIgnored() {

	Task task = (new DaemonBlockedAtSignal("funit-cpp-scopes-class")).getMainTask();
	      
	DebugInfoFrame frame = DebugInfoStackFactory
		.createVirtualStackTrace(task);
	Subprogram subprogram = frame.getSubprogram();

	LinkedList parameters = subprogram.getParameters();
	assertEquals("Correct number of parameters", 1, parameters.size());

	// Check that the non artificial variable was not ignored
	Variable variable = (Variable) parameters.getFirst();
	assertEquals("Correct variable was found", "i", variable.getName());

    }

}
