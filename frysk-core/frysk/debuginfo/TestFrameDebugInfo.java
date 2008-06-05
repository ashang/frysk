// This file is part of the program FRYSK.
//
// Copyright 2007, 2008, Red Hat Inc.
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

import javax.naming.NameNotFoundException;

import lib.dwfl.DwTag;
import lib.dwfl.DwarfDie;
import lib.dwfl.Dwfl;
import lib.dwfl.DwflDie;
import frysk.dwfl.DwflCache;
import frysk.proc.Task;
import frysk.scopes.Function;
import frysk.scopes.LexicalBlock;
import frysk.scopes.Scope;
import frysk.scopes.Variable;
import frysk.stack.Frame;
import frysk.stack.StackFactory;
import frysk.testbed.DaemonBlockedAtSignal;
import frysk.testbed.TestLib;

public class TestFrameDebugInfo extends TestLib {

  public void testFrameDebugInfoStackTrace()
  {
    frameDebugInfoStackTrace("");
  }

  public void testFrameDebugInfoStackTraceNoDebug()
  {
    frameDebugInfoStackTrace("-nodebug");
  }

  public void testFrameDebugInfoStackTraceNoEH()
  {
    frameDebugInfoStackTrace("-noeh");
  }

  public void frameDebugInfoStackTrace(String ext)
  {
      
    Task task = (new DaemonBlockedAtSignal("funit-stacks" + ext)).getMainTask();
    
    StringWriter stringWriter = new StringWriter();
    DebugInfoFrame frame = DebugInfoStackFactory.createDebugInfoStackTrace(task);
    PrintStackOptions options = new PrintStackOptions();
    options.setNumberOfFrames(20);
    options.setPrintParams(true);
    options.setPrintLocals(true);
    options.setPrintFullPaths(true);
    DebugInfoStackFactory.printStackTrace(new PrintWriter(stringWriter),frame, options);
      
    String string = stringWriter.getBuffer().toString();
    assertTrue("first", string.contains("first"));
    assertTrue("second", string.contains("second"));
    assertTrue("third",string.contains("third"));
    assertTrue("fourth",string.contains("fourth"));
    
  }
  
  public void testFrameCompilerInlinedFunctions()
  {
    frameCompilerInlinedFunctions("");
  }

  public void testFrameCompilerInlinedFunctionsNoDebug()
  {
    frameCompilerInlinedFunctions("-nodebug");
  }

  public void testFrameCompilerInlinedFunctionsNoEH()
  {
    frameCompilerInlinedFunctions("-noeh");
  }

  public void frameCompilerInlinedFunctions(String ext)
  {

    Task task = (new DaemonBlockedAtSignal("funit-empty-functions" + ext)).getMainTask();
    
    DebugInfoFrame frame = DebugInfoStackFactory.createVirtualStackTrace(task);
    Function subprogram = frame.getSubprogram();
    assertNotNull(subprogram);
  }
  
  public void testFrameAdjustedAddress()
  {
    frameAdjustedAddress("");
  }

  public void testFrameAdjustedAddressNoDebug()
  {
    frameAdjustedAddress("-nodebug");
  }

  public void testFrameAdjustedAddressNoEH()
  {
    frameAdjustedAddress("-noeh");
  }

  public void frameAdjustedAddress(String ext)
  {
    if(unresolved(4676))
        return;

    Task task = (new DaemonBlockedAtSignal("funit-stacks-exit" + ext)).getMainTask();
    
    Frame frame = StackFactory.createFrame(task);
    StringWriter stringWriter = new StringWriter();
    StackFactory.printStack(new PrintWriter(stringWriter),frame);
      
    String string = stringWriter.getBuffer().toString();
    
    assertTrue("first", string.contains("first"));
    assertTrue("second", string.contains("second"));
    assertTrue("third",string.contains("third"));
    assertTrue("fourth",string.contains("fourth"));
    
  }
  
  public void testFrameScopes()
  {
    frameScopes("");
  }

  public void testFrameScopesNoDebug()
  {
    frameScopes("-nodebug");
  }

  public void testFrameScopesNoEH()
  {
    frameScopes("-noeh");
  }

