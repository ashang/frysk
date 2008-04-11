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

#include "frysk_sys_Signal.h"


JNIEXPORT void
Java_frysk_sys_Signal_kill__IILjava_lang_String_2 (JNIEnv *env, jclass, jint, jint, jstring)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_frysk_sys_Signal_kill__IILjava_lang_String_2 not implemented");
  }
  return;
}

JNIEXPORT void
Java_frysk_sys_Signal_tkill (JNIEnv *env, jclass, jint, jint, jstring)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_frysk_sys_Signal_tkill not implemented");
  }
  return;
}

JNIEXPORT void
Java_frysk_sys_Signal_drain (JNIEnv *env, jclass, jint)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_frysk_sys_Signal_drain not implemented");
  }
  return;
}

JNIEXPORT jint
Java_frysk_sys_Signal_nsig (JNIEnv *env, jclass)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_frysk_sys_Signal_nsig not implemented");
  }
  return 0;
}

JNIEXPORT jint
Java_frysk_sys_Signal_rtMin (JNIEnv *env, jclass)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_frysk_sys_Signal_rtMin not implemented");
  }
  return 0;
}

JNIEXPORT jint
Java_frysk_sys_Signal_rtMax (JNIEnv *env, jclass)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_frysk_sys_Signal_rtMax not implemented");
  }
  return 0;
}

JNIEXPORT jint
Java_frysk_sys_Signal_hup (JNIEnv *env, jclass)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_frysk_sys_Signal_hup not implemented");
  }
  return 0;
}

JNIEXPORT jint
Java_frysk_sys_Signal_int_1 (JNIEnv *env, jclass)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_frysk_sys_Signal_int_1 not implemented");
  }
  return 0;
}

JNIEXPORT jint
Java_frysk_sys_Signal_quit (JNIEnv *env, jclass)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_frysk_sys_Signal_quit not implemented");
  }
  return 0;
}

JNIEXPORT jint
Java_frysk_sys_Signal_ill (JNIEnv *env, jclass)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_frysk_sys_Signal_ill not implemented");
  }
  return 0;
}

JNIEXPORT jint
Java_frysk_sys_Signal_abrt (JNIEnv *env, jclass)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_frysk_sys_Signal_abrt not implemented");
  }
  return 0;
}

JNIEXPORT jint
Java_frysk_sys_Signal_fpe (JNIEnv *env, jclass)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_frysk_sys_Signal_fpe not implemented");
  }
  return 0;
}

JNIEXPORT jint
Java_frysk_sys_Signal_kill__ (JNIEnv *env, jclass)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_frysk_sys_Signal_kill__ not implemented");
  }
  return 0;
}

JNIEXPORT jint
Java_frysk_sys_Signal_segv (JNIEnv *env, jclass)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_frysk_sys_Signal_segv not implemented");
  }
  return 0;
}

JNIEXPORT jint
Java_frysk_sys_Signal_pipe (JNIEnv *env, jclass)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_frysk_sys_Signal_pipe not implemented");
  }
  return 0;
}

JNIEXPORT jint
Java_frysk_sys_Signal_alrm (JNIEnv *env, jclass)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_frysk_sys_Signal_alrm not implemented");
  }
  return 0;
}

JNIEXPORT jint
Java_frysk_sys_Signal_term (JNIEnv *env, jclass)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_frysk_sys_Signal_term not implemented");
  }
  return 0;
}

JNIEXPORT jint
Java_frysk_sys_Signal_usr1 (JNIEnv *env, jclass)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_frysk_sys_Signal_usr1 not implemented");
  }
  return 0;
}

JNIEXPORT jint
Java_frysk_sys_Signal_usr2 (JNIEnv *env, jclass)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_frysk_sys_Signal_usr2 not implemented");
  }
  return 0;
}

JNIEXPORT jint
Java_frysk_sys_Signal_chld (JNIEnv *env, jclass)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_frysk_sys_Signal_chld not implemented");
  }
  return 0;
}

JNIEXPORT jint
Java_frysk_sys_Signal_cont (JNIEnv *env, jclass)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_frysk_sys_Signal_cont not implemented");
  }
  return 0;
}

JNIEXPORT jint
Java_frysk_sys_Signal_stop (JNIEnv *env, jclass)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_frysk_sys_Signal_stop not implemented");
  }
  return 0;
}

