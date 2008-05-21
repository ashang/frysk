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

#include "jni.hxx"

#include "jnixx/elements.hxx"

using namespace java::lang;

bool
jnixx::Native::isJni(::jnixx::env) {
  return true;
}

jint
jnixx::Native::sizeOfJnixxEnv(::jnixx::env) {
  return sizeof(::jnixx::env);
}

jint
jnixx::Native::sizeOfClass(::jnixx::env) {
  return sizeof(::java::lang::Class);
}

jint
jnixx::Native::sizeOfObject(::jnixx::env) {
  return sizeof(::java::lang::Object);
}

jint
jnixx::Native::sizeOfObjectArray(::jnixx::env) {
  return sizeof(::jnixx::array<java::lang::Object>);
}

::jnixx::array<java::lang::String>
jnixx::Native::copy(::jnixx::env env,
		    ::jnixx::array<java::lang::String> strings) {
  char** chars = strings2chars(env, strings);
  ::jnixx::array<java::lang::String> copiedStrings = chars2strings(env, chars);
  delete chars;
  return copiedStrings;
}

void
jnixx::Native::throwRuntimeException(jnixx::env env) {
  java::lang::RuntimeException::New(env).Throw(env);
}

bool
jnixx::Native::catchRuntimeException(jnixx::env env, jnixx::Native e) {
  try {
    e.execute(env);
    return false;
  } catch (java::lang::Throwable r) {
    if (r.IsInstanceOf(env, java::lang::RuntimeException::_class_(env))) {
      return true;
    } else {
      throw;
    }
  }
}

static void
throwCopy(jnixx::env env, int n, StringChars stringChars,
	  StringArrayChars stringArrayChars,
	  ArrayBytes arrayBytes) {
  if (n <= 0) {
    java::lang::RuntimeException::ThrowNew(env, "oops!");
  } else {
    stringChars.elements();
    stringArrayChars.elements();
    arrayBytes.elements();
    throwCopy(env, n-1, stringChars, stringArrayChars, arrayBytes);
  }
}

void
jnixx::Native::throwElements(jnixx::env env, String string,
			     jnixx::array<String> stringArray,
			     jnixx::byteArray bytes) {
  StringChars stringChars = StringChars(env, string);
  StringArrayChars stringArrayChars = StringArrayChars(env, stringArray);
  ArrayBytes arrayBytes = ArrayBytes(env, bytes);
  throwCopy(env, 4, stringChars, stringArrayChars, arrayBytes);
}
