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

import java.io.File;

import frysk.Config;
import frysk.proc.Task;
import frysk.testbed.DaemonBlockedAtSignal;
import frysk.testbed.TestLib;
import frysk.testbed.TestfileTokenScanner;
import frysk.value.ObjectDeclaration;
import frysk.value.Variable;

/**
 * Tests @link CppVariableSearchEngine.
 * 
 * Each tests is testing the the @link {@link ObjectDeclarationSearchEngine} can find the
 * correct variable, by insuring that the line number of the found 
 * {@link Variable} is the same as the line number of the variable in the source
 * file.
 * 
 * To add a new test copy one of the tests here and change the variable name the
 * variable token if the variable name is not unique, fileName if you do not want
 * to use funit-c-scopes, and finally exec and src paths of they are something
 * other than the default. 
 *
 */
public class TestObjectDeclarationSearchEngine extends TestLib{
    
    ObjectDeclarationSearchEngine objectDeclarationSearchEngine = new ObjectDeclarationSearchEngine();
    
    public void testFindVar1Scopes(){
	String variableName = "var1"; 
	String variableToken = variableName; 
	String fileName = "funit-c-scopes";
	String srcPath = Config.getPkgLibSrcDir() + fileName + ".c";
	
	verifyVariable(variableName, variableToken, fileName, srcPath);
    }
    
    public void testFindVar2Scopes(){
	String variableName = "var2"; 
	String variableToken = variableName; 
	String fileName = "funit-c-scopes";
	String srcPath = Config.getPkgLibSrcDir() + fileName + ".c";
	
	verifyVariable(variableName, variableToken, fileName, srcPath);
    }
    
    public void testFindArg1Scopes(){
	String variableName = "arg1"; 
	String variableToken = variableName; 
	String fileName = "funit-c-scopes";
	String srcPath = Config.getPkgLibSrcDir() + fileName + ".c";
	
	verifyVariable(variableName, variableToken, fileName, srcPath);
    }
    
    public void testFindIScopesShadowing(){
	String variableName = "i"; 
	String variableToken = "second i"; 
	String fileName = "funit-c-scopes-shadowing";
	String srcPath = Config.getPkgLibSrcDir() + fileName + ".c";
	
	verifyVariable(variableName, variableToken, fileName, srcPath);
    }
    
    public void testFindFirstScopesNamespace(){
	String variableName = "first"; 
	String variableToken = variableName; 
	String fileName = "funit-cpp-scopes-namespace";
	String srcPath = Config.getPkgLibSrcDir() + fileName + ".cxx";
	
	verifyVariable(variableName, variableToken, fileName, srcPath);
    }
    
    public void testFindTwoScopesEnum(){
//	if(unresolved(5166)){
//	    return;
//	}
	
	String variableName = "two"; 
	String fileName = "funit-c-scopes-enum";
	
	Task task = (new DaemonBlockedAtSignal(fileName)).getMainTask();
    	DebugInfoFrame frame = DebugInfoStackFactory.createVirtualStackTrace(task);
    	
    	ObjectDeclaration declaredObject = objectDeclarationSearchEngine.get(frame, variableName);
 
    	assertEquals("Object has the correct name", declaredObject.getName(), variableName);

    }
    
    
    
    
    /**
         * Runs the given executable until it sigfaults then searches for the
         * variable with the given name from that point. Then it verifies that
         * the line number of the variable found is the same as the line number
         * in the source file where the given token is.
         * 
         * @param variableName
         *                Name of the variable to search for.
         * @param variableToken
         *                a token string from the source file that is found on
         *                the same line as the variable.
         * @param fileName
         *                name of the test file.
         * @param execPath
         *                path to the executable to be run.
         * @param srcPath
         *                path to the source file from which the executable was
         *                created
         */
    private void verifyVariable(String variableName,
	    String variableToken,
	    String fileName,
	    String srcPath){
	
	TestfileTokenScanner scanner = new TestfileTokenScanner(new File(srcPath));
	int variableLine = scanner.findTokenLine(variableToken);
	Task task = (new DaemonBlockedAtSignal(fileName)).getMainTask();
	DebugInfoFrame frame = DebugInfoStackFactory.createVirtualStackTrace(task);
	
	Variable variable = (Variable) objectDeclarationSearchEngine.get(frame, variableName);

	assertNotNull("Variable found", variable);
	assertTrue("Found the correct variable", variable.getLineNumber() == variableLine);
	
	//Negative test:
	variable = (Variable) objectDeclarationSearchEngine.get(frame, "NOT"+variableName);
	assertNull("Bogus object was not found", variable);
    }

}