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
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.TreeMap;

import frysk.debuginfo.DebugInfoStackFactory;
import frysk.debuginfo.PrintStackOptions;
import frysk.event.Event;
import frysk.event.ProcEvent;
import frysk.proc.Proc;
import frysk.proc.Task;
import frysk.rsl.Log;
import frysk.stack.StackFactory;
import frysk.util.ProcStopUtil;
import gnu.classpath.tools.getopt.Option;
import gnu.classpath.tools.getopt.OptionException;

public final class fstack {

  private static PrintWriter printWriter = new PrintWriter(System.out);  
  static PrintStackOptions options = new PrintStackOptions();
  private static final Log fine = Log.fine(fstack.class);
  
  public static void main (String[] args)
  {
      ProcStopUtil fstack = new ProcStopUtil("fstack", args, 
                                              new StackerEvent());
      fstack.setUsage("Usage: fstack <PID> || fstack <COREFILE> [<EXEFILE>]");   
      addOptions (fstack);
      fstack.execute();
  }
  
  /**
   * Implements a ProcEvent for core file creation.
   */
  private static class StackerEvent implements ProcEvent {
      public void executeLive(Proc proc) {
          fine.log(this, "printTasks");
          printTasks(proc);
          proc.requestAbandonAndRunEvent(new Event()
          {
              public void execute ()
              {   
                  printWriter.flush();
              }
          });
      }

      public void executeDead(Proc proc) {
          fine.log(this, "printTasks");
          printTasks(proc);
          printWriter.flush();
      }     
  }
  
  private static void printTasks(Proc proc) {
      
      Task[] tasks = (Task[]) proc.getTasks().toArray
      (new Task[proc.getTasks().size()]);

      TreeMap sortedTasks = new TreeMap();
      for (int i=0; i<tasks.length; i++)
          sortedTasks.put(new Integer(tasks[i].getTid()), tasks[i]);

      Iterator iter = sortedTasks.values().iterator();
      while (iter.hasNext())
      {
          Task task =  (Task) iter.next();

          if(options.elfOnly()){
              StackFactory.printTaskStackTrace(printWriter,task,options);
          }else{
              if(options.printVirtualFrames()){
                  DebugInfoStackFactory.printVirtualTaskStackTrace(printWriter,task,options);
              }else{
                  DebugInfoStackFactory.printTaskStackTrace(printWriter,task,options);
              }
          }
          printWriter.println();
      }
  }
  
  /**
   * Add options to fstack.
   */
  private static void addOptions (ProcStopUtil fstack) {
      fstack.addOption(new Option("number-of-frames", 'n', "number of frames to print. Use -n 0 or" +
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
 
      fstack.addOption(new Option("fullpath", 'f', "print full path." +
                                  "-f prints full path") {
          public void parsed(String arg) throws OptionException {
                options.setPrintFullpath(true);
          }
      });


      fstack.addOption(new Option("all", 'a', "print all information that can currently be retrieved" +
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
    
      fstack.addOption(new Option(
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


      fstack.addOption(new Option("common", 'c', "print commonly used debug information:" +
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
              
      fstack.addOption(new Option("print", 'p', "itmes to print. Possible items:\n" +
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
  }
}
