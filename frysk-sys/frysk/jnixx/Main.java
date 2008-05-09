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

import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Member;

class Main {

    /**
     * Does this method require native bindings?  Methods that are not
     * part of this JNI library, that are native, must be treated as
     * virtual.
     */
    static boolean treatAsNative(Method method) {
	// FIXME: Should be filtering based on something smarter than
	// this.
	if (method.getDeclaringClass().getName().startsWith("java."))
	    return false;
	if (method.getDeclaringClass().getName().startsWith("gnu."))
	    return false;
	if (Modifier.isNative(method.getModifiers()))
	    return true;
	return false;
    }

    /**
     * Is the member visible to this generated JNI code.  Private
     * methods and fileds and the java package are not visible.
     */
    static boolean treatAsInvisible(Member member) {
	if (Modifier.isPrivate(member.getModifiers())) {
	    // FIXME: Should be filtering based on something smarter
	    // than is.
	    if (member.getDeclaringClass().getName().startsWith("java."))
		return true;
	    if (member.getDeclaringClass().getName().startsWith("gnu."))
		return true;
	}
	return false;
    }

    private static void printHxxFile(Printer p, Class[] classes) {
	p.println("#include \"frysk/jnixx/jnixx.hxx\"");
	new PrintNamespaces(p).walk(classes);
	new PrintDeclarations(p).walk(classes);
	new PrintHxxDefinitions(p).walk(classes);
    }

    private static void printCxxFile(Printer p, Class[] classes) {
	printHxxFile(p, classes); // #include
	p.println();
	p.println("\f");
	p.println();
	new PrintCxxDefinitions(p).walk(classes);
    }

    public static void main(String[] args) throws ClassNotFoundException {
	if (args.length < 2) {
	    throw new RuntimeException("Usage: jnixx cxx}hxx <class-name> ...");
	}

	Class[] classes = new Class[args.length - 1];
	for (int i = 0; i < classes.length; i++) {
	    classes[i] = Class.forName(args[i + 1], false,
				       Main.class.getClassLoader());
	}

	Printer p = new Printer(new PrintWriter(System.out));
	if (args[0].equals("hxx"))
	    printHxxFile(p, classes);
	else
	    printCxxFile(p, classes);
	p.flush();
    }
}
