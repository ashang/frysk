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
#include <stdio.h>
#include <string.h>
#include <errno.h>
#include <stdlib.h>

#include "jni.hxx"

#include "jnixx/exceptions.hxx"
#include "jnixx/print.hxx"

static void throwErrno(::jnixx::env& env, int error, const char *fmt, ...)
  __attribute__((noreturn));
void
throwErrno(::jnixx::env& env, int error, const char *fmt, ...) {
  va_list ap;
  va_start(ap, fmt);
  java::lang::String message = vajprintf(env, fmt, ap);
  switch (error) {
#ifdef EBADF
  case EBADF:
    frysk::sys::Errno$Ebadf::New(env, message).Throw(env);
#endif
#ifdef ENOMEM
  case ENOMEM:
    frysk::sys::Errno$Enomem::New(env, message).Throw(env);
#endif
#ifdef EFAULT
  case EFAULT:
    frysk::sys::Errno$Efault::New(env, message).Throw(env);
#endif
#ifdef EINVAL
  case EINVAL:
    frysk::sys::Errno$Einval::New(env, message).Throw(env);
#endif
#ifdef ESRCH
  case ESRCH:
    frysk::sys::Errno$Esrch::New(env, message).Throw(env);
#endif
#ifdef ECHILD
  case ECHILD:
    frysk::sys::Errno$Echild::New(env, message).Throw(env);
#endif
#ifdef EPERM
  case EPERM:
    frysk::sys::Errno$Eperm::New(env, message).Throw(env);
#endif
#ifdef EIO
  case EIO:
    frysk::sys::Errno$Eio::New(env, message).Throw(env);
#endif
#ifdef ENOENT
  case ENOENT:
    frysk::sys::Errno$Enoent::New(env, message).Throw(env);
#endif
  default:
    frysk::sys::Errno::New(env, error, message).Throw(env);
  }
  va_end(ap);
}

void
errnoException(::jnixx::env& env, int error, const char *prefix) {
  // Hack; for moment just throw something.
  throwErrno(env, errno, "%s: %s", prefix, strerror(error));
}

void
errnoException(::jnixx::env& env, int error, const char *prefix,
	       const char *fmt, ...) {
  va_list ap;
  va_start(ap, fmt);
  char message[256];
  if (::snprintf(message, sizeof(message), fmt, ap) < 0) {
    errnoException(env, errno, "malloc");
  }
  throwErrno(env, errno, "%s: %s (%s)", prefix, strerror(error), message);
  va_end(ap);
}

void
runtimeException(::jnixx::env& env, const char *fmt, ...) {
  jclass cls = env.FindClass("java/lang/RuntimeException");
  va_list ap;
  va_start(ap, fmt);
  char *msg = NULL;
  int status = ::vasprintf(&msg, fmt, ap);
  va_end(ap);
  if (status < 0) {
    fprintf(stderr, "runtimeException: vasprintf failed: %s",
	    ::strerror(errno));
    env.ThrowNew(cls, "runtimeException: vasprintf failed");
  }
  try {
    env.ThrowNew(cls, msg);
  } catch (java::lang::Throwable e) {
    // XXX: Work around lack of finally by catchching, then
    // re-throwing, the exception
    ::free(msg);
    throw e;
  }
}
