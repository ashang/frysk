// This file is part of the program FRYSK.
//
// Copyright 2005, Red Hat Inc.
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

package frysk.proc;

import frysk.event.RequestStopEvent;
import frysk.testbed.TestLib;
import frysk.testbed.SlaveOffspring;

public class TestAbandon
    extends TestLib
{

  class TestObserver
      implements TaskObserver.Instruction
  {

    public Action updateExecuted (Task task)
    {
      return Action.CONTINUE;
    }

    public void addFailed (Object observable, Throwable w)
    {
    }

    public void addedTo (Object observable)
    {
    }

    public void deletedFrom (Object observable)
    {
    }
  }

  class AbandonOnAddedTo
      extends TestObserver
  {
    public void addedTo (Object observable)
    {
      ((Task) observable).getProc().requestAbandonAndRunEvent(
                                                              new RequestStopEvent(
                                                                                   Manager.eventLoop));
    }
  }

  class AbandonOnUpdateExecuted
      extends TestObserver
  {

    public Action updateExecuted (Task task)
    {

      task.getProc().requestAbandonAndRunEvent(
                                               new RequestStopEvent(
                                                                    Manager.eventLoop));

      return super.updateExecuted(task);
    }

  }
  
  class AbandonOnDeletedFrom
  extends TestObserver
  {

    public void deletedFrom (Object observable)
    {
      ((Task) observable).getProc().requestAbandonAndRunEvent(
                                                              new RequestStopEvent(
                                                                                   Manager.eventLoop));    
    }
    
  }
  

  public void testAbandonThenRemoveAckDaemon ()
  {
    SlaveOffspring ackProc = SlaveOffspring.createDaemon();
    TaskObserver.Instruction testObserver = new TestObserver();

    abandonThenRemove(ackProc, testObserver);
  }

  public void testAbandonThenRemoveDetached ()
  {
    SlaveOffspring ackProc = SlaveOffspring.createChild();
    TaskObserver.Instruction testObserver = new TestObserver();

    abandonThenRemove(ackProc, testObserver);
  }

  public void testAbandonThenRemoveAttached ()
  {
    SlaveOffspring ackProc = SlaveOffspring.createAttachedChild();
    TaskObserver.Instruction testObserver = new TestObserver();

    abandonThenRemove(ackProc, testObserver);
  }

  public void testAbandonThenRemoveAckDaemonUpdateExecuted ()
  {
    SlaveOffspring ackProc = SlaveOffspring.createDaemon();
    TaskObserver.Instruction testObserver = new AbandonOnUpdateExecuted();

    abandonThenRemove(ackProc, testObserver);
  }

  public void testAbandonThenRemoveDetachedUpdateExecuted ()
  {
    SlaveOffspring ackProc = SlaveOffspring.createChild();
    TaskObserver.Instruction testObserver = new AbandonOnUpdateExecuted();

    abandonThenRemove(ackProc, testObserver);
  }

  public void testAbandonThenRemoveAttachedUpdateExecuted ()
  {
    SlaveOffspring ackProc = SlaveOffspring.createAttachedChild();
    TaskObserver.Instruction testObserver = new AbandonOnUpdateExecuted();

    abandonThenRemove(ackProc, testObserver);
  }

  public void testAbandonThenRemoveAckDaemonAddedTo ()
  {
    SlaveOffspring ackProc = SlaveOffspring.createDaemon();
    TaskObserver.Instruction testObserver = new AbandonOnAddedTo();

    abandonThenRemove(ackProc, testObserver);
  }

  public void testAbandonThenRemoveDetachedAddedTo ()
  {
    SlaveOffspring ackProc = SlaveOffspring.createChild();
    TaskObserver.Instruction testObserver = new AbandonOnAddedTo();

    abandonThenRemove(ackProc, testObserver);
  }

  public void testAbandonThenRemoveAttachedAddedTo ()
  {
    SlaveOffspring ackProc = SlaveOffspring.createAttachedChild();
    TaskObserver.Instruction testObserver = new AbandonOnAddedTo();

    abandonThenRemove(ackProc, testObserver);
  }
  
  public void testAbandonThenRemoveAckDaemonDeletedFrom ()
  {
    SlaveOffspring ackProc = SlaveOffspring.createDaemon();
    TaskObserver.Instruction testObserver = new AbandonOnDeletedFrom();

    abandonThenRemove(ackProc, testObserver);
  }

  public void testAbandonThenRemoveDetachedDeletedFrom ()
  {
    SlaveOffspring ackProc = SlaveOffspring.createChild();
    TaskObserver.Instruction testObserver = new AbandonOnDeletedFrom();

    abandonThenRemove(ackProc, testObserver);
  }

  public void testAbandonThenRemoveAttachedDeletedFrom ()
  {
    SlaveOffspring ackProc = SlaveOffspring.createAttachedChild();
    TaskObserver.Instruction testObserver = new AbandonOnDeletedFrom();

    abandonThenRemove(ackProc, testObserver);
  }

  public void abandonThenRemove (SlaveOffspring ackProc,
                                 TaskObserver.Instruction testObserver)
  {

    ackProc.findTaskUsingRefresh(true).requestAddInstructionObserver(
                                                                     testObserver);

    Proc proc = ackProc.assertRunToFindProc();
    proc.getMainTask().requestDeleteInstructionObserver(testObserver);

    proc.requestAbandonAndRunEvent(new RequestStopEvent(Manager.eventLoop));

    assertRunUntilStop("testAbandonThenRemove");
  } 

  public void testRemoveThenAbandonAckDaemon ()
  {
    SlaveOffspring ackProc = SlaveOffspring.createDaemon();

    TaskObserver.Instruction testObserver = new TestObserver();
    removeThenAbandon(ackProc, testObserver);
  }

  public void testRemoveThenAbandonDetached ()
  {
    SlaveOffspring ackProc = SlaveOffspring.createChild();

    TaskObserver.Instruction testObserver = new TestObserver();
    removeThenAbandon(ackProc, testObserver);
  }

  public void testRemoveThenAbandonAttached ()
  {
    SlaveOffspring ackProc = SlaveOffspring.createAttachedChild();

    TaskObserver.Instruction testObserver = new TestObserver();
    removeThenAbandon(ackProc, testObserver);
  }

  public void testRemoveThenAbandonAckDaemonAddedTo ()
  {
    SlaveOffspring ackProc = SlaveOffspring.createDaemon();

    TaskObserver.Instruction testObserver = new AbandonOnAddedTo();
    removeThenAbandon(ackProc, testObserver);
  }

  public void testRemoveThenAbandonDetachedAddedTo ()
  {
    SlaveOffspring ackProc = SlaveOffspring.createChild();

    TaskObserver.Instruction testObserver = new AbandonOnAddedTo();
    removeThenAbandon(ackProc, testObserver);
  }

  public void testRemoveThenAbandonAttachedAddedTo ()
  {
    SlaveOffspring ackProc = SlaveOffspring.createAttachedChild();

    TaskObserver.Instruction testObserver = new AbandonOnAddedTo();
    removeThenAbandon(ackProc, testObserver);
  }

  public void testRemoveThenAbandonAckDaemonUpdateExecuted ()
  {
    SlaveOffspring ackProc = SlaveOffspring.createDaemon();

    TaskObserver.Instruction testObserver = new AbandonOnUpdateExecuted();
    removeThenAbandon(ackProc, testObserver);
  }

  public void testRemoveThenAbandonDetachedUpdateExecuted ()
  {
    SlaveOffspring ackProc = SlaveOffspring.createChild();

    TaskObserver.Instruction testObserver = new AbandonOnUpdateExecuted();
    removeThenAbandon(ackProc, testObserver);
  }

  public void testRemoveThenAbandonAttachedUpdateExecuted ()
  {
    SlaveOffspring ackProc = SlaveOffspring.createAttachedChild();

    TaskObserver.Instruction testObserver = new AbandonOnUpdateExecuted();
    removeThenAbandon(ackProc, testObserver);
  }

  public void testRemoveThenAbandonAckDaemonDeletedFrom ()
  {
    SlaveOffspring ackProc = SlaveOffspring.createDaemon();

    TaskObserver.Instruction testObserver = new AbandonOnDeletedFrom();
    removeThenAbandon(ackProc, testObserver);
  }

  public void testRemoveThenAbandonDetachedDeletedFrom ()
  {
    SlaveOffspring ackProc = SlaveOffspring.createChild();

    TaskObserver.Instruction testObserver = new AbandonOnDeletedFrom();
    removeThenAbandon(ackProc, testObserver);
  }

  public void testRemoveThenAbandonAttachedDeletedFrom ()
  {
    SlaveOffspring ackProc = SlaveOffspring.createAttachedChild();

    TaskObserver.Instruction testObserver = new AbandonOnDeletedFrom();
    removeThenAbandon(ackProc, testObserver);
  }  
  
  public void removeThenAbandon (SlaveOffspring ackProc,
                                 TaskObserver.Instruction testObserver)
  {

    ackProc.findTaskUsingRefresh(true).requestAddInstructionObserver(
                                                                     testObserver);

    Proc proc = ackProc.assertRunToFindProc();

    proc.requestAbandonAndRunEvent(new RequestStopEvent(Manager.eventLoop));

    proc.getMainTask().requestDeleteInstructionObserver(testObserver);

    assertRunUntilStop("testRemoveThenAbandon");
  }

  public void testAbandonAndAbandon ()
  {
    SlaveOffspring ackProc = SlaveOffspring.createDaemon();

    TaskObserver.Instruction testObserver = new TestObserver();

    ackProc.findTaskUsingRefresh(true).requestAddInstructionObserver(
                                                                     testObserver);

    Proc proc = ackProc.assertRunToFindProc();

    proc.requestAbandonAndRunEvent(new RequestStopEvent(Manager.eventLoop));

    proc.requestAbandonAndRunEvent(new RequestStopEvent(Manager.eventLoop));

    assertRunUntilStop("testAbandonAndAbandon");
  }  
}
