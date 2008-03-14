// This file is part of the program FRYSK.
//
// Copyright 2008 Red Hat Inc.
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

package frysk.proc.dead;

import frysk.rsl.Log;

/**
 * Given a String lifted from the start of a file and an ARGV,
 * transform it into an ARGV suitable for invoking a #! interpreter.
 */
class InterpreterFactory {
    private static final Log fine = Log.fine(InterpreterFactory.class);

    /**
     * Parse FILE (taken from the head of a shell file) and args
     * transforming them into a script invocation.
     */
    static String[] parse(String file, String[] args) {
	fine.log("file", file);
	if (!file.startsWith("#!"))
	    return null;
	String[] fields = file.replaceFirst("#! *", "").split(" +");
	fine.log("fields", fields);
	String[] interpreter;
	int start;
	if (fields.length == 1) {
	    // #!interpreter
	    interpreter = new String[args.length + 1];
	    interpreter[0] = fields[0];
	    fine.log("interpreter", interpreter[0]);
	    start = 1;
	} else {
	    // #!interpreter option
	    interpreter = new String[args.length + 2];
	    interpreter[0] = fields[0];
	    interpreter[1] = fields[1];
	    fine.log("interpreter", interpreter[0], "options", interpreter[1]);
	    start = 2;
	}
	for (int i = 0; i < args.length; i++) {
	    interpreter[i + start] = args[i];
	    fine.log("interpreter", i, "is", interpreter[i]);
	}
	return interpreter;
    }
}
