package frysk.proc;

import java.util.Observable;

import frysk.event.RequestStopEvent;

public class TestProcStopped extends TestLib
{

  public void testStoppedAckDaemon () 
  {
    if (brokenXXX(3316))
      return;
    
    AckProcess ackProc = new AckDaemonProcess();

    ackProc.assertSendStop();
    
    Proc proc = ackProc.findProcUsingRefresh();

    proc.observableAttached.addObserver(
                                        new java.util.Observer ()
                                        {

                                          public void update (Observable o, Object arg)
                                          {                                           
                                            Manager.eventLoop.add(new RequestStopEvent(Manager.eventLoop));
                                          } 
                                         
                                        });
    new ProcAttachedObserver(proc, new MyTester());
    
    assertRunUntilStop("testStoppedAckDaemon");
  }
  
  public class MyTester implements ProcObserver.ProcTasks
  {

    public void existingTask (Task task)
    {
      // TODO Auto-generated method stub
      
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
