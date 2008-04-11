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

#include "lib_dwfl_DwflModule.h"


JNIEXPORT void
Java_lib_dwfl_DwflModule_get_1pubnames (JNIEnv *env, jobject)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_lib_dwfl_DwflModule_get_1pubnames not implemented");
  }
  return;
}

JNIEXPORT void
Java_lib_dwfl_DwflModule_getSymtab (JNIEnv *env, jobject, jobject)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_lib_dwfl_DwflModule_getSymtab not implemented");
  }
  return;
}

JNIEXPORT void
Java_lib_dwfl_DwflModule_getPLTEntries (JNIEnv *env, jobject, jobject)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_lib_dwfl_DwflModule_getPLTEntries not implemented");
  }
  return;
}

JNIEXPORT jobject
Java_lib_dwfl_DwflModule_offdie (JNIEnv *env, jobject, jlong, jlong)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_lib_dwfl_DwflModule_offdie not implemented");
  }
  return 0;
}

JNIEXPORT jstring
Java_lib_dwfl_DwflModule_getName (JNIEnv *env, jobject)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_lib_dwfl_DwflModule_getName not implemented");
  }
  return 0;
}

JNIEXPORT void
Java_lib_dwfl_DwflModule_getSymbol (JNIEnv *env, jobject, jlong, jobject)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_lib_dwfl_DwflModule_getSymbol not implemented");
  }
  return;
}

JNIEXPORT jobject
Java_lib_dwfl_DwflModule_module_1getelf (JNIEnv *env, jobject)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_lib_dwfl_DwflModule_module_1getelf not implemented");
  }
  return 0;
}

JNIEXPORT jobjectArray
Java_lib_dwfl_DwflModule_getLines (JNIEnv *env, jobject, jstring, jint, jint)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_lib_dwfl_DwflModule_getLines not implemented");
  }
  return 0;
}

JNIEXPORT void
Java_lib_dwfl_DwflModule_getSymbolByName (JNIEnv *env, jobject, jstring, jobject)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_lib_dwfl_DwflModule_getSymbolByName not implemented");
  }
  return;
}

JNIEXPORT void
Java_lib_dwfl_DwflModule_setUserData (JNIEnv *env, jobject, jobject)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_lib_dwfl_DwflModule_setUserData not implemented");
  }
  return;
}

JNIEXPORT jstring
Java_lib_dwfl_DwflModule_getDebuginfo (JNIEnv *env, jobject)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_lib_dwfl_DwflModule_getDebuginfo not implemented");
  }
  return 0;
}
