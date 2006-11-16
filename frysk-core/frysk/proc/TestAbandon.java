

package frysk.proc;

import frysk.event.RequestStopEvent;

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
      // TODO Auto-generated method stub

    }

    public void addedTo (Object observable)
    {
      // TODO Auto-generated method stub

    }

    public void deletedFrom (Object observable)
    {
      // TODO Auto-generated method stub

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
    AckProcess ackProc = new AckDaemonProcess();
    TaskObserver.Instruction testObserver = new TestObserver();

    abandonThenRemove(ackProc, testObserver);
  }

  public void testAbandonThenRemoveDetached ()
  {
    AckProcess ackProc = new DetachedAckProcess();
    TaskObserver.Instruction testObserver = new TestObserver();

    abandonThenRemove(ackProc, testObserver);
  }

  public void testAbandonThenRemoveAttached ()
  {
    AckProcess ackProc = new AttachedAckProcess();
    TaskObserver.Instruction testObserver = new TestObserver();

    abandonThenRemove(ackProc, testObserver);
  }

  public void testAbandonThenRemoveAckDaemonUpdateExecuted ()
  {
    AckProcess ackProc = new AckDaemonProcess();
    TaskObserver.Instruction testObserver = new AbandonOnUpdateExecuted();

    abandonThenRemove(ackProc, testObserver);
  }

  public void testAbandonThenRemoveDetachedUpdateExecuted ()
  {
    AckProcess ackProc = new DetachedAckProcess();
    TaskObserver.Instruction testObserver = new AbandonOnUpdateExecuted();

    abandonThenRemove(ackProc, testObserver);
  }

  public void testAbandonThenRemoveAttachedUpdateExecuted ()
  {
    AckProcess ackProc = new AttachedAckProcess();
    TaskObserver.Instruction testObserver = new AbandonOnUpdateExecuted();

    abandonThenRemove(ackProc, testObserver);
  }

  public void testAbandonThenRemoveAckDaemonAddedTo ()
  {
    AckProcess ackProc = new AckDaemonProcess();
    TaskObserver.Instruction testObserver = new AbandonOnAddedTo();

    abandonThenRemove(ackProc, testObserver);
  }

  public void testAbandonThenRemoveDetachedAddedTo ()
  {
    AckProcess ackProc = new DetachedAckProcess();
    TaskObserver.Instruction testObserver = new AbandonOnAddedTo();

    abandonThenRemove(ackProc, testObserver);
  }

  public void testAbandonThenRemoveAttachedAddedTo ()
  {
    AckProcess ackProc = new AttachedAckProcess();
    TaskObserver.Instruction testObserver = new AbandonOnAddedTo();

    abandonThenRemove(ackProc, testObserver);
  }
  
  public void testAbandonThenRemoveAckDaemonDeletedFrom ()
  {
    AckProcess ackProc = new AckDaemonProcess();
    TaskObserver.Instruction testObserver = new AbandonOnDeletedFrom();

    abandonThenRemove(ackProc, testObserver);
  }

  public void testAbandonThenRemoveDetachedDeletedFrom ()
  {
    AckProcess ackProc = new DetachedAckProcess();
    TaskObserver.Instruction testObserver = new AbandonOnDeletedFrom();

    abandonThenRemove(ackProc, testObserver);
  }

  public void testAbandonThenRemoveAttachedDeletedFrom ()
  {
    AckProcess ackProc = new AttachedAckProcess();
    TaskObserver.Instruction testObserver = new AbandonOnDeletedFrom();

    abandonThenRemove(ackProc, testObserver);
  }

  public void abandonThenRemove (AckProcess ackProc,
                                 TaskObserver.Instruction testObserver)
  {

    ackProc.findTaskUsingRefresh(true).requestAddInstructionObserver(
                                                                     testObserver);

    Proc proc = ackProc.findProcUsingRefresh(true);
    proc.getMainTask().requestDeleteInstructionObserver(testObserver);

    proc.requestAbandonAndRunEvent(new RequestStopEvent(Manager.eventLoop));

    assertRunUntilStop("testAbandonThenRemove");
  }

  public void testRemoveThenAbandonAckDaemon ()
  {
    AckProcess ackProc = new AckDaemonProcess();

    TaskObserver.Instruction testObserver = new TestObserver();
    removeThenAbandon(ackProc, testObserver);
  }

  public void testRemoveThenAbandonDetached ()
  {
    AckProcess ackProc = new DetachedAckProcess();

    TaskObserver.Instruction testObserver = new TestObserver();
    removeThenAbandon(ackProc, testObserver);
  }

  public void testRemoveThenAbandonAttached ()
  {
    AckProcess ackProc = new AttachedAckProcess();

    TaskObserver.Instruction testObserver = new TestObserver();
    removeThenAbandon(ackProc, testObserver);
  }

  public void testRemoveThenAbandonAckDaemonAddedTo ()
  {
    AckProcess ackProc = new AckDaemonProcess();

    TaskObserver.Instruction testObserver = new AbandonOnAddedTo();
    removeThenAbandon(ackProc, testObserver);
  }

  public void testRemoveThenAbandonDetachedAddedTo ()
  {
    AckProcess ackProc = new DetachedAckProcess();

    TaskObserver.Instruction testObserver = new AbandonOnAddedTo();
    removeThenAbandon(ackProc, testObserver);
  }

  public void testRemoveThenAbandonAttachedAddedTo ()
  {
    AckProcess ackProc = new AttachedAckProcess();

    TaskObserver.Instruction testObserver = new AbandonOnAddedTo();
    removeThenAbandon(ackProc, testObserver);
  }

  public void testRemoveThenAbandonAckDaemonUpdateExecuted ()
  {
    AckProcess ackProc = new AckDaemonProcess();

    TaskObserver.Instruction testObserver = new AbandonOnUpdateExecuted();
    removeThenAbandon(ackProc, testObserver);
  }

  public void testRemoveThenAbandonDetachedUpdateExecuted ()
  {
    AckProcess ackProc = new DetachedAckProcess();

    TaskObserver.Instruction testObserver = new AbandonOnUpdateExecuted();
    removeThenAbandon(ackProc, testObserver);
  }

  public void testRemoveThenAbandonAttachedUpdateExecuted ()
  {
    AckProcess ackProc = new AttachedAckProcess();

    TaskObserver.Instruction testObserver = new AbandonOnUpdateExecuted();
    removeThenAbandon(ackProc, testObserver);
  }

  public void testRemoveThenAbandonAckDaemonDeletedFrom ()
  {
    AckProcess ackProc = new AckDaemonProcess();

    TaskObserver.Instruction testObserver = new AbandonOnDeletedFrom();
    removeThenAbandon(ackProc, testObserver);
  }

  public void testRemoveThenAbandonDetachedDeletedFrom ()
  {
    AckProcess ackProc = new DetachedAckProcess();

    TaskObserver.Instruction testObserver = new AbandonOnDeletedFrom();
    removeThenAbandon(ackProc, testObserver);
  }

  public void testRemoveThenAbandonAttachedDeletedFrom ()
  {
    AckProcess ackProc = new AttachedAckProcess();

    TaskObserver.Instruction testObserver = new AbandonOnDeletedFrom();
    removeThenAbandon(ackProc, testObserver);
  }  
  
  public void removeThenAbandon (AckProcess ackProc,
                                 TaskObserver.Instruction testObserver)
  {

    ackProc.findTaskUsingRefresh(true).requestAddInstructionObserver(
                                                                     testObserver);

    Proc proc = ackProc.findProcUsingRefresh(true);

    proc.requestAbandonAndRunEvent(new RequestStopEvent(Manager.eventLoop));

    proc.getMainTask().requestDeleteInstructionObserver(testObserver);

    assertRunUntilStop("testRemoveThenAbandon");
  }

  public void testAbandonAndAbandon ()
  {
    AckProcess ackProc = new AckDaemonProcess();

    TaskObserver.Instruction testObserver = new TestObserver();

    ackProc.findTaskUsingRefresh(true).requestAddInstructionObserver(
                                                                     testObserver);

    Proc proc = ackProc.findProcUsingRefresh(true);

    proc.requestAbandonAndRunEvent(new RequestStopEvent(Manager.eventLoop));

    proc.requestAbandonAndRunEvent(new RequestStopEvent(Manager.eventLoop));

    assertRunUntilStop("testAbandonAndAbandon");
  }  
}
