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

import java.util.logging.Level;
import java.util.logging.Logger;

import frysk.EventLogger;

import frysk.event.Event;
import frysk.event.RequestStopEvent;

import frysk.proc.Manager;
import frysk.proc.Proc;
import frysk.proc.ProcId;
import frysk.proc.Host;

import frysk.util.StacktraceAction;
import frysk.util.Util;

import gnu.classpath.tools.getopt.FileArgumentCallback;
import gnu.classpath.tools.getopt.OptionException;
import gnu.classpath.tools.getopt.Parser;

import frysk.Config;

public class fstack
{

  private static boolean hasProc = false;

  private static StacktraceAction stacker;

  private static Parser parser;

  protected static final Logger logger = EventLogger.get("logs/",
                                                         "frysk_core_event.log");

  private static String levelValue;

  private static Level level;

  private static void addOptions (Parser parser)
  {
    Util.addConsoleOptions(logger, parser);
  }

  public static void main (String[] args)
  {

    parser = new Parser("fstack", Config.VERSION, true)
    {
      protected void validate () throws OptionException
      {
        if (! hasProc)
          {
            throw new OptionException("no pid provided");
          }
      }
    };
    addOptions(parser);
    parser.setHeader("Usage: fstack <PID>");

    parser.parse(args, new FileArgumentCallback()
    {
      public void notifyFile (String arg) throws OptionException
      {
        try
          {
            hasProc = true;
            int pid = Integer.parseInt(arg);
            Manager.host.requestFindProc(new ProcId(pid), new Host.FindProc()
            {
              public void procFound (ProcId procId)
              {
                final Proc proc = Manager.host.getProc(procId);

                stacker = new StacktraceAction(proc, new Event()
                {
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
                })
                {
                  public void addFailed (Object observable, Throwable w)
                  {
                    w.printStackTrace();
                    proc.requestAbandonAndRunEvent(new RequestStopEvent(
                                                                        Manager.eventLoop));

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
                };

              }

              public void procNotFound (ProcId procId, Exception e)
              {
                System.err.println("Couldn't find the proc with proc Id"
                                   + procId);
                Manager.eventLoop.requestStop();
              }
            });

            Manager.eventLoop.run();

          }
        catch (NumberFormatException _)
          {
            throw new OptionException("couldn't parse pid");
          }
      }
    });

    if (levelValue != null)
      {
        logger.setLevel(level);
      }

  }
}
