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

#include "lib_unwind_UnwindPPC32.h"


JNIEXPORT jobject
Java_lib_unwind_UnwindPPC32_initRemote (JNIEnv *env, jobject, jobject)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_lib_unwind_UnwindPPC32_initRemote not implemented");
  }
  return 0;
}

JNIEXPORT jobject
Java_lib_unwind_UnwindPPC32_createAddressSpace (JNIEnv *env, jobject, jobject)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_lib_unwind_UnwindPPC32_createAddressSpace not implemented");
  }
  return 0;
}

JNIEXPORT void
Java_lib_unwind_UnwindPPC32_destroyAddressSpace (JNIEnv *env, jobject, jobject)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_lib_unwind_UnwindPPC32_destroyAddressSpace not implemented");
  }
  return;
}

JNIEXPORT void
Java_lib_unwind_UnwindPPC32_setCachingPolicy (JNIEnv *env, jobject, jobject, jobject)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_lib_unwind_UnwindPPC32_setCachingPolicy not implemented");
  }
  return;
}

JNIEXPORT jint
Java_lib_unwind_UnwindPPC32_isSignalFrame (JNIEnv *env, jobject, jobject)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_lib_unwind_UnwindPPC32_isSignalFrame not implemented");
  }
  return 0;
}

JNIEXPORT jint
Java_lib_unwind_UnwindPPC32_step (JNIEnv *env, jobject, jobject)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_lib_unwind_UnwindPPC32_step not implemented");
  }
  return 0;
}

JNIEXPORT jobject
Java_lib_unwind_UnwindPPC32_getProcInfo (JNIEnv *env, jobject, jobject)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_lib_unwind_UnwindPPC32_getProcInfo not implemented");
  }
  return 0;
}

JNIEXPORT void
Java_lib_unwind_UnwindPPC32_getRegister (JNIEnv *env, jobject, jobject, jobject, jlong, jint, jbyteArray, jint)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_lib_unwind_UnwindPPC32_getRegister not implemented");
  }
  return;
}

JNIEXPORT void
Java_lib_unwind_UnwindPPC32_setRegister (JNIEnv *env, jobject, jobject, jobject, jlong, jint, jbyteArray, jint)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_lib_unwind_UnwindPPC32_setRegister not implemented");
  }
  return;
}

JNIEXPORT jlong
Java_lib_unwind_UnwindPPC32_getIP (JNIEnv *env, jobject, jobject)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_lib_unwind_UnwindPPC32_getIP not implemented");
  }
  return 0;
}

JNIEXPORT jlong
Java_lib_unwind_UnwindPPC32_getSP (JNIEnv *env, jobject, jobject)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_lib_unwind_UnwindPPC32_getSP not implemented");
  }
  return 0;
}

JNIEXPORT jlong
Java_lib_unwind_UnwindPPC32_getCFA (JNIEnv *env, jobject, jobject)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_lib_unwind_UnwindPPC32_getCFA not implemented");
  }
  return 0;
}

JNIEXPORT jobject
Java_lib_unwind_UnwindPPC32_copyCursor (JNIEnv *env, jobject, jobject)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_lib_unwind_UnwindPPC32_copyCursor not implemented");
  }
  return 0;
}

JNIEXPORT jint
Java_lib_unwind_UnwindPPC32_getContext (JNIEnv *env, jobject, jobject)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_lib_unwind_UnwindPPC32_getContext not implemented");
  }
  return 0;
}

JNIEXPORT jobject
Java_lib_unwind_UnwindPPC32_createProcInfoFromElfImage (JNIEnv *env, jobject, jobject, jlong, jboolean, jobject)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_lib_unwind_UnwindPPC32_createProcInfoFromElfImage not implemented");
  }
  return 0;
}

JNIEXPORT jobject
Java_lib_unwind_UnwindPPC32_createElfImageFromVDSO (JNIEnv *env, jobject, jobject, jlong, jlong, jlong)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_lib_unwind_UnwindPPC32_createElfImageFromVDSO not implemented");
  }
  return 0;
}

JNIEXPORT jlong
Java_lib_unwind_UnwindPPC32_getStartIP (JNIEnv *env, jobject, jobject)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_lib_unwind_UnwindPPC32_getStartIP not implemented");
  }
  return 0;
}

JNIEXPORT jlong
Java_lib_unwind_UnwindPPC32_getEndIP (JNIEnv *env, jobject, jobject)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_lib_unwind_UnwindPPC32_getEndIP not implemented");
  }
  return 0;
}

JNIEXPORT jlong
Java_lib_unwind_UnwindPPC32_getLSDA (JNIEnv *env, jobject, jobject)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_lib_unwind_UnwindPPC32_getLSDA not implemented");
  }
  return 0;
}

JNIEXPORT jlong
Java_lib_unwind_UnwindPPC32_getHandler (JNIEnv *env, jobject, jobject)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_lib_unwind_UnwindPPC32_getHandler not implemented");
  }
  return 0;
}

JNIEXPORT jlong
Java_lib_unwind_UnwindPPC32_getGP (JNIEnv *env, jobject, jobject)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_lib_unwind_UnwindPPC32_getGP not implemented");
  }
  return 0;
}

JNIEXPORT jlong
Java_lib_unwind_UnwindPPC32_getFlags (JNIEnv *env, jobject, jobject)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_lib_unwind_UnwindPPC32_getFlags not implemented");
  }
  return 0;
}

JNIEXPORT jint
Java_lib_unwind_UnwindPPC32_getFormat (JNIEnv *env, jobject, jobject)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_lib_unwind_UnwindPPC32_getFormat not implemented");
  }
  return 0;
}

JNIEXPORT jint
Java_lib_unwind_UnwindPPC32_getUnwindInfoSize (JNIEnv *env, jobject, jobject)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_lib_unwind_UnwindPPC32_getUnwindInfoSize not implemented");
  }
  return 0;
}

JNIEXPORT jobject
Java_lib_unwind_UnwindPPC32_getUnwindInfo (JNIEnv *env, jobject, jobject)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_lib_unwind_UnwindPPC32_getUnwindInfo not implemented");
  }
  return 0;
}