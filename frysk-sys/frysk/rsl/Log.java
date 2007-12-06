// This file is part of the program FRYSK.
// 
// Copyright 2007, Red Hat Inc.
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

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Generate log information when enabled.
 */
public final class Log {

    private int level;
    private final String path;
    private Log(String path, Log parent) {
	this.path = path;
	this.level = parent.level;
    }
    /**
     * Create a root logger; package private so that test code can
     * create their own root logger.
     */
    Log() {
	this.level = 0;
	this.path = "";
    }

    public String toString() {
	return ("{" + super.toString()
		+ ",path=" + path
		+ ",level=" + level
		+ "}");
    }

    private final HashMap children = new HashMap();
    public synchronized final Log set(Level level) {
	this.level = level.intValue();
	for (Iterator i = children.values().iterator(); i.hasNext(); ) {
	    Log child = (Log)i.next();
	    child.set(level);
	}
	return this;
    }
    public Level level() {
	return Level.valueOf(level);
    }

    /**
     * POS starts at -1, then points at "." or the end of the name.
     */
    synchronized final Log get(String path, int pos) {
	if (pos >= path.length())
	    // Reached end if the string.
	    return this;
	// Split
	int dot = path.indexOf(".", pos + 1);
	if (dot < 0)
	    dot = path.length();
	String name = path.substring(pos + 1, dot);
	Log child = (Log)children.get(name);
	if (child == null) {
	    child = new Log(path.substring(0, dot), this);
	    children.put(name, child);
	}
	return child.get(path, dot);
    }

    private static final Log root = new Log();
    public static Log get(String klass) {
	return root.get(klass, -1);
    }
    public static Log get(Class klass) {
	return root.get(klass.getName(), -1);
    }

    // Static?
    private static PrintStream out = System.out;
    static void set(PrintStream out) {
	Log.out = out;
    }

    private void prefix() {
	out.print(path);
	out.print(": ");
    }

    private void prefix(Object o) {
	out.print(path);
	out.print("[");
	out.print(o.toString());
	out.print("]: ");
    }

    private void postfix() {
	out.println();
    }

    // Add at will and on demand.
    private void log(String s1) {
	prefix();
	out.print(s1);
	postfix();
    }
    public final void fine(String s1) {
	log(s1);
    }
    public final void finest(String s1) {
	log(s1);
    }

    // Add at will and on demand.
    private void log(Object self, String s1) {
	prefix(self);
	out.print(s1);
	postfix();
    }
    public final void fine(Object self, String s1) {
	if (level < Level.FINE_)
	    return;
	log(self, s1);
    }
    public final void finest(Object self, String s1) {
	if (level < Level.FINEST_)
	    return;
	log(self, s1);
    }
}
