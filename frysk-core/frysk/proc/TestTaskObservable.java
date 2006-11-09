

package frysk.proc;

import java.util.Iterator;

public class TestTaskObservable
    extends TestLib
{
  
  public void test()
  {
    AckProcess ackProc = new AckDaemonProcess();
    DummyObserver observer = new DummyObserver();
    
    testIterator(ackProc, observer);
  }
  
  public void testExtended()
  {
    AckProcess ackProc = new AckDaemonProcess();
    DummyObserver observer = new ExtendedDummyObserver();
    
    testIterator(ackProc, observer);
  }

  public void testIterator (AckProcess ackProc, DummyObserver observer)
  {
    Task task = ackProc.findTaskUsingRefresh(true);

    TaskObservable testObservable = new TaskObservable(task);

  

    testObservable.add(observer);

    Iterator iter = testObservable.iterator(TaskObserver.Terminating.class);

    assertTrue("Terminating Iterator has next", iter.hasNext());

    assertEquals("Terminating iter is this iter", iter.next(), observer);

    assertFalse("Terminating Iter is empty", iter.hasNext());

    iter = testObservable.iterator(TaskObserver.Terminated.class);

    assertFalse("Terminated iter is empty", iter.hasNext());

    iter = testObservable.iterator(TaskObserver.Instruction.class);

    assertTrue("Instruction Iterator has next", iter.hasNext());

    assertEquals("Instruction iter is this iter", iter.next(), observer);

    assertFalse("Instruction iter is empty", iter.hasNext());

    testObservable.delete(observer);

    iter = testObservable.iterator(TaskObserver.class);

    assertFalse("Observable is empty", iter.hasNext());

  }
  
  

  class DummyObserver
      implements TaskObserver.Terminating, TaskObserver.Instruction
  {

    public Action updateTerminating (Task task, boolean signal, int value)
    {
      // TODO Auto-generated method stub
      return null;
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

    public Action updateExecuted (Task task)
    {
      // TODO Auto-generated method stub
      return null;
    }

  }

  //This class does not have any interfaces but it should inherit the interfaces of 
  //DummyObserver.
  class ExtendedDummyObserver
      extends DummyObserver
  {

  }

}
