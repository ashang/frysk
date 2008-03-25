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

package frysk.bindir;

import java.io.PrintWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import frysk.debuginfo.PrintStackOptions;
import frysk.isa.syscalls.Syscall;
import frysk.proc.Action;
import frysk.proc.Task;
import frysk.proc.TaskObserver;
import frysk.proc.TaskObserver.Syscalls;
import frysk.util.ProcFollowUtil;
import frysk.util.ProcRunUtil;
import frysk.util.StackPrintUtil;
import gnu.classpath.tools.getopt.Option;
import gnu.classpath.tools.getopt.OptionException;
import gnu.classpath.tools.getopt.OptionGroup;

public class ferror {
    
    private static final PrintStackOptions stackPrintOptions
	= new PrintStackOptions();
    private static final PrintWriter printWriter = new PrintWriter(System.out);
    private static Pattern writePattern;
    private static OptionGroup[] options() {
	OptionGroup group = new OptionGroup("ferror options");
	group.add(new Option('e', "--error",
			     "error regex to catch in double quotes -e \"<error string>\"") {
		public void parsed(String argument) throws OptionException {
		    writePattern = Pattern.compile(argument);
		}  
	    });
	return new OptionGroup[] {
	    group,
	    StackPrintUtil.options(stackPrintOptions)
	};
    }
    
    public static void main(String[] args) {
	stackPrintOptions.setPrintFullpath(false);
	stackPrintOptions.setPrintParameters(true);
	stackPrintOptions.setPrintVirtualFrames(true);
	stackPrintOptions.setPrintLibrary(true);

	ProcFollowUtil procRunningUtil = 
	    new ProcFollowUtil("ferror",
			    "ferror -e \"<error string>\" -- <executbale|PID> [ARGS]",
			    args, 
			    new TaskObserver[]{ syscallObserver },
			    options(),
			    ProcRunUtil.DEFAULT);
	procRunningUtil.start();
    }

    
    static Syscalls syscallObserver = new Syscalls() {

	    public Action updateSyscallExit (Task task) {
		return Action.CONTINUE;
	    }

	    public void addFailed (Object observable, Throwable w){}
	    public void addedTo (Object observable){
		Task task = (Task) observable;
		printWriter.println("Tracing " + task.getProc().getPid() + "." + task.getTid());
		printWriter.flush();
	    }
	    public void deletedFrom (Object observable){}

	    public Action updateSyscallEnter(Task task, Syscall syscall) {
		if (syscall.getName().equals("write")){
		    long address = syscall.getArguments(task, 2);
		    long length = syscall.getArguments(task, 3);
		    
		    StringBuffer x = new StringBuffer ();
		    task.getMemory().get (address, length, x);
		    String xString = new String(x);
		    
		    Matcher match = writePattern.matcher(xString);
		    if (match.find()) {
			printWriter.println("Process is trying to output: " +
					    xString + 
					    " which matches pattern: " + 
					    writePattern.pattern());
			
			printWriter.println("Stack trace:\n");
			StackPrintUtil.print(task, stackPrintOptions,
					     printWriter);
		    }
		    
		}
		return Action.CONTINUE;
	    }
	};
}
