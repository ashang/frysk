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

package frysk.scopes;

import java.util.Iterator;
import java.util.LinkedList;
import lib.dwfl.DwAt;
import lib.dwfl.DwTag;
import lib.dwfl.DwarfDie;
import lib.dwfl.Dwfl;
import lib.dwfl.DwflDieBias;
import lib.dwfl.DwflModule;
import frysk.config.Prefix;
import frysk.debuginfo.DebugInfoFrame;
import frysk.debuginfo.DebugInfoStackFactory;
import frysk.debuginfo.ObjectDeclarationSearchEngine;
import frysk.dwfl.DwflCache;
import frysk.proc.Task;
import frysk.stack.Frame;
import frysk.stack.StackFactory;
import frysk.testbed.DaemonBlockedAtSignal;
import frysk.testbed.TestLib;
import frysk.testbed.TestfileTokenScanner;

public class TestDie
    extends TestLib
{
    
    
    public void testGetLine(){
	String fileName = "funit-cpp-scopes-namespace";
	Task task = (new DaemonBlockedAtSignal(fileName)).getMainTask();
	DebugInfoFrame frame = DebugInfoStackFactory.createDebugInfoStackTrace(task);
	ObjectDeclarationSearchEngine objectDeclarationSearchEngine = new ObjectDeclarationSearchEngine(frame);
	
	Variable variable = (Variable) objectDeclarationSearchEngine.getObjectInScope("first");
	
	assertNotNull("Variable found", variable);
	
	
	TestfileTokenScanner scanner = new TestfileTokenScanner(Prefix.sourceFile("frysk-core/frysk/pkglibdir/" + fileName + ".cxx"));
	int expectedLine = scanner.findTokenLine("first");

	assertEquals("Correct line number was found", expectedLine, variable.getLineNumber());
    }

    public void testGetOriginalDie(){
	String fileName = "funit-cpp-scopes-class";
	Task task = (new DaemonBlockedAtSignal(fileName)).getMainTask();
	DebugInfoFrame frame = DebugInfoStackFactory.createDebugInfoStackTrace(task);
	DwarfDie die = frame.getSubprogram().getDie();
	
	
	boolean hasAttribute = die.hasAttribute(DwAt.ABSTRACT_ORIGIN) ||
	                       die.hasAttribute(DwAt.SPECIFICATION);
	
	assertTrue("Function has abstract origin ", hasAttribute);
	
	die = die.getOriginalDie();
	
	assertNotNull("Found original die", die);
	assertEquals("Die has correct name", "crash" ,die.getName());
	
    }
    
    public void testGetPubnames(){
	String fileName = "funit-class-static";
	Task task = (new DaemonBlockedAtSignal(fileName)).getMainTask();
	long pc = StackFactory.createFrame(task).getAdjustedAddress();
	
	Dwfl dwfl = DwflCache.getDwfl(task);
	DwflModule dwflModule = dwfl.getModule(pc);
	LinkedList pubnames = dwflModule.getPubNames();
	Iterator iterator = pubnames.iterator();
	
	assertEquals("Size of pubnames ", 3, pubnames.size());
	
	DwarfDie die = ((DwflDieBias) iterator.next()).die;
	assertEquals("Die name", "crash", die.getName());
	
	die = ((DwflDieBias) iterator.next()).die;
	assertEquals("Die name", "main", die.getName());
	
	die = ((DwflDieBias) iterator.next()).die;
	assertEquals("Die name", "static_i", die.getName());
    }

    public void testGetDefinition(){
	
	String fileName = "funit-class-static";
	Task task = (new DaemonBlockedAtSignal(fileName)).getMainTask();
	Frame frame = StackFactory.createFrame(task);
	long pc = frame.getAdjustedAddress();
	
	Dwfl dwfl = DwflCache.getDwfl(task);
	DwarfDie cu = dwfl.getCompilationUnit(pc).die;
	DwarfDie[] scopes = cu.getScopes(pc);
	DwarfDie die = null;
	
	for (int i = 0; i < scopes.length; i++) {
	    if(scopes[i].getTag().equals(DwTag.SUBPROGRAM)){
		die = scopes[i];
		break;
	    }
	}
	
	assertTrue("Die is a definition",
		die.hasAttribute(DwAt.SPECIFICATION) ||
		die.hasAttribute(DwAt.ABSTRACT_ORIGIN));
	
	DwarfDie originalDie = die.getOriginalDie();
	
	assertTrue("Found declaration die", originalDie.isDeclaration());
	
	DwarfDie definitionDie = originalDie.getDefinition();
	
	assertNotNull(definitionDie);
	
	assertEquals("dies have the same name ", die.getName(), definitionDie.getName());
	assertEquals("dies have the same modules ", die.getOffset(), definitionDie.getOffset());
    }

}
