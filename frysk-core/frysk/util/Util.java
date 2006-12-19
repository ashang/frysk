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


package frysk.util;

import java.io.PrintStream;
import java.util.LinkedList;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import frysk.Config;
import frysk.proc.ProcId;
import frysk.proc.Task;
import frysk.proc.TaskException;
import frysk.rt.StackFactory;
import frysk.rt.StackFrame;
import gnu.classpath.tools.getopt.FileArgumentCallback;
import gnu.classpath.tools.getopt.Option;
import gnu.classpath.tools.getopt.OptionException;
import gnu.classpath.tools.getopt.Parser;

public class Util
{

  private Util ()
  {
  }

  public static void printStackTrace (PrintStream writer, Task task)
  {
    writer.println("Stack trace for task " + task);
    try
      {
        for (StackFrame frame = StackFactory.createStackFrame(task); frame != null; frame = frame.getOuter())
          {
            // FIXME: do valgrind-like '=== PID ===' ?
            writer.print("  ");
            writer.println(frame);
          }
      }
    catch (TaskException _)
      {
        // FIXME: log exception, or rethrow?
        writer.println("... couldn't print stack trace");
      }
  }

  public static class PidParser
      extends Parser
  {
    LinkedList pidList = new LinkedList();

    public PidParser (String programName)
    {
      super(programName, Config.VERSION, true);
    }

    protected void validate () throws OptionException
    {
      if (pidList.isEmpty())
        throw new OptionException("No pid(s) provided");
    }

  }

  public static LinkedList parsePids (final PidParser parser, String[] args)
  {
    parser.parse(args, new FileArgumentCallback()
    {
      public void notifyFile (String arg) throws OptionException
      {
        try
          {
            int pid = Integer.parseInt(arg);
            parser.pidList.add(new ProcId(pid));
          }
        catch (NumberFormatException nfe)
          {
            throw new OptionException("Argument " + arg + " does "
                                      + "not appear to be a valid pid.");

          }
      }
    });
    return parser.pidList;
  }

  public static void addConsoleOptions (final Logger logger, Parser parser)
  {
    parser.add(new Option(
                          "console",
                          "Set the console level. The console-level can be "
                              + "[ OFF | SEVERE | WARNING | INFO | CONFIG | FINE | FINER | FINEST | ALL]",
                          "<console-level>")
    {
      public void parsed (String consoleValue) throws OptionException
      {
        try
          {
            Level consoleLevel = Level.parse(consoleValue);
            // Need to set both the console and the main logger as
            // otherwize the console won't see the log messages.

            System.out.println("console " + consoleLevel);
            Handler consoleHandler = new ConsoleHandler();
            consoleHandler.setLevel(consoleLevel);
            logger.addHandler(consoleHandler);
            logger.setLevel(consoleLevel);
            System.out.println(consoleHandler);

          }
        catch (IllegalArgumentException e)
          {
            throw new OptionException("Invalid log console: " + consoleValue);
          }

      }
    });
    parser.add(new Option(
                          "log",
                          "Set the log level. The log-level can be "
                              + "[ OFF | SEVERE | WARNING | INFO | CONFIG | FINE | FINER | FINEST | ALL]",
                          "<log-level>")
    {
      public void parsed (String arg0) throws OptionException
      {
        String levelValue = arg0;
        try
          {
            Level level = Level.parse(levelValue);
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
