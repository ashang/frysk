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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.logging.Logger;

import frysk.EventLogger;

import frysk.event.Event;

import frysk.proc.Manager;
import frysk.proc.Proc;
import frysk.proc.ProcId;
import frysk.proc.Host;

import frysk.util.CoredumpAction;
import frysk.util.Util;

import gnu.classpath.tools.getopt.Option;
import gnu.classpath.tools.getopt.OptionException;
import gnu.classpath.tools.getopt.Parser;


public class fcore
{

  private static String filename = "core";

  private static boolean writeAllMaps = false;

  private static CoredumpAction stacker;

  protected static final Logger logger = EventLogger.get("logs/",
                                                         "frysk_core_event.log");

  private static Util.PidParser parser;

  /**
   * Entry function. Starts the fcore dump process. Belongs in bindir/fcore. But
   * here for now.
   * 
   * @param args - pid of the process to core dump
   */
  public static void main (String[] args)
  {

    // Parse command line. Check pid provided.
    parser = new Util.PidParser("fcore");

    addOptions(parser);

    parser.setHeader("Usage: fcore [-a] [-o filename] [-c level] [-l level] <pids>");
    
    LinkedList pidList = Util.parsePids(parser, args);
    
    Iterator iter = pidList.iterator();

    while (iter.hasNext())
      {
    
        ProcId procId = (ProcId) iter.next();

        Manager.host.requestFindProc(procId, new Host.FindProc()
        {
          public void procFound (ProcId procId)
          {
            final Proc coreProc = Manager.host.getProc(procId);

            stacker = new CoredumpAction(coreProc, filename, new Event()
            {
              public void execute ()
              {
                coreProc.requestAbandonAndRunEvent(new Event()
                {

                  public void execute ()
                  {
                    Manager.eventLoop.requestStop();
                  }
                });
              }
            }, writeAllMaps);

          }

          public void procNotFound (ProcId procId, Exception e)
          {
            System.err.println("fcore: Could not find the process: "
                               + procId.toString());
            Manager.eventLoop.requestStop();
          }
        });

        Manager.eventLoop.run();
      }

    stacker.getClass();
  }

  /**
   * Add options to the the option parser. Belongs
   * 
   * @param parser - the parser that is to be worked on.
   */
  private static void addOptions (Parser parser)
  {

    parser.add(new Option("allmaps", 'a',
                          " Writes all readable maps. Does not elide"
                              + " or omit any readable map. Caution: could"
                              + " take considerable amount of time to"
                              + " construct core file.")
    {
      public void parsed (String mapsValue) throws OptionException
      {
        try
          {
            writeAllMaps = true;

          }
        catch (IllegalArgumentException e)
          {
            throw new OptionException("Invalid maps parameter " + mapsValue);
          }

      }
    });

    parser.add(new Option("outputfile", 'o',
                          "Sets the name (not extension) of the core"
                              + " file. Default is core.{pid}. The extension"
                              + " will always be the pid.", "<filename>")
    {
      public void parsed (String filenameValue) throws OptionException
      {
        try
          {
            filename = filenameValue;

          }
        catch (IllegalArgumentException e)
          {
            throw new OptionException("Invalid output filename: "
                                      + filenameValue);
          }

      }
    });
    
    Util.addConsoleOptions(logger, parser);
  }
}
