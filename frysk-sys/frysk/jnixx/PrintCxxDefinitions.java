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

import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

class PrintCxxDefinitions extends ClassWalker {

    private final Printer p;
    PrintCxxDefinitions(Printer p) {
	this.p = p;
    }

    private ClassVisitor printer = new ClassVisitor() {

	    private void printNativeMethodDefinition(Method method) {
		boolean isStatic = Modifier.isStatic(method.getModifiers());
		p.println();
		while (p.dent(0, "extern \"C\" {", "};")) {
		    p.print("JNIEXPORT ");
		    p.printJniType(method.getReturnType());
		    p.print(" JNICALL ");
		    p.printJniName(method);
		    p.print("(");
		    p.printFormalJniParameters(method, false);
		    p.println(");");
		}
		p.println();
		p.printJniType(method.getReturnType());
		p.println();
		p.printJniName(method);
		p.print("(");
		p.printFormalJniParameters(method, true);
		p.print(")");
		while (p.dent(0, "{", "};")) {
		    p.println("try {");
		    {
			p.indent();
			p.println("::jnixx::env _env = ::jnixx::env(_jni);");
			Class returnType = method.getReturnType();
			if (returnType != Void.TYPE) {
			    p.printCxxType(returnType);
			    p.print(" ret = ");
			}
			if (isStatic) {
			    p.printQualifiedCxxName(method);
			} else {
			    p.printCxxType(method.getDeclaringClass());
			    p.print("(object).");
			    p.print(method.getName());
			}
			p.print("(");
			p.printActualCxxParameters(method);
			p.println(");");
			if (returnType != Void.TYPE) {
			    p.print("return ");
			    if (returnType.isPrimitive()) {
				p.print("ret");
			    } else if (returnType == String.class) {
				p.print("(jstring) ret._object");
			    } else if (returnType.isArray()) {
				if (returnType.getComponentType().isPrimitive()) {
				    p.print("ret");
				} else {
				    p.print("(jobjectArray) ret._object");
				}
			    } else {
				p.print("ret._object");
			    }
			    p.println(";");
			}
			p.outdent();
		    }
		    p.println("} catch (jnixx::exception) {");
		    {
			p.indent();
			if (method.getReturnType() != Void.TYPE) {
			    p.println("return 0;");
			} else {
			    p.println("return;");
			}
			p.outdent();
		    }
		    p.println("}");
		}
	    }

	    public void acceptMethod(Method method) {
		if (Main.treatAsNative(method)) {
		    printNativeMethodDefinition(method);
		}
	    }
	    void acceptInterface(Class constructor) {
	    }
	    void acceptConstructor(Constructor constructor) {
	    }
	    void acceptField(Field field) {
	    }
	    void acceptComponent(Class klass) {
	    }
	    void acceptClass(Class klass) {
	    }
	};

    void acceptArray(Class klass) {
	acceptClass(klass);
    }
    void acceptPrimitive(Class klass) {
	acceptClass(klass);
    }
    void acceptClass(Class klass) {
	if (klass.isPrimitive())
	    return;
	if (klass.isArray())
	    return;
	p.println();
	p.print("jclass ");
	p.printQualifiedCxxName(klass);
	p.println("::_class;");
	printer.visit(klass);
    }
    void acceptInterface(Class klass) {
	acceptClass(klass);
    }
}

