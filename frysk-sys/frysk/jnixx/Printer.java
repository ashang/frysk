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

import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

/**
 * A class for generating indented output.
 */
class Printer {

    /**
     * Print CH.  If the start of the line, prefix with indent.
     */
    Printer print(char ch) {
	printIndentation();
	System.out.print(ch);
	return this;
    }
    /**
     * Print the OBJECT, convered to a string using toString().  If
     * the start of the line, prefix with indentation.
     */
    Printer print(Object o) {
	printIndentation();
	System.out.print(o.toString());
	return this;
    }
    /**
     * Start a new line.
     */
    Printer println() {
	System.out.println();
	indentationNeeded = true;
	return this;
    }
    /**
     * Print the object, then start a new line.
     */
    Printer println(Object o) {
	return print(o).println();
    }

    /**
     * Increase indentation by one unit.
     */
    Printer indent() {
	indentation++;
	return this;
    }
    /**
     * Decrease indentation by one unit.
     */
    Printer outdent() {
	indentation--;
	if (indentation < 0)
	    throw new RuntimeException("indentation botch");
	return this;
    }
    private void printIndentation() {
	if (indentationNeeded)
	    pad(indentation);
	indentationNeeded = false;
    }
    private int indentation = 0;
    private boolean indentationNeeded = true;

    /**
     * Pad using white space by N indentation units.
     */
    Printer pad(int n) {
	for (int i = 0 ; i < n; i++)
	    System.out.print("  ");
	return this;
    }

    /**
     * Print the class's fully qualified C++ name; that is "."
     * replaced by "::".
     */
    Printer printQualifiedCxxName(Class klass) {
	print(klass.getName().replaceAll("\\.", "::"));
	return this;
    }
    /**
     * Print the method's fully qualified C++ name; that is "."
     * replaced by "::".
     */
    Printer printQualifiedCxxName(Method method) {
	printQualifiedCxxName(method.getDeclaringClass());
	print("::");
	print(method.getName());
	return this;
    }
    /**
     * Print the constructor's fully qualified C++ name; that is "."
     * replaced by "::".
     */
    Printer printQualifiedCxxName(Constructor constructor) {
	printQualifiedCxxName(constructor.getDeclaringClass());
	print("::");
	print("New");
	return this;
    }

    /**
     * Print the name of the header file generated for the class.
     */
    Printer printHeaderFileName(Class klass) {
	print(klass.getName().replaceAll("\\.", "/"));
	print("-jni.hxx");
	return this;
    }

    /**
     * Print the modifiers associated with the declaration.
     */
    Printer printModifiers(Member member) {
	print("// ");
	print(Modifier.toString(member.getModifiers()));
	println();
	print("public: static");
	return this;
    }

    /**
     * Return the JNI signature for the klass.
     */
    private String jniSignature(Class klass) {
	StringBuffer signature = new StringBuffer();
	while (klass.isArray()) {
	    klass = klass.getComponentType();
	    signature.append("[");
	}
	if (klass == Boolean.TYPE) {
	    signature.append("Z");
	} else if (klass == Byte.TYPE) {
	    signature.append("B");
	} else if (klass == Character.TYPE) {
	    signature.append("C");
	} else if (klass == Double.TYPE) {
	    signature.append("D");
	} else if (klass == Float.TYPE) {
	    signature.append("F");
	} else if (klass == Integer.TYPE) {
	    signature.append("I");
	} else if (klass == Long.TYPE) {
	    signature.append("J");
	} else if (klass == Short.TYPE) {
	    signature.append("S");
	} else if (klass == Void.TYPE) {
	    signature.append("V");
	} else if (klass.isPrimitive()) {
	    throw new RuntimeException("unhandled primitive type: " + klass);
	} else {
	    signature.append("L");
	    signature.append(klass.getName().replaceAll("\\.", "/"));
	    signature.append(";");
	}
	return signature.toString();
    }

    /**
     * Print the JNI signature for the class.
     */
    Printer printJniSignature(Class klass) {
	print(jniSignature(klass));
	return this;
    }

    /**
     * Print the parameter list's JNI signature.
     */
    Printer printJniSignature(Class[] params) {
	for (int i = 0; i < params.length; i++) {
	    print(jniSignature(params[i]));
	}
	return this;
    }

    /**
     * Print the method's JNI signature.
     */
    Printer printJniSignature(Method method) {
	print("(");
	printJniSignature(method.getParameterTypes());
	print(")");
	print(jniSignature(method.getReturnType()));
	return this;
    }

    /**
     * Given a method, print its JNI mangled name.
     */
    Printer printJniName(Method method) {
	print("Java_");
	print(method.getDeclaringClass().getName()
	      .replaceAll("_", "_1")
	      .replaceAll("\\.", "_"));
	print("_");
	print(method.getName().replaceAll("_", "_1"));
	print("__");
	Class[] params = method.getParameterTypes();
	for (int i = 0; i < params.length; i++) {
	    print(jniSignature(params[i])
		  .replaceAll("_", "_1")
		  .replaceAll(";", "_2")
		  .replaceAll("\\[", "_3")
		  .replaceAll("/", "_"));
	}
	return this;
    }

