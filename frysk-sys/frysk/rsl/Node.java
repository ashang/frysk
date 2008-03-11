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
import java.util.LinkedList;

/**
 * Generate log information when enabled.
 */
public final class Node {

    // The children of this node, indexed by name.
    private final TreeMap children = new TreeMap();
    private final Log[] loggers = new Log[Level.MAX.intValue()];
    private final String path; // path.to.Node
    private final String name; // Node

    // The current level according to the parent child relationship.
    private Setting childSetting;

    // This node's super class if known (java.lang.Object points at
    // itself).
    private Node superNode;
    // The list of sub-classes.
    private final List extensions = new LinkedList();
    // The current class level.
    private Setting extensionSetting;

    private Node(String path, String name, Setting childSetting) {
	this.path = path;
	this.name = name;
	this.childSetting = childSetting;
	this.extensionSetting = Setting.EPOCH;
    }

    /**
     * Package private; the root node.
     */
    Node() {
	this(null, "<root>", new Setting(Level.DEFAULT));
    }

    public String toString() {
	return ("{" + super.toString()
		+ ",path=" + path
		+ ",childSetting=" + childSetting
		+ ",extensionSetting=" + extensionSetting
		+ "}");
    }

    /**
     * Set this Node, all child nodes, and extensions, to level.
     */
    public Node set(Level level) {
	synchronized (LogFactory.root) {
	    Setting newSetting = new Setting(level);
	    setChildren(newSetting);
	    setExtensions(newSetting);
	    setLoggers(level);
	}
	return this;
    }
    private void setLoggers(Level level) {
	for (int i = 0; i < Level.MAX.intValue(); i++) {
	    if (loggers[i] != null) {
		loggers[i].set(level);
	    }
	}
    }
    private void setChildren(Setting setting) {
	childSetting = setting;
	for (Iterator i = children.values().iterator(); i.hasNext(); ) {
	    Node child = (Node)i.next();
	    child.setChildren(setting);
	    child.setLoggers(setting.level());
	}
    }
    private void setExtensions(Setting setting) {
	extensionSetting = setting;
	for (Iterator i = extensions.iterator(); i.hasNext(); ) {
	    Node extension = (Node)i.next();
	    extension.setExtensions(setting);
	    extension.setLoggers(setting.level());
	}
    }

    /**
     * Find the node NAME that is a child of this node; if the node is
     * missing, create it.  Caller must synchronize.
     */
    Node get(String name) {
	Node child = (Node) children.get(name);
	if (child == null) {
	    if (path == null) {
		child = new Node(name, name, childSetting);
	    } else {
		child = new Node(path + "." + name, name, childSetting);
	    }
	    children.put(name, child);
	}
	return child;
    }

    /**
     * Wire this node's super-class links up using KLASS; also update
     * the node's level settings.
     */
    Node setClass(Node root, Class klass) {
	// If this node doesn't know it's super-class find and
	// fill it in.  At the same time, if the super's
	// level-settings prove to be newer, adjust the levels
	// accordingly.
	if (superNode == null) {
	    Class superKlass = klass.getSuperclass();
	    if (superKlass == null) {
		// found java.lang.Object; link it to itself.
		superNode = this;
	    } else {
		// Find the super and link this to it.
		superNode = LogFactory.get(root, superKlass.getName())
		    .setClass(root, superKlass);
		superNode.extensions.add(this);
		// If the just discovered super's extension setting is
		// newer, copy it down.
		if (superNode.extensionSetting.isNewer(extensionSetting))
		    extensionSetting = superNode.extensionSetting;
		// If the newly discovered extension setting is
		// newer than the child setting; update the
		// loggers.
		if (extensionSetting.isNewer(childSetting))
		    setLoggers(extensionSetting.level());
	    }
	}
	return this;
    }

    /**
     * Return the requested level.
     */
    public Log get(Level level) {
	synchronized (LogFactory.root) {
	    int l = level.intValue();
	    if (loggers[l] == null) {
		// Determine the level.
		Level curr = childSetting.level(extensionSetting);
		loggers[l] = new Log(path, name, level).set(curr);
	    }
	    return loggers[l];
	}
    }

    /**
     * Complete the NAME using the children of this node.  Returns the
     * offset within NAME at which completions start or -1.
     */
    int complete(String name, List candidates) {
	for (Iterator i = children.keySet().iterator(); i.hasNext(); ) {
	    String child = (String)i.next();
	    if (child.startsWith(name))
		candidates.add(child);
	}
	switch (candidates.size()) {
	case 0:
	    return -1;
	case 1:
	    if (candidates.get(candidates.size() - 1).equals(name)) {
		// The NAME was an exact match for a child; and there
		// are no other possible completions; change the
		// expansion to either "."  (have children) or " "
		// (childless).
		Node child = (Node)children.get(name);
		candidates.remove(candidates.size() - 1);
		if (child.children.size() > 0) {
		    candidates.add(".");
		} else {
		    candidates.add(" ");
		}
		return name.length();
	    } else {
		// A single partial completion e.g., <<foo<TAB>>> ->
		// <<foobar>>.  The next completion request will fill in the
		// "." or <space>.
		return 0;
	    }
	default:
	    return 0;
	}
    }
}
