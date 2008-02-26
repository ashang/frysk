// This file is part of the program FRYSK.
//
// Copyright 2006, 2007, 2008 Red Hat Inc.
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

import frysk.event.Event;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import frysk.hpd.CLI;
import jline.Completor;
import jline.ConsoleReader;
import frysk.util.CoreExePair;
import frysk.proc.Manager;
import frysk.util.CommandlineParser;
import frysk.util.ObservingTerminal;
import gnu.classpath.tools.getopt.Option;
import gnu.classpath.tools.getopt.OptionException;
import frysk.sys.FileDescriptor;
import frysk.proc.Proc;

public class fhpd {
    private static Proc[] procs;
    private static File execFile;
    private static File core;
    private static File exeFile;
    private static boolean noExe = false;
    private static String sysroot;
    private static int exitStatus;

    final static class FhpdCompletor implements Completor {
        CLI cli;
        public FhpdCompletor (CLI cli) {
            this.cli = cli;
        }
        public int complete (String buffer, int cursor, List candidates) {
            return cli.complete (buffer, cursor, candidates);
        }
    }

    // Start the command line in its own thread; but from within
    // the event-loop.  This ensures that the event-loop is up and
    // running before the CLI.
    static class CommandLine extends Thread implements Event {
	private String line = "";
	private CLI cli;
	private ConsoleReader reader;
	CommandLine() {
	    // Construct a command to pass in as initialization
	    try {
		if (procs != null)
		    line = "attach " + procs[0].getPid();
		else if (execFile != null)
		    line = "load " + execFile.getCanonicalPath();
		else if (core != null) {
		    line = "core " + core.getCanonicalPath();      
		    if (exeFile != null)
			line += " " + exeFile.getCanonicalPath();
		    else if (noExe)
			line +=" -noexe";
		}
		if (sysroot != null)
		    line = line + " -sysroot " + sysroot;
	    } catch (IOException e) {
		System.err.println("Error: " + e);
		System.exit(1);
		return;
	    }
	    // Construct the HPD.
	    cli = new CLI("(fhpd) ", System.out);
	    try {
		reader = new ConsoleReader
		    (new FileInputStream(java.io.FileDescriptor.in),
		     new PrintWriter(System.out),
		     null,
		     new ObservingTerminal(FileDescriptor.in));
	    } catch (IOException ioe) {
		System.out.println("ERROR: Could not create a command line");
		System.out.print(ioe.getMessage());
		System.exit(1);
		return;
	    }
	    Completor fhpdCompletor = new FhpdCompletor(cli);
	    reader.addCompletor(fhpdCompletor);
	}
	public void execute() {
	    start();
	}
	public void run() {
	    try {
		cli.execCommand(line);
		while (line != null && ! (line.equals("quit")
					  || line.equals("q")
					  || line.equals("exit"))) {
		    line = reader.readLine(cli.getPrompt());
		    cli.execCommand(line);
		}
	    } catch (IOException ioe) {
		System.out.println("ERROR: Could not read from command line");
		System.out.print(ioe.getMessage());
		exitStatus = 1;
	    }
	    Manager.eventLoop.requestStop();
	}
    }
    
    public static void main (String[] args) {
        CommandlineParser parser = new CommandlineParser ("fhpd") {
                //@Override
                public void parseCommandFIXME(String[] command) {
                    execFile = new File (command[0]);
                    if (execFile.canRead() == false) {
                        printHelp();
                        throw new RuntimeException("command not readable: " 
                                                   + command[0]);
                    }
                }
                //@Override
                public void parsePids(Proc[] procs) {
		    fhpd.procs = procs;
                }

                public void parseCoresFIXME(CoreExePair[] corePairs) {
                    core = corePairs[0].coreFile;
                    exeFile = corePairs[0].exeFile;
                }
            };
        parser.add(new Option("noexe", "Do not attempt to read an"+
                              " executable for a corefile ") {
                public void parsed(String exeValue) throws OptionException {
                    try {
                        noExe = true;
	    
                    } catch (IllegalArgumentException e) {
                        throw new OptionException("Invalid noexe parameter "
                                                  + exeValue);
                    }
                }
            });
        parser.add(new Option("sysroot", 's',
                              "Assume the executable is from a sysroot build ",
                              "SysRoot-Path") {
                public void parsed(String sysrootValue) throws OptionException {
                    try {
                        sysroot = sysrootValue;
                    } catch (IllegalArgumentException e) {
                        throw new OptionException("Invalid sysroot parameter "
                                                  + sysrootValue);
                    }
                }
            });
    
        parser.setHeader("Usage: fhpd <PID> || fhpd <EXEFILE> || fhpd <COREFILE> [<EXEFILE>]");
        parser.parse(args);

	Manager.eventLoop.add(new CommandLine());

	// Run the event loop then exit when it exits (or crashes).
	Manager.eventLoop.run();
	System.exit(exitStatus);
    }
}
