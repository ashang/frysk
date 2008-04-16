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

#include <stdarg.h>
#include <stdlib.h>

#include <jni.h>

#include "frysk/jni/print.hxx"
#include "frysk/jni/members.hxx"
#include "frysk/rsl/jni/Log.hxx"

static bool
logging(JNIEnv* env, jobject logger) {
  static jmethodID loggingID = NULL;
  if (!getMethodID(&loggingID, env, logger, "logging", "()B"))
    return false;
  jboolean result = env->CallBooleanMethod(logger, loggingID);
  if (env->ExceptionCheck())
    return false;
  return result;
}

static void
log(JNIEnv* env, jobject logger, jstring msg) {
  static jmethodID logID = NULL;
  if (!getMethodID(&logID, env, logger, "log", "(Ljava/lang/String;)V"))
    return;
  env->CallVoidMethod(logger, logID, msg);
}

void
logf(JNIEnv *env, jobject logger, const char* format, ...) {
  if (!logging(env, logger))
    return;
  va_list ap;
  va_start(ap, format);
  jstring message = vajprintf(env, format, ap);
  if (message != NULL)
    log(env, logger, message);
  va_end(ap);
}

void
logf(JNIEnv *env, jobject logger, jobject object, const char* format, ...) {
  if (!logging(env, logger))
    return;
  va_list ap;
  va_start(ap, format);
  jstring message = vajprintf(env, format, ap);
  if (message != NULL)
    log(env, logger, message);
  va_end(ap);
}

void
log(JNIEnv *env, jobject logger, const char* p1, jobject p2) {
  if (!logging(env, logger))
    return;
  static jmethodID logID = NULL;
  if (!getMethodID(&logID, env, logger, "log",
		   "(Ljava/lang/String;Ljava/lang/Object;)V"))
    return;
  env->CallVoidMethod(logger, logID, env->NewStringUTF(p1), p2);
}

void
log(JNIEnv *env, jobject logger, jobject self, const char* p1, jobject p2) {
  if (!logging(env, logger))
    return;
  static jmethodID logID = NULL;
  if (!getMethodID(&logID, env, logger, "log",
		   "(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/Object;)V"))
    return;
  env->CallVoidMethod(logger, logID, self, env->NewStringUTF(p1), p2);
}
