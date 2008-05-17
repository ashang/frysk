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

package jnixx;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * Given a class, print any JNI bindings.
 */
class JniBindings {
    private static class Binding {
	static final Binding STATIC = new Binding();
	static final Binding DYNAMIC = new Binding();
	static final Binding CHILDREN = new Binding();
    }

    private static void printCodes(Printer p, int level, Object[] codes) {
	boolean nl = false;
	for (int i = 0; i < codes.length; i++) {
	    if (codes[i] instanceof String) {
		if (nl) {
		    p.println();
		}
		p.print(codes[i]);
		nl = true;
	    } else {
		while (p.dent(level, "{", "}")) {
		    printCodes(p, level + 1, (Object[]) codes[i]);
		}
		nl = false;
	    }
	}
	if (nl) {
	    p.println();
	}
    }

    private static class Method {
	private final Class klass;
	private final Binding binding;
	private final String returnType;
	private final String name;
	private final String[] params;
	private final Object[] code;
	Method(Class klass, Binding binding, String returnType, String name,
	       String[] params, Object[] code) {
	    this.klass = klass;
	    this.binding = binding;
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
	void printDeclaration(Class klass, Printer p) {
	    if (binding == Binding.STATIC && klass != this.klass) {
		return;
	    }
	    if (binding == Binding.DYNAMIC && klass != this.klass) {
		return;
	    }
	    if (binding == Binding.STATIC || binding == Binding.CHILDREN) {
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
	    p.print("(");
	    for (int i = 0; i < params.length; i += 2) {
		if (i > 0) {
		    p.print(", ");
		}
		p.print(params[i]);
	    }
	    p.println(");");
	}
	void printDefinition(Class klass, Printer p) {
	    if (binding == Binding.STATIC && klass != this.klass) {
		return;
	    }
	    if (binding == Binding.DYNAMIC && klass != this.klass) {
		return;
	    }
	    p.println();
	    if (returnType == null) {
		p.println("void");
	    } else {
		p.println(returnType);
	    }
	    p.printQualifiedCxxName(klass);
	    p.print("::");
	    p.print(name);
	    p.print("(");
	    for (int i = 0; i < params.length; i += 2) {
		if (i > 0) {
		    p.print(", ");
		}
		p.print(params[i]);
		p.print(" ");
		p.print(params[i + 1]);
	    }
	    p.print(")");
	    while (p.dent(0, "{", "}")) {
		printCodes(p, 1, code);
	    }
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
	JniMap put(Class klass, Binding binding, String returnType,
		   String name, String[] params,
		   Object[] code) {
	    get(klass).add(new Method(klass, binding, returnType, name,
				      params, code));
	    return this;
	}
	void printDeclarations(Class klass, Printer p) {
	    for (Class parent = klass; parent != null;
		 parent = parent.getSuperclass()) {
		for (Iterator i = get(parent).iterator(); i.hasNext(); ) {
		    Method method = (Method)i.next();
		    method.printDeclaration(klass, p);
		}
	    }
	}
	void printDefinitions(Class klass, Printer p) {
	    for (Class parent = klass; parent != null;
		 parent = parent.getSuperclass()) {
		for (Iterator i = get(parent).iterator(); i.hasNext(); ) {
		    Method method = (Method)i.next();
		    method.printDefinition(klass, p);
		}
	    }
	}
    }

    private static JniMap bindings = new JniMap()
	/**
	 * java.lang.Object
	 */
	.put(Object.class, Binding.STATIC,
	     "::jnixx::env", "_env_",
	     new String[] {
	     },
	     new Object[] {
		 "void* _jni;",
		 "::jnixx::vm->GetEnv(&_jni, JNI_VERSION_1_2);",
		 "return ::jnixx::env((JNIEnv*)_jni);",
	     })
	.put(Object.class, Binding.DYNAMIC,
	     "bool", "operator==",
	     new String[] {
		 "jobject", "_object",
	     },
	     new Object[] {
		 "return this->_object == _object;",
	     })
	.put(Object.class, Binding.DYNAMIC,
	     "bool", "operator!=",
	     new String[] {
		 "jobject", "_object",
	     },
	     new Object[] {
		 "return this->_object != _object;",
	     })
	// DeleteLocalRef
	.put(Object.class, Binding.DYNAMIC,
	     null, "DeleteLocalRef",
	     new String[] {
		 "::jnixx::env", "env",
	     },
	     new Object[] {
		 "env.DeleteLocalRef(_object);",
		 "_object = NULL;"
	     })
	// IsInstanceOf
	.put(Object.class, Binding.DYNAMIC,
	     "bool", "IsInstanceOf",
	     new String[] {
		 "::jnixx::env", "env",
		 "jclass", "klass",
	     },
	     new Object[] {
		 "return env.IsInstanceOf(_object, klass);",
	     })

	/**
	 * java.lang.Throwable
	 */
	.put(Throwable.class, Binding.DYNAMIC,
	     null, "Throw",
	     new String[] {
		 "::jnixx::env", "env",
	     },
	     new Object[] {
		 "env.Throw((jthrowable)_object);",
	     })
	.put(Throwable.class, Binding.CHILDREN,
	     null, "ThrowNew",
	     new String[] {
		 "::jnixx::env", "env",
		 "const char*", "message",
	     },
	     new Object[] {
		 "env.ThrowNew(_class_(env), message);",
		 "env.throwPendingException();",
	     })

	/**
	 * java.lang.String
	 */
	// NewString
	// GetStringLength
	.put(String.class, Binding.DYNAMIC,
	     "jsize", "GetStringLength",
	     new String[] {
		 "::jnixx::env", "env",
	     },
	     new Object[] {
		 "return env.GetStringLength((jstring)_object);",
	     })
	// GetStringChars
	// ReleaseStringChars
	// NewStringUTF
	.put(String.class, Binding.STATIC,
	     "::java::lang::String", "NewStringUTF",
	     new String[] {
		 "::jnixx::env", "env",
		 "const char*", "utf",
	     },
	     new Object[] {
		 "return String(env, env.NewStringUTF(utf));",
	     })
	// GetStringUTFLength
	.put(String.class, Binding.DYNAMIC,
	     "jsize", "GetStringUTFLength",
	     new String[] {
		 "::jnixx::env", "env",
	     },
	     new Object[] {
		 "return env.GetStringUTFLength((jstring) _object);",
	     })
	// GetStringUTFChars
	.put(String.class, Binding.DYNAMIC,
	     "const char*", "GetStringUTFChars",
	     new String[] {
		 "::jnixx::env", "env",
	     },
	     new Object[] {
		 "return env.GetStringUTFChars((jstring)_object, NULL);",
	     })
	// ReleaseStringUTFChars
	.put(String.class, Binding.DYNAMIC,
	     null, "ReleaseStringUTFChars",
	     new String[] {
		 "::jnixx::env", "env",
		 "const char *", "utf",
	     },
	     new Object[] {
		 "env.ReleaseStringUTFChars((jstring)_object, utf);",
	     })
	// GetStringRegion
	// GetStringUTFRegion
	.put(String.class, Binding.DYNAMIC,
	     null, "GetStringUTFRegion",
	     new String[] {
		 "::jnixx::env", "env",
		 "jsize", "start",
		 "jsize", "len", 
		 "char*", "buf",
	     },
	     new Object[] {
		 "env.GetStringUTFRegion((jstring)_object, start, len, buf);",
	     })
	// GetStringCritical
	// Release StringCritical
	;

    static {
	Class[] types = {
	    boolean[].class,
	    byte[].class,
	    short[].class,
	    char[].class,
	    int[].class,
	    long[].class,
	    float[].class,
	    double[].class,
	};
	for (int i = 0; i < types.length; i++) {
	    String type = types[i].getComponentType().getName();
	    String Type = (Character.toUpperCase(type.charAt(0))
			   + type.substring(1));
	    bindings
		.put(types[i], Binding.DYNAMIC,
		     "jsize", "GetArrayLength",
		     new String[] {
			 "::jnixx::env", "env",
		     },
		     new Object[] {
			 "return env.GetArrayLength((j" + type + "Array) _object);"
		     })
		.put(types[i], Binding.STATIC,
		     "::jnixx::" + type + "Array", "New" + Type + "Array",
		     new String[] {
			 "::jnixx::env", "env",
			 "jsize", "length",
		     },
		     new Object[] {
			 "return " + type + "Array(env, env.New" + Type + "Array(length));",
		     })
		.put(types[i], Binding.DYNAMIC,
		     "j" + type + "*", "Get" + Type + "ArrayElements",
		     new String[] {
			 "::jnixx::env", "env",
			 "jboolean*", "isCopy",
		     },
		     new Object[] {
			 "return env.Get" + Type + "ArrayElements((j" + type + "Array) _object, isCopy);"
		     })
		.put(types[i], Binding.DYNAMIC,
		     null, "Release" + Type +"ArrayElements",
		     new String[] {
			 "::jnixx::env", "env",
			 "j" + type + "*", "elements",
			 "jint", "mode"
		     },
		     new Object[] {
			 "env.Release" + Type + "ArrayElements((j" + type + "Array)_object, elements, mode);",
		     })
		.put(types[i], Binding.DYNAMIC,
		     "void", "Get" + Type + "ArrayRegion",
		     new String[] {
			 "::jnixx::env", "env",
			 "jsize", "start",
			 "jsize", "length",
			 "j" + type + "*", "buf",
		     },
		     new Object[] {
			 "env.Get" + Type + "ArrayRegion((j" + type + "Array) _object, start, length, buf);"
		     })
		.put(types[i], Binding.DYNAMIC,
		     "void", "Set" + Type + "ArrayRegion",
		     new String[] {
			 "::jnixx::env", "env",
			 "jsize", "start",
			 "jsize", "length",
			 "j" + type + "*", "buf",
		     },
		     new Object[] {
			 "env.Set" + Type + "ArrayRegion((j" + type + "Array) _object, start, length, buf);"
		     })
		;
	}
    }
    
    static void printDeclarations(Printer p, Class klass) {
	bindings.printDeclarations(klass, p);
    }
    static void printDefinitions(Printer p, Class klass) {
	bindings.printDefinitions(klass, p);
    }


    private static void printObjectGlobals(Printer p) {
	p.println(new Object[] {
		"",
		"/**",
		" * The JNIXX array; all object-array uses this as their template.",
		" */",
	    });
	while (p.dent(0, "namespace jnixx {", "}")) {
	    while (p.dent(1, "template <typename component> class array : public ::java::lang::Object {", "};")) {
		printCodes(p, 2, new Object[] {
			"public:",
			"array(::jnixx::env env, jobject _object) : ::java::lang::Object(env, _object)", new Object[] {
			},
			"array() : ::java::lang::Object()", new Object[] {
			},
			"jsize GetArrayLength(::jnixx::env env)", new Object[] {
			    "return env.GetArrayLength((jarray)_object);",
			},
			"static array<component> NewObjectArray(::jnixx::env env, jsize length)", new Object[] {
			    "return array<component>(env, env.NewObjectArray(length, component::_class_(env), NULL));",
			},
			"static array<component> NewObjectArray(::jnixx::env env, jsize length, component init)", new Object[] {
			    "return array<component>(env, env.NewObjectArray(length, component::_class_(env), init._object));",
			},
			"component GetObjectArrayElement(::jnixx::env env, jsize index)", new Object[] {
			    "return component(env, env.GetObjectArrayElement((jobjectArray)_object, index));",
			},
			"void SetObjectArrayElement(::jnixx::env env, jsize index, component object)", new Object[] {
			    "env.SetObjectArrayElement((jobjectArray)_object, index, object._object);",
			},
		    });
	    }
	}
    }

    private static void printRuntimeGlobals(Printer p) {
	p.print("void jnixx::env::throwPendingException()");
	while (p.dent(0, "{", "}")) {
	    p.println("jthrowable exception = _jni->ExceptionOccurred();");
	    p.println("_jni->ExceptionClear();");
	    p.println("throw java::lang::Throwable(*this, exception);");
	}
    }

    static void printGlobals(Printer p, Class klass) {
	if (klass == Object.class) {
	    printObjectGlobals(p);
	} else if (klass == Throwable.class) {
	    printRuntimeGlobals(p);
	}
    }
}
