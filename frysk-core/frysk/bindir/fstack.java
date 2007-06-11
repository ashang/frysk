// This file is part of the program FRYSK.
//
// Copyright 2005, 2006, 2007, Red Hat Inc.
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

import java.io.File;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import frysk.event.Event;
import frysk.event.RequestStopEvent;
import frysk.proc.Manager;
import frysk.proc.Proc;
import frysk.proc.ProcBlockAction;
import frysk.proc.ProcCoreAction;
import frysk.proc.ProcId;
import frysk.util.CommandlineParser;
import frysk.util.StacktraceAction;
import frysk.util.Util;
import gnu.classpath.tools.getopt.Option;
import gnu.classpath.tools.getopt.OptionException;
import gnu.classpath.tools.getopt.Parser;

public final class fstack
{
  private static StacktraceAction stacker;

  private static Parser parser;

  protected static final Logger logger = Logger.getLogger("frysk"); 
  
  static boolean elfOnly = true;
  static boolean printParameters = false;
  static boolean printScopes = false;
  static boolean fullpath = false;
  
  private static class Stacker extends StacktraceAction
  {

    Proc proc;
    public Stacker (Proc theProc, Event theEvent,boolean elfOnly, boolean printParameters, boolean printScopes, boolean fullpath)
    {
      super(theProc, theEvent, elfOnly, printParameters, printScopes, fullpath);
      System.err.println("Stacker created");
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
        System.out.print(stacker.toPrint());
      }
    });
    }
    
  }
  
  private static class PrintEvent implements Event
  {
    public void execute()
    {
      Manager.eventLoop.requestStop();
      System.out.println(stacker.toPrint());
    }
  }
  
  private static void stackCore(File coreFile)
  {
    Proc proc = Util.getProcFromCoreFile(coreFile);
    stacker = new Stacker(proc, new PrintEvent(),elfOnly,printParameters,printScopes, fullpath);
    new ProcCoreAction(proc, stacker);
    Manager.eventLoop.run();
  }
  
  private static void stackPid (ProcId procId)
  {
    Proc proc = Util.getProcFromPid(procId);
    stacker = new Stacker(proc, new AbandonPrintEvent(proc),elfOnly,printParameters,printScopes, fullpath);
    new ProcBlockAction(proc, stacker);
    Manager.eventLoop.run();
  }
  
  public static void main (String[] args)
  {
    parser = new CommandlineParser("fstack")
    {
      //@Override
      public void parseCores (File[] coreFiles)
      {
       for (int i = 0; i < coreFiles.length; i++)
         stackCore(coreFiles[i]);
      }

      //@Override
      public void parsePids (ProcId[] pids)
      {
        for (int i = 0; i < pids.length; i++)
          stackPid(pids[i]);
      }
      
    };
    
    parser.add(new Option("print", 'p', "itmes to print. Possible items:\n" +
                "functions : print function names using debug information\n" +
                "scopes : print variables declared in each scope within the " +
                "function.\n" +
                "params : print function parameters\n" +
                "fullpath : print full executbale path" , "[item],...") {
      
      public void parsed(String arg) throws OptionException
      {
        elfOnly = true;
        printParameters = false;
        printScopes = false;
        fullpath = false;
        
          StringTokenizer st = new StringTokenizer(arg, ",");
          while (st.hasMoreTokens())
          {
            String name = st.nextToken();
            if(name.equals("functions")){
              elfOnly = false;
            }
            
            if(name.equals("params")){
              elfOnly = false;
              printParameters = true;
            }
            
            if(name.equals("scopes")){
              elfOnly = false;
              printScopes = true;
            }
            
            if(name.equals("fullpath")){
              elfOnly = false;
              fullpath = true;
            }
            
          }
      }
    });
    parser.parse(args);    
  }
}
