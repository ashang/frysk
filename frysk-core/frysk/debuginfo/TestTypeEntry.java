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

import java.util.Iterator;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lib.dwfl.DwarfDie;
import lib.dwfl.Dwfl;
import lib.dwfl.DwflDieBias;
import frysk.dwfl.DwflCache;
import frysk.proc.Task;
import frysk.testbed.DaemonBlockedAtSignal;
import frysk.testbed.TestLib;
import frysk.value.CompositeType;
import frysk.value.Type;
import frysk.value.CompositeType.DynamicMember;
import frysk.value.CompositeType.Member;
import frysk.value.CompositeType.StaticMember;

public class TestTypeEntry
    extends TestLib
{
    private class Expect
    {
	String symbol;
	String output;
	Expect (String symbol, String expect)
	{
	    this.symbol = symbol;
	    this.output = expect;
	}
    }
  Logger logger = Logger.getLogger("frysk");

  public void testScalar () {
      Expect [] expect  = {
	      new Expect("long_21", "long int"),
	      new Expect("int_21","int"),
	      new Expect("int_22", "volatile simode"),
	      new Expect("static_int", "int"),
	      new Expect("int_p", "volatile int *"),
	      new Expect("short_21", "short int"),
	      new Expect("char_21", "char"),
	      new Expect("float_21", "float"),
	      new Expect("double_21", "double"),
      };
  
      Task task = (new DaemonBlockedAtSignal("funit-scalar")).getMainTask();
      
      DebugInfoFrame frame = DebugInfoStackFactory.createVirtualStackTrace(task);
      Dwfl dwfl;
      DwarfDie[] allDies;
      Type varType;
      DwarfDie varDie;
      long pc = frame.getAdjustedAddress();
      dwfl = DwflCache.getDwfl(frame.getTask());
      DwflDieBias bias = dwfl.getCompilationUnit(pc);
      DwarfDie die = bias.die;
      allDies = die.getScopes(pc - bias.bias);
      TypeEntry typeEntry = new TypeEntry(frame.getTask().getISA());
    
      for (int i = 0; i < expect.length; i++) {
	  varDie = die.getScopeVar(allDies, expect[i].symbol);
	  assertNotNull(varDie);
	  varType = typeEntry.getType(varDie.getType());
	  assertNotNull(varType);
	  assertEquals("testScalar " + expect[i].symbol, expect[i].output, varType.toPrint());
      }
  }

  public void testArray () {
      Expect[] expect = {
	      new Expect("arr_1", "long int [32]"),
	      new Expect("arr_2","int [5][6]"),
	      new Expect("arr_3", "float [4][5]"),
	      new Expect("arr_4", "char [4]"),
      };
  
      Task task = (new DaemonBlockedAtSignal("funit-array")).getMainTask();
      
      DebugInfoFrame frame = DebugInfoStackFactory.createVirtualStackTrace(task);
      Dwfl dwfl;
      DwarfDie[] allDies;
      Type varType;
      DwarfDie varDie;
      long pc = frame.getAdjustedAddress();
      dwfl = DwflCache.getDwfl(frame.getTask());
      DwflDieBias bias = dwfl.getCompilationUnit(pc);
      DwarfDie die = bias.die;
      allDies = die.getScopes(pc - bias.bias);
      TypeEntry typeEntry = new TypeEntry(frame.getTask().getISA());
    
      for (int i = 0; i < expect.length; i++) {
	  varDie = die.getScopeVar(allDies, expect[i].symbol);
	  assertNotNull(varDie);
	  varType = typeEntry.getType(varDie.getType());
	  assertNotNull(varType);
	  assertEquals("testArray " + expect[i].symbol, expect[i].output, varType.toPrint());
      }
  }

  public void testEnum () {
      if (unresolved(4998))
	  return;

      Expect[] expect = {
	      new Expect("sportscar", ".*enum.*bmw=0.*porsche=1.*"),
	      new Expect("ssportscar", ".*enum.*bmw.*porsche.*"),
      };
  
      Task task = (new DaemonBlockedAtSignal("funit-enum")).getMainTask();
      DebugInfoFrame frame = DebugInfoStackFactory.createVirtualStackTrace(task);
      Dwfl dwfl;
      DwarfDie[] allDies;
      Type varType;
      DwarfDie varDie;
      long pc = frame.getAdjustedAddress();
      dwfl = DwflCache.getDwfl(frame.getTask());
      DwflDieBias bias = dwfl.getCompilationUnit(pc);
      DwarfDie die = bias.die;
      allDies = die.getScopes(pc - bias.bias);
      TypeEntry typeEntry = new TypeEntry(frame.getTask().getISA());
    
      for (int i = 0; i < expect.length; i++) {
	  varDie = die.getScopeVar(allDies, expect[i].symbol);
	  if (varDie == null)
	      varDie = DwarfDie.getDeclCU(allDies, expect[i].symbol);
	  if (varDie == null) {
	      continue;
	  }
	  varType = typeEntry.getType(varDie.getType());
	  Pattern p = Pattern.compile(expect[i].output, Pattern.DOTALL);
	  Matcher m = p.matcher(varType.toPrint());
	  assertTrue("testEnum " + expect[i].symbol, m.matches());
      }
  }

  public void testStruct () {
      Expect[] expect = {
	      new Expect("static_class", ".*static_class_t.*"),
	      new Expect("class_1", ".*static_class_t.*"),
	      new Expect("class_2", ".*c1;.*c2;.*"),
	      new Expect("class_3", ".*int.*arr.*.2..2..*"),
	      new Expect("class_4", ".*int x;.*float y;.*"),
	      new Expect("class_5", ".*simode.*x;.*float.*y;.*"),
	      new Expect("union_1", ".*union_t.*"),
	      new Expect("class_p", ".*static_class_t.*"),
      };
  
      Task task = (new DaemonBlockedAtSignal("funit-struct")).getMainTask();

      DebugInfoFrame frame = DebugInfoStackFactory.createVirtualStackTrace(task);
      Dwfl dwfl;
      DwarfDie[] allDies;
      Type varType;
      DwarfDie varDie;
      long pc = frame.getAdjustedAddress();
      dwfl = DwflCache.getDwfl(frame.getTask());
      DwflDieBias bias = dwfl.getCompilationUnit(pc);
      DwarfDie die = bias.die;
      allDies = die.getScopes(pc - bias.bias);
      TypeEntry typeEntry = new TypeEntry(frame.getTask().getISA());
    
      for (int i = 0; i < expect.length; i++) {
	  varDie = die.getScopeVar(allDies, expect[i].symbol);
	  varType = typeEntry.getType(varDie.getType());
	  Pattern p = Pattern.compile(expect[i].output, Pattern.DOTALL);
	  Matcher m = p.matcher(varType.toPrint());
	  assertTrue("testStruct " + expect[i].symbol, m.matches());
      }
  }

  public void testClass () {
      Expect[] expect = {
	      new Expect("mb", ".*public:.*char.*\\*.*msg;.*void.*Base1.*"
		      + "char.*\\*.*void.*~Base1.*char.*\\*.*msg;.*" 
		      + "void.*Base2.*char.*\\*.*void.*~Base2.*private:.*"
		      + "char.*\\*.*note;.*void.*Type.*char.*\\*.*char.*\\*.*"
		      + "char.*void.*~Type.*"
		      )
      };
  
      Task task = (new DaemonBlockedAtSignal("funit-class")).getMainTask();

      DebugInfoFrame frame = DebugInfoStackFactory.createVirtualStackTrace(task);
     

      Dwfl dwfl;
      DwarfDie[] allDies;
      Type varType;
      DwarfDie varDie;
      long pc = frame.getAdjustedAddress();
      dwfl = DwflCache.getDwfl(frame.getTask());
      DwflDieBias bias = dwfl.getCompilationUnit(pc);
      DwarfDie die = bias.die;
      allDies = die.getScopes(pc - bias.bias);
      TypeEntry typeEntry = new TypeEntry(frame.getTask().getISA());
    
      for (int i = 0; i < expect.length; i++) {
	  varDie = die.getScopeVar(allDies, expect[i].symbol);
	  varType = typeEntry.getType(varDie.getType());
	  Pattern p = Pattern.compile(expect[i].output, Pattern.DOTALL);
	  Matcher m = p.matcher(varType.toPrint());
	  assertTrue("testClass " + expect[i].symbol, m.matches());
      }
  }
  
  public void testClassWithStaticMembers () {

      if(unresolved(5301)){
	  return;
      }
      
      Task task = (new DaemonBlockedAtSignal("funit-class-static")).getMainTask();

      DebugInfoFrame frame = DebugInfoStackFactory.createVirtualStackTrace(task);
      
      CompositeType compositeType = (CompositeType) frame.getSubprogram().getComposite().getType();
      
      assertNotNull("Retrieved type successfully", compositeType);
      
      Iterator iterator = compositeType.iterator();
      
      while(iterator.hasNext()){
	  Member member = (Member) iterator.next();
	  
	  if(member.getName().equals("i")){
	      assertTrue("Member " + member.getName() + " has the correct class", member instanceof DynamicMember);
	  }
	
	  if(member.getName().equals("static_i")){
	      assertTrue("Member " + member.getName() + " has the correct class", member instanceof StaticMember);
	  }
	
	  if(member.getName().equals("crash")){
	      assertTrue("Member " + member.getName() + " has the correct class", member instanceof DynamicMember);
	  }
	
	  if(member.getName().equals("static_crash")){
	      assertTrue("Member " + member.getName() + " has the correct class", member instanceof StaticMember);
	  }
      }
  }

}  
