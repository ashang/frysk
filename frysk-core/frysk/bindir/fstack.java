// This file is part of the program FRYSK.
//
// Copyright 2005, 2006, 2007, 2008, Red Hat Inc.
//
// FRYSK is free software; you can redistribute it and/or modify it
// under the terms of the GNU General Public License as published by
// the Free Software Foundation; version 2 of the License.
//
// FRYSK is distributed in the hope that it will be useful, but
// WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
// General Public License for more details.
// 
// You should have received a copy of the GNU General Public License
// along with FRYSK; if not, write to the Free Software Foundation,
// Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
// 
// In addition, as a special exception, Red Hat, Inc. gives You the
// additional right to link the code of FRYSK with code not covered
// under the GNU General Public License ("Non-GPL Code") and to
// distribute linked combinations including the two, subject to the
// limitations in this paragraph. Non-GPL Code permitted under this
// exception must only link to the code of FRYSK through those well
// defined interfaces identified in the file named EXCEPTION found in
// the source code files (the "Approved Interfaces"). The files of
// Non-GPL Code may instantiate templates or use macros or inline
// functions from the Approved Interfaces without causing the
// resulting work to be covered by the GNU General Public
// License. Only Red Hat, Inc. may make changes or additions to the
// list of Approved Interfaces. You must obey the GNU General Public
// License in all respects for all of the FRYSK code and other code
// used in conjunction with FRYSK except the Non-GPL Code covered by
// this exception. If you modify this file, you may extend this
// exception to your version of the file, but you are not obligated to
// do so. If you do not wish to provide this exception without
// modification, you must delete this exception statement from your
// version and license this file solely under the GPL without
// exception.

package frysk.bindir;

import java.io.PrintWriter;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import frysk.debuginfo.PrintStackOptions;
import frysk.event.Event;
import frysk.event.RequestStopEvent;
import frysk.proc.Manager;
import frysk.proc.Proc;
import frysk.proc.ProcBlockAction;
import frysk.proc.ProcCoreAction;
import frysk.util.CommandlineParser;
import frysk.util.StacktraceAction;
import gnu.classpath.tools.getopt.Option;
import gnu.classpath.tools.getopt.OptionException;

public final class fstack
{
  private static StacktraceAction stacker;

  private static CommandlineParser parser;

  protected static final Logger logger = Logger.getLogger("frysk"); 
  
  private static PrintWriter printWriter = new PrintWriter(System.out);
  
  static PrintStackOptions options = new PrintStackOptions();
  
  private static class Stacker extends StacktraceAction
  {

    Proc proc;
    public Stacker (PrintWriter printWriter, Proc theProc, Event theEvent,PrintStackOptions options)
    {
      super(printWriter, theProc, theEvent, options);
      this.proc = theProc;
    }

    //@Override
    public void addFailed (Object observable, Throwable w)
    {
      w.printStackTrace();
      proc.requestAbandonAndRunEvent(new RequestStopEvent(Manager.eventLoop));

      try
        {
          // Wait for eventLoop to finish.
          Manager.eventLoop.join();
        }
      catch (InterruptedException e)
        {
          e.printStackTrace();
        }
      System.exit(1);
    } 
  }
  
  private static class AbandonPrintEvent implements Event
  {
    private Proc proc;
    
    AbandonPrintEvent(Proc proc)
    {
      this.proc = proc;
    }
    public void execute ()
    {
      proc.requestAbandonAndRunEvent(new Event()
    {

      public void execute ()
      {
	  Manager.eventLoop.requestStop();
	  stacker.flush();
      }
    });
    }
    
  }
  
  private static class PrintEvent implements Event
  {
    public void execute()
    {
      Manager.eventLoop.requestStop();
      stacker.flush();
    }
  }
  
    private static void stackCore(Proc proc) {
	stacker = new Stacker(printWriter, proc, new PrintEvent(), options);
	new ProcCoreAction(proc, stacker);
	Manager.eventLoop.run();
    }
  
    private static void stackPid(Proc proc) {
	stacker = new Stacker(printWriter, proc, new AbandonPrintEvent(proc), options);
	new ProcBlockAction(proc, stacker);
	Manager.eventLoop.run();
    }
  
  public static void main (String[] args)
  {

      parser = new CommandlineParser("fstack") {
	      //@Override
	      public void parseCores(Proc[] cores) {
		  for (int i = 0; i < cores.length; i++)
		      stackCore(cores[i]);
	      }
	      //@Override
	      public void parsePids(Proc[] procs) {
		  for (int i = 0; i < procs.length; i++)
		      stackPid(procs[i]);
	      }
	  };

      parser.add(new Option("number-of-frames", 'n', "number of frames to print. Use -n 0 or" +
      		" -n all to print all frames.", "<number of frames>") {
	  public void parsed(String arg) throws OptionException {
	      if(arg.equals("all")){
		  options.setNumberOfFrames(0);
	      }else{
		  options.setNumberOfFrames(Integer.parseInt(arg));
		  return;
	      }
	  }
      });
 
      parser.add(new Option("fullpath", 'f', "print full path." +
  		"-f prints full path") {
	  public void parsed(String arg) throws OptionException {
	        options.setPrintFullpath(true);
	  }
      });


    parser.add(new Option("all", 'a', "print all information that can currently be retrieved" +
                          "about the stack\n" +
                          "this is equivalent to -p functions,params,scopes,fullpath"){

                public void parsed (String argument) throws OptionException
                {
                  options.setElfOnly(false);
                  options.setPrintParameters(true);
                  options.setPrintScopes(true);
                  options.setPrintFullpath(true);
                }
              });
    
    parser.add(new Option(
			"virtual",
			'v',
			"Includes virtual frames in the stack trace.\n" +
			"Virtual frames are artificial frames corresponding" +
			" to calls to inlined functions") {

		    public void parsed(String argument) throws OptionException {
			options.setPrintVirtualFrames(true);
			options.setElfOnly(false);
		    }
		});


    parser.add(new Option("common", 'c', "print commonly used debug information:" +
                          "this is equivalent to fstack -v -p functions,params,fullpath"){

                public void parsed (String argument) throws OptionException
                {
                  options.setElfOnly(false);
                  options.setPrintParameters(true);
                  options.setPrintScopes(false);
                  options.setPrintFullpath(true);
                  options.setPrintVirtualFrames(true);
                }
    });
              
    parser.add(new Option("print", 'p', "itmes to print. Possible items:\n" +
                "functions : print function names using debug information\n" +
                "scopes : print variables declared in each scope within the " +
                "function.\n" +
                "params : print function parameters\n" +
                "fullpath : print full executbale path" , "[item],...") {
      
      public void parsed(String arg) throws OptionException
      {
        options.setElfOnly(true);
        options.setPrintParameters(false);
        options.setPrintScopes(false);
        options.setPrintFullpath(false);
        options.setPrintVirtualFrames(false);

          StringTokenizer st = new StringTokenizer(arg, ",");
          while (st.hasMoreTokens())
          {
            String name = st.nextToken();
            if(name.equals("functions")){
              options.setElfOnly(false);
            }
            
            if(name.equals("params")){
              options.setElfOnly(false);
              options.setPrintParameters(true);
            }
            
            if(name.equals("scopes")){
              options.setElfOnly(false);
              options.setPrintScopes(true);
            }
            
            if(name.equals("fullpath")){
              options.setElfOnly(false);
              options.setPrintFullpath(true);
            }
            
          }
      }
    });
    parser.parse(args);    
  }
}
