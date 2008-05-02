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

#include "frysk/jnixx/xx.hxx"

jclass
findClass(JNIEnv* env, const char *signature) {
  jclass klass = env->FindClass(signature);
  if (klass == NULL) {
    throw jnixx_exception();
  }
  return klass;
}

jstring
newStringUTF(JNIEnv* env, const char *string) {
  jstring utf = env->NewStringUTF(string);
  if (utf == NULL) {
    throw jnixx_exception();
  }
  return utf;
}

jmethodID
getMethodID(JNIEnv* env, jobject object, const char* name,
	    const char* signature) {
  jclass klass = env->GetObjectClass(object);
  if (klass == NULL) {
    fprintf(stderr, "frysk: failed to find class for method %s%s\n",
	    name, signature);
    throw jnixx_exception();
  }
  return getMethodID(env, klass, name, signature);
}

jmethodID
getMethodID(JNIEnv* env, jclass klass, const char* name,
	    const char* signature) {
  jmethodID methodID = env->GetMethodID(klass, name, signature);
  if (methodID == NULL) {
    fprintf(stderr, "frysk: failed to find method %s%s\n",
	    name, signature);
    throw jnixx_exception();
  }
  return methodID;
}

jmethodID
getStaticMethodID(JNIEnv* env, jobject object, const char* name,
		  const char* signature) {
  jclass klass = env->GetObjectClass(object);
  if (klass == NULL) {
    fprintf(stderr, "frysk: failed to find class for method %s%s\n",
	    name, signature);
    throw jnixx_exception();
  }
  return getStaticMethodID(env, klass, name, signature);
}

jmethodID
getStaticMethodID(JNIEnv* env, jclass klass, const char* name,
		  const char* signature) {
  jmethodID methodID = env->GetStaticMethodID(klass, name, signature);
  if (methodID == NULL) {
    fprintf(stderr, "frysk: failed to find method %s%s\n",
	    name, signature);
    throw jnixx_exception();
  }
  return methodID;
}

jfieldID
getFieldID(JNIEnv* env, jobject object, const char* name,
	   const char* signature) {
    jclass klass = env->GetObjectClass(object);
    if (klass == NULL) {
      fprintf(stderr, "frysk: failed to find class for field %s%s\n",
	      name, signature);
      throw jnixx_exception();
    }
    return getFieldID(env, klass, name, signature);
}

jfieldID
getFieldID(JNIEnv* env, jclass klass, const char* name,
	   const char* signature) {
    jfieldID fieldID = env->GetFieldID(klass, name, signature);
    if (fieldID == NULL) {
      fprintf(stderr, "frysk: failed to find field %s%s\n",
	      name, signature);
      throw jnixx_exception();
    }
    return fieldID;
}

jfieldID
getStaticFieldID(JNIEnv* env, jobject object, const char* name,
		 const char* signature) {
    jclass klass = env->GetObjectClass(object);
    if (klass == NULL) {
      fprintf(stderr, "frysk: failed to find class for field %s%s\n",
	      name, signature);
      throw jnixx_exception();
    }
    return getStaticFieldID(env, klass, name, signature);
}

jfieldID
getStaticFieldID(JNIEnv* env, jclass klass, const char* name,
		 const char* signature) {
  jfieldID fieldID = env->GetStaticFieldID(klass, name, signature);
  if (fieldID == NULL) {
    fprintf(stderr, "frysk: failed to find static field %s%s\n",
	    name, signature);
    throw jnixx_exception();
  }
  return fieldID;
}