  public void frameScopes(String ext)
  {

      Task task = (new DaemonBlockedAtSignal("funit-scopes" + ext)).getMainTask();
    
    Frame frame = StackFactory.createFrame(task);
    
    Dwfl dwfl = DwflCache.getDwfl(task);
    DwflDie bias = dwfl.getCompilationUnit(frame.getAdjustedAddress());
    DwarfDie[] scopes = bias.getScopes(frame.getAdjustedAddress() - bias.getBias());
    
    assertEquals("number of scopes", 3, scopes.length);
    
    assertEquals("lexical block die" , DwTag.LEXICAL_BLOCK,scopes[0].getTag());
    assertEquals("inlined function die", DwTag.INLINED_SUBROUTINE, scopes[1].getTag());
    assertEquals("compliation unit die", DwTag.COMPILE_UNIT, scopes[2].getTag());
    
  }
  
  public void testDebugInfoFrameScopes()
  {
    debugInfoFrameScopes("");
  }

  public void testDebugInfoFrameScopesNoDebug()
  {
    debugInfoFrameScopes("-nodebug");
  }

  public void testDebugInfoFrameScopesNoEH()
  {
    debugInfoFrameScopes("-noeh");
  }

  public void debugInfoFrameScopes(String ext)
  {

      Task task = (new DaemonBlockedAtSignal("funit-scopes" + ext)).getMainTask();
    DebugInfoFrame frame = DebugInfoStackFactory.createDebugInfoStackTrace(task);
    
    Scope scope1 = frame.getScopes();
    Scope scope2 = scope1.getOuter();
    Scope scope3 = scope2.getOuter();
    
    assertTrue("lexical block scope" , scope1 instanceof LexicalBlock);
    assertTrue("InlinedSubroutine scope" , scope2 instanceof Function && ((Function)scope2).isInlined());
    assertTrue("lexical block scope" , scope3 instanceof Scope);
    
  }
  
  public void testFrameScopesWorkAround()
  {
    frameScopesWorkAround("");
  }

  public void testFrameScopesWorkAroundNoDebug()
  {
    frameScopesWorkAround("-nodebug");
  }

  public void testFrameScopesWorkAroundNoEH()
  {
    frameScopesWorkAround("-noeh");
  }

  public void frameScopesWorkAround(String ext)
  {
    
      Task task = (new DaemonBlockedAtSignal("funit-scopes-workaround" + ext)).getMainTask();
    Frame frame = StackFactory.createFrame(task);
    
    Dwfl dwfl = DwflCache.getDwfl(task);
    DwflDie bias = dwfl.getCompilationUnit(frame.getAdjustedAddress());
    DwarfDie[] scopes = bias.getScopes(frame.getAdjustedAddress() - bias.getBias());
    scopes = scopes[0].getScopesDie();
    
    assertEquals("number of scopes", 4, scopes.length);
    
    assertEquals("inlined die" , DwTag.INLINED_SUBROUTINE,scopes[1].getTag());
    assertEquals("function die", DwTag.SUBPROGRAM, scopes[2].getTag());
    assertEquals("compliation unit die", DwTag.COMPILE_UNIT, scopes[3].getTag());
    
  }
  
  public void testGetInlinedSubroutines()
  {
    getInlinedSubroutines("");
  }

  public void testGetInlinedSubroutinesNoDebug()
  {
    getInlinedSubroutines("-nodebug");
  }

  public void testGetInlinedSubroutinesNoEH()
  {
    getInlinedSubroutines("-noeh");
  }

  public void getInlinedSubroutines(String ext)
  {
    
      Task task = (new DaemonBlockedAtSignal("funit-stack-inlined" + ext)).getMainTask();
    DebugInfoFrame frame = DebugInfoStackFactory.createDebugInfoStackTrace(task);
    
    LinkedList inlinedSubprograms =  frame.getInlinedSubprograms();
    
    assertEquals("Number of inline functions",3,inlinedSubprograms.size());
    
  }
  
  public void testVirtualStackTrace()
  {
    virtualStackTrace("");
  }

  public void testVirtualStackTraceNoDebug()
  {
    virtualStackTrace("-nodebug");
  }

  public void testVirtualStackTraceNoEH()
  {
    virtualStackTrace("-noeh");
  }

