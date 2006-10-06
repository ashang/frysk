package frysk.util;

import java.io.PrintStream;

import frysk.proc.Task;
import frysk.proc.TaskException;
import frysk.rt.StackFactory;
import frysk.rt.StackFrame;

public class Util {

	private Util()
	{
	}

    public static void printStackTrace(PrintStream writer, Task task)
    {
    	writer.println("Stack trace for task " + task);
    	try
    	{
    		for (StackFrame frame = StackFactory.createStackFrame(task);
    			frame != null;
    			frame = frame.getOuter())
    		{
    			// FIXME: do valgrind-like '=== PID ===' ?
    			writer.print("  ");
    			writer.println(frame);
    		}
    	}
    	catch (TaskException _)
    	{
    		// FIXME: log exception, or rethrow?
    		writer.println("... couldn't print stack trace");
    	}
    }
}
