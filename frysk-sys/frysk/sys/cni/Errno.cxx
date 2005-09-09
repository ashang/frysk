// This file is part of FRYSK.
//
// Copyright 2005, Red Hat Inc.
//
// FRYSK is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
//
// FRYSK is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with FRYSK; if not, write to the Free Software
// Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA

#include <errno.h>
#include <stdio.h>
#include <stdlib.h>

#include <gcj/cni.h>

#include "frysk/sys/Errno.h"
#include "frysk/sys/Errno$Ebadf.h"
#include "frysk/sys/Errno$Enomem.h"
#include "frysk/sys/Errno$Efault.h"
#include "frysk/sys/Errno$Einval.h"
#include "frysk/sys/Errno$Echild.h"
#include "frysk/sys/Errno$Esrch.h"
#include "frysk/sys/cni/Errno.hxx"

void
throwErrno (int err, const char *prefix)
{
  jstring jmessage;
  {
    char* message;
    if (::asprintf (&message, "%s: %s", prefix, strerror (err)) < 0) {
      throw new frysk::sys::Errno ();
    }
    jmessage = JvNewStringLatin1 (message, strlen (message));
    ::free (message);
  }

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
throwException (const char *message)
{
  throw new java::lang::RuntimeException
    (JvNewStringLatin1 (message, strlen (message)));
}
