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

import frysk.util.CoredumpAction;

import gnu.classpath.tools.getopt.FileArgumentCallback;
import gnu.classpath.tools.getopt.Option;
import gnu.classpath.tools.getopt.OptionException;
import gnu.classpath.tools.getopt.Parser;

public class fcore
{

  protected static final Logger logger = EventLogger.get("logs/",
  "frysk_core_event.log");
    private static Parser parser;

  private static String levelValue;

  private static Level level;

  private static Proc proc;

  /**
   * Entry function. Starts the fcore dump process. Belongs in bindir/fcore. But
   * here for now.
   * 
   * @param args - pid of the process to core dump
   */
  public static void main (String[] args)
  {

    System.out.println("Experimental do not use for 'real life' core file generation.");

    // Parse command line. Check pid provided.
    parser = new Parser("fcore", "1.0", true)
    {
      protected void validate () throws OptionException
      {
        if (proc == null)
            throw new OptionException("no pid provided");
      }
    };
    addOptions(parser);

    parser.setHeader("Usage: fcore <PID>");

    // XXX: should support > 1 pid, but for now, just one pid.
    parser.parse(args, new FileArgumentCallback()
    {
      public void notifyFile (String arg) throws OptionException
      {
        try
        {
          if (proc == null)
            {
              Manager.host.requestRefreshXXX(true);

              Manager.eventLoop.runPending();

              int pid = Integer.parseInt(arg);
              proc = Manager.host.getProc(new ProcId(pid));
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

    // Set log level.
    if (levelValue != null)
      {
        logger.setLevel(level);
      }
    
    boolean isOwned = (proc.getUID() == Manager.host.getSelf().getUID() || 
        proc.getGID() == Manager.host.getSelf().getGID());

    // Do we have permission to work on this process?
    if (! isOwned)
      {
        System.err.println("Process " + proc.getPid()
                           + " is not owned by user/group. Cannot coredump.");
        System.exit(- 1);
      }
    
    if (proc == null)
      {
        System.err.println("Couldn't get the process " + proc.getPid()
                           + ". It might have disappeared.");
        System.exit(- 1);
      }

    final CoredumpAction stacker = new CoredumpAction(proc, new Event()
    {
      public void execute ()
      {
        proc.requestAbandonAndRunEvent(new RequestStopEvent(Manager.eventLoop));
      }
    });

    Manager.eventLoop.run();

    stacker.getClass();
  }
  
  /**
   * Add options ot the the option parser. Belongs in bindir/fcore but here for
   * now
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
