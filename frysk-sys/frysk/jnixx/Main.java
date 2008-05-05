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

class Main {

    static void printHxxBody(Printer p, Class klass,
			     PrintNamespaces printNamespaces) {
	Class parent = klass.getSuperclass();
	p.println();
	WalkClass.visit(klass, printNamespaces);
	p.println();
	p.print("struct ");
	p.printQualifiedCxxName(klass);
	p.print(" : public ");
	if (parent == Object.class) {
	    p.print("jnixx::object");
	} else if (parent == null) {
	    p.print("jnixx::interface");
	} else {
	    p.printQualifiedCxxName(parent);
	}
	while(p.dent(0, "{", "};")) {
	    // Constructor.
	    p.printUnqualifiedCxxName(klass);
	    p.print("(jobject o)");
	    p.print(" : ");
	    if (parent == Object.class) {
		p.print("jnixx::object(o)");
	    } else if (parent == null) {
		p.print("jnixx::interface(o)");
	    } else {
		p.printQualifiedCxxName(parent);
		p.print("(o)");
	    }
	    p.println(" { }");
	    // Static get-class method - a class knows its own class.
	    p.println("static jclass Class(jnixx::env& env);");
	    WalkClass.visit(klass, new PrintDeclarations(p));
	    p.println();
	}
    }

    private static void printHxxFile(Printer p, Class klass) {
	String header = klass.getName().replaceAll("\\.", "_") + "_jni_hxx";
	p.println("#ifndef " + header);
	p.println("#define " + header);
	p.println();
	p.println("#include \"frysk/jnixx/jnixx.hxx\"");
	Class parent = klass.getSuperclass();
	if (parent != Object.class) {
	    p.println();
	    p.print("#include \"");
	    p.printHeaderFileName(parent);
	    p.print("\"");
	    p.println();
	}
	printHxxBody(p, klass, new PrintNamespaces(p));
	p.println();
	p.println("#endif");
    }

    private static void printCxxFile(Printer p, Class klass) {
	p.println("#include \"frysk/jnixx/jnixx.hxx\"");
	WalkClass.visit(klass, new PrintIncludes(p, new PrintNamespaces(p)));
	WalkClass.visit(klass, new PrintDefinitions(p));
    }

    public static void main(String[] args) throws ClassNotFoundException {
	if (args.length != 2) {
	    throw new RuntimeException("Usage: jnixx <class-name>");
	}

	Class klass = Class.forName(args[0], false,
				    Main.class.getClassLoader());
	Printer p = new Printer();

	if (args[1].equals("hxx"))
	    printHxxFile(p, klass);
	else
	    printCxxFile(p, klass);
    }
}
