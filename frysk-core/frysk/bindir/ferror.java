package frysk.bindir;

import frysk.debuginfo.DebugInfoStackFactory;
import frysk.debuginfo.PrintStackOptions;
import frysk.isa.syscalls.Syscall;
import frysk.proc.Action;
import frysk.proc.Task;
import frysk.proc.TaskObserver;
import frysk.proc.TaskObserver.Syscalls;
import frysk.util.ProcRunUtil;
import gnu.classpath.tools.getopt.Option;
import gnu.classpath.tools.getopt.OptionGroup;
import gnu.classpath.tools.getopt.OptionException;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import java.io.PrintWriter;

public class ferror {
    
    private static final PrintWriter printWriter = new PrintWriter(System.out);
    private static Pattern writePattern;
    private static OptionGroup[] options() {
	OptionGroup group = new OptionGroup("ferror options");
	group.add(new Option('e', "--error",
			     "error regex to catch in double quotes -e \"<error string>\"") {
		public void parsed(String argument) throws OptionException {
		    writePattern = Pattern.compile(argument);
		}  
	    });
	return new OptionGroup[] { group };
		  
    }
    
    public static void main(String[] args) {
	ProcRunUtil procRunningUtil = 
	    new ProcRunUtil("ferror",
			    "ferror -e \"<error string>\" -- <executbale|PID> [ARGS]",
			    args, 
			    new TaskObserver[]{ syscallObserver },
			    options(),
			    ProcRunUtil.DEFAULT);
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
  	        
		Matcher match = writePattern.matcher(xString);
		if (match.find()) {
		   printWriter.println("Process is trying to output: " +
				       xString + 
				       " which matches pattern: " + 
				       writePattern.pattern());
		    
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
