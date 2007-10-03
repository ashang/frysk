// This file is part of the program FRYSK.
//
// Copyright 2005, Red Hat Inc.
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

import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import frysk.EventLogger;

import frysk.event.Event;
import frysk.event.RequestStopEvent;

import frysk.proc.Manager;
import frysk.proc.Proc;
import frysk.proc.ProcId;
import frysk.proc.Host;

import frysk.util.CoredumpAction;

import gnu.classpath.tools.getopt.FileArgumentCallback;
import gnu.classpath.tools.getopt.Option;
import gnu.classpath.tools.getopt.OptionException;
import gnu.classpath.tools.getopt.Parser;

public class fcore
{

  private static boolean hasProc = false;
  
  private static CoredumpAction stacker;
  
  protected static final Logger logger = EventLogger.get("logs/",
  "frysk_core_event.log");
    private static Parser parser;

  private static String levelValue;

  private static Level level;

  

  /**
   * Entry function. Starts the fcore dump process. Belongs in bindir/fcore. But
   * here for now.
   * 
   * @param args - pid of the process to core dump
   */
  public static void main (String[] args)
  {

    // Parse command line. Check pid provided.
    parser = new Parser("fcore", "1.0", true)
    {
      protected void validate () throws OptionException
      {
        if (!hasProc)
            throw new OptionException("No pid(s) provided");
      }
    };
    addOptions(parser);

    parser.setHeader("Usage: fcore <pids>");

    parser.parse(args, new FileArgumentCallback()
    {
      public void notifyFile (String arg) throws OptionException
      {
        try
        {
              hasProc = true;
              int pid = 0;
              try
              {
                pid = Integer.parseInt(arg);
              } catch (NumberFormatException nfe)
              {
                System.err.println("Argument " + arg + " does not appear to be a valid pid");
                return;
              }
              Manager.host.requestFindProc(new ProcId (pid), new Host.FindProc() {

                public void procFound (ProcId procId)
                {
                  final Proc proc = Manager.host.getProc(procId);
                  boolean isOwned = (proc.getUID() == Manager.host.getSelf().getUID() || 
                      proc.getGID() == Manager.host.getSelf().getGID());

                  // Do we have permission to work on this process?
                  if (! isOwned)
                    {
                      System.err.println("Process " + proc.getPid()
                                         + " is not owned by user/group. Cannot dump core.");
                      System.exit(2);
                    }
                  
                  if (proc == null)
                    {
                      System.err.println("Could not attach to the process " + procId.toString());                    
                      System.exit(3);
                    }

                 
                  stacker = new CoredumpAction(proc, new Event()
                  {
                    public void execute ()
                    {
                      proc.requestAbandonAndRunEvent(new RequestStopEvent(Manager.eventLoop));
                    }
                  });
                }

                public void procNotFound (ProcId procId, Exception e)
                {
                  System.err.println("Could not find the process: " + procId.toString());
                  System.exit(4);
                }});            
              

        }
      catch (Exception _)
        {
          throw new OptionException("Could not parse the PID list.");
        }
    }
    });

    // Set log level.
    if (levelValue != null)
      {
        logger.setLevel(level);
      }
    

    Manager.eventLoop.run();

    stacker.getClass();
  }
  
 
  /**
   * Add options to the the option parser. Belongs
   * 
   * @param parser - the parser that is to be worked on.
  */
  private static void addOptions (Parser parser)
  {
    parser.add(new Option(
                          "console",
                          'c',
                          "Set the console level. The console-level can be "
                              + "[ OFF | SEVERE | WARNING | INFO | CONFIG | FINE | FINER | FINEST | ALL]",
                          "<console-level>")
    {
      public void parsed (String consoleValue) throws OptionException
      {
        try
          {
            Level consoleLevel = Level.parse(consoleValue);
            Handler consoleHandler = new ConsoleHandler();
            consoleHandler.setLevel(consoleLevel);
            logger.addHandler(consoleHandler);
            logger.setLevel(consoleLevel);
          }
        catch (IllegalArgumentException e)
          {
            throw new OptionException("Invalid log console: " + consoleValue);
          }

      }
    });
    
    parser.add(new Option(
                          "level",
                          'l',
                          "Set the log level. The log-level can be "
                              + "[ OFF | SEVERE | WARNING | INFO | CONFIG | FINE | FINER | FINEST | ALL]",
                          "<log-level>")
    {

      public void parsed (String logLevel) throws OptionException
      {
        levelValue = logLevel;
        try
          {
            level = Level.parse(levelValue);
            logger.setLevel(level);
          }
        catch (IllegalArgumentException e)
          {
            throw new OptionException("Invalid log level: " + levelValue);
          }
      }
    });
  }
}
