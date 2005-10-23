// This file is part of the program FRYSK.
//
// Copyright 2005, Red Hat Inc.
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

#include <gcj/cni.h>

#include "frysk/sys/Errno.h"
#include "frysk/sys/Errno$Ebadf.h"
#include "frysk/sys/Errno$Enomem.h"
#include "frysk/sys/Errno$Efault.h"
#include "frysk/sys/Errno$Einval.h"
#include "frysk/sys/Errno$Echild.h"
#include "frysk/sys/Errno$Esrch.h"
#include "frysk/sys/cni/Errno.hxx"

/**
 * Like vasprintf, only it returns a Java string.
 */
static jstring vajprintf (const char *fmt, ...)
  __attribute__ ((format (printf, 1, 2)));
static jstring
vajprintf (const char *fmt, ...)
{
  jstring jmessage;
  char* message = NULL;
  va_list ap;
  va_start (ap, fmt);
  if (::vasprintf (&message, fmt, ap) < 0)
    throw new frysk::sys::Errno ();
  va_end (ap);
  jmessage = JvNewStringLatin1 (message, strlen (message));
  ::free (message);
  return jmessage;
}

/**
 * Create, and throw, an Errno.
 */
static void
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
  default:
    throw new frysk::sys::Errno (jmessage);
  }
}

void
throwErrno (int err, const char *prefix, const char *suffix)
{
  throwErrno (err, vajprintf ("%s: %s (%s)", prefix, strerror (err), suffix));
}

void
throwErrno (int err, const char *prefix, const char *suffix, int val)
{
  throwErrno (err, vajprintf ("%s: %s (%s %d)", prefix, strerror (err),
			      suffix, val));

}

void
throwErrno (int err, const char *prefix)
{
  throwErrno (err, vajprintf ("%s: %s", prefix, strerror (err)));
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
    (vajprintf ("%s (%s %d)", message, suffix, val));
}
