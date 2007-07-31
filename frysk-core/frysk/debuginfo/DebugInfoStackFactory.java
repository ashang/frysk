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
import frysk.rt.Line;
import frysk.stack.Frame;
import frysk.stack.StackFactory;

public class DebugInfoStackFactory {

    public static DebugInfoFrame createDebugInfoStackTrace (Task task)
    {
	return new DebugInfoFrame(StackFactory.createFrame(task));
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
        printStackTrace(printWriter, frame,printParameters,printScopes,fullpath);
      }
      printWriter.flush();
    }

    public static void printStackTrace(PrintWriter writer, DebugInfoFrame topFrame, boolean printParameters, boolean printScopes, boolean fullpath){
        
        int count = 0;
        for (DebugInfoFrame frame = topFrame;
        frame != null; frame = frame.getOuterDebugInfoFrame()) {
          
          writer.print("#" + count + " ");
          
          Subprogram subprogram = frame.getSubprogram();
    
          if(subprogram != null){
            writer.print("0x");
            String addr = Long.toHexString(frame.getAddress());
            int padding = 2 * frame.getTask().getIsa().getWordSize() - addr.length();
            
            for (int i = 0; i < padding; ++i)
              writer.print('0');
            
            writer.print(addr);
            
            writer.print(" in " + subprogram.getName() + "(");
            if(printParameters){
    	    subprogram.printParameters(writer, frame);
            }
            writer.print(") ");
            
            if(fullpath){
              Line line = frame.getLines()[0];
              writer.print(line.getFile().getPath());
              writer.print("#");
              writer.print(line.getLine());
            }else{
              Line line = frame.getLines()[0];
              writer.print(line.getFile().getName());
              writer.print("#");
              writer.print(line.getLine());
            }
            
            if(printScopes){
    	    subprogram.printScopes(writer, frame);
            }
            
          } else {
              frame.toPrint(writer, true);
          }
          
          writer.println();
          writer.flush();
          count++;
        }
    
      }

}
