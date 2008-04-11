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

#include "lib_unwind_UnwindX8664.h"


JNIEXPORT jobject
Java_lib_unwind_UnwindX8664_initRemote (JNIEnv *env, jobject, jobject)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_lib_unwind_UnwindX8664_initRemote not implemented");
  }
  return 0;
}

JNIEXPORT jobject
Java_lib_unwind_UnwindX8664_createAddressSpace (JNIEnv *env, jobject, jobject)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_lib_unwind_UnwindX8664_createAddressSpace not implemented");
  }
  return 0;
}

JNIEXPORT void
Java_lib_unwind_UnwindX8664_destroyAddressSpace (JNIEnv *env, jobject, jobject)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_lib_unwind_UnwindX8664_destroyAddressSpace not implemented");
  }
  return;
}

JNIEXPORT void
Java_lib_unwind_UnwindX8664_setCachingPolicy (JNIEnv *env, jobject, jobject, jobject)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_lib_unwind_UnwindX8664_setCachingPolicy not implemented");
  }
  return;
}

JNIEXPORT jint
Java_lib_unwind_UnwindX8664_isSignalFrame (JNIEnv *env, jobject, jobject)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_lib_unwind_UnwindX8664_isSignalFrame not implemented");
  }
  return 0;
}

JNIEXPORT jint
Java_lib_unwind_UnwindX8664_step (JNIEnv *env, jobject, jobject)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_lib_unwind_UnwindX8664_step not implemented");
  }
  return 0;
}

JNIEXPORT jobject
Java_lib_unwind_UnwindX8664_getProcInfo (JNIEnv *env, jobject, jobject)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_lib_unwind_UnwindX8664_getProcInfo not implemented");
  }
  return 0;
}

JNIEXPORT void
Java_lib_unwind_UnwindX8664_getRegister (JNIEnv *env, jobject, jobject, jobject, jlong, jint, jbyteArray, jint)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_lib_unwind_UnwindX8664_getRegister not implemented");
  }
  return;
}

JNIEXPORT void
Java_lib_unwind_UnwindX8664_setRegister (JNIEnv *env, jobject, jobject, jobject, jlong, jint, jbyteArray, jint)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_lib_unwind_UnwindX8664_setRegister not implemented");
  }
  return;
}

JNIEXPORT jlong
Java_lib_unwind_UnwindX8664_getIP (JNIEnv *env, jobject, jobject)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_lib_unwind_UnwindX8664_getIP not implemented");
  }
  return 0;
}

JNIEXPORT jlong
Java_lib_unwind_UnwindX8664_getSP (JNIEnv *env, jobject, jobject)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_lib_unwind_UnwindX8664_getSP not implemented");
  }
  return 0;
}

JNIEXPORT jlong
Java_lib_unwind_UnwindX8664_getCFA (JNIEnv *env, jobject, jobject)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_lib_unwind_UnwindX8664_getCFA not implemented");
  }
  return 0;
}

JNIEXPORT jobject
Java_lib_unwind_UnwindX8664_copyCursor (JNIEnv *env, jobject, jobject)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_lib_unwind_UnwindX8664_copyCursor not implemented");
  }
  return 0;
}

JNIEXPORT jint
Java_lib_unwind_UnwindX8664_getContext (JNIEnv *env, jobject, jobject)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_lib_unwind_UnwindX8664_getContext not implemented");
  }
  return 0;
}

JNIEXPORT jobject
Java_lib_unwind_UnwindX8664_createProcInfoFromElfImage (JNIEnv *env, jobject, jobject, jlong, jboolean, jobject)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_lib_unwind_UnwindX8664_createProcInfoFromElfImage not implemented");
  }
  return 0;
}

JNIEXPORT jobject
Java_lib_unwind_UnwindX8664_createElfImageFromVDSO (JNIEnv *env, jobject, jobject, jlong, jlong, jlong)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_lib_unwind_UnwindX8664_createElfImageFromVDSO not implemented");
  }
  return 0;
}

JNIEXPORT jlong
Java_lib_unwind_UnwindX8664_getStartIP (JNIEnv *env, jobject, jobject)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_lib_unwind_UnwindX8664_getStartIP not implemented");
  }
  return 0;
}

JNIEXPORT jlong
Java_lib_unwind_UnwindX8664_getEndIP (JNIEnv *env, jobject, jobject)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_lib_unwind_UnwindX8664_getEndIP not implemented");
  }
  return 0;
}

JNIEXPORT jlong
Java_lib_unwind_UnwindX8664_getLSDA (JNIEnv *env, jobject, jobject)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_lib_unwind_UnwindX8664_getLSDA not implemented");
  }
  return 0;
}

JNIEXPORT jlong
Java_lib_unwind_UnwindX8664_getHandler (JNIEnv *env, jobject, jobject)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_lib_unwind_UnwindX8664_getHandler not implemented");
  }
  return 0;
}

JNIEXPORT jlong
Java_lib_unwind_UnwindX8664_getGP (JNIEnv *env, jobject, jobject)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_lib_unwind_UnwindX8664_getGP not implemented");
  }
  return 0;
}

JNIEXPORT jlong
Java_lib_unwind_UnwindX8664_getFlags (JNIEnv *env, jobject, jobject)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_lib_unwind_UnwindX8664_getFlags not implemented");
  }
  return 0;
}

JNIEXPORT jint
Java_lib_unwind_UnwindX8664_getFormat (JNIEnv *env, jobject, jobject)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_lib_unwind_UnwindX8664_getFormat not implemented");
  }
  return 0;
}

JNIEXPORT jint
Java_lib_unwind_UnwindX8664_getUnwindInfoSize (JNIEnv *env, jobject, jobject)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_lib_unwind_UnwindX8664_getUnwindInfoSize not implemented");
  }
  return 0;
}

JNIEXPORT jobject
Java_lib_unwind_UnwindX8664_getUnwindInfo (JNIEnv *env, jobject, jobject)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_lib_unwind_UnwindX8664_getUnwindInfo not implemented");
  }
  return 0;
}
