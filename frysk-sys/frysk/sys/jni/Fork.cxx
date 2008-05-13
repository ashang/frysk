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

#include <jni.hxx>

#include "frysk/jnixx/chars.hxx"
#include "frysk/jnixx/exceptions.hxx"

using namespace java::lang;

enum tracing {
  DAEMON,
  NO_TRACE,
  PTRACE,
  UTRACE,
};

static void
reopen(const char* file, const char* mode, FILE *stream) {
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

/**
 * Spawn a child, return the PID or the -ERROR.
 */
static int
spawn(const char* exePath,
      const char* inPath, const char* outPath, const char* errPath,
      int argc, char** argv, char** environ, tracing trace) {

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
      pid = ::spawn(exePath, inPath, outPath, errPath, argc, argv, 0, NO_TRACE);
      _exit (0);
    case -1:
      // This is executed after a vfork barfs.
      return -errno;
    default:
      // This is executed after the child has set PID with a FORK and
      // then exited (which helps guarentee that the waitpid, below,
      // doesn't block.
      if (pid < 0)
	return -errno;
      // Consume the middle players wait.
      int status;
      errno = 0;
      if (waitpid (v, &status, 0) < 0)
	return -errno;
      return pid;
    }
  }

  // Fork/exec
  errno = 0;
  pid_t pid = fork ();
  switch (pid) {
  case -1: // Fork failed.
    return -errno;
  default: // Parent
    return pid;
  case 0: // Child
    // Scrub the signal mask.
    sigset_t mask;
    sigfillset(&mask);
    ::sigprocmask(SIG_UNBLOCK, &mask, NULL);
    // Redirect stdio.
    reopen(inPath, "r", stdin);
    reopen(outPath, "w", stdout);
    reopen(errPath, "w", stderr);
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
    case NO_TRACE:
      break;
    case DAEMON:
      break;
    }
    if (environ != NULL) {
      ::execve(exePath, argv, environ);
    } else
      ::execv(exePath, argv);
    // This should not happen.
    ::perror("execvp");
    ::_exit (errno);
  }
}

/**
 * Convert convert to native and then spawn.
 */
static int
spawn(jnixx::env env, java::io::File exe,
      String in, String out, String err,
      ::jnixx::array<String> args, jlong environ, tracing trace) {
  String exeFile = exe.getPath(env);
  StringChars exePath = StringChars(env, exeFile);
  StringChars inPath = StringChars(env, in);
  StringChars outPath = StringChars(env, out);
  StringChars errPath = StringChars(env, err);
  int argc = args.GetLength(env);
  StringArrayChars argv = StringArrayChars(env, args);
  int pid = ::spawn(exePath.p, inPath.p, outPath.p, errPath.p,
		    argc, argv.p, (char**)environ, trace);
  argv.free();
  exePath.free();
  inPath.free();
  outPath.free();
  errPath.free();
  if (pid < 0) {
    switch (trace) {
    case NO_TRACE:
      errnoException(env, -pid, "fork/exec");
    case DAEMON:
      errnoException(env, -pid, "vfork/wait");
    case PTRACE:
      errnoException(env, -pid, "fork/ptrace/exec");
    case UTRACE:
      errnoException(env, -pid, "utrace");
    }
  }
  return pid;
}

jint
frysk::sys::Fork::spawn(::jnixx::env env, java::io::File exe,
			String in, String out, String err,
			::jnixx::array<String> args, jlong environ) {
  return ::spawn(env, exe, in, out, err, args, environ, NO_TRACE);
}

jint
frysk::sys::Fork::ptrace(::jnixx::env env, java::io::File exe,
			 String in, String out, String err,
			 ::jnixx::array<String> args, jlong environ) {
  return ::spawn(env, exe, in, out, err, args, environ, PTRACE);
}

jint
frysk::sys::Fork::utrace(::jnixx::env env, java::io::File exe,
			 String in, String out, String err,
			 ::jnixx::array<String> args, jlong environ) {
  return ::spawn(env, exe, in, out, err, args, environ, UTRACE);
}

jint
frysk::sys::Fork::daemon (::jnixx::env env, java::io::File exe,
			  String in, String out, String err,
			  ::jnixx::array<String> args,
			  jlong environ) {
  return ::spawn(env, exe, in, out, err, args, environ, DAEMON);
}
