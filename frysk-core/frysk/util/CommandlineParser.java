// This file is part of the program FRYSK.
//
// Copyright 2007, 2008 Red Hat Inc.
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

import frysk.config.FryskVersion;
import java.io.File;
import java.util.LinkedList;
import lib.dwfl.Elf;
import lib.dwfl.ElfCommand;
import lib.dwfl.ElfEHeader;
import gnu.classpath.tools.getopt.Option;
import gnu.classpath.tools.getopt.OptionException;
import gnu.classpath.tools.getopt.Parser;
import frysk.rsl.LogOption;
import frysk.rsl.Log;
import frysk.sysroot.SysRootCache;
import frysk.proc.dead.LinuxCoreFactory;
import frysk.proc.dead.LinuxExeFactory;
import frysk.proc.Proc;

/**
 * CommandlineParser extends the getopt {@link Parser} class with
 * common options for Frysk command-line applications.
 */
public class CommandlineParser {
    private static final Log fine = Log.fine(CommandlineParser.class);

    private final Parser parser;
    private boolean extendedCore = true;
    private String explicitExe = null;

    public CommandlineParser(String name, String version) {
	parser = new Parser(name, version, true);
	parser.add(new LogOption("debug"));
	add(new Option("noexe", "Do not attempt to read an"+
		       " executable for a corefile ") {
		public void parsed(String exeValue) throws OptionException {
		    extendedCore = false;
		    explicitExe = null;
		}
	    });
	add(new Option("exe",
		       "Specify the full path of the executable to read",
		       "<executable>") {
		public void parsed(String exeValue) throws OptionException {
		    extendedCore = true;
		    explicitExe = exeValue;
		}
	    });
	add(new Option("sysroot", "Special root directory", "<path to sysroot>") {
		public void parsed(String arg) throws OptionException {
		    parseSysRoot(arg);
		}
	    });
    }

    public CommandlineParser(String programName) {
	this(programName, FryskVersion.getVersion());
    }

    /**
     * Callback function. Gives an array of pids if pids were detected
     * on the command line.
     * 
     * @param pids The array of pids passed on the command line.
     */
    public void parsePids(Proc[] pids) {
	System.err.println("Error: Pids not supported.");
	System.exit(1);
    }

    /**
     * Callback function. Gives an array of core files if core files
     * were detected on the command line.
     * 
     * @param coreFiles The array of core files passed on the command
     * line.
     */
    public void parseCores(Proc[] cores) {
	System.err.println("Error: Corefiles not supported.");
	System.exit(1);
    }

    /**
     * Callback function. Gives a Proc represented a parsed command.
     * 
     * @param command The parsed command.
     */
    public void parseCommand(Proc command) {
	System.err.println("Error: Commands not supported.");
	System.exit(1);
    }

    /**
     * Callback function. Sets the default sysroot
     * 
     * @param sysrootPath The special root directory
     */
    private void parseSysRoot(String sysrootPath) {
	SysRootCache.setDefaultSysroot(sysrootPath);
    }

    public String[] parse(String[] args) {
	try {
	    fine.log(this, "parse", args);
	    String[] result = doParse(args);
	    validate();
	    return result;
	} catch (Exception e) {
	    fine.log(this, "parse failed", e);
	    if (e.getMessage() == null)
		System.err.println("Error: " + e.toString());
	    else
		System.err.println("Error: " + e.getMessage());
	    System.exit(1);
	    return null; // To fool Java
	}
    }

    private String[] doParse(String[] args) throws OptionException {
	String[] result = parser.parse(args);

	// XXX: Should parseCommand be called with an empty array here?
	if (result == null)
	    return null;
	if (result.length == 0)
	    return result;

	// Check if arguments are all pids.
	try {
	    Proc[] procs = new Proc[result.length];
	    procs[0] = Util.getProcFromPid(Integer.parseInt(result[0]));
	    for (int i = 1; i < result.length; i++) {
		try {
		    procs[i] = Util.getProcFromPid(Integer.parseInt(result[i]));
		} catch (NumberFormatException e) {
		    throw new OptionException("Please don't mix pids with core files or executables");
		}
	    }
	    fine.log(this, "parse pids", procs);
	    parsePids(procs);
	    return result;
	} catch (NumberFormatException e) {
	    // Not a pid, continue on.
	}

	// Check if arguments are all core file/ exe file pairs..
	if (isCoreFile(result[0])) {
	    LinkedList coreList = new LinkedList();
	    for (int file = 0; file < result.length; /*see below*/) {
		if (isCoreFile(result[file])) {
		    Proc proc;
		    File coreFile = new File(result[file]);
		    if (extendedCore
			&& file + 1 < result.length
			&& isExeFile(result[file + 1])) {
			File exeFile = new File(result[file + 1]);
			proc = LinuxCoreFactory.createProc(coreFile, exeFile);
			file += 2;
		    } else if (explicitExe != null) {
			File exeFile = new File(explicitExe);
			proc = LinuxCoreFactory.createProc(coreFile, exeFile);
			file += 1;
		    } else {
			proc = LinuxCoreFactory.createProc(coreFile,
							   extendedCore);
			file += 1;
		    }
		    coreList.add(proc);
		} else {
		    throw new OptionException("Please don't mix core files with pids or executables.");
		}
	    }
	    Proc[] cores = new Proc[coreList.size()];
	    coreList.toArray(cores);
	    fine.log(this, "parse cores", cores);
	    parseCores(cores);
	    return result;
	}

	// If not above, then this is an executable command.
	Proc command;
	if (explicitExe != null)
	    command = LinuxExeFactory.createProc(new File(explicitExe), result);
	else
	    command = LinuxExeFactory.createProc(result);
	fine.log(this, "parse command", command);
	parseCommand(command);
	return result;
    }

    /**
     * Check if the given file is a coreFile.
     * 
     * @param fileName A file to check.
     * @return if fileName is a corefile returns true, otherwise false.
     */
    private boolean isCoreFile(String fileName) {
	Elf elf;
	try {
	    elf = new Elf(new File(fileName), ElfCommand.ELF_C_READ);
	} catch (Exception e) {
	    return false;
	}

	boolean ret = elf.getEHeader().type == ElfEHeader.PHEADER_ET_CORE;
	elf.close();
	return ret;
    }

    private boolean isExeFile(String fileName) {
	Elf elf;
	try {
	    elf = new Elf(new File(fileName), ElfCommand.ELF_C_READ);
	} catch (Exception e) {
	    return false;
	}

	boolean ret = elf.getEHeader().type == ElfEHeader.PHEADER_ET_EXEC;
	elf.close();
	return ret;

    }

    // @Override
    protected void validate() throws OptionException {
	// Base implementation does nothing.
    }

    public void setHeader(String string) {
	parser.setHeader(string);
    }

    public void add(Option option) {
	parser.add(option);
    }

    public void printHelp() {
	parser.printHelp();
    }
}
