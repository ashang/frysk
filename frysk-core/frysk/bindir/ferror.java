package frysk.bindir;

import frysk.debuginfo.DebugInfoStackFactory;
import frysk.debuginfo.PrintStackOptions;
import frysk.isa.syscalls.Syscall;
import frysk.proc.Action;
import frysk.proc.Task;
import frysk.proc.TaskObserver;
import frysk.proc.TaskObserver.Syscalls;
import frysk.util.ProcRunningUtil;
import gnu.classpath.tools.getopt.Option;
import gnu.classpath.tools.getopt.OptionException;

import java.io.PrintWriter;

public class ferror {
    
    static final PrintWriter printWriter = new PrintWriter(System.out);
    
    private static String errorString;
    public static void main (String[] args)
    {
       Option option = new Option('e', "--error", "error string to catch in double quotes -e \"<error string>\""){

	public void parsed(String argument) throws OptionException {
	    errorString = argument;
	}  
       };
	
       ProcRunningUtil procRunningUtil = 
	   new ProcRunningUtil("ferror",
		   "ferror -e \"<error string>\" -- <executbale|PID> [ARGS]",
		   args, 
		   new TaskObserver[]{syscallObserver},new Option[]{option} , ProcRunningUtil.DEFAULT);
       procRunningUtil.start();
    }

    
    static Syscalls syscallObserver = new Syscalls(){

      public Action updateSyscallExit (Task task)
      {
        return Action.CONTINUE;
      }

      public void addFailed (Object observable, Throwable w){}
      public void addedTo (Object observable){
	  Task task = (Task) observable;
	  printWriter.println("Tracing " + task.getProc().getPid() + "." + task.getTid());
	  printWriter.flush();
      }
      public void deletedFrom (Object observable){}

      public Action updateSyscallEnter(Task task, Syscall syscall) {
  	if(syscall.getName().equals("write")){
  	        long address = syscall.getArguments(task, 2);
  	        long length = syscall.getArguments(task, 3);
  	      
  	        StringBuffer x = new StringBuffer ();
  	        task.getMemory().get (address, length, x);
  	        String xString = new String(x);
  	        
  	        if(xString.contains(errorString)){
  	          printWriter.println("Process is trying to output " + errorString);
  	          
  	          printWriter.println("Stack trace:\n");
  	          PrintStackOptions options = new PrintStackOptions();

  	          options.setPrintFullpath(false);
  	          options.setPrintParameters(true);
  	          options.setPrintVirtualFrames(true);
  	          options.setPrintLibrary(true);

  	          DebugInfoStackFactory.printTaskStackTrace(printWriter, task, options);
  	          
  	        }
  	        
  	      }
  	      return Action.CONTINUE;
      }
      
    };
}
