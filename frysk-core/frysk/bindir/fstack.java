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
import java.util.Iterator;
import java.util.logging.Logger;

import frysk.event.Event;
import frysk.event.RequestStopEvent;

import frysk.proc.Manager;
import frysk.proc.Proc;
import frysk.proc.ProcBlockAction;
import frysk.proc.ProcId;
import frysk.proc.Host;
import frysk.proc.corefile.LinuxHost;

import frysk.util.CommandlineParser;
import frysk.util.StacktraceAction;

import gnu.classpath.tools.getopt.Parser;

public final class fstack
{
  private static StacktraceAction stacker;

  private static Parser parser;

  protected static final Logger logger = Logger.getLogger("frysk"); 

  

  private static void stackPid(ProcId procId)
  {
    Manager.host.requestFindProc(procId, new Host.FindProc()
    {
      public void procFound (ProcId procId)
      {
        final Proc proc = Manager.host.getProc(procId);
        stackProc(proc);
      }

      public void procNotFound (ProcId procId, Exception e)
      {
        System.err.println("Couldn't find the process: "
                           + procId.toString());
        Manager.eventLoop.requestStop();
      }
    });

    Manager.eventLoop.run();
  }
  
  private static void stackCore(File coreFile)
  {
    LinuxHost core = new LinuxHost(Manager.eventLoop, coreFile);
    
    core.requestRefreshXXX();
    Manager.eventLoop.runPending();
   Iterator iterator =  core.getProcIterator();
   
   while (iterator.hasNext())
     {
       Proc proc = (Proc) iterator.next();
       stackProc(proc);
     }
  }
  
  private static void stackProc (final Proc proc)
  {
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
    };
    
    new ProcBlockAction(proc, stacker);
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
    parser.setHeader("Usage: fstack <PID> | <CORE> ...");

    parser.parse(args);    
  }
}
