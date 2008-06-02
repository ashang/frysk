// This file is part of the program FRYSK.
//
// Copyright 2005, 2007, 2008, Red Hat Inc.
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

#include <stdio.h>
#include <errno.h>
#include <unistd.h>
#include <sys/ptrace.h>
#include <sys/types.h>
#include <sys/wait.h>

#include "jni.hxx"

#include "jnixx/elements.hxx"
#include "jnixx/exceptions.hxx"

#include "frysk/sys/jni/Fork.hxx"

using namespace java::lang;

/**
 * Spawn a child, return the PID or throw the error.
 */
int
spawn(jnixx::env env, tracing trace, redirect& redirection, exec& execute) {
  if (trace == DAEMON) {
    // Do a vfork(), fork(), exec() which lets the top level process
    // capture the middle level fork()'s return value in a volatile.
    volatile int pid = -1;
    register int v;
    errno = 0;
    v = vfork ();
    switch (v) {
    case 0:
      // This is executed by the child with the parent blocked, the
      // final process id ends up in PID.
      pid = ::spawn(env, CHILD, redirection, execute);
      _exit (0);
    case -1:
      // This is executed after a vfork barfs.
      errnoException(env, errno, "vfork");
    default:
      // This is executed after the child has set PID with a FORK and
      // then exited (which helps guarentee that the waitpid, below,
      // doesn't block.
      if (pid < 0) {
	errnoException(env, errno, "vfork/fork");
      }
      // Consume the middle players wait.
      int status;
      errno = 0;
      if (waitpid(v, &status, 0) < 0) {
	errnoException(env, errno, "waitpid");
      }
      return pid;
    }
  }

  // Fork/exec
  errno = 0;
  pid_t pid = fork ();
  switch (pid) {
  case -1: // Fork failed.
    errnoException(env, errno, "fork");
  default: // Parent
    return pid;
  case 0: // Child
    // Scrub the signal mask.
    sigset_t mask;
    sigfillset(&mask);
    ::sigprocmask(SIG_UNBLOCK, &mask, NULL);
    // Redirect stdio.
    redirection.reopen();
    switch (trace) {
    case PTRACE:
      errno = 0;
      ::ptrace((enum __ptrace_request) PTRACE_TRACEME, 0, 0, 0);
      if (errno != 0) {
	::perror ("ptrace.traceme");
	::_exit(errno);
      }
      break;
    case UTRACE:
      fprintf(stderr, "\n\n>>>>> in spawn(...utrace)\n\n");
      break;
    case CHILD:
      break;
    case DAEMON:
      break;
    }
    execute.execute();
    ::_exit(errno);
  }
}

class redirect_stdio : public redirect {
private:
  void reopen(const char* file, const char* mode, FILE *stream) {
    if (file == NULL)
      return;
    errno = 0;
    ::freopen(file, mode, stream);
    if (errno != 0) {
      // Should not happen!
      ::perror("freopen");
      ::_exit(errno);
    }
  }
  jstringUTFChars in;
  jstringUTFChars out;
  jstringUTFChars err;
  const char* inElements;
  const char* outElements;
  const char* errElements;
public:
  redirect_stdio(jnixx::env env, String in, String out, String err) {
    this->in = jstringUTFChars(env, in);
    this->out = jstringUTFChars(env, out);
    this->err = jstringUTFChars(env, err);
    // allocate memory for the arrays before the fork.
    inElements = this->in.elements();
    outElements = this->out.elements();
    errElements = this->err.elements();
  }
  void reopen() {
    reopen(inElements, "r", stdin);
    reopen(outElements, "w", stdout);
    reopen(errElements, "w", stderr);
  }
  ~redirect_stdio() {
    in.release();
    out.release();
    err.release();
  }
};

/**
 * Convert convert to native and then spawn.
 */
static int
spawn(jnixx::env env, String exe,
      String in, String out, String err,
      jnixx::array<String> args,
      jnixx::array<String> environ, tracing trace) {
  redirect_stdio io = redirect_stdio(env, in, out, err);
  exec_program program = exec_program(env, exe, args, environ);
  return ::spawn(env, trace, io, program);
}

jint
frysk::sys::Fork::spawn(jnixx::env env, String exe,
			String in, String out, String err,
			jnixx::array<String> args,
			jnixx::array<String> environ) {
  return ::spawn(env, exe, in, out, err, args, environ, CHILD);
}

jint
frysk::sys::Fork::ptrace(jnixx::env env, String exe,
			 String in, String out, String err,
			 jnixx::array<String> args,
			 jnixx::array<String> environ) {
  return ::spawn(env, exe, in, out, err, args, environ, PTRACE);
}

jint
frysk::sys::Fork::utrace(jnixx::env env, String exe,
			 String in, String out, String err,
			 jnixx::array<String> args,
			 jnixx::array<String> environ) {
  return ::spawn(env, exe, in, out, err, args, environ, UTRACE);
}

jint
frysk::sys::Fork::daemon(jnixx::env env, String exe,
			 String in, String out, String err,
			 jnixx::array<String> args,
			 jnixx::array<String> environ) {
  return ::spawn(env, exe, in, out, err, args, environ, DAEMON);
}
