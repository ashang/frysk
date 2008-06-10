// This file is part of the program FRYSK.
//
// Copyright 2008, Red Hat Inc.
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

import frysk.stack.StackFactory;
import frysk.debuginfo.DebugInfoStackFactory;
import java.io.PrintWriter;
import frysk.proc.Task;
import java.util.StringTokenizer;
import gnu.classpath.tools.getopt.Option;
import gnu.classpath.tools.getopt.OptionException;
import gnu.classpath.tools.getopt.OptionGroup;
import frysk.debuginfo.PrintDebugInfoStackOptions;

/**
 * Framework for printing stack backtraces; both providing a standard
 * set of options, and a standard print behavior.
 */
public class StackPrintUtil {
    /**
     * Create, in a separate group, the standard set of stack-print
     * options provided by utilities.
     */
    public static OptionGroup options(final PrintDebugInfoStackOptions options) {
	// Set the default, which matches the documentation; and is
	// consistent across all utilities.
	options.setAbi();
	OptionGroup group = new OptionGroup("Stack print options");
	group.add(new Option("number-of-frames",
			     ("number of frames to print.  Specify '0' or"
			      + "'all' to list all frames."),
			     "NUMBER") {
		public void parsed(String arg) throws OptionException {
		    if(arg.equals("all")){
			options.setNumberOfFrames(0);
		    } else {
			options.setNumberOfFrames(Integer.parseInt(arg));
		    }
		}
	    });
	group.add(new Option("lite",
			     "produce a low-cost or quick back-trace\n"
			     + "only ELF symbols and ABI frames are "
			     + "displayed") {
		public void parsed(String argument) throws OptionException {
		    options.setLite();
		}
	    });
	group.add(new Option("rich",
			     "produce a rich or detailed back-trace\n"
			     + "in-line frames and parameters are displayed") {
		public void parsed(String argument) throws OptionException {
		    options.setRich();
		}
	    });
	group.add(new Option
		  ("print", "select the back-trace information to display\n"
		   + "OPTION is:\n"
		   + "debug-names: print debug-info names (e.g., DWARF)\n"
		   + "full-path: include the full (untruncated) path to files\n"
		   + "inline: include inlined frames\n"
		   + "locals: include each function's local variables\n"
		   //+ "params: include function parameters\n"
		   + "values: include values of parameters and variables\n"
		   + "OPTIONs can be negated by prefixing a '-'",
		   "OPTION,...") {
		public void parsed(String arg) throws OptionException {
		    StringTokenizer st = new StringTokenizer(arg, ",");
		    while (st.hasMoreTokens()) {
			String name = st.nextToken();
			boolean val;
			if (name.startsWith("-")) {
			    val = false;
			    name = name.substring(1);
			} else {
			    val = true;
			}
			if (name.equals("debug-names")) {
			    options.setPrintDebugNames(val);
			} else if (name.equals("full-path")) {
			    options.setPrintFullPaths(val);
			} else if (name.equals("inline")) {
			    options.setPrintInlineFunctions(val);
			} else if (name.equals("locals")) {
			    options.setPrintLocals(val);
			} else if (name.equals("params")) {
			    options.setPrintParameters(val);
			} else if (name.equals("values")) {
			    options.setPrintValues(val);
			} else {
			    throw new OptionException
				("unknown -print OPTION: " + name);
			}
		    }
		}
	    });
	return group;
    }

    /**
     * Given a task, a writer, and the selected stack-print-options,
     * produce a stack back-trace.
     */
    public static void print(Task task, PrintDebugInfoStackOptions options,
			     PrintWriter printWriter) {
          if (options.abiOnly()) {
              StackFactory.printTaskStackTrace(printWriter, task, options);
          } else if (options.printInlineFunctions()) {
	      DebugInfoStackFactory.printVirtualTaskStackTrace(printWriter,
							       task,
							       options);
	  } else {
	      DebugInfoStackFactory.printTaskStackTrace(printWriter, task,
							options);
          }
    }
}
