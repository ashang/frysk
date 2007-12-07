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
import java.util.TreeMap;
import java.util.Iterator;
import java.util.List;

/**
 * Generate log information when enabled.
 */
public final class Log {

    private int level;
    private final String path;
    private final String name;
    private Log(String path, String name, Log parent) {
	this.path = path;
	this.level = parent.level;
	this.name = name;
    }

    /**
     * Create a root logger; package private so that test code can
     * create their own root logger.
     */
    Log() {
	this.level = 0;
	this.path = "";
	this.name = "";
    }

    public String toString() {
	return ("{" + super.toString()
		+ ",path=" + path
		+ ",level=" + level
		+ "}");
    }

    /**
     * Return the "basename" of the logger.
     */
    public final String name() {
	return name;
    }
    /**
     * Return the full path of the logger.
     */
    public final String path() {
	return path;
    }

    private final TreeMap children = new TreeMap();
    /**
     * Set this logger's logging level.
     */
    public synchronized final Log set(Level level) {
	this.level = level.intValue();
	for (Iterator i = children.values().iterator(); i.hasNext(); ) {
	    Log child = (Log)i.next();
	    child.set(level);
	}
	return this;
    }
    /**
     * Return this loggers current logging level.
     */
    public final Level level() {
	return Level.valueOf(level);
    }

    /**
     * POS starts at -1, then points at "." or the end of the name.
     * Package private so it can be called from test code.
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
	    child = new Log(path.substring(0, dot), name, this);
	    children.put(name, child);
	}
	return child.get(path, dot);
    }

    private static final Log root = new Log();
    /**
     * Find the logger by the name KLASS.
     */
    public static Log get(String klass) {
	return root.get(klass, -1);
    }
    /**
     * Find the logger by with KLASS's name.
     */
    public static Log get(Class klass) {
	return root.get(klass.getName(), -1);
    }

    /**
     * Complete the logger.  On entry, POS is either -1 or the
     * location of the last DOT indicating further name completion is
     * needed; or the length indicating that either a "." or " "
     * completion is needed.
     *
     * Returns the offset into INCOMPLETE that the completions apply
     * to, or -1.
     *
     * Package private to allow testing.
     */
    synchronized final int complete(String incomplete, int pos,
				    List candidates) {
	int dot = incomplete.indexOf('.', pos + 1);
	if (dot >= 0) {
	    // More tokens to follow; recursively resolve.
	    String name = incomplete.substring(pos + 1, dot);
	    Log child = (Log)children.get(name);
	    if (child == null)
		return -1;
	    else
		return child.complete(incomplete, dot, candidates);
	} else {
	    // Final token, scan children for all partial matches.
	    String name = incomplete.substring(pos + 1);
	    for (Iterator i = children.keySet().iterator(); i.hasNext(); ) {
		String child = (String)i.next();
		if (child.startsWith(name))
		    candidates.add(child);
	    }
	    switch (candidates.size()) {
	    case 0:
		return -1;
	    case 1:
		Log child = (Log)children.get(name);
		if (child != null) {
		    // The final NAME was an exact match for a child;
		    // and there are no other possible completions
		    // (size == 1); change the expansion to either "."
		    // (have children) or " " (childless).
		    candidates.remove(0);
		    synchronized (child) {
			if (child.children.size() > 0) {
			    candidates.add(".");
			} else {
			    candidates.add(" ");
			}
			return incomplete.length();
		    }
		} else {
		    // A single partial completion e.g., <<foo<TAB>>>
		    // -> <<foobar>>.
		    return pos + 1;
		}
	    default:
		return pos + 1;
	    }
	}
    }

    /**
     * Complete the logger path using constructed loggers.  Return the
     * offset into incomplete where the completions apply, or -1 when
     * no completions.
     */
    public static int complete(String incomplete, List candidates) {
	return root.complete(incomplete, -1, candidates);
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
