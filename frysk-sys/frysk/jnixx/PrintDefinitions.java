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

class PrintDefinitions implements ClassWalker {

    private final Printer p;
    PrintDefinitions(Printer p) {
	this.p = p;
    }

    public boolean acceptClass(Class klass) {
	// The class, via reflection.
	p.println();
	p.println("static jclass _class;");
	p.println();
	p.println("jclass");
	p.printQualifiedCxxName(klass);
	p.print("::Class(jnixx::env& env)");
	while (p.dent(0, "{", "}")) {
	    p.print("if (_class == NULL)");
	    while (p.dent(1, "{", "}")) {
		p.println("_class = env.findClass(\""
			  + klass.getName().replace("\\.", "/") + "\");");
	    }
	    p.println("return _class;");
	}
	return true;
    }

    public void acceptConstructor(Constructor constructor) {
	p.println();
	p.printCxxType(constructor.getDeclaringClass());
	p.println();
	p.printQualifiedCxxName(constructor);
	p.print("(");
	p.printFormalCxxParameters(constructor, true);
	p.print(")");
	while (p.dent(0, "{", "}")) {
	    p.println("static jmethodID id;");
	    while (p.dent(1, "if (id == NULL) {", "}")) {
		p.print("id = env.getMethodID(Class(env), \"<init>\", \"(");
		p.printJniSignature(constructor.getParameterTypes());
		p.println(")V\");");
	    }
	    p.print("jobject object = env.newObject(");
	    p.printActualJniParameters(constructor);
	    p.println(");");
	    while (p.dent(1, "if (object == NULL) {", "}")) {
		p.println("throw jnixx::exception();");
	    }
	    p.print("return ");
	    p.printCxxType(constructor.getDeclaringClass());
	    p.println("(object);");
	}
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
	p.print("(jnixx::env& env");
	if (!get) {
	    p.print(", ");
	    p.printCxxType(type);
	    p.print(" p0");
	}
	p.print(")");
	while (p.dent(0, "{", "}")) {
	    while (p.dent(1, "if (" + name + "ID == NULL) {", "}")) {
		p.print(name + "ID = env.get");
		if (isStatic) {
		    p.print("Static");
		}
		p.print("FieldID(Class(env), \"");
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
		p.print(" env.get");
	    } else {
		p.print("env.set");
	    }
	    if (isStatic) {
		p.print("Static");
	    }
	    p.printJniReturnTypeName(type);
	    p.print("Field(");
	    if (get) {
		p.printActualJniParameters(isStatic, name + "ID",
					   new Class[0]);
	    } else {
		p.printActualJniParameters(isStatic, name + "ID",
					   new Class[] { type });
	    }
	    p.println(");");
	    if (get) {
		p.print("return ");
		if (type.isPrimitive()) {
		    p.print("ret");
		} else if (type.getName().startsWith("java.")) {
		    p.print("ret");
		} else {
		    p.printCxxType(type);
		    p.print("(ret)");
		}
		p.println(";");
	    }
	}
    }

    public void acceptField(Field field) {
	p.println();
	p.print("static jfieldID ");
	p.print(field.getName());
	p.println("ID;");
	printCxxFieldAccessorDefinition(field, true);
	if (!Modifier.isFinal(field.getModifiers())) {
	    printCxxFieldAccessorDefinition(field, false);
	}
    }

    private void printCxxMethodDefinition(Method method) {
	boolean isStatic = Modifier.isStatic(method.getModifiers());
	Class returnType = method.getReturnType();
	p.println();
	p.printCxxType(returnType);
	p.println();
	p.printQualifiedCxxName(method.getDeclaringClass());
	p.print("::");
	p.print(method.getName());
	p.print("(");
	p.printFormalCxxParameters(method, true);
	p.print(")");
	while (p.dent(0, "{", "}")) {
	    p.println("static jmethodID id;");
	    while (p.dent(1, "if (id == NULL) {", "}")) {
		p.print("id = env.get");
		if (isStatic) {
		    p.print("Static");
		}
		p.print("MethodID(Class(env), \"");
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
	    p.print("env.call");
	    if (isStatic) {
		p.print("Static");
	    }
	    p.printJniReturnTypeName(returnType);
	    p.print("Method(");
	    p.printActualJniParameters(method);
	    p.println(");");
	    if (returnType != Void.TYPE) {
		p.print("return ");
		if (returnType.isPrimitive()) {
		    p.print("ret;");
		} else {
		    p.printCxxType(returnType);
		    p.print("(ret)");
		}
		p.println(";");
	    }
	}
    }

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
		p.println("jnixx::env env = jnixx::env(jni);");
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
	if (Modifier.isNative(method.getModifiers())) {
	    printNativeMethodDefinition(method);
	} else {
	    printCxxMethodDefinition(method);
	}
    }
}
