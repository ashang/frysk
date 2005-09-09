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

#include <signal.h>
#include <sys/time.h>
#include <errno.h>

#include <gcj/cni.h>

#include "frysk/sys/Itimer.h"
#include "frysk/sys/cni/Errno.hxx"

struct timeval
timeval (jlong milliseconds)
{
  struct timeval val;
  val.tv_sec = milliseconds / 1000;
  val.tv_usec = (milliseconds % 1000) * 1000;
  return val;
}


void
setItimer (int which, jlong interval, jlong value)
{
  struct itimerval itimer;
  itimer.it_interval = timeval (interval);
  itimer.it_value = timeval (value);
  errno = 0;
  if (::setitimer (which, &itimer, NULL) < 0)
    throwErrno (errno, "setitimer");
}

jint
frysk::sys::Itimer::real (jlong interval, jlong value)
{
  setItimer (ITIMER_REAL, interval, value);
  return SIGALRM;
}