  public void virtualStackTrace(String ext)
  {
    
      Task task = (new DaemonBlockedAtSignal("funit-stack-inlined" + ext)).getMainTask();
    StringWriter stringWriter = new StringWriter();
    
    PrintStackOptions options = new PrintStackOptions();
    options.setNumberOfFrames(0);
    options.setPrintParams(true);
    options.setPrintLocals(true);
    options.setPrintFullPaths(true);
    DebugInfoStackFactory.printVirtualTaskStackTrace(new PrintWriter(stringWriter), task, options);
    
    assertTrue("contains inline", stringWriter.getBuffer().toString().contains("inline"));
    assertTrue("contains first", stringWriter.getBuffer().toString().contains("first"));
    assertTrue("contains second", stringWriter.getBuffer().toString().contains("second"));
    assertTrue("contains third", stringWriter.getBuffer().toString().contains("third"));
    assertTrue("contains main", stringWriter.getBuffer().toString().contains("main"));
  }
  
  public void testVirtualStackTraceWithLocals()
  {
    
      Task task = (new DaemonBlockedAtSignal("funit-stack-inlined")).getMainTask();
    
    PrintStackOptions options = new PrintStackOptions();
    options.setNumberOfFrames(0); 
    options.setPrintParams(true);
    options.setPrintLocals(true);
    options.setPrintFullPaths(true);
    
    DebugInfoFrame frame = DebugInfoStackFactory.createVirtualStackTrace(task);
    frame = frame.getOuterDebugInfoFrame();
    
    StringWriter stringWriter = new StringWriter();
    frame.printScopes(new PrintWriter(stringWriter));
    
    assertTrue("Contains var2", stringWriter.getBuffer().toString().contains("var2"));
  }
  
  public void testInlinedFunctionDerailment()
  {
    inlinedFunctionDerailment("");
  }

  public void testInlinedFunctionDerailmentNoDebug()
  {
    inlinedFunctionDerailment("-nodebug");
  }

  public void testInlinedFunctionDerailmentNoEH()
  {
    inlinedFunctionDerailment("-noeh");
  }

  // test that a Subprogram can be retrieved for a function even
  // if the call stack contains calls to inlined functions inner
  // to it.
  public void inlinedFunctionDerailment(String ext)
  {
  
      Task task = (new DaemonBlockedAtSignal("funit-stack-inlined" + ext)).getMainTask();
    
    DebugInfoFrame frame = DebugInfoStackFactory.createVirtualStackTrace(task);
    Function subprogram = null;
    
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
    values("");
  }

  public void testValuesNoDebug() throws NameNotFoundException
  {
    values("-nodebug");
  }

  public void testValuesNoEH() throws NameNotFoundException
  {
    values("-noeh");
  }

  public void values(String ext) throws NameNotFoundException
  {
    Task task = (new DaemonBlockedAtSignal("funit-stacks-values" + ext)).getMainTask();
    Function subprogram;
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
  
  public void testLineNumbers()
  {
    lineNumbers("");
  }

  public void testLineNumbersNoDebug()
  {
    lineNumbers("-nodebug");
  }

  public void testLineNumbersNoEH()
  {
    lineNumbers("-noeh");
  }

  public void lineNumbers(String ext)
  {
      Task task = (new DaemonBlockedAtSignal("funit-stacks-linenum" + ext)).getMainTask();
      
      Function subprogram;
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

  public void testThatArtificialParametersAreIgnored()
  {
    artificialParametersAreIgnored("");
  }

  public void testThatArtificialParametersAreIgnoredNoDebug()
  {
    artificialParametersAreIgnored("-nodebug");
  }

  public void testThatArtificialParametersAreIgnoredNoEH()
  {
    artificialParametersAreIgnored("-noeh");
  }

  public void artificialParametersAreIgnored(String ext)
  {

	Task task = (new DaemonBlockedAtSignal("funit-cpp-scopes-class" + ext)).getMainTask();
	      
	DebugInfoFrame frame = DebugInfoStackFactory
		.createVirtualStackTrace(task);
	Function subprogram = frame.getSubprogram();

	LinkedList parameters = subprogram.getParameters();
	assertEquals("Correct number of parameters", 1, parameters.size());

	// Check that the non artificial variable was not ignored
	Variable variable = (Variable) parameters.getFirst();
	assertEquals("Correct variable was found", "i", variable.getName());

    }

}
