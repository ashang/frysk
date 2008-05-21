// This file is part of the program FRYSK.
//
// Copyright 2007, 2008, Red Hat Inc.
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

#include <sys/types.h>
#include <unistd.h>
#include <termios.h>
#include <errno.h>

#include "jni.hxx"

#include "jnixx/exceptions.hxx"

using namespace java::lang;
using namespace frysk::sys::termios;

jlong
Termios::malloc(jnixx::env env) {
  return (jlong) (long) new struct termios();
}

void
Termios::free(jnixx::env env, jlong t) {
  delete (struct termios*)(void*)(long) t;
}

void
Termios::get(jnixx::env env, jlong termios, jint fd) {
  if (::tcgetattr (fd, (struct termios*) termios) < 0)
    errnoException(env, errno, "tcsetattr", "fd %d", (int)fd);
}

void
Termios::set(jnixx::env env, jlong termios, jint fd, Action act) {
  int action;
  if (act == Action::GetNOW(env))
    action = TCSANOW;
  else if (act == Action::GetDRAIN(env))
    action = TCSADRAIN;
  else if (act == Action::GetFLUSH(env))
    action = TCSAFLUSH;
  else
    runtimeException(env, "Unknown Termios.Action");
  errno = 0;
  if (::tcsetattr (fd, action, (struct termios*) termios) < 0)
    errnoException(env, errno, "tcsetattr", "fd %d", (int)fd);
}

void
Termios::setRaw(jnixx::env env, jlong termios) {
  ::cfmakeraw((struct termios*)termios);
}

void
Termios::sendBreak(jnixx::env env, jint fd, jint duration) {
  if (::tcsendbreak(fd, duration) < 0)
    errnoException(env, errno, "tcsendbreak", "fd %d", (int)fd);
}

void
Termios::drain(jnixx::env env, jint fd) {
  if (::tcdrain(fd) < 0)
    errnoException(env, errno, "tcdrain", "fd %d", (int)fd);
}
