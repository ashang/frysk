// This file is part of the program FRYSK.
//
// Copyright 2007 Red Hat Inc.
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

package frysk.sys;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

/**
 * Interface to the process environment.
 */
public class Environ
{
    private HashMap env;

    public Environ ()
    {
	env = new HashMap();   
	getEnvironment();
    }
    
    /**
     * Get an environment variable.
     * @param name is the environment variable name.
     * @return the value of the variable.
     */
    String getEnv(String name) {
	return (String)env.get(name);
    }
    
    /**
     * Set the value of an environment variable.
     * @param name is the environment variable name.
     * @param value is the environment variable value.
     */
    void setEnv(String name, String value) {
	env.put(name, value);
    }

    /**
     * Used by CNI code to add an environment variable.
     * @param name is the variable=value pair.
     */
    void addEnviron(String name) {
	if (name.length() != 0) {
	    String envMember [] = name.split("=");
	    env.put(envMember[0], envMember.length == 2 ? envMember[1] : "");
	}
    }

    /**
     * Put the environment variables in env into char **environ form.
     * @return the environ.
     */
    long putEnviron() {
        Set keys = env.keySet();
        Collection values= env.values();
        Iterator valueIter = values.iterator();
        Iterator keyIter= keys.iterator();
        int idx = 0;
        Object[] envs = new String[values.size()];

        while (keyIter.hasNext()) {
            envs[idx] = (String)keyIter.next() + "=" + valueIter.next();
            idx += 1;
        }
        return putEnvironment(envs);
    }

    native void getEnvironment();
    native long putEnvironment(Object[] envs);
}
