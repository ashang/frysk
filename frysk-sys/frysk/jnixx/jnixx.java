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
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.io.PrintWriter;

class jnixx {

    private static PrintWriter out = new PrintWriter(System.out, true);
    private static PrintWriter err = new PrintWriter(System.err, true);

    static void print(char ch) {
	out.print(ch);
    }
    static void print(Object o) {
	out.print(o.toString());
    }
    static void println(Object o) {
	out.println(o.toString());
    }
    static void println() {
	out.println();
    }

    /**
     * Pad with white space.
     */
    static void pad(int n) {
	for (int i = 0 ; i < n; i++)
	    print("  ");
    }

    /**
     * Print the name in a C++ friendly form; that is "." replaced by
     * "::".
     */
    static void printCxxName(Class klass) {
	print(klass.getName().replaceAll("\\.", "::"));
    }

    /**
     * Print the name in a C++ friendly form; that is "." replaced by
     * "::".
     */
    static void printCxxName(Method method) {
	printCxxName(method.getDeclaringClass());
	print("::");
	print(method.getName());
    }
    static void printCxxName(Constructor constructor) {
	printCxxName(constructor.getDeclaringClass());
	print("::");
	print("New");
    }

    /**
     * Print the namespace spec for the klass.
     */
    static void printCxxNamespace(Class klass) {
	while (klass.isArray()) {
	    klass = klass.getComponentType();
	}
	if (klass.isPrimitive())
	    return;
	if (printedNamespaces.contains(klass))
	    return;
	println();
	String[] names = klass.getName().split("\\.");
	for (int i = 0; i < names.length - 1; i++) {
	    pad(i);
	    print("namespace ");
	    print(names[i]);
	    println(" {");
	}
	pad(names.length - 1);
	print("struct ");
	print(names[names.length - 1]);
	println(";");
	for (int i = names.length - 2; i >= 0; i--) {
	    pad(i);
	    println("}");
	}
	printedNamespaces.add(klass);
    }
    private static HashSet printedNamespaces = new HashSet();

    /**
     * Iterate over the klasses printing any referenced name spaces.
     */
    static void printCxxNamespaces(Class[] klasses) {
	for (int i = 0; i < klasses.length; i++) {
	    // Should this recurse?
	    printCxxNamespace(klasses[i]);
	}
    }

    /**
     * Iterate over the klass printing all referenced name-space
     * information.
     */
    static void printCxxNamespaces(Class klass) {
	printCxxNamespace(klass);
	Constructor[] constructors = klass.getDeclaredConstructors();
	for (int i = 0; i < constructors.length; i++) {
	    Constructor constructor = constructors[i];
	    printCxxNamespaces(constructor.getParameterTypes());
	}
	Method[] methods = klass.getDeclaredMethods();
	for (int i = 0; i < methods.length; i++) {
	    Method method = methods[i];
	    printCxxNamespace(method.getReturnType());
	    printCxxNamespaces(method.getParameterTypes());
	}
	Field[] fields = klass.getDeclaredFields();
	for (int i = 0; i < fields.length; i++) {
	    printCxxNamespace(fields[i].getType());
	}
     
    }

    static boolean isStatic(Member member) {
	return Modifier.isStatic(member.getModifiers());
    }
    static void printModifiers(Member member) {
	print("  // ");
	print(Modifier.toString(member.getModifiers()));
	println();
	print("  public: static");
    }

    /**
     * Return the JNI signature for the klass.
     */
    static String jniSignature(Class klass) {
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
     * Print the parameter list's JNI signature.
     */
    static void printJniSignature(Class[] params) {
	for (int i = 0; i < params.length; i++) {
	    print(jniSignature(params[i]));
	}
    }

    /**
     * Print the method's JNI signature.
     */
    static void printJniSignature(Method method) {
	print("(");
	printJniSignature(method.getParameterTypes());
	print(")");
	print(jniSignature(method.getReturnType()));
    }

    /**
     * Given a method, print its JNI mangled name.
     */
    static void printJniName(Method method) {
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
    }


    /**
     * Given a class describing a type (class of basic), print the JNI
     * equivalent name.  For classes, print the XX type.
     */
    static void printJniType(Class klass) {
	printCxxType(klass);
    }

    /**
     * Given a class describing a type (class of basic), print the JNI
     * equivalent name.  For classes, print the XX type.
     */
    static void printCxxType(Class klass) {
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
	    printCxxName(klass);
	    print("*");
	}
    }

