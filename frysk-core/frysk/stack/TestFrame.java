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

package frysk.stack;

import java.util.logging.Level;
import java.util.logging.Logger;

import frysk.proc.Action;
import frysk.proc.Manager;
import frysk.proc.Task;
import frysk.proc.TaskObserver;
import frysk.testbed.TestLib;

public class TestFrame
    extends TestLib
{

  Logger logger = Logger.getLogger("frysk");
  
  public void testAttached()
  {
    Task task = new AttachedAckProcess().findTaskUsingRefresh(true);
    
    backtrace (task, new BlockingObserver());
    
  }
  
  class BlockingObserver implements TaskObserver.Instruction
  {
    public Action updateExecuted (Task task)
    {
      Manager.eventLoop.requestStop();
      return Action.BLOCK;
    }

    public void addFailed (Object observable, Throwable w)
    {
    }

    public void addedTo (Object observable)
    {
    }

    public void deletedFrom (Object observable)
    {
      Manager.eventLoop.requestStop();
    }
  }
    
  public Frame backtrace(Task task, BlockingObserver blocker)
  {
    task.requestAddInstructionObserver(blocker);
    
    assertRunUntilStop("Attach to process");
    
    Frame baseFrame = StackFactory.createFrame(task);
 
    Frame frame = baseFrame;
    while (frame != null)
      {
     // System.err.println(frame.cursor.getProcName(100).name);
      logger.log(Level.FINE, "testAttached, frame name: {0}\n", 
                 frame.getSymbol().getName());
      frame = frame.getOuter();
    } 
    
    return baseFrame;
  }
  
  public void testFrameSame ()
  {
    Task task = new AttachedAckProcess().findTaskUsingRefresh(true);
    
    Frame frame = backtrace(task, new BlockingObserver());
    
    Frame otherFrame = StackFactory.createFrame(task);
    
    assertSame("Frames should be the same", frame, otherFrame);
    
  }
  
  public void testContinueNotSame()
  {
    Task task = new AttachedAckProcess().findTaskUsingRefresh(true);
    BlockingObserver blocker = new BlockingObserver();
    
    Frame frame = backtrace(task, blocker);
    
    task.requestDeleteInstructionObserver(blocker);
    assertRunUntilStop("Removing observer, running process again");
    
    Frame otherFrame = backtrace(task, blocker);
    
    assertNotSame("Frames should be different", frame, otherFrame);
    
  }
  
}
