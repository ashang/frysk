// This file is part of the program FRYSK.
//
// Copyright 2008 Red Hat Inc.
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

package frysk.hpd;

import java.util.List;

import frysk.proc.Action;
import frysk.proc.Manager;
import frysk.proc.Task;
import frysk.proc.TaskObserver;
import frysk.sys.FileDescriptor;
import frysk.util.CountDownLatch;
import frysk.util.PtyTerminal;
import frysk.isa.signals.Signal;

class ShellCommand extends NoOptsCommand {
    
    ShellCommand() {
	super(  "Execute a shell command.", "shell <command string>", 
	        "The shell command invokes a bash shell and executes the "
	      + "command string.");
    }
    
    int complete(CLI cli, Input buffer, int cursor, List candidates) {
	return -1;
    }
    
    void interpretCommand(CLI cli, Input cmd) {
		
	if (cmd.size() == 0) {
	    throw new InvalidCommandException ("Missing command string");
	}
	// Use latch to keep fhpd waiting until 
	// shell command terminates.
	final CountDownLatch latch = new CountDownLatch(1);
	
	String[] command = new String [3];
	command [0] = "/bin/bash";
	command[1] = "-c";
	command[2] = cmd.toString().substring(cmd.toString().indexOf(" "));
	
	// Set terminal to initial setting.
	PtyTerminal.setToInitConsole(FileDescriptor.in);

	// Request an attached proc to execute command.
  	Manager.host.requestCreateAttachedProc(null, null, null, command,
  		new TaskObserver.Attached()
  	{  	    
  	    public Action updateAttached (Task task)
  	    {
  		// On termination of command, resume fhpd.
  		task.requestAddTerminatedObserver(new TaskObserver.Terminated () 
  		{
  		    public Action updateTerminated(Task task, Signal signal, int value) {
  			// Count down the latch so fhpd can continue
  			latch.countDown();
  			return Action.CONTINUE;
  		    }  	  	    
  	  	    public void addFailed  (Object observable, Throwable w) {
  	  	    }
  	  	    public void addedTo(Object observable) {
  	  	    }
  	  	    public void deletedFrom(Object observable) {
  	  	    }
  		    
  		});	   		
  		return Action.CONTINUE;
  	    }
  	    
  	    public void addFailed  (Object observable, Throwable w) {
  		throw new RuntimeException("addFailed: " + observable + " cause: " + w);
  	    }
  	    public void addedTo(Object observable) {
  	    }
  	    public void deletedFrom(Object observable) {
  	    }
  	});
  	
  	// Make fhpd wait till shell command is complete.
  	while (true) {
  	    try {
  		latch.await();
  		break;
  	    } catch (InterruptedException e) {
  		throw new RuntimeException(e.getMessage());
  	    }
  	}  	  	
  	
  	// Set terminal back to fhpd settings, i.e. character buffered.
	PtyTerminal.setToCharBufferedConsole(FileDescriptor.in);
    }
}
