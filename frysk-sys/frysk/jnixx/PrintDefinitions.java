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
	p.println("static jclass _Class;");
	p.println();
	p.println("jclass");
	p.printQualifiedCxxName(klass);
	p.println("::Class(jnixx::env& env) {");
	p.println("  if (_Class == NULL) {");
	p.println("    _Class = env.findClass(\""
		+ klass.getName().replace("\\.", "/") + "\");");
	p.println("  }");
	p.println("  return _Class;");
	p.println("}");
	return true;
    }

    public void acceptConstructor(Constructor constructor) {
	p.println();
	p.printCxxType(constructor.getDeclaringClass());
	p.println();
	p.printQualifiedCxxName(constructor);
	p.print("(");
	p.printFormalCxxParameters(constructor, true);
	p.println(") {");
	p.println("  static jmethodID id;");
	p.println("  if (id == NULL)");
	p.print("    id = env.getMethodID(Class(env), \"<init>\", \"(");
	p.printJniSignature(constructor.getParameterTypes());
	p.println(")V\");");
	p.print("  ");
	p.printCxxType(constructor.getDeclaringClass());
	p.print(" object = (");
	p.printCxxType(constructor.getDeclaringClass());
	p.print(") env.newObject(");
	p.printActualJniParameters(constructor);
	p.println(");");
	p.println("  if (object == NULL)");
	p.println("    throw jnixx::exception();");
	p.println("  return object;");
	p.println("}");
    }

    private void printCxxFieldAccessorDefinition(Field field, boolean get) {
	boolean isStatic = Modifier.isStatic(field.getModifiers());
	p.println();
	if (get) {
	    p.printCxxType(field.getType());
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
	if (!isStatic) {
	    p.print(", ");
	    p.printCxxType(field.getDeclaringClass());
	    p.print(" object");
	}
	if (!get) {
	    p.print(", ");
	    p.printCxxType(field.getType());
	    p.print(" value");
	}
	p.println(") {");
	p.println("  if (" + name + "ID == NULL) {");
	p.print("    " + name + "ID = env.get");
	if (isStatic) {
	    p.print("Static");
	}
	p.print("FieldID(Class(env), \"");
	p.print(name);
	p.print("\", \"");
	p.printJniSignature(field.getType());
	p.print("\"");
	p.println(");");
	p.println("  }");
	if (get) {
	    p.print("  return");
	    if (!field.getType().isPrimitive()) {
		p.print(" (");
		p.printCxxType(field.getType());
		p.print(")");
	    }
	    p.print(" env.get");
	} else {
	    p.print("  env.set");
	}
	if (isStatic) {
	    p.print("Static");
	}
	p.printJniReturnTypeName(field.getType());
	p.print("Field(");
	if (isStatic) {
	    p.print("_Class");
	} else {
	    p.print("object");
	}
	p.print(", " + name + "ID");
	if (!get) {
	    p.print(",");
	    if (!field.getType().isPrimitive()) {
		p.print(" (jobject)");
	    }
	    p.print(" value");
	}
	p.println(");");
	p.println("}");
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
	p.println(") {");
	p.println();
	p.println("  static jmethodID id;");
	p.println("  if (id == NULL)");
	p.print("    id = env.get");
	if (isStatic) {
	    p.print("Static");
	}
	p.print("MethodID(Class(env), \"");
	p.print(method.getName());
	p.print("\", \"");
	p.printJniSignature(method);
	p.println("\");");
	p.print("  ");
	if (returnType != Void.TYPE) {
	    p.printCxxType(returnType);
	    p.print(" ret = ");
	    if (!returnType.isPrimitive()) {
		p.print("(");
		p.printCxxType(returnType);
		p.print(") ");
	    }
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
	    p.println("  return ret;");
	}
	p.println("}");
    }

    private void printNativeMethodDefinition(Method method) {
	p.println();
	p.println("extern \"C\" {");
	p.print("  JNIEXPORT ");
	p.printJniType(method.getReturnType());
	p.print(" JNICALL ");
	p.printJniName(method);
	p.print("(");
	p.printFormalJniParameters(method, false);
	p.println(");");
	p.println("}");
	p.println();
	p.printJniType(method.getReturnType());
	p.println();
	p.printJniName(method);
	p.print("(");
	p.printFormalJniParameters(method, true);
	p.println(") {");
	p.println("  try {");
	p.println("    jnixx::env jnixxEnv = jnixx::env(jniEnv);");
	p.print("    ");
	if (method.getReturnType() != Void.TYPE) {
	    p.print("return ");
	}
	p.printQualifiedCxxName(method);
	p.print("(");
	p.printActualCxxParameters(method);
	p.println(");");
	p.println("  } catch (jnixx::exception) {");
	if (method.getReturnType() != Void.TYPE) {
	    p.println("    return 0;");
	}
	p.println("  }");
	p.println("}");
    }

    public void acceptMethod(Method method) {
	if (Modifier.isNative(method.getModifiers())) {
	    printNativeMethodDefinition(method);
	} else {
	    printCxxMethodDefinition(method);
	}
    }
}
