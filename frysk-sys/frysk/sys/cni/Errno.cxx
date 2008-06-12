// This file is part of the program FRYSK.
//
// Copyright 2005, 2006, 2007, 2008, Red Hat Inc.
// Copyright 2007 Oracle Corporation.
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

#include <errno.h>
#include <stdio.h>
#include <stdlib.h>
#include <stdarg.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>

#include <gcj/cni.h>

#include <java/lang/System.h>
#include <java/lang/Thread.h>
#include <java/lang/ArrayIndexOutOfBoundsException.h>

#include "frysk/UserException.h"
#include "frysk/sys/Errno.h"
#include "frysk/sys/Errno$Ebadf.h"
#include "frysk/sys/Errno$Enomem.h"
#include "frysk/sys/Errno$Efault.h"
#include "frysk/sys/Errno$Einval.h"
#include "frysk/sys/Errno$Echild.h"
#include "frysk/sys/Errno$Esrch.h"
#include "frysk/sys/Errno$Eperm.h"
#include "frysk/sys/Errno$Enoent.h"
#include "frysk/sys/Errno$Eio.h"
#include "frysk/sys/GarbageCollect.h"
#include "frysk/sys/cni/Errno.hxx"

/**
 * Like asprintf, only it returns a Java string.
 */
jstring
ajprintf (const char *fmt, ...)
{
  va_list ap;
  va_start (ap, fmt);
  jstring jmessage = vajprintf(fmt, ap);
  va_end (ap);
  return jmessage;
}

/**
 * Like vasprintf, only it returns a Java string.
 */
jstring
vajprintf (const char *fmt, va_list ap)
{
  char* message = NULL;
  if (::vasprintf (&message, fmt, ap) < 0)
    throwRuntimeException("vasprintf failed");      
  jstring jmessage = JvNewStringUTF (message);  
  ::free (message);  
  if (jmessage == NULL)
    throwRuntimeException("JvNewStringUTF failed in vajprintf");  
  return jmessage;
}

/**
 * Create, and throw, an Errno.
 */
static void throwErrno (int err, jstring jmessage) __attribute__ ((noreturn));
void
throwErrno (int err, jstring jmessage)
{
  switch (err) {
#ifdef EBADF
  case EBADF:
    throw new frysk::sys::Errno$Ebadf (jmessage);
#endif
#ifdef ENOMEM
  case ENOMEM:
    throw new frysk::sys::Errno$Enomem (jmessage);
#endif
#ifdef EFAULT
  case EFAULT:
    throw new frysk::sys::Errno$Efault (jmessage);
#endif
#ifdef EINVAL
  case EINVAL:
    throw new frysk::sys::Errno$Einval (jmessage);
#endif
#ifdef ESRCH
  case ESRCH:
    throw new frysk::sys::Errno$Esrch (jmessage);
#endif
#ifdef ECHILD
  case ECHILD:
    throw new frysk::sys::Errno$Echild (jmessage);
#endif
#ifdef EPERM
  case EPERM:
    throw new frysk::sys::Errno$Eperm (jmessage);
#endif
#ifdef EIO
  case EIO:
    throw new frysk::sys::Errno$Eio (jmessage);
#endif
#ifdef ENOENT
  case ENOENT:
    throw new frysk::sys::Errno$Enoent (jmessage);
#endif
  default:
    throw new frysk::sys::Errno(err, jmessage);
  }
}

void
throwErrno (int err, const char *prefix, const char *suffix, ...)
{
  va_list ap;
  va_start (ap, suffix);
  char *message = NULL;
  vasprintf(&message, suffix, ap);
  va_end (ap);
  jstring jmessage = ajprintf("%s: %s (%s)", prefix, strerror (err), message);
  free(message);
  throwErrno (err, jmessage);
}

void
throwErrno (int err, const char *prefix)
{
  throwErrno (err, ajprintf ("%s: %s", prefix, strerror (err)));
}

void
throwRuntimeException (const char *message)
{
  throw new java::lang::RuntimeException
    (JvNewStringLatin1 (message, strlen (message)));
}

void
throwRuntimeException (const char *message, const char *suffix, int val)
{
  throw new java::lang::RuntimeException
    (ajprintf ("%s (%s %d)", message, suffix, val));
}

int
tryOpen (const char *file, int flags, int mode)
{
  int fd;
  
  while (1)
    {
      errno = 0;
      fd = ::open (file, flags, mode);
      
      if (fd >= 0)
	return fd;
      
      int err = errno;
      switch (err)
	{
	case EMFILE:
	  if (!frysk::sys::GarbageCollect::run())
	    throwErrno(err, "open");
	  continue;
	  
	default:
	  throwErrno (err, "open", "file %s", file);
	  return 0;
	}
    }
}

int
tryOpen (const char *file, int flags)
{
  return tryOpen (file, flags, 0);
}

// Returns the total size required by ARGS an a unix argv[] array.
size_t
sizeof_argv (jstringArray args)
{
  size_t required = 0;
  // Convert args into argv, argc.
  int argc = JvGetArrayLength (args);
  required += (argc + 1) * sizeof (char*);
  for (int i = 0; i < argc; i++) {
    jstring arg = elements (args)[i];
    int len = JvGetStringUTFLength (arg);
    required += len + 1;
  }
  return required;
}

// Converts ARGS into an argv[] array storing it into BUF, which is
// returned.
char**
fill_argv (void *p, jstringArray args)
{
  char* buf = (char*) p;
  // Convert args into argv, argc.
  int argc = JvGetArrayLength (args);
  char** argv = (char**) buf;
  buf += ((argc + 1) * sizeof (char*));
  for (int i = 0; i < argc; i++) {
    jstring arg = elements (args)[i];
    int len = JvGetStringUTFLength (arg);
    argv[i] = (char*) buf;
    buf += (len + 1);
    JvGetStringUTFRegion (arg, 0, arg->length (), argv[i]);
    argv[i][len] = '\0';
  }
  argv[argc] = 0;
  return argv;
}

// Return the number of bytes needed to store the String with a
// trailing null.  If ARG is NULL, 1 is returned, thus ensuring that a
// zero size buffer is never allocated.
size_t
sizeof_string (jstring s)
{
  if (s != NULL)
    return JvGetStringUTFLength (s) + 1;
  else
    return 1;
}

// Converts S into a char *string stored into BUF, which is returned.
// If S is NULL, NULL is returned.
char*
fill_string (void *p, jstring s)
{
  char *c = (char*)p;
  if (s != NULL) {
    JvGetStringUTFRegion (s, 0, s->length (), c);
    c[JvGetStringUTFLength (s)] = '\0';
    return c;
  }
  else
    return NULL;
}

void
verifyBounds (jbyteArray bytes, jint start, jint length)
{
  if (start < 0)
    throw new java::lang::ArrayIndexOutOfBoundsException ();
  if (length < 0)
    throw new java::lang::ArrayIndexOutOfBoundsException ();
  if (start + length < 0)
    throw new java::lang::ArrayIndexOutOfBoundsException ();
  if (start + length > bytes->length)
    throw new java::lang::ArrayIndexOutOfBoundsException ();
}
