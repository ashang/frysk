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

#include "lib_dwfl_Elf.h"


JNIEXPORT jobject
Java_lib_dwfl_Elf_elfBegin (JNIEnv *env, jclass, jobject, jobject)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_lib_dwfl_Elf_elfBegin not implemented");
  }
  return 0;
}

JNIEXPORT void
Java_lib_dwfl_Elf_elfEnd (JNIEnv *env, jclass, jobject)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_lib_dwfl_Elf_elfEnd not implemented");
  }
  return;
}

JNIEXPORT jint
Java_lib_dwfl_Elf_elf_1next (JNIEnv *env, jobject)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_lib_dwfl_Elf_elf_1next not implemented");
  }
  return 0;
}

JNIEXPORT jint
Java_lib_dwfl_Elf_elf_1get_1version (JNIEnv *env, jobject)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_lib_dwfl_Elf_elf_1get_1version not implemented");
  }
  return 0;
}

JNIEXPORT void
Java_lib_dwfl_Elf_elf_1update (JNIEnv *env, jobject, jint)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_lib_dwfl_Elf_elf_1update not implemented");
  }
  return;
}

JNIEXPORT jint
Java_lib_dwfl_Elf_elf_1kind (JNIEnv *env, jobject)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_lib_dwfl_Elf_elf_1kind not implemented");
  }
  return 0;
}

JNIEXPORT jlong
Java_lib_dwfl_Elf_elf_1getbase (JNIEnv *env, jobject)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_lib_dwfl_Elf_elf_1getbase not implemented");
  }
  return 0;
}

JNIEXPORT jstring
Java_lib_dwfl_Elf_elf_1getident (JNIEnv *env, jobject)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_lib_dwfl_Elf_elf_1getident not implemented");
  }
  return 0;
}

JNIEXPORT jobject
Java_lib_dwfl_Elf_elf_1getehdr (JNIEnv *env, jobject)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_lib_dwfl_Elf_elf_1getehdr not implemented");
  }
  return 0;
}

JNIEXPORT void
Java_lib_dwfl_Elf_elf_1newehdr (JNIEnv *env, jobject, jint)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_lib_dwfl_Elf_elf_1newehdr not implemented");
  }
  return;
}

JNIEXPORT void
Java_lib_dwfl_Elf_elf_1updatehdr (JNIEnv *env, jobject, jobject)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_lib_dwfl_Elf_elf_1updatehdr not implemented");
  }
  return;
}

JNIEXPORT jobject
Java_lib_dwfl_Elf_elf_1getphdr (JNIEnv *env, jobject, jint)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_lib_dwfl_Elf_elf_1getphdr not implemented");
  }
  return 0;
}

JNIEXPORT jint
Java_lib_dwfl_Elf_elf_1updatephdr (JNIEnv *env, jobject, jint, jobject)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_lib_dwfl_Elf_elf_1updatephdr not implemented");
  }
  return 0;
}

JNIEXPORT jint
Java_lib_dwfl_Elf_elf_1newphdr (JNIEnv *env, jobject, jlong)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_lib_dwfl_Elf_elf_1newphdr not implemented");
  }
  return 0;
}

JNIEXPORT jlong
Java_lib_dwfl_Elf_elf_1offscn (JNIEnv *env, jobject, jlong)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_lib_dwfl_Elf_elf_1offscn not implemented");
  }
  return 0;
}

JNIEXPORT jlong
Java_lib_dwfl_Elf_elf_1getscn (JNIEnv *env, jobject, jlong)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_lib_dwfl_Elf_elf_1getscn not implemented");
  }
  return 0;
}

JNIEXPORT jlong
Java_lib_dwfl_Elf_elf_1nextscn (JNIEnv *env, jobject, jlong)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_lib_dwfl_Elf_elf_1nextscn not implemented");
  }
  return 0;
}

JNIEXPORT jlong
Java_lib_dwfl_Elf_elf_1newscn (JNIEnv *env, jobject)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_lib_dwfl_Elf_elf_1newscn not implemented");
  }
  return 0;
}

JNIEXPORT jlong
Java_lib_dwfl_Elf_elf_1getshnum (JNIEnv *env, jobject)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_lib_dwfl_Elf_elf_1getshnum not implemented");
  }
  return 0;
}

JNIEXPORT jlong
Java_lib_dwfl_Elf_elf_1getshstrndx (JNIEnv *env, jobject)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_lib_dwfl_Elf_elf_1getshstrndx not implemented");
  }
  return 0;
}

JNIEXPORT jint
Java_lib_dwfl_Elf_elf_1flagelf (JNIEnv *env, jobject, jint, jint)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_lib_dwfl_Elf_elf_1flagelf not implemented");
  }
  return 0;
}

JNIEXPORT jint
Java_lib_dwfl_Elf_elf_1flagehdr (JNIEnv *env, jobject, jint, jint)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_lib_dwfl_Elf_elf_1flagehdr not implemented");
  }
  return 0;
}

JNIEXPORT jint
Java_lib_dwfl_Elf_elf_1flagphdr (JNIEnv *env, jobject, jint, jint)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_lib_dwfl_Elf_elf_1flagphdr not implemented");
  }
  return 0;
}

JNIEXPORT jstring
Java_lib_dwfl_Elf_elf_1strptr (JNIEnv *env, jobject, jlong, jlong)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_lib_dwfl_Elf_elf_1strptr not implemented");
  }
  return 0;
}

JNIEXPORT jobject
Java_lib_dwfl_Elf_elf_1getarhdr (JNIEnv *env, jobject)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_lib_dwfl_Elf_elf_1getarhdr not implemented");
  }
  return 0;
}

JNIEXPORT jlong
Java_lib_dwfl_Elf_elf_1getaroff (JNIEnv *env, jobject)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_lib_dwfl_Elf_elf_1getaroff not implemented");
  }
  return 0;
}

JNIEXPORT jlong
Java_lib_dwfl_Elf_elf_1rand (JNIEnv *env, jobject, jint)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_lib_dwfl_Elf_elf_1rand not implemented");
  }
  return 0;
}

JNIEXPORT jlong
Java_lib_dwfl_Elf_elf_1getarsym (JNIEnv *env, jobject, jlong)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_lib_dwfl_Elf_elf_1getarsym not implemented");
  }
  return 0;
}

JNIEXPORT jint
Java_lib_dwfl_Elf_elf_1cntl (JNIEnv *env, jobject, jint)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_lib_dwfl_Elf_elf_1cntl not implemented");
  }
  return 0;
}

JNIEXPORT jstring
Java_lib_dwfl_Elf_elf_1rawfile (JNIEnv *env, jobject, jlong)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_lib_dwfl_Elf_elf_1rawfile not implemented");
  }
  return 0;
}

JNIEXPORT jstring
Java_lib_dwfl_Elf_elf_1get_1last_1error_1msg (JNIEnv *env, jclass)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_lib_dwfl_Elf_elf_1get_1last_1error_1msg not implemented");
  }
  return 0;
}

JNIEXPORT jint
Java_lib_dwfl_Elf_elf_1get_1last_1error_1no (JNIEnv *env, jclass)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_lib_dwfl_Elf_elf_1get_1last_1error_1no not implemented");
  }
  return 0;
}

JNIEXPORT jobject
Java_lib_dwfl_Elf_elf_1get_1raw_1data (JNIEnv *env, jobject, jlong, jlong)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_lib_dwfl_Elf_elf_1get_1raw_1data not implemented");
  }
  return 0;
}
