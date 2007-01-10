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

import inua.util.PrintWriter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import frysk.util.Ftrace;
import frysk.util.StracePrinter;

import gnu.classpath.tools.getopt.FileArgumentCallback;
import gnu.classpath.tools.getopt.Option;
import gnu.classpath.tools.getopt.OptionException;
import gnu.classpath.tools.getopt.Parser;

import frysk.Config;
import frysk.EventLogger;


class ftrace
{
//Where to send output.
    PrintWriter writer;
   
    protected static final Logger logger = Logger.getLogger("frysk");
    
    // Set of all Syscalls we want to trace.
    // This is null if the user hasn't specified any.
    HashSet tracedCalls;
    // True if a PID was requested.
    boolean requestedPid;
    // Command and arguments to exec.
    ArrayList commandAndArguments;

    Ftrace tracer = new Ftrace();

    private void addOptions(Parser parser)
    {
	EventLogger.addConsoleOptions(parser);
        parser.add(new Option('o', "output file name", "FILE") {
            public void parsed(String filename) throws OptionException
            {
                // FIXME: strace supports '|' and '!' here for piping.
                try {
                    writer = new PrintWriter(new FileOutputStream(filename));
                } catch (FileNotFoundException fnfe) {
                    OptionException oe = new OptionException(fnfe.getMessage());
                    oe.initCause(fnfe);
                    throw oe;
                }
            }
        });
        parser.add(new Option('c', "trace children as well") {
            public void parsed(String arg0) throws OptionException
            {
                tracer.setTraceChildren();
            }
        });
        parser.add(new Option("trace", 't', "syscalls to trace", "CALL[,CALL]...") {
            public void parsed(String arg) throws OptionException
            {
                StringTokenizer st = new StringTokenizer(arg, ",");
                while (st.hasMoreTokens())
                {
                    String name = st.nextToken();
                    // FIXME: there's no good way to error out if the
                    // syscall is unknown.
                    if (tracedCalls == null)
                        tracedCalls = new HashSet();
                    tracedCalls.add(name);
                }
            }
        });
        parser.add(new Option('p', "pid to trace", "PID") {
            public void parsed(String arg) throws OptionException
            {
                try {
                    int pid = Integer.parseInt(arg);
                    // FIXME: we have no good way of giving the user an
                    // error message if the PID is not available.
                    tracer.addTracePid(pid);
                    requestedPid = true;
                } catch (NumberFormatException _) {
                    OptionException oe = new OptionException("couldn't parse pid: " + arg);
                    oe.initCause(_);
                    throw oe;
                }
            }
        });
    }

    public void run(String[] args)
    {
        Parser parser = new Parser("ftrace", Config.getVersion (), true) {
            protected void validate() throws OptionException {
                if (! requestedPid && commandAndArguments == null)
                    throw new OptionException("no command or PID specified");
            }
        };
        addOptions(parser);
        parser.setHeader("usage: ftrace [OPTIONS] COMMAND ARGS...");
        
        parser.parse(args, new FileArgumentCallback() {
            public void notifyFile(String arg) throws OptionException
            {
                if (commandAndArguments == null) {
                  
                  //Check if the first argument is a pid, otherwise it will be a command.
                  try {
                  int pid = Integer.parseInt(arg);
                  tracer.addTracePid(pid);
                  requestedPid = true;
                  return;
                  } catch (NumberFormatException _) {
                    commandAndArguments = new ArrayList();
                  }
                }
                commandAndArguments.add(arg);
            }
        });
        if (writer == null)
            writer = new PrintWriter(System.out);
        StracePrinter printer = new StracePrinter(writer, tracedCalls);

        tracer.setWriter(writer);
        tracer.setEnterHandler(printer);
        tracer.setExitHandler(printer);

        if (commandAndArguments != null)
        {
            String[] cmd = (String[]) commandAndArguments.toArray(new String[0]);
            tracer.trace(cmd);
        }
        else
            tracer.trace();
    }

    public ftrace()
    {
    }

    public static void main(String[] args)
    {
        ftrace m = new ftrace();
        m.run(args);
    }
}
