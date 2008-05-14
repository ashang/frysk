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

import java.io.File;

import frysk.config.Prefix;
import frysk.proc.Task;
import frysk.scopes.Variable;
import frysk.testbed.DaemonBlockedAtSignal;
import frysk.testbed.TestLib;
import frysk.testbed.TestfileTokenScanner;
import frysk.value.ObjectDeclaration;

/**
 * These tests test the top-down search which of the search engine. Top down
 * search does not require a scope, and is simply a flat search of the
 * debuginfo.
 */
public class TestObjectDeclarationSearchEngineTopDown extends TestLib {

    ObjectDeclarationSearchEngine objectDeclarationSearchEngine;

    private File getSrc(String name) {
	return Prefix.sourceFile("frysk-core/frysk/pkglibdir/" + name);
    }

    public void testGetObject() {
	String objectName = "first";
	String objectToken = objectName;
	String fileName = "funit-c-scopes";
	File srcPath = getSrc(fileName + ".c");

	verifyObjectFound(objectName, objectToken, fileName, srcPath);
    }
    
    public void testGetObjectHashFileHashSymbol() {
	
	String objectName = "funit-scopes-multi-file-b.c#first";
	String objectToken = "*this*";
	String fileName = "funit-scopes-multi-file";
	File srcPath = getSrc("funit-scopes-multi-file-b.c");

	verifyObjectFound(objectName, objectToken, fileName, srcPath);
    }
    
    public void testGetObjectHashFileHashSymbolOther() {
	
	String objectName = "funit-scopes-multi-file-a.c#first";
	String objectToken = "*other*";
	String fileName = "funit-scopes-multi-file";
	File srcPath = getSrc("funit-scopes-multi-file-a.c");

	verifyObjectFound(objectName, objectToken, fileName, srcPath);
    }
    
    
    private void verifyObjectFound(String objectName, String objectToken,
	    String fileName, File srcPath) {

	TestfileTokenScanner scanner = new TestfileTokenScanner(srcPath);
	int objectLine = scanner.findTokenLine(objectToken);
	
	Task task = (new DaemonBlockedAtSignal(fileName)).getMainTask();
	DebugInfoFrame frame = DebugInfoStackFactory
		.createVirtualStackTrace(task);
	objectDeclarationSearchEngine = new ObjectDeclarationSearchEngine(frame);
	ObjectDeclaration objectDeclaration = (ObjectDeclaration) objectDeclarationSearchEngine
		.getObject(objectName);

	assertNotNull("Variable found", objectDeclaration);
	assertTrue("Correct name", objectName.endsWith(objectDeclaration.getName()));
	assertEquals("Found the correct variable on the correct line ",
		objectLine, objectDeclaration.getSourceLocation().getLine());

	// Negative test:
	try {
	    objectDeclaration = (Variable) objectDeclarationSearchEngine
		    .getObject("NOT" + objectName);
	    assertTrue("Exception was not thrown", false);
	} catch (ObjectDeclarationNotFoundException e) {
	    // exception was thrown
	}
    }

}