    /**
     * Given a class describing a type (class of basic), print the JNI
     * equivalent name.
     */
    Printer printJniType(Class klass) {
	printCxxType(klass);
	return this;
    }

    /**
     * Given a class describing a type (class of basic), print the JNI
     * equivalent name.  For classes, print the XX type.
     */
    Printer printCxxType(Class klass) {
	if (klass.isPrimitive()) {
	    if (klass == Void.TYPE) {
		print("void");
	    } else {
		print("j");
		print(klass.getName());
	    }
	} else if (klass.isArray()) {
	    Class component = klass.getComponentType();
	    if (component.isPrimitive()) {
		printCxxType(component);
		print("Array");
	    } else if (component == String.class) {
		print("jstringArray");
	    } else {
		print("jobjectArray");
	    }
	} else if (klass == String.class) {
	    print("jstring");
	} else if (klass == Object.class) {
	    print("jobject");
	} else if (klass == Class.class) {
	    print("jclass");
	} else {
	    printQualifiedCxxName(klass);
	    print("*");
	}
	return this;
    }

    /**
     * Given an array of types, print them as a list (presumably this
     * is a list of parameters).
     */
    private Printer printFormalCxxParameters(Class klass, Class[] params,
					     boolean isStatic,
					     boolean printArgs) {
	print("jnixx::env&");
	if (printArgs)
	    print(" env");
	if (!isStatic) {
	    print(", ");
	    printJniType(klass);
	    if (printArgs)
		print(" object");
	}
	for (int i = 0; i < params.length; i++) {
	    print(", ");
	    printCxxType(params[i]);
	    if (printArgs)
		print(" p" + i);
	}
	return this;
    }
    /**
     * Print the METHOD's formal parameters, possibly including
     * parameter names.
     */
    Printer printFormalCxxParameters(Method f, boolean printArgs) {
	return printFormalCxxParameters(f.getDeclaringClass(),
					f.getParameterTypes(),
					Modifier.isStatic(f.getModifiers()),
					printArgs);
    }
    /**
     * Print the CONSTRUCTOR's formal parameters, possibly including
     * parameter names.
     */
    Printer printFormalCxxParameters(Constructor f, boolean printArgs) {
	return printFormalCxxParameters(f.getDeclaringClass(),
					f.getParameterTypes(),
					true/*isStatic*/, printArgs);
    }

    /**
     * Print the list actual parameters for FUNC.
     */
    private Printer printActualCxxParameters(Member func,
					     Class[] params) {
	print("jnixxEnv");
	if (!Modifier.isStatic(func.getModifiers())) {
	    print(", object");
	}
	for (int i = 0; i < params.length; i++) {
	    print(", p" + i);
	}
	return this;
    }
    /**
     * Print the METHOD's list of actual parameters.
     */
    Printer printActualCxxParameters(Method f) {
	return printActualCxxParameters(f, f.getParameterTypes());
    }
    /**
     * Print the CONSTRUCOR's list of actual parameters.
     */
    Printer printActualCxxParameters(Constructor f) {
	return printActualCxxParameters(f, f.getParameterTypes());
    }

    /**
     * Print the JNI formal parameters for method; possibly including
     * parameter names.
     */
    Printer printFormalJniParameters(Method method, boolean printArgs) {
	print("JNIEnv*");
	if (printArgs)
	    print(" jniEnv");
	if (Modifier.isStatic(method.getModifiers())) {
	    print(", jclass");
	    if (printArgs)
		print(" klass");
	} else {
	    print(", ");
	    printJniType(method.getDeclaringClass());
	    if (printArgs)
		print(" object");
	}
	Class[] params = method.getParameterTypes();
	for (int i = 0; i < params.length; i++) {
	    print(", ");
	    printCxxType(params[i]);
	    if (printArgs)
		print(" p" + i);
	}
	return this;
    }

    /**
     * Print the actual JNI parameter list.
     */
    private Printer printActualJniParameters(boolean isStatic,
					     Class[] params) {
	if (isStatic)
	    print("_Class");
	else
	    print("object");
	print(", id");
	for (int i = 0; i < params.length; i++) {
	    print(", p" + i);
	}
	return this;
    }
    /**
     * Print the method's JNI actual parameter list.
     */
    Printer printActualJniParameters(Method f) {
	return printActualJniParameters(Modifier.isStatic(f.getModifiers()),
					f.getParameterTypes());
    }
    /**
     * Print the constructor's JNI actual parameter list.
     */
    Printer printActualJniParameters(Constructor f) {
	return printActualJniParameters(/*isStatic=*/true,
					f.getParameterTypes());
    }

    /**
     * Print the JNI return name used in Set/Get and Call methods.
     */
    Printer printJniReturnTypeName(Class klass) {
	if (klass.isPrimitive()) {
	    String name = klass.getName();
	    print(Character.toUpperCase(name.charAt(0)));
	    print(name.substring(1));
	} else {
	    print("Object");
	}
	return this;
    }
}
