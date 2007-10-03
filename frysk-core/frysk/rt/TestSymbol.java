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

package frysk.rt;

import frysk.proc.Action;
import frysk.proc.Manager;
import frysk.proc.Task;
import frysk.proc.TaskObserver;

public class TestSymbol
    extends frysk.proc.TestLib
{
    private void frameTest (String command, int numberOfArgs,
			    String name, boolean addressValid, boolean sizeValid)
    {
	class RunToCrash
	    extends TaskObserverBase
	    implements TaskObserver.Attached, TaskObserver.Signaled
	{
	    Task task;
	    public Action updateAttached (Task task)
	    {
		task.requestAddSignaledObserver (this);
		task.requestDeleteAttachedObserver(this);
		return Action.CONTINUE;
	    }
	    public Action updateSignaled (Task task, int value)
	    {
		this.task = task;
		Manager.eventLoop.requestStop();
		return Action.BLOCK;
	    }
	}
	RunToCrash runToCrash = new RunToCrash ();

	// Construct an argument list containing numberOfArgs dummy
	// arguments.  The inferior program just looks at ARGC to
	// determine what to do.
	String[] fullCommand = new String[numberOfArgs];
	fullCommand[0] =  getExecPath (command);
    	for (int i = 1; i < fullCommand.length; i++) {
	    fullCommand[i] = "0";
	}
    
	// Run the target program throuth to its termination.
	Manager.host.requestCreateAttachedProc(fullCommand, runToCrash);
	assertRunUntilStop("Run to crash");

	// Extract the stack from the signalled program and validate
	// the inner-most frame's symbol matches the expected.
	Frame frame = StackFactory.createStackFrame(runToCrash.task);

	// XXX: Need to call frame.getSymbol () and then validate
	// that.
	Symbol symbol = frame.getSymbol ();
	assertEquals ("symbol " + name, name, symbol.getDemangledName ());
	assertEquals ("symbol address valid", addressValid, symbol.getAddress() != 0);
	// XXX: Can't yet get the size of the symbol
	// assertEquals ("symbol size valid", sizeValid, symbol.getSize() > 0);
    }

    /**
     * What to expect when there is no symbol.
     */
    private String unknown = Symbol.UNKNOWN.getName ();

    public void testDebug ()
    {
	frameTest("funit-stackframe", 1, "global_st_size", true, true);
    }
  
    public void testNoDebug ()
    {
	frameTest("funit-stackframe-nodebug", 1, "global_st_size", true, true);
    }
  
    public void testStripped ()
    {
	frameTest("funit-stackframe-stripped", 1, unknown, false, false);
    }
  
    public void testStaticDebug ()
    {
	frameTest("funit-stackframe", 2, "local_st_size", true, true);
    }
  
    public void testStaticNoDebug ()
    {
	frameTest("funit-stackframe-nodebug", 2, "local_st_size", true, true);
    }
  
    public void testStaticStripped ()
    {
	frameTest("funit-stackframe-stripped", 2, unknown, false, false);
    }
  
    public void testNoSize()
    {
	frameTest("funit-stackframe", 3, "global_st_size_0", true, false);
    }
  
    public void testNoDebugNoSize()
    {
	frameTest("funit-stackframe-nodebug", 3, "global_st_size_0", true, false);   
    }
  
    public void testStrippedNoSize()
    {
	frameTest("funit-stackframe-stripped", 3, unknown, false, false);    
    }
  
    public void testStaticNoSize()
    {
	frameTest("funit-stackframe", 4, "local_st_size_0", true, false);    
    }
  
    public void testStaticNoDebugNoSize()
    {
	frameTest("funit-stackframe-nodebug", 4, "local_st_size_0", true, false);   
    }
  
    public void testStaticStrippedNoSize()
    {
	frameTest("funit-stackframe-stripped", 4, unknown, false, false);    
    }
}
