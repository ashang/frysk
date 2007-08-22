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
import java.util.Iterator;
import java.util.LinkedList;

import frysk.proc.Task;
import frysk.stack.Frame;
import frysk.stack.StackFactory;

/**
 * Create a stack backtrace based on debug information.
 */
public class DebugInfoStackFactory {

    /**
     * Create an ABI stack backtrace, make the simpler debug-info
     * methods.
     */
    public static DebugInfoFrame createDebugInfoStackTrace (Task task)
    {
	return new DebugInfoFrame(StackFactory.createFrame(task));
    }

    /**
     * Create a full virtual stack backtrace where inlined functions
     * exist as separate frames.
     */
    public static DebugInfoFrame createVirtualStackTrace (Task task)
    {
	DebugInfoFrame frame = createDebugInfoStackTrace (task);
	DebugInfoFrame tempFrame = null;
	DebugInfoFrame virtualFrame = null;
	
	DebugInfoFrame innermostFrame = null;
	
	while(frame != null){
	    LinkedList inlineList = frame.getInlinedSubprograms(); 
	    if(inlineList.size() != 0 ){
		Iterator iterator = inlineList.iterator();
		while (iterator.hasNext()) {
		    InlinedSubroutine subroutine = (InlinedSubroutine) iterator.next();
		    tempFrame = new DebugInfoFrame(frame.getUndecoratedFrame());
		    tempFrame.setSubprogram(subroutine);
		    
		    if(virtualFrame!=null){
			virtualFrame.setOuterDebugInfoFrame(tempFrame);
			tempFrame.setInnerDebugInfoFrame(virtualFrame);
			virtualFrame = virtualFrame.getOuterDebugInfoFrame();
		    }else{
			virtualFrame = tempFrame;
			innermostFrame = tempFrame;
		    }
		}
	    }
	    
	    tempFrame = new DebugInfoFrame(frame.getUndecoratedFrame());
	    
	    if(virtualFrame!=null){
		virtualFrame.setOuterDebugInfoFrame(tempFrame);
		tempFrame.setInnerDebugInfoFrame(virtualFrame);
		virtualFrame = virtualFrame.getOuterDebugInfoFrame();
	    }else{
		virtualFrame = tempFrame;
		innermostFrame = tempFrame;
	    }
	    
	    frame = frame.getOuterDebugInfoFrame();
	}
	
	return innermostFrame;
    }

    public static DebugInfoFrame createDebugInfoFrame (Frame frame)
    {
	if(frame == null){
	    return null;
	}
	
	return new DebugInfoFrame(frame);
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
          
          writer.print("#" + count + " ");
          
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
