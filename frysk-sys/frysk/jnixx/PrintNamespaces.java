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

package frysk.jnixx;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.lang.reflect.Constructor;

class PrintNamespaces implements ClassWalker {

    private final Printer p;
    PrintNamespaces(Printer p) {
	this.p = p;
    }

    /**
     * Print the namespace spec for the klass.
     */
    private void printCxxNamespace(Class klass) {
	while (klass.isArray()) {
	    klass = klass.getComponentType();
	}
	if (klass.isPrimitive())
	    return;
	if (printedNamespaces.contains(klass))
	    return;
	p.println();
	String[] names = klass.getName().split("\\.");
	for (int i = 0; i < names.length - 1; i++) {
	    p.print("namespace ");
	    p.print(names[i]);
	    p.println("{");
	    p.indent();
	}
	p.print("struct ");
	p.print(names[names.length - 1]);
	p.print(";");
	p.println();
	for (int i = names.length - 2; i >= 0; i--) {
	    p.outdent();
	    p.println("}");
	}
	printedNamespaces.add(klass);
    }
    private HashSet printedNamespaces = new HashSet();

    /**
     * Iterate over the klasses printing any referenced name spaces.
     */
    private void printCxxNamespaces(Class[] klasses) {
	for (int i = 0; i < klasses.length; i++) {
	    // Should this recurse?
	    printCxxNamespace(klasses[i]);
	}
    }

    public boolean acceptClass(Class klass) {
	printCxxNamespace(klass);
	return true;
    }

    public void acceptConstructor(Constructor constructor) {
	printCxxNamespaces(constructor.getParameterTypes());
    }

    public void acceptField(Field field) {
	printCxxNamespace(field.getType());
    }

    public void acceptMethod(Method method) {
	printCxxNamespace(method.getReturnType());
	printCxxNamespaces(method.getParameterTypes());
    }
}
