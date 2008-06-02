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

package frysk.proc;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Iterator;
import java.util.Set;

/**
 * The environment vector.
 */
public class Environ {
    private final HashMap environ;

    /**
     * Create a new empty environment.
     */
    public Environ() {
	environ = new HashMap();   
    }
    /**
     * Create a new environment populated by the existing environ.
     */
    public Environ(String[] environ) {
	this();
	put(environ);
    }

    /**
     * Return the environ as a string array.
     */
    public String[] toStringArray() {
        Set entries = environ.entrySet();
	String[] env = new String[environ.size()];
	int j = 0;
	for (Iterator i = entries.iterator(); i.hasNext(); ) {
	    Entry e = (Entry)i.next();
            String name = (String)e.getKey();
	    String value = (String)e.getValue();
	    env[j++] = name + "=" + value;
	}
	return env;
    }

    /**
     * Get an environment variable.
     * @param name is the environment variable name.
     * @return the value of the variable.
     */
    public String get(String name) {
	return (String)environ.get(name);
    }
    
    /**
     * Put the variable into the environ set with the provided value.
     * @param name is the environment variable name.
     * @param value is the environment variable value.
     */
    public void put(String name, String value) {
	environ.put(name, value);
    }

    /**
     * Decode then add an environment variable.
     * @param name is the variable=value pair.
     */
    public void put(String name) {
	String[] member = name.split("=");
	if (member.length == 2) {
	    environ.put(member[0], member[1]);
	} else {
	    environ.put(member[0], "");
	}
    }

    /**
     * Put all elements of the the ENVIRON array into the ENVIRON set.
     */
    public void put(String[] environ) {
	for (int i = 0; i < environ.length; i++) {
	    put(environ[i]);
	}
    }

    /**
     * Delete the entry.
     */
    public void remove(String name) {
	environ.remove(name);
    }
}
