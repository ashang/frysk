import inua.util.PrintWriter;

import java.util.ArrayList;

import frysk.util.FCrash;
//import frysk.util.StracePrinter;

import gnu.classpath.tools.getopt.FileArgumentCallback;
//import gnu.classpath.tools.getopt.Option;
import gnu.classpath.tools.getopt.Parser;
import gnu.classpath.tools.getopt.OptionException;

public class fcrash
{

  FCrash crash = new FCrash();
  
  private static PrintWriter writer;
  
  //private static Parser parser;

  private static int pid;
  
  private static boolean requestedPid;
  
  private static ArrayList arguments;

  private void run (String[] args)
  {
    Parser parser = new Parser("ftrace", "0.0", true)
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
        try
          {
            if (0 == pid)
              {
                pid = Integer.parseInt(arg);
              }
            else
              {
                throw new OptionException("too many pids");
              }

          }
        catch (Exception _)
          {
            throw new OptionException("couldn't parse pid");
          }
      }
    });

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
  }
  
  public void addOptions (Parser p)
  {
    
  }
    
    
  public static void main(String[] args)
  {
    fcrash crash = new fcrash();
    crash.run(args);
  }
}
