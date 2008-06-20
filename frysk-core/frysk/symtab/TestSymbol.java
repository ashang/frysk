// This file is part of the program FRYSK.
//
// Copyright 2006, 2007 Red Hat Inc.
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

package frysk.symtab;

import frysk.proc.Task;
import frysk.testbed.TestLib;
import frysk.testbed.DaemonBlockedAtSignal;

public class TestSymbol
    extends TestLib
{
    private void symbolTest(String command, int numberOfArgs,
			    String name, boolean addressValid,
			    boolean sizeValid) {
	// Construct an argument list numberOfArgs long (including
	// arg[0] the program).  The inferior program just looks at
	// ARGC to determine what to do.
	String[] fullCommand = new String[numberOfArgs];
	fullCommand[0] =  getExecPath (command);
    	for (int i = 1; i < fullCommand.length; i++) {
	    fullCommand[i] = Integer.toString(i);
	}
    	
    	DaemonBlockedAtSignal daemon = new DaemonBlockedAtSignal(fullCommand);
    	Task task = daemon.getMainTask();

	long pc = task.getPC();
	Symbol symbol = SymbolFactory.getSymbol(task, pc);
	assertEquals ("symbol name", name, symbol.getDemangledName ());
	assertEquals ("symbol address valid", addressValid,
		      symbol.getAddress() != 0);
	assertEquals ("symbol size valid", sizeValid, symbol.getSize() > 0);
    }

    private void symbolTest(int numberOfArgs, String name, boolean addressValid,
			    boolean sizeValid) {
	symbolTest("funit-symbols", numberOfArgs, name, addressValid, sizeValid);
    }

    /**
     * What to expect when there is no symbol.
     */
    private String unknown = SymbolFactory.UNKNOWN.getName ();

    public void testDebug () {
	symbolTest("funit-symbols", 2, "global_st_size", true, true);
    }
  
    public void testNoDebug () {
	symbolTest("funit-symbols-nodebug", 2, "global_st_size", true, true);
    }
  
    public void testStripped () {
	symbolTest("funit-symbols-stripped", 2, unknown, false, false);
    }
  
    public void testStaticDebug () {
	symbolTest("funit-symbols", 3, "local_st_size", true, true);
    }
  
    public void testStaticNoDebug () {
	symbolTest("funit-symbols-nodebug", 3, "local_st_size", true, true);
    }
  
    public void testStaticStripped () {
	symbolTest("funit-symbols-stripped", 3, unknown, false, false);
    }
  
    public void testNoSize() {
	symbolTest("funit-symbols", 4, "global_st_size_0", true, false);
    }
  
    public void testNoDebugNoSize() {
	symbolTest("funit-symbols-nodebug", 4, "global_st_size_0",
		   true, false);   
    }
  
    public void testStrippedNoSize() {
	symbolTest("funit-symbols-stripped", 4, unknown, false, false);    
    }
  
    public void testStaticNoSize() {
	symbolTest("funit-symbols", 5, "local_st_size_0", true, false);    
    }
    public void testStaticNoDebugNoSize() {
	symbolTest("funit-symbols-nodebug", 5, "local_st_size_0", true, false);   
    }
    public void testStaticStrippedNoSize() {
	symbolTest("funit-symbols-stripped", 5, unknown, false, false);    
    }

    public void testGlobalInGlobal() {
	symbolTest(6, "global_in_global", true, true);
    }
    public void testLocalInGlobal() {
        if (unresolved(5941))
            return;
	symbolTest(7, "local_in_global", true, true);
    }
    public void testGlobalInLocal() {
	symbolTest(8, "global_in_local", true, true);
    }
    public void testLocalInLocal() {
	symbolTest(9, "local_in_local", true, true);
    }

    public void testGlobalAfterNested() {
	symbolTest(10, "global_outer", true, true);
    }
    public void testLocalAfterNested() {
	symbolTest(11, "local_outer", true, true);
    }

    public void testNoSymbolAfterGlobal() {
	symbolTest(12, unknown, false, false);
    }
    public void testNoSymbolAfterLocal() {
	symbolTest(13, unknown, false, false);
    }

    public void testGlobalSize0InGlobal() {
	symbolTest(14, "global_after_0", true, true);
    }
    public void testLocalSize0InGlobal() {
	symbolTest(15, "global_after_0", true, true);
    }
    public void testGlobalSize0InLocal() {
	symbolTest(16, "local_after_0", true, true);
    }
    public void testLocalSize0InLocal() {
	symbolTest(17, "local_after_0", true, true);
    }

    public void testGlobalAfterNestedSize0() {
	symbolTest(18, "global_after_0", true, true);
    }
    public void testLocalAfterNestedSize0() {
	symbolTest(19, "local_after_0", true, true);
    }

    public void testSmallGlobalAtLargeGlobal() {
	symbolTest(20, "small_global_at_large_global", true, true);
    }
    public void testSmallLocalAtLargeGlobal() {
        if (unresolved(5941))
            return;
	symbolTest(21, "small_local_at_large_global", true, true);
    }
    public void testSmallGlobalAtLargeLocal() {
	symbolTest(22, "small_global_at_large_local", true, true);
    }
    public void testSmallLocalAtLargeLocal() {
	symbolTest(23, "small_local_at_large_local", true, true);
    }

    public void testAfterGlobalContiningSize0() {
	symbolTest(24, unknown, false, false);
    }
    public void testAfterLocalContiningSize0() {
	symbolTest(25, unknown, false, false);
    }
}
