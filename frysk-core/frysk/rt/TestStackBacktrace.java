

package frysk.rt;

import frysk.proc.Action;
import frysk.proc.Manager;
import frysk.proc.Task;
import frysk.proc.TaskObserver;
import frysk.proc.TestLib;

public class TestStackBacktrace
    extends TestLib
{
  private Task myTask;
  
  public void testBacktrace ()
  {
    if(broken())
      return;
    
    class TaskCreatedObserver implements TaskObserver.Attached
    {

      public Action updateAttached (Task task)
      {
        TestStackBacktrace.this.myTask = task;
        Manager.eventLoop.requestStop();
        return Action.BLOCK;
      }

      public void addedTo (Object observable)
      {
        // TODO Auto-generated method stub
        
      }

      public void addFailed (Object observable, Throwable w)
      {
        // TODO Auto-generated method stub
        
      }

      public void deletedFrom (Object observable)
      {
        // TODO Auto-generated method stub
        
      }
    }
    
    TaskCreatedObserver obs = new TaskCreatedObserver();
    String command[] = {"pwd"};
    
    host.requestCreateAttachedProc(command, obs);
    
    assertRunUntilStop("Observer was not added");
    
    assertNotNull(myTask);
    
    StackFrame frame = StackFactory.createStackFrame(myTask);
    
    assertNotNull(frame);
    
    int indent = 0;
    while(frame != null){
      for(int i = 0; i < indent; i++)
        System.out.print("\t");
      System.out.println("Frame " + (indent + 1));
      frame = frame.outer;
    }
  }
  
  private boolean broken(){
    return true;
  }
}
