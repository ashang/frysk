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
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

class PrintHxxDefinitions extends ClassWalker {

    private final Printer p;
    PrintHxxDefinitions(Printer p) {
	this.p = p;
    }

    private void printCxxFieldAccessorDefinition(Field field, boolean get) {
	boolean isStatic = Modifier.isStatic(field.getModifiers());
	Class type = field.getType();
	p.println();
	if (get) {
	    p.printCxxType(type);
	    p.println();
	} else { 
	    p.println("void");
	}
	p.printQualifiedCxxName(field.getDeclaringClass());
	p.print("::");
	if (get) {
	    p.print("Get");
	} else {
	    p.print("Set");
	}
	String name = field.getName();
	p.print(Character.toUpperCase(name.charAt(0)));
	p.print(name.substring(1));
	p.print("(::jnixx::env _env");
	if (!get) {
	    p.print(", ");
	    p.printCxxType(type);
	    p.print(" p0");
	}
	p.print(")");
	while (p.dent(0, "{", "}")) {
	    p.print("if (");
	    p.printID(field);
	    p.print(" == NULL)");
	    while (p.dent(1, "{", "}")) {
		p.printID(field);
		p.print(" = _env.get");
		if (isStatic) {
		    p.print("Static");
		}
		p.print("FieldID(_class_(_env), \"");
		p.print(name);
		p.print("\", \"");
		p.printJniSignature(type);
		p.print("\"");
		p.println(");");
	    }
	    if (get) {
		p.printJniType(type);
		p.print(" ret = ");
		p.print("(");
		p.printJniType(type);
		p.print(")");
		p.print(" _env.get");
	    } else {
		p.print("_env.set");
	    }
	    if (isStatic) {
		p.print("Static");
	    }
	    p.printJniReturnTypeName(type);
	    p.print("Field(");
	    if (get) {
		p.printActualJniParameters(isStatic, field, new Class[0]);
	    } else {
		p.printActualJniParameters(isStatic, field, new Class[] { type });
	    }
	    p.println(");");
	    if (get) {
		p.printReturn(isStatic, type, "ret");
		p.println(";");
	    }
	}
    }

    private void printCxxMethodDefinition(Method method) {
	boolean isStatic = Modifier.isStatic(method.getModifiers());
	Class returnType = method.getReturnType();
	p.println();
	p.printCxxType(returnType);
	p.println();
	p.printQualifiedCxxName(method);
	p.print("(");
	p.printFormalCxxParameters(method, true);
	p.print(")");
	while (p.dent(0, "{", "}")) {
	    p.print("static jmethodID ");
	    p.printID(method);
	    p.println(";");
	    p.print("if (");
	    p.printID(method);
	    p.print(" == NULL)");
	    while (p.dent(1, "{", "}")) {
		p.printID(method);
		p.print(" = _env.get");
		if (isStatic) {
		    p.print("Static");
		}
		p.print("MethodID(_class_(_env), \"");
		p.print(method.getName());
		p.print("\", \"");
		p.printJniSignature(method);
		p.println("\");");
	    }
	    if (returnType != Void.TYPE) {
		if (returnType.isPrimitive()) {
		    p.printJniType(returnType);
		} else {
		    p.print("jobject");
		}
		p.print(" ret = ");
	    }
	    p.print("_env.call");
	    if (isStatic) {
		p.print("Static");
	    }
	    p.printJniReturnTypeName(returnType);
	    p.print("Method(");
	    p.printActualJniParameters(method);
	    p.println(");");
	    if (returnType != Void.TYPE) {
		p.printReturn(isStatic, returnType, "ret");
		p.println(";");
	    }
	}
    }

    private final ClassVisitor printer = new ClassVisitor() {

	    void acceptComponent(Class klass) {
	    }
	    void acceptInterface(Class klass) {
	    }
	    void acceptClass(Class klass) {
	    }
	    void acceptConstructor(Constructor constructor) {
		p.println();
		p.printCxxType(constructor.getDeclaringClass());
		p.println();
		p.printQualifiedCxxName(constructor);
		p.print("(");
		p.printFormalCxxParameters(constructor, true);
		p.print(")");
		while (p.dent(0, "{", "}")) {
		    p.print("static jmethodID ");
		    p.printID(constructor);
		    p.println(";");
		    p.print("if (");
		    p.printID(constructor);
		    p.print(" == NULL)");
		    while (p.dent(1, "{", "}")) {
			p.printID(constructor);
			p.print(" = _env.getMethodID(_class_(_env), \"<init>\", \"(");
			p.printJniSignature(constructor.getParameterTypes());
			p.println(")V\");");
		    }
		    p.print("jobject object = _env.newObject(");
		    p.printActualJniParameters(constructor);
		    p.println(");");
		    while (p.dent(1, "if (object == NULL) {", "}")) {
			p.println("throw ::jnixx::exception();");
		    }
		    p.printReturn(true, constructor.getDeclaringClass(),
				  "object");
		    p.println(";");
		}
	    }

	    void acceptField(Field field) {
		p.println();
		printCxxFieldAccessorDefinition(field, true);
		if (!Modifier.isFinal(field.getModifiers())) {
		    printCxxFieldAccessorDefinition(field, false);
		}
	    }

	    public void acceptMethod(Method method) {
		if (!Modifier.isNative(method.getModifiers())) {
		    printCxxMethodDefinition(method);
		}
	    }
	};

    void acceptArray(Class klass) {
    }
    void acceptPrimitive(Class klass) {
    }
    void acceptInterface(Class klass) {
	acceptClass(klass);
    }
    void acceptClass(Class klass) {
	// The class, via reflection.
	p.println();
	p.println("jclass");
	p.printQualifiedCxxName(klass);
	p.print("::_class_(::jnixx::env _env)");
	while (p.dent(0, "{", "}")) {
	    while (p.dent(1, "if (_class == NULL) {", "}")) {
		p.print("_class = _env.findClass(\"");
		p.print(klass.getName());
		p.println("\");");
	    }
	    p.println("return _class;");
	}
	printer.visit(klass);
    }
}