    /**
     * Given an array of types, print them as a list (presumably this
     * is a list of parameters).
     */
    private static void printFormalCxxParameters(Class klass, Class[] params,
						 boolean isStatic,
						 boolean printArgs) {
	print("JNIEnv*");
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
    }
    static void printFormalCxxParameters(Method f, boolean printArgs) {
	printFormalCxxParameters(f.getDeclaringClass(), f.getParameterTypes(),
				 isStatic(f), printArgs);
    }
    static void printFormalCxxParameters(Constructor f, boolean printArgs) {
	printFormalCxxParameters(f.getDeclaringClass(), f.getParameterTypes(),
				 true/*isStatic*/, printArgs);
    }

    static void printActualCxxParameters(Member func, Class[] params) {
	print("env");
	if (!isStatic(func)) {
	    print(", object");
	}
	for (int i = 0; i < params.length; i++) {
	    print(", p" + i);
	}
    }
    static void printActualCxxParameters(Method f) {
	printActualCxxParameters(f, f.getParameterTypes());
    }
    static void printActualCxxParameters(Constructor f) {
	printActualCxxParameters(f, f.getParameterTypes());
    }

    static void printFormalJniParameters(Method method, boolean printArgs) {
	print("JNIEnv*");
	if (printArgs)
	    print(" env");
	if (isStatic(method)) {
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
    }

    static void printActualJniParameters(boolean isStatic, Class[] params) {
	if (isStatic)
	    print("_Class");
	else
	    print("object");
	print(", id");
	for (int i = 0; i < params.length; i++) {
	    print(", p" + i);
	}
    }
    static void printActualJniParameters(Method f) {
	printActualJniParameters(isStatic(f), f.getParameterTypes());
    }
    static void printActualJniParameters(Constructor f) {
	printActualJniParameters(/*isStatic=*/true, f.getParameterTypes());
    }

    static void printReturnType(Class klass) {
	if (klass.isPrimitive()) {
	    String name = klass.getName();
	    print(Character.toUpperCase(name.charAt(0)));
	    print(name.substring(1));
	} else {
	    print("Object");
	}
    }

    static void printCxxMethodDeclaration(Method method) {
	println();
	printModifiers(method);
	print(" ");
	printCxxType(method.getReturnType());
	print(" ");
	print(method.getName());
	print("(");
	printFormalCxxParameters(method, false);
	print(");");
	println();
    }

    static void printCxxMethodDefinition(Method method) {
	Class returnType = method.getReturnType();
	println();
	printCxxType(returnType);
	println();
	printCxxName(method.getDeclaringClass());
	print("::");
	print(method.getName());
	print("(");
	printFormalCxxParameters(method, true);
	print(") {");
	println();
	println("  static jmethodID id;");
	println("  if (id == NULL)");
	print("    id = getMethodID(env");
	if (isStatic(method)) {
	    print(", Class(env)");
	} else {
	    print(", object");
	}
	print(", \"");
	print(method.getName());
	print("\", \"");
	printJniSignature(method);
	println("\");");
	print("  ");
	if (returnType != Void.TYPE) {
	    printCxxType(returnType);
	    print(" ret = ");
	    if (!returnType.isPrimitive()) {
		print("(");
		printCxxType(returnType);
		print(") ");
	    }
	}
	print("env->Call");
	if (isStatic(method))
	    print("Static");
	printReturnType(returnType);
	print("Method(");
	printActualJniParameters(method);
	println(");");
	println("  if (env->ExceptionCheck())");
	println("    throw jnixx_exception();");
	if (returnType != Void.TYPE) {
	    println("  return ret;");
	}
	println("}");
    }

    static void printNativeMethodDefinition(Method method) {
	println();
	println("extern \"C\" {");
	print("  JNIEXPORT ");
	printJniType(method.getReturnType());
	print(" JNICALL ");
	printJniName(method);
	print("(");
	printFormalJniParameters(method, false);
	println(");");
	println("}");
	println();
	printJniType(method.getReturnType());
	println();
	printJniName(method);
	print("(");
	printFormalJniParameters(method, true);
	println(") {");
	println("  try {");
	print("    ");
	if (method.getReturnType() != Void.TYPE) {
	    print("return ");
	}
	printCxxName(method);
	print("(");
	printActualCxxParameters(method);
	println(");");
	println("  } catch (jnixx_exception) {");
	if (method.getReturnType() != Void.TYPE) {
	    println("    return 0;");
	}
	println("  }");
	println("}");
    }

    static void printCxxFieldAccessorDeclaration(Field field, boolean get) {
	printModifiers(field);
	if (get) {
	    print(" ");
	    printCxxType(field.getType());
	    print(" Get");
	} else {
	    print(" void Set");
	}
	String name = field.getName();
	print(Character.toUpperCase(name.charAt(0)));
	print(name.substring(1));
	print("(JNIEnv*");
	if (!isStatic(field)) {
	    print(", ");
	    printCxxType(field.getDeclaringClass());
	}
	if (!get) {
	    print(", ");
	    printCxxType(field.getType());
	    print(" value");
	}
	println(");");
    }

    static void printCxxFieldAccessorDefinition(Field field, boolean get) {
	println();
	if (get) {
	    printCxxType(field.getType());
	    println();
	} else { 
	    println("void");
	}
	printCxxName(field.getDeclaringClass());
	print("::");
	if (get) {
	    print("Get");
	} else {
	    print("Set");
	}
	String name = field.getName();
	print(Character.toUpperCase(name.charAt(0)));
	print(name.substring(1));
	print("(JNIEnv* env");
	if (!isStatic(field)) {
	    print(", ");
	    printCxxType(field.getDeclaringClass());
	    print(" object");
	}
	if (!get) {
	    print(", ");
	    printCxxType(field.getType());
	    print(" value");
	}
	println(") {");
	println("  if (" + name + "ID == NULL) {");
	print("    " + name + "ID = getFieldID(env, ");
	if (isStatic(field)) {
	    print("Class(env)");
	} else {
	    print("object");
	}
	print(", \"" + name + "\"");
	print(", \"" + jniSignature(field.getType()) + "\"");
	println(");");
	println("  }");
	if (get) {
	    print("  return");
	    if (!field.getType().isPrimitive()) {
		print(" (");
		printCxxType(field.getType());
		print(")");
	    }
	    print(" env->Get");
	} else {
	    print("  env->Set");
	}
	if (isStatic(field)) {
	    print("Static");
	}
	printReturnType(field.getType());
	print("Field(");
	if (isStatic(field)) {
	    print("_Class");
	} else {
	    print("object");
	}
	print(", " + name + "ID");
	if (!get) {
	    print(",");
	    if (!field.getType().isPrimitive()) {
		print(" (jobject)");
	    }
	    print(" value");
	}
	println(");");
	println("}");
    }

    static void printCxxConstructorDeclaration(Constructor constructor) {
	println();
	printModifiers(constructor);
	print(" ");
	printCxxType(constructor.getDeclaringClass());
	print(" New(");
	printFormalCxxParameters(constructor, false);
	print(");");
	println();
    }

    static void printCxxConstructorDefinition(Constructor constructor) {
	println();
	printCxxType(constructor.getDeclaringClass());
	println();
	printCxxName(constructor);
	print("(");
	printFormalCxxParameters(constructor, true);
	print(") {");
	println();
	println("  static jmethodID id;");
	println("  if (id == NULL)");
	print("    id = getMethodID(env, Class(env), \"<init>\", \"(");
	printJniSignature(constructor.getParameterTypes());
	println(")V\");");
	print("  ");
	printCxxType(constructor.getDeclaringClass());
	print(" object = (");
	printCxxType(constructor.getDeclaringClass());
	print(") env->NewObject(");
	printActualJniParameters(constructor);
	println(");");
	println("  if (object == NULL)");
	println("    throw jnixx_exception();");
	println("  return object;");
	print("}");
    }

    static void printHxxFile(Class klass) {
	String header = klass.getName().replaceAll("\\.", "_") + "_jni_hxx";
	println("#ifndef " + header);
	println("#define " + header);
	println();
	println("#include \"frysk/jnixx/xx.hxx\"");
	printCxxNamespaces(klass);
	println();
	Class parent = klass.getSuperclass();
	if (parent != Object.class) {
	    print("#include \"");
	    print(parent.getName().replaceAll("\\.", "/"));
	    println("-jni.hxx\"");
	    println();
	}
	print("struct ");
	printCxxName(klass);
	if (parent == Object.class) {
	    print(" : public __jobject");
	} else if (parent != null) {
	    print(" : public ");
	    printCxxName(parent);
	}
	println(" {");
	// Static get-class method - a class knows its own class.
	println();
	print("  public: static jclass Class(JNIEnv* env);");
	// Print the constructors.
	Constructor[] constructors = klass.getDeclaredConstructors();
	for (int i = 0; i < constructors.length; i++) {
	    printCxxConstructorDeclaration(constructors[i]);
	}
	// Print the field accessors.
	Field[] fields = klass.getDeclaredFields();
	for (int i = 0; i < fields.length; i++) {
	    Field field = fields[i];
	    printCxxFieldAccessorDeclaration(field, true);
	    if (!Modifier.isFinal(field.getModifiers())) {
		printCxxFieldAccessorDeclaration(field, false);
	    }
	}
	// Print the methods
	Method[] methods = klass.getDeclaredMethods();
	for (int i = 0; i < methods.length; i++) {
	    Method method = methods[i];
	    printCxxMethodDeclaration(method);
	}
	println();
	println("};");
	println();
	println("#endif");
    }

    static void printCxxFile(Class klass) {
	print("#include \"");
	print(klass.getName().replaceAll("\\.", "/"));
	print("-jni.hxx\"");
	println();

	// The class, via reflection.
	println();
	println("static jclass _Class;");
	println();
	println("jclass");
	printCxxName(klass);
	println("::Class(JNIEnv* env) {");
	println("  if (_Class == NULL) {");
	println("    _Class = findClass(env, \""
		+ klass.getName().replace("\\.", "/") + "\");");
	println("  }");
	println("  return _Class;");
	println("}");

	// The constructors.
	Constructor[] constructors = klass.getDeclaredConstructors();
	for (int i = 0; i < constructors.length; i++) {
	    printCxxConstructorDefinition(constructors[i]);
	}

	// The field accessors.
	Field[] fields = klass.getDeclaredFields();
	for (int i = 0; i < fields.length; i++) {
	    Field field = fields[i];
	    String name = field.getName();
	    println();
	    println("static jfieldID " + name + "ID;");
	    printCxxFieldAccessorDefinition(field, true);
	    if (!Modifier.isFinal(field.getModifiers())) {
		printCxxFieldAccessorDefinition(field, false);
	    }
	}

	// The methods.
	Method[] methods = klass.getDeclaredMethods();
	for (int i = 0; i < methods.length; i++) {
	    Method method = methods[i];
	    if (Modifier.isNative(method.getModifiers())) {
		printNativeMethodDefinition(method);
	    } else {
		printCxxMethodDefinition(method);
	    }
	}
    }

    public static void main(String[] args) throws ClassNotFoundException {
	if (args.length != 2) {
	    err.println("Usage: jnixx <class-name>");
	    System.exit(1);
	}

	Class klass = Class.forName(args[0], false,
				    jnixx.class.getClassLoader());
	if (args[1].equals("hxx"))
	    printHxxFile(klass);
	else
	    printCxxFile(klass);
    }
}
