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

package frysk.rsl;

import java.util.TreeMap;
import java.util.Iterator;
import java.util.List;

/**
 * Generate log information when enabled.
 */
public final class Node {

    // The children of this node, indexed by name.
    private final TreeMap children = new TreeMap();
    // The extensions of this node, indexed by name.
    private final TreeMap extensions = new TreeMap();
    private final Log[] loggers = new Log[Level.MAX.intValue()];
    private Level level;
    private final String path; // path.to.Node
    private final String name; // Node

    private Node(String path, String name, Level level) {
	this.path = path;
	this.name = name;
	this.level = level;
    }

    public String toString() {
	return ("{" + super.toString()
		+ ",path=" + path
		+ ",level=" + level
		+ "}");
    }

    /**
     * Package private for testing.
     */
    Node() {
	this("<root>", "<root>", Level.NONE);
    }
    /**
     * The root Node; also serves as a single global lock.
     */
    static final Node root = new Node();

    /**
     * Set this Node, and all sub-notes logging level.
     */
    public final void set(Level level) {
	synchronized (root) {
	    this.level = level;
	    for (int i = 0; i < Level.MAX.intValue(); i++) {
		if (loggers[i] != null) {
		    loggers[i].set(level);
		}
	    }
	    for (Iterator i = children.values().iterator(); i.hasNext(); ) {
		Node child = (Node)i.next();
		child.set(level);
	    }
	    for (Iterator i = extensions.values().iterator(); i.hasNext(); ) {
		Node extension = (Node)i.next();
		extension.set(level);
	    }
	}
    }

    /**
     * POS starts at -1, then points at "." or the end of the name.
     */
    private Node get(String path, int pos) {
	if (pos >= path.length()) {
	    // Reached end if the string; find the logger.
	    return this;
	} else if (path.length() == 0) {
	    // Got <<a.b.c.>> or even just <<>>.
	    return this;
	} else {
	    // Split
	    int dot = path.indexOf(".", pos + 1);
	    if (dot < 0)
		dot = path.length();
	    String name = path.substring(pos + 1, dot);
	    Node child = (Node)children.get(name);
	    if (child == null) {
		child = new Node(path.substring(0, dot), name, level);
		children.put(name, child);
	    }
	    return child.get(path, dot);
	}
    }
    Node get(String path) {
	synchronized (root) {
	    return get(path, -1);
	}
    }

    Node get(Class klass) {
	synchronized (root) {
	    Node childNode = get(klass.getName());
	    Class parentClass = klass.getSuperclass();
	    if (parentClass != null) {
		Node parentNode = get(parentClass);
		// Did the parent class know about this child class?
		if (!parentNode.extensions.containsKey(childNode.path)) {
		    // It didn't need to link it in; and ...
		    parentNode.extensions.put(childNode.path, childNode);
		    // .. make certain that the child's logging level
		    // is at least equal to the parent's logging
		    // level.  XXX: This compareTo() is backwards!
		    if (childNode.level.compareTo(parentNode.level) > 0)
			childNode.set(parentNode.level);
		}
	    }
	    return childNode;
	}
    }

    /**
     * Return the requested level.
     */
    Log get(Level level) {
	synchronized (root) {
	    int l = level.intValue();
	    if (loggers[l] == null) {
		loggers[l] = new Log(path, name, level).set(this.level);
	    }
	    return loggers[l];
	}
    }

    /**
     * Complete the logger.  On entry, POS is either -1 or the
     * location of the last DOT indicating further name completion is
     * needed; or the length indicating that either a "." or " "
     * completion is needed.
     *
     * Returns the offset into INCOMPLETE that the completions apply
     * to, or -1.
     */
    private int complete(String incomplete, int pos,
			 List candidates) {
	int dot = incomplete.indexOf('.', pos + 1);
	if (dot >= 0) {
	    // More tokens to follow; recursively resolve.
	    String name = incomplete.substring(pos + 1, dot);
	    Node child = (Node)children.get(name);
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
		Node child = (Node)children.get(name);
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
     * Complete the logger.
     */
    int complete(String incomplete, List candidates) {
	synchronized(root) {
	    return complete(incomplete, -1, candidates);
	}
    }
}
