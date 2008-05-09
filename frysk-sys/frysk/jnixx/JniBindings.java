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

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * Given a class, print any JNI bindings.
 */
class JniBindings {
    private static class Method {
	private final Class klass;
	private final boolean isStatic;
	private final String returnType;
	private final String name;
	private final String[] params;
	private final Object[] code;
	Method(Class klass, boolean isStatic, String returnType, String name, String[] params, Object[] code) {
	    this.klass = klass;
	    this.isStatic = isStatic;
	    this.returnType = returnType;
	    this.name = name;
	    this.params = params;
	    this.code = code;
	}
	public int hashCode() {
	    return name.hashCode();
	}
	public boolean equals(Object o) {
	    Method rhs = (Method)o;
	    return klass.equals(rhs.klass) && name.equals(rhs.name);
	}
	void printDeclaration(Printer p) {
	    if (isStatic) {
		p.print("static ");
	    }
	    p.print("inline ");
	    if (returnType == null) {
		p.print("void");
	    } else {
		p.print(returnType);
	    }
	    p.print(" ");
	    p.print(name);
	    p.print("(::jnixx::env");
	    for (int i = 0; i < params.length; i += 2) {
		p.print(", ");
		p.print(params[i]);
	    }
	    p.println(");");
	}
	private void printCodes(Printer p, int level, Object[] codes) {
	    while (p.dent(level, "{", "}")) {
		if (level == 0) {
		    p.println("JNIEnv *_jni = env._jni;");
		}
		for (int i = 0; i < codes.length; i++) {
		    if (codes[i] instanceof String) {
			p.println(codes[i]);
		    } else {
			printCodes(p, level+1, (Object[]) codes[i]);
		    }
		}
	    }
	}
	void printDefinition(Printer p) {
	    p.println();
	    if (returnType == null) {
		p.println("void");
	    } else {
		p.println(returnType);
	    }
	    p.printQualifiedCxxName(klass);
	    p.print("::");
	    p.print(name);
	    p.print("(::jnixx::env env");
	    for (int i = 0; i < params.length; i += 2) {
		p.print(", ");
		p.print(params[i]);
		p.print(" ");
		p.print(params[i + 1]);
	    }
	    p.print(")");
	    printCodes(p, 0, code);
	}
    }

    private static class JniMap {
	private final HashMap map = new HashMap();
	private LinkedList get(Class klass) {
	    LinkedList methods = (LinkedList)map.get(klass);
	    if (methods == null) {
		methods = new LinkedList();
		map.put(klass, methods);
	    }
	    return methods;
	}
	JniMap put(Class klass, boolean isStatic, String returnType, String name, String[] params,
		   Object[] code) {
	    get(klass).add(new Method(klass, isStatic, returnType, name, params, code));
	    return this;
	}
	void printDeclarations(Class klass, Printer p) {
	    for (Iterator i = get(klass).iterator(); i.hasNext(); ) {
		Method method = (Method)i.next();
		method.printDeclaration(p);
	    }
	}
	void printDefinitions(Class klass, Printer p) {
	    for (Iterator i = get(klass).iterator(); i.hasNext(); ) {
		Method method = (Method)i.next();
		method.printDefinition(p);
	    }
	}
    }

    private static JniMap bindings = new JniMap()
	// NewString
	// GetStringLength
	.put(String.class, false,
	     "jsize", "GetStringLength",
	     new String[] {
	     },
	     new Object[] {
		 "jsize len = _jni->GetStringLength((jstring)_object);",
		 "// No exceptions",
		 "return len;",
	     })
	// GetStringChars
	// ReleaseStringChars
	// NewStringUTF
	.put(String.class, true,
	     "::java::lang::String", "NewStringUTF",
	     new String[] {
		 "const char*", "utf",
	     },
	     new Object[] {
		 "jstring string = _jni->NewStringUTF(utf);",
		 "if (string == NULL)", new Object[] {
		     "throw jnixx::exception();",
		 },
		 "return String(string);",
	     })
	// GetStringUTFLength
	.put(String.class, false,
	     "jsize", "GetStringUTFLength",
	     new String[] {
	     },
	     new Object[] {
		 "jsize len = _jni->GetStringUTFLength((jstring) _object);",
		 "// No exceptions",
		 "return len;",
	     })
	// GetStringUTFChars
	.put(String.class, false,
	     "const char*", "GetStringUTFChars",
	     new String[] {
	     },
	     new Object[] {
		 "const char* utf = _jni->GetStringUTFChars((jstring)_object, NULL);",
		 "if (utf == NULL)", new Object[] {
		     "throw jnixx::exception();",
		 },
		 "return utf;",
	     })
	// ReleaseStringUTFChars
	.put(String.class, false,
	     null, "ReleaseStringUTFChars",
	     new String[] {
		 "const char *", "utf",
	     },
	     new Object[] {
		 "_jni->ReleaseStringUTFChars((jstring)_object, utf);",
		 "// No exceptions",
	     })
	// GetStringRegion
	// GetStringUTFRegion
	.put(String.class, false,
	     null, "GetStringUTFRegion",
	     new String[] {
		 "jsize", "start",
		 "jsize", "len", 
		 "char*", "buf",
	     },
	     new Object[] {
		 "_jni->GetStringUTFRegion((jstring)_object, start, len, buf);",
		 "if (_jni->ExceptionCheck())", new Object[] {
		     "throw jnixx::exception();",
		 },
	     })
	// GetStringCritical
	// Release StringCritical
	;
    
    static void printDeclarations(Printer p, Class klass) {
	bindings.printDeclarations(klass, p);
    }
    static void printDefinitions(Printer p, Class klass) {
	bindings.printDefinitions(klass, p);
    }
}
