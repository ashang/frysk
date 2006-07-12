package frysk.rt;

import lib.unwind.FrameCursor;
import lib.unwind.StackTraceCreator;
import frysk.proc.Task;

public class StackFactory
{
  public static StackFrame createStackFrame(Task task){
    StackCallbacks callbacks = new StackCallbacks(task);
    FrameCursor innermost = StackTraceCreator.createStackTrace(callbacks);
    StackFrame toReturn = new StackFrame(innermost);
    
    StackFrame current = toReturn;
    FrameCursor currentCursor = innermost.getOuter();
    while(currentCursor != null){
      StackFrame outerFrame = new StackFrame(currentCursor);
      
      current.outer = outerFrame;
      outerFrame.inner = current;
      current = outerFrame;
      
      currentCursor = currentCursor.getOuter();
    }
    
    return toReturn;
  }
}
