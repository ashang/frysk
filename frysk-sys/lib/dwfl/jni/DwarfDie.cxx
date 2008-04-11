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

#include "lib_dwfl_DwarfDie.h"


JNIEXPORT jobject
Java_lib_dwfl_DwarfDie_getEntryBreakpoints (JNIEnv *env, jobject)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_lib_dwfl_DwarfDie_getEntryBreakpoints not implemented");
  }
  return 0;
}

JNIEXPORT jboolean
Java_lib_dwfl_DwarfDie_isInlineDeclaration (JNIEnv *env, jobject)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_lib_dwfl_DwarfDie_isInlineDeclaration not implemented");
  }
  return 0;
}

JNIEXPORT jobject
Java_lib_dwfl_DwarfDie_getInlinedInstances (JNIEnv *env, jobject)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_lib_dwfl_DwarfDie_getInlinedInstances not implemented");
  }
  return 0;
}

JNIEXPORT jlong
Java_lib_dwfl_DwarfDie_get_1lowpc (JNIEnv *env, jobject)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_lib_dwfl_DwarfDie_get_1lowpc not implemented");
  }
  return 0;
}

JNIEXPORT jlong
Java_lib_dwfl_DwarfDie_get_1highpc (JNIEnv *env, jobject)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_lib_dwfl_DwarfDie_get_1highpc not implemented");
  }
  return 0;
}

JNIEXPORT jlong
Java_lib_dwfl_DwarfDie_get_1entrypc (JNIEnv *env, jobject)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_lib_dwfl_DwarfDie_get_1entrypc not implemented");
  }
  return 0;
}

JNIEXPORT jstring
Java_lib_dwfl_DwarfDie_get_1diename (JNIEnv *env, jobject)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_lib_dwfl_DwarfDie_get_1diename not implemented");
  }
  return 0;
}

JNIEXPORT jstring
Java_lib_dwfl_DwarfDie_get_1decl_1file (JNIEnv *env, jobject, jlong)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_lib_dwfl_DwarfDie_get_1decl_1file not implemented");
  }
  return 0;
}

JNIEXPORT jint
Java_lib_dwfl_DwarfDie_get_1decl_1line (JNIEnv *env, jobject, jlong)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_lib_dwfl_DwarfDie_get_1decl_1line not implemented");
  }
  return 0;
}

JNIEXPORT jint
Java_lib_dwfl_DwarfDie_get_1decl_1column (JNIEnv *env, jobject, jlong)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_lib_dwfl_DwarfDie_get_1decl_1column not implemented");
  }
  return 0;
}

JNIEXPORT jlongArray
Java_lib_dwfl_DwarfDie_get_1scopes (JNIEnv *env, jobject, jlong)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_lib_dwfl_DwarfDie_get_1scopes not implemented");
  }
  return 0;
}

JNIEXPORT jlongArray
Java_lib_dwfl_DwarfDie_get_1scopes_1die (JNIEnv *env, jobject)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_lib_dwfl_DwarfDie_get_1scopes_1die not implemented");
  }
  return 0;
}

JNIEXPORT jlong
Java_lib_dwfl_DwarfDie_get_1scopevar (JNIEnv *env, jobject, jlongArray, jlongArray, jstring)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_lib_dwfl_DwarfDie_get_1scopevar not implemented");
  }
  return 0;
}

JNIEXPORT jlong
Java_lib_dwfl_DwarfDie_get_1scopevar_1names (JNIEnv *env, jobject, jlongArray, jstring)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_lib_dwfl_DwarfDie_get_1scopevar_1names not implemented");
  }
  return 0;
}

JNIEXPORT void
Java_lib_dwfl_DwarfDie_get_1addr (JNIEnv *env, jobject, jlong, jlong)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_lib_dwfl_DwarfDie_get_1addr not implemented");
  }
  return;
}

JNIEXPORT jlong
Java_lib_dwfl_DwarfDie_get_1type (JNIEnv *env, jobject, jlong, jboolean)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_lib_dwfl_DwarfDie_get_1type not implemented");
  }
  return 0;
}

JNIEXPORT jlong
Java_lib_dwfl_DwarfDie_get_1child (JNIEnv *env, jobject, jlong)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_lib_dwfl_DwarfDie_get_1child not implemented");
  }
  return 0;
}

JNIEXPORT jlong
Java_lib_dwfl_DwarfDie_get_1sibling (JNIEnv *env, jobject, jlong)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_lib_dwfl_DwarfDie_get_1sibling not implemented");
  }
  return 0;
}

JNIEXPORT jboolean
Java_lib_dwfl_DwarfDie_get_1attr_1boolean (JNIEnv *env, jobject, jlong, jint)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_lib_dwfl_DwarfDie_get_1attr_1boolean not implemented");
  }
  return 0;
}

JNIEXPORT jint
Java_lib_dwfl_DwarfDie_get_1attr_1constant (JNIEnv *env, jobject, jlong, jint)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_lib_dwfl_DwarfDie_get_1attr_1constant not implemented");
  }
  return 0;
}

JNIEXPORT jstring
Java_lib_dwfl_DwarfDie_get_1attr_1string (JNIEnv *env, jobject, jlong, jint)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_lib_dwfl_DwarfDie_get_1attr_1string not implemented");
  }
  return 0;
}

JNIEXPORT jlong
Java_lib_dwfl_DwarfDie_get_1offset (JNIEnv *env, jobject, jlong)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_lib_dwfl_DwarfDie_get_1offset not implemented");
  }
  return 0;
}

JNIEXPORT jint
Java_lib_dwfl_DwarfDie_get_1tag (JNIEnv *env, jclass, jlong)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_lib_dwfl_DwarfDie_get_1tag not implemented");
  }
  return 0;
}

JNIEXPORT void
Java_lib_dwfl_DwarfDie_get_1framebase (JNIEnv *env, jobject, jlong, jlong, jlong)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_lib_dwfl_DwarfDie_get_1framebase not implemented");
  }
  return;
}

JNIEXPORT jlong
Java_lib_dwfl_DwarfDie_get_1data_1member_1location (JNIEnv *env, jobject, jlong)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_lib_dwfl_DwarfDie_get_1data_1member_1location not implemented");
  }
  return 0;
}

JNIEXPORT jboolean
Java_lib_dwfl_DwarfDie_is_1inline_1func (JNIEnv *env, jobject)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_lib_dwfl_DwarfDie_is_1inline_1func not implemented");
  }
  return 0;
}

JNIEXPORT jlong
Java_lib_dwfl_DwarfDie_get_1decl (JNIEnv *env, jclass, jlong, jstring)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_lib_dwfl_DwarfDie_get_1decl not implemented");
  }
  return 0;
}

JNIEXPORT jlong
Java_lib_dwfl_DwarfDie_get_1decl_1cu (JNIEnv *env, jclass, jlong, jstring)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_lib_dwfl_DwarfDie_get_1decl_1cu not implemented");
  }
  return 0;
}

JNIEXPORT void
Java_lib_dwfl_DwarfDie_finalize (JNIEnv *env, jobject)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_lib_dwfl_DwarfDie_finalize not implemented");
  }
  return;
}

JNIEXPORT jboolean
Java_lib_dwfl_DwarfDie_hasattr (JNIEnv *env, jobject, jlong, jint)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_lib_dwfl_DwarfDie_hasattr not implemented");
  }
  return 0;
}

JNIEXPORT jlong
Java_lib_dwfl_DwarfDie_get_1original_1die (JNIEnv *env, jobject, jlong)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_lib_dwfl_DwarfDie_get_1original_1die not implemented");
  }
  return 0;
}
