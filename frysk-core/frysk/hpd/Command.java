// This file is part of the program FRYSK.
//
// Copyright 2007, 2008, Red Hat Inc.
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

package frysk.hpd;

import java.util.List;
import java.io.PrintWriter;

/**
 * A handler class for the CLI that supplies its own help messages.
 */

public abstract class Command {
    private final String description;
    private final String syntax;
    private final String full;
  
    Command (String description, String syntax, String full) {
	this.description = description;
	this.syntax = syntax;
	this.full = full;
    }
  
    abstract void interpret(CLI cli, Input cmd);

    /**
     * Fill CANDIDATES with the possible completion strings and return
     * the start position of those strings.  E.g., given buffer=foo
     * and completion={foobar}, 0 would be returned to indicate where
     * "foobar" can be inserted.  Rreturn -1 when completion isn't
     * supported.
     */
    abstract int complete(CLI cli, Input buffer, int cursor, List candidates);

    /**
     * Print a full "help" message (syntax and then details) for this
     * command.
     */
    void help(CLI cli, Input buffer) {
	PrintWriter out = cli.getWordWrapWriter();
	out.print("Usage: ");
	out.print(syntax);
	out.println();
	out.print(full);
	out.println();
    }

    /**
     * Return a brief (one line) description of the command.
     */
    String description() {
	return description;
    }
}
