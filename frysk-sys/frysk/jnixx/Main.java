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
import java.util.jar.JarFile;
import java.util.jar.JarEntry;
import java.util.Collections;
import java.util.List;
import java.util.HashSet;
import java.util.Iterator;
import java.io.IOException;

class Main {

    private static HashSet localClasses = new HashSet();

    /**
     * Does this method require native bindings?  Methods that are not
     * part of this JNI library, that are native, must be treated as
     * virtual.
     */
    static boolean treatAsNative(Method method) {
	// FIXME: Should be filtering based on something smarter than
	// this.
	if (!localClasses.contains(method.getDeclaringClass()))
	    return false;
	return Modifier.isNative(method.getModifiers());
    }

    /**
     * Is the member visible to this generated JNI code.  Private
     * methods and Fields outside of the package of interest are not
     * visible.
     */
    static boolean treatAsInvisible(Member member) {
	// Local or defining classea are always visible.
	if (localClasses.contains(member.getDeclaringClass()))
	    return false;
	return Modifier.isPrivate(member.getModifiers());
    }

    private static void printHxxFile(Printer p, String headerFile,
				     Class[] classes) {
	p.println("#include \"frysk/jnixx/jnixx.hxx\"");
	p.println();
	p.println("namespace jnixx {");
	p.println("  extern JavaVM* vm;");
	p.println("}");
	p.println();
	p.println("\f");
	System.err.println("Generating namespaces");
	new PrintNamespaces(p).walk(classes);
	System.err.println("Generating declarations");
	new PrintDeclarations(p).walk(classes);
	p.println();
	p.println("\f");
	System.err.println("Generating definitions");
	new PrintHxxDefinitions(p).walk(classes);
    }

    private static void printCxxFile(Printer p, String headerFile,
				     Class[] classes) {
	p.print("#include \"");
	p.print(headerFile);
	p.println("\"");
	p.println();
	p.println("JavaVM* ::jnixx::vm;");
	p.println();
	p.println("JNIEXPORT jint");
	p.println("JNI_OnLoad(JavaVM* javaVM, void* reserved)");
	while (p.dent(0, "{", "}")) {
	    p.println("fprintf(stderr, \"vm loaded\\n\");");
	    p.println("::jnixx::vm = javaVM;");
	    p.println("return JNI_VERSION_1_2;");
	}
	p.println();
	p.println("\f");
	System.err.println("Generating definitions");
	new PrintCxxDefinitions(p).walk(classes);
    }

    public static void main(String[] args)
	throws ClassNotFoundException, IOException
    {
	if (args.length != 3) {
	    throw new RuntimeException("Usage: jnixx cxx|hxx <header-filename> <jar-file>");
	}

	boolean generateHeader = args[0].equals("hxx");
	String headerFile = args[1];
	String jarFile = args[2];

	System.err.println("Reading " + jarFile);
	List entries = Collections.list(new JarFile(jarFile).entries());
	for (Iterator i = entries.iterator(); i.hasNext(); ) {
	    JarEntry entry = (JarEntry) i.next();
	    String name = entry.getName();
	    if (!name.endsWith(".class"))
		continue;
	    String className = name
		.replaceAll(".class$", "")
		.replaceAll("/", ".");
	    Class klass = Class.forName(className, false,
					Main.class.getClassLoader());
	    localClasses.add(klass);
	}

	Class[] classes = new Class[localClasses.size()];
	localClasses.toArray(classes);

	Printer p = new Printer(new PrintWriter(System.out));
	if (generateHeader)
	    printHxxFile(p, headerFile, classes);
	else
	    printCxxFile(p, headerFile, classes);
	p.flush();
    }
}
