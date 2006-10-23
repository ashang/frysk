import inua.util.PrintWriter;

import java.util.ArrayList;

import frysk.util.FCrash;
//import frysk.util.StracePrinter;

import gnu.classpath.tools.getopt.FileArgumentCallback;
import gnu.classpath.tools.getopt.Option;
import gnu.classpath.tools.getopt.Parser;
import gnu.classpath.tools.getopt.OptionException;

public class fcrash
{

  FCrash crash = new FCrash();
  
  private static PrintWriter writer;
  
  //private static Parser parser;

  //private static int pid;
  
  private static boolean requestedPid;
  
  private static ArrayList arguments;

  private void run (String[] args)
  {
    Parser parser = new Parser("fcrash", "0.1", true)
    {
      protected void validate () throws OptionException
      {
        if (! requestedPid && arguments == null)
          throw new OptionException("no command or PID specified");
      }
    };
    addOptions(parser);
    parser.setHeader("Usage: fcrash [OPTIONS] -- PATH ARGS || fcrash [OPTIONS] PID");

    parser.parse(args, new FileArgumentCallback()
    {
      public void notifyFile (String arg) throws OptionException
      {
        if (arguments == null)
          arguments = new ArrayList();
        arguments.add(arg);
      }
    });
    
    if (writer == null)
      writer = new PrintWriter(System.out);

  crash.setWriter(writer);
  //crash.setEnterHandler(printer);
  //crash.setExitHandler(printer);

  if (arguments != null)
  {
      String[] cmd = (String[]) arguments.toArray(new String[0]);
      crash.trace(cmd);
  }
  else
    crash.trace();
  }
  
  public void addOptions (Parser p)
  {
    p.add(new Option('p', "pid to trace", "PID") {
      public void parsed(String arg) throws OptionException
      {
          try {
              int pid = Integer.parseInt(arg);
              // FIXME: we have no good way of giving the user an
              // error message if the PID is not available.
              crash.addTracePid(pid);
              requestedPid = true;
          } catch (NumberFormatException _) {
              OptionException oe = new OptionException("couldn't parse pid: " + arg);
              oe.initCause(_);
              throw oe;
          }
      }
  });
  }
    
    
  public static void main(String[] args)
  {
    fcrash crash = new fcrash();
    crash.run(args);
  }
}
