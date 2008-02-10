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

import java.util.List;

/**
 * Create the specified logger.
 */
public final class LogFactory {

    /**
     * The root Node; also serves as a single global lock.
     */
    static final Node root = new Node();

    /**
     * Find the Node corresponding to PATH.
     */
    public static Node get(String path) {
	return get(root, path);
    }
    public static Log fine(String klass) {
	return get(klass).get(Level.FINE);
    }
    public static Log finest(String klass) {
	return get(klass).get(Level.FINEST);
    }
    static Node get(Node node, String path) {
	synchronized (root) {
	    String[] names = path.split("\\.", -1);
	    for (int i = 0; i < names.length; i++) {
		String name = names[i];
		if (name.length() > 0)
		    node = node.get(name);
	    }
	    return node;
	}
    }

    /**
     * Find the node corresponding to KLASS.
     */
    public static Node get(Class klass) {
	return get(root, klass);
    }
    public static Log fine(Class klass) {
	return get(klass).get(Level.FINE);
    }
    public static Log finest(Class klass) {
	return get(klass).get(Level.FINEST);
    }
    static Node get(Node root, Class klass) {
	return get(root, klass.getName()).setClass(root, klass);
    }

    /**
     * Return completions.
     */
    public static int complete(String incomplete, List candidates) {
	return complete(root, incomplete, candidates);
    }
    static int complete(Node node, String incomplete, List candidates) {
	synchronized (root) {
	    String[] names = incomplete.split("\\.", -1);
	    System.out.println(java.util.Arrays.asList(names));
	    System.out.println(names.length);
	    for (int i = 0; i < names.length - 1; i++) {
		if (names[i].length() > 0)
		    node = node.get(names[i]);
	    }
	    String name;
	    if (names.length > 0)
		name = names[names.length - 1];
	    else
		name = "";
	    int pos = node.complete(name, candidates);
	    if (pos < 0)
		return -1;
	    else
		return incomplete.lastIndexOf(name) + pos;
	}
    }
}
