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

import frysk.proc.Task;
import frysk.scopes.InlinedSubroutine;
import frysk.scopes.Scope;
import frysk.stack.StackFactory;

/**
 * Create a stack backtrace based on debug information.
 */
public class DebugInfoStackFactory {

    /**
     * Create an ABI stack backtrace, make the simpler debug-info
     * methods.
     */
    public static DebugInfoFrame createDebugInfoStackTrace (Task task) {
	return new DebugInfoFrame(null, StackFactory.createFrame(task));
    }

    /**
     * Create a full virtual stack backtrace where inlined functions
     * exist as separate frames; backed by debug-info frames.
     *
     * FIXME: This creates the entire tree of frames when only the
     * inner most might be needed.
     */
    public static DebugInfoFrame createVirtualStackTrace (Task task) {
	DebugInfoFrame currentFrame = null;
	DebugInfoFrame innermostFrame = null;
	
	int count = 0;
	
	for (DebugInfoFrame debugFrame = createDebugInfoStackTrace (task);
	     debugFrame != null;
	     debugFrame = debugFrame.getOuterDebugInfoFrame()) {
	    
	    // For any inlined scopes, create virtual frames.
	    int inlineCount = 1;
	    for (Scope scope = debugFrame.getScopes();
		 scope != null; scope = scope.getOuter()) {
		if (scope instanceof InlinedSubroutine) {
		    InlinedSubroutine subroutine = (InlinedSubroutine) scope;
		    currentFrame = new VirtualDebugInfoFrame(currentFrame,
							     debugFrame);
		    currentFrame.setIndex(inlineCount++);
		    currentFrame.setSubprogram(subroutine);
		    if (innermostFrame == null)
			innermostFrame = currentFrame;
		}
	    }
	    
	    // Append to that a non-virtual frame.
	    currentFrame = new DebugInfoFrame(currentFrame, debugFrame);
	    currentFrame.setIndex(count++);
	    
	    if (innermostFrame == null)
		innermostFrame = currentFrame;
	}
	
	return innermostFrame;
    }

    public static final void printTaskStackTrace (PrintWriter printWriter, Task task, boolean printParameters, boolean printScopes, boolean fullpath)
    {
      if (task != null){
        printWriter.println("Task #" + task.getTid());
        DebugInfoFrame frame = createDebugInfoStackTrace(task);
        printStackTrace(printWriter, frame, 20, printParameters,printScopes,fullpath);
      }
      printWriter.flush();
    }

    public static final void printVirtualTaskStackTrace (PrintWriter printWriter, Task task, boolean printParameters, boolean printScopes, boolean fullpath)
    {
      if (task != null){
        printWriter.println("Task #" + task.getTid());
        DebugInfoFrame frame = createVirtualStackTrace(task);
        printStackTrace(printWriter,frame, 20, printParameters,printScopes,fullpath);
      }
      printWriter.flush();
    }

    public static void printStackTrace(PrintWriter writer, DebugInfoFrame topFrame, int numberOfFrames, boolean printParameters, boolean printScopes, boolean fullpath){
        
        int count = 0;
        for (DebugInfoFrame frame = topFrame;
        frame != null; frame = frame.getOuterDebugInfoFrame()) {
            
          writer.print("#");

          frame.printIndex(writer);
          writer.print(" ");
            
          frame.toPrint(writer, printParameters, printScopes, fullpath);
          writer.println();
          writer.flush();
          count++;
          if(count == numberOfFrames){
              writer.println("...");
              break;
          }
          
        }
    
      }

}
