// This file is part of the program FRYSK.
//
// Copyright 2005, 2007 Red Hat Inc.
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

package frysk.rsl;

import gnu.classpath.tools.getopt.Option;
import gnu.classpath.tools.getopt.OptionException;
import java.io.File;
import java.io.PrintStream;
import java.io.FileOutputStream;

public class LogOption extends Option {

    public LogOption(String name) {
	super(name,
	      ("Set the logger LOG to level LEVEL.\n"
	       + "The LEVEL can be [ NONE | FINE | FINEST ].\n"
	       + "Example -" + name + " frysk=FINE"),
	      "<LOG=LEVEL,...>");
    }
    public void parsed (String arg0) throws OptionException {
	level(arg0);
    }
    /**
     * Parse ARG0 setting log levels.
     */
    public static void level(String arg0) throws OptionException {
	level(Tree.root, arg0);
    }
    /**
     * Parse ARG0 setting log levels.
     */
    static void level(Tree root, String arg0) throws OptionException {
	String[] logs = arg0.split(",");
	for (int i = 0; i < logs.length; i++) {
	    String[] logLevel = logs[i].split("=");
	    Tree logger;
	    Level level;
	    switch (logLevel.length) {
	    case 1:
		// LEVEL
		logger = root.get("");
		level = Level.valueOf(logLevel[0]);
		break;
	    case 2:
		// LOGGER=LEVEL
		logger = root.get(logLevel[0]);
		level = Level.valueOf(logLevel[1]);
		break;
	    default:
		throw new OptionException("Could not parse: " + logs[i]);
	    }
	    if (logger == null)
		throw new OptionException("Couldn't find logger for: "
					  + logs[i]);
	    if (level == null)
		throw new OptionException("Invalid log level for: "
					  + logs[i]);
	    logger.set(level);
	}
    }

    /**
     * Parse ARG0 setting log levels, and switching to the specified
     * log file.
     */
    public static void file(File file) {
	// try creating the directory for the log file
	File dir = file.getParentFile();
	if (!dir.exists() && !dir.mkdirs()) {
	    System.out.println("logger warning: can not create log directory: "
			       + dir);
	    return;
	}
	try {
	    Log.set(new PrintStream(new FileOutputStream(file)));
	} catch (java.io.FileNotFoundException e) {
	    System.out.println("logger warning: can not create log file: "
			       + file);
	    return;
	}
    }
}
