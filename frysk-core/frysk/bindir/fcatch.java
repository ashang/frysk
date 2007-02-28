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


//import java.util.ArrayList;
import java.util.logging.Logger;

import frysk.util.FCatch;
//import frysk.util.StracePrinter;

import gnu.classpath.tools.getopt.FileArgumentCallback;
import gnu.classpath.tools.getopt.Option;
import gnu.classpath.tools.getopt.Parser;
import gnu.classpath.tools.getopt.OptionException;

import frysk.Config;
import frysk.EventLogger;

public class fcatch
{

  FCatch catcher = new FCatch();
  
  //private static Parser parser;

  //private static int pid;
  protected static final Logger logger = Logger.getLogger("frysk");
  
  private boolean requestedPid;
  
//  private static ArrayList arguments;
  private static String pidString;

  private void run (String[] args)
  {
    Parser parser = new Parser("fcatch", Config.getVersion (), true)
    {
      protected void validate () throws OptionException
      {
        if (! requestedPid && fcatch.pidString == null) //fcatch.arguments == null)
          throw new OptionException("no command or PID specified");
      }
    };
    addOptions(parser);
    EventLogger.addConsoleOptions(parser);
    parser.setHeader("Usage: fcatch [OPTIONS] -- PATH ARGS || fcatch [OPTIONS] PID");

    parser.parse(args, new FileArgumentCallback()
    {
      public void notifyFile (String arg) throws OptionException
      {
//        System.err.println("notifyFile " + arg);
//        if (fcatch.arguments == null)
//          fcatch.arguments = new ArrayList();
//        fcatch.arguments.add(arg);
      }
    });

    if (pidString != null)
  {
      //String[] cmd = (String[]) arguments.toArray(new String[0]);
    String[] cmd = { pidString };
      catcher.trace(cmd, requestedPid);
  }
  }
  
  public void addOptions (Parser p)
  {
    p.add(new Option('p', "pid to trace", "PID") {
      public void parsed(String arg) throws OptionException
      {
          try {
              int pid = Integer.parseInt(arg);
              // FIXME: we have no good way of giving the user an
              // error message if the PID is not available.
              //System.out.println("Option pid: " + pid);
              catcher.addTracePid(pid);
              requestedPid = true;
              pidString = new String();
              pidString = "" + pid; //.add(new Integer(pid));
          } catch (NumberFormatException e) {
              OptionException oe = new OptionException("couldn't parse pid: " + arg);
              oe.initCause(e);
              throw oe;
          }
      }
  });
  }
    
    
  public static void main(String[] args)
  {
    fcatch fc = new fcatch();
    fc.run(args);
  }
}
