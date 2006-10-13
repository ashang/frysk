

package frysk.proc;

import java.util.Observable;
import java.util.Observer;

import frysk.event.RequestStopEvent;

public class TestProcForceDetach
    extends TestLib
{

  public void requestRemove (AckProcess ackProc, int count)
  {

    new ProcAttachedObserver(ackProc.findProcUsingRefresh(),
                             new MyObserver(count));

    ackProc.findProcUsingRefresh().observableDetached.addObserver(new Observer()
    {

      public void update (Observable o, Object arg)
      {
        Manager.eventLoop.add(new RequestStopEvent(Manager.eventLoop));
      }
    });

    assertRunUntilStop("test");
  }

  public void testRequestRemoveAckDaemon ()
  {
    AckProcess ackProc = new AckDaemonProcess();
    requestRemove(ackProc, 1);
  }
  
  public void testRequestRemoveDetached ()
  {
    AckProcess ackProc = new DetachedAckProcess();
    requestRemove(ackProc, 1);
  }
  
  public void testMultiThreadedRequestRemoveAckDaemon ()
  {
    AckProcess ackProc = new AckDaemonProcess(2);
    requestRemove(ackProc, 3);
  }
  
  public void testMultiThreadedRequestRemoveDetached ()
  {
    AckProcess ackProc = new DetachedAckProcess(2);
    requestRemove(ackProc, 3);
  }

  class MyObserver
      implements ProcObserver.ProcTasks
  {

    int count;

    public MyObserver (int c)
    {
      count = c;
    }

    public void existingTask (Task task)
    {
      count--;
      // TODO Auto-generated method stub
      if (0 == count)
        task.getProc().requestRemoveAllObservations();
    }

    public void taskAdded (Task task)
    {
      // TODO Auto-generated method stub

    }

    public void taskRemoved (Task task)
    {
      // TODO Auto-generated method stub

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
}