JNIEXPORT jint
Java_frysk_sys_Signal_tstp (JNIEnv *env, jclass)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_frysk_sys_Signal_tstp not implemented");
  }
  return 0;
}

JNIEXPORT jint
Java_frysk_sys_Signal_ttin (JNIEnv *env, jclass)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_frysk_sys_Signal_ttin not implemented");
  }
  return 0;
}

JNIEXPORT jint
Java_frysk_sys_Signal_ttou (JNIEnv *env, jclass)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_frysk_sys_Signal_ttou not implemented");
  }
  return 0;
}

JNIEXPORT jint
Java_frysk_sys_Signal_bus (JNIEnv *env, jclass)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_frysk_sys_Signal_bus not implemented");
  }
  return 0;
}

JNIEXPORT jint
Java_frysk_sys_Signal_prof (JNIEnv *env, jclass)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_frysk_sys_Signal_prof not implemented");
  }
  return 0;
}

JNIEXPORT jint
Java_frysk_sys_Signal_sys (JNIEnv *env, jclass)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_frysk_sys_Signal_sys not implemented");
  }
  return 0;
}

JNIEXPORT jint
Java_frysk_sys_Signal_trap (JNIEnv *env, jclass)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_frysk_sys_Signal_trap not implemented");
  }
  return 0;
}

JNIEXPORT jint
Java_frysk_sys_Signal_urg (JNIEnv *env, jclass)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_frysk_sys_Signal_urg not implemented");
  }
  return 0;
}

JNIEXPORT jint
Java_frysk_sys_Signal_vtalrm (JNIEnv *env, jclass)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_frysk_sys_Signal_vtalrm not implemented");
  }
  return 0;
}

JNIEXPORT jint
Java_frysk_sys_Signal_xcpu (JNIEnv *env, jclass)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_frysk_sys_Signal_xcpu not implemented");
  }
  return 0;
}

JNIEXPORT jint
Java_frysk_sys_Signal_xfsz (JNIEnv *env, jclass)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_frysk_sys_Signal_xfsz not implemented");
  }
  return 0;
}

JNIEXPORT jint
Java_frysk_sys_Signal_emt (JNIEnv *env, jclass)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_frysk_sys_Signal_emt not implemented");
  }
  return 0;
}

JNIEXPORT jint
Java_frysk_sys_Signal_stkflt (JNIEnv *env, jclass)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_frysk_sys_Signal_stkflt not implemented");
  }
  return 0;
}

JNIEXPORT jint
Java_frysk_sys_Signal_io (JNIEnv *env, jclass)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_frysk_sys_Signal_io not implemented");
  }
  return 0;
}

JNIEXPORT jint
Java_frysk_sys_Signal_pwr (JNIEnv *env, jclass)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_frysk_sys_Signal_pwr not implemented");
  }
  return 0;
}

JNIEXPORT jint
Java_frysk_sys_Signal_lost (JNIEnv *env, jclass)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_frysk_sys_Signal_lost not implemented");
  }
  return 0;
}

JNIEXPORT jint
Java_frysk_sys_Signal_winch (JNIEnv *env, jclass)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_frysk_sys_Signal_winch not implemented");
  }
  return 0;
}

JNIEXPORT jint
Java_frysk_sys_Signal_unused (JNIEnv *env, jclass)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_frysk_sys_Signal_unused not implemented");
  }
  return 0;
}

JNIEXPORT jint
Java_frysk_sys_Signal_poll (JNIEnv *env, jclass)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_frysk_sys_Signal_poll not implemented");
  }
  return 0;
}

JNIEXPORT jint
Java_frysk_sys_Signal_iot (JNIEnv *env, jclass)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_frysk_sys_Signal_iot not implemented");
  }
  return 0;
}

JNIEXPORT jint
Java_frysk_sys_Signal_info (JNIEnv *env, jclass)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_frysk_sys_Signal_info not implemented");
  }
  return 0;
}

JNIEXPORT jint
Java_frysk_sys_Signal_cld (JNIEnv *env, jclass)
{
  jclass cls = env->FindClass("java/lang/RuntimeException");
  if (cls != NULL) {
    env->ThrowNew(cls, __FILE__ ":Java_frysk_sys_Signal_cld not implemented");
  }
  return 0;
}
