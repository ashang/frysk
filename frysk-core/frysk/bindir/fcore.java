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

import java.util.logging.Logger;

import frysk.event.Event;

import frysk.proc.Manager;
import frysk.proc.Proc;
import frysk.proc.ProcBlockAction;
import frysk.proc.ProcId;

import frysk.util.CommandlineParser;
import frysk.util.CoredumpAction;
import frysk.util.Util;

import gnu.classpath.tools.getopt.Option;
import gnu.classpath.tools.getopt.OptionException;


public class fcore
{

  private static String filename = "core";

  private static boolean writeAllMaps = false;

  private static CoredumpAction stacker;

  protected static final Logger logger = Logger.getLogger("frysk");
  private static CommandlineParser parser;

  private static class AbandonCoreEvent implements Event
  {
    private Proc coreProc;

    AbandonCoreEvent(Proc coreProc)
    {
      this.coreProc = coreProc;
    }
    
    public void execute()
    {
      coreProc.requestAbandonAndRunEvent(new Event()
      {

        public void execute ()
        {
          Manager.eventLoop.requestStop();
        }
      });
    }
  }
  
  public static void dumpPid(ProcId procId)
  {
    Proc coreProc = Util.getProcFromPid(procId);
    stacker = new CoredumpAction(coreProc, filename, 
                                 new AbandonCoreEvent(coreProc), writeAllMaps);

    new ProcBlockAction(coreProc, stacker);
    Manager.eventLoop.run();
  }
  
  /**
   * Entry function. Starts the fcore dump process. Belongs in bindir/fcore. But
   * here for now.
   * 
   * @param args - pid of the process to core dump
   */
  public static void main (String[] args)
  {

    // Parse command line. Check pid provided.
    parser = new CommandlineParser("fcore")
    {

      //@Override
      public void parsePids (ProcId[] pids)
      {
        for (int i= 0; i< pids.length; i++)
          dumpPid(pids[i]);
        
        System.exit(0);
      }
    };

    addOptions(parser);

    parser.setHeader("Usage: fcore [-a] [-o filename] [-c level] [-l level] <pids>");

    parser.parse(args);

    //If we got here, we didn't find a pid.
    System.err.println("Error: No pid provided.");
    parser.printHelp();
    System.exit(1);
  }

  /**
   * Add options to the the option parser. Belongs
   * 
   * @param parser - the parser that is to be worked on.
   */
  private static void addOptions (CommandlineParser parser)
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
  }
}
