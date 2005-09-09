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

#include <stdio.h>
#include <alloca.h>
#include <errno.h>
#include <unistd.h>
#include <sys/ptrace.h>
#include <sys/types.h>
#include <sys/wait.h>

#include <gcj/cni.h>

#include "frysk/sys/Fork.h"
#include "frysk/sys/cni/Errno.hxx"

static void
reopen (jstring file, const char *mode, FILE *stream)
{
  if (file == NULL)
    return;
  int len = JvGetStringUTFLength (file);
  char *fileName = (char *) alloca (len + 1);
  JvGetStringUTFRegion (file, 0, file->length (), fileName);
  fileName[len] = '\0';
  errno = 0;
  ::freopen (fileName, mode, stream);
  if (errno != 0) {
    ::perror ("freopen");
    ::_exit (errno);
  }
}

jint
spawn (jstring in, jstring out, jstring err, jstringArray args,
       bool ptraceIt)
{
  // Convert args into argv, argc.
  int argc = JvGetArrayLength (args);
  char **argv = (char **) alloca ((argc + 1) * sizeof (void*));
  for (int i = 0; i < argc; i++) {
    jstring arg = elements (args)[i];
    int len = JvGetStringUTFLength (arg);
    argv[i] = (char *) alloca (len + 1);
    JvGetStringUTFRegion (arg, 0, arg->length (), argv[i]);
    argv[i][len] = '\0';
  }
  argv[argc] = 0;

  // Fork/exec
  errno = 0;
  pid_t pid = fork ();
  switch (pid) {
  case -1:
    // Fork failed.
    throwErrno (errno, "fork");
  case 0:
    // Child
    reopen (in, "r", stdin);
    reopen (out, "w", stdout);
    reopen (err, "w", stderr);
    if (ptraceIt) {
      errno = 0;
      ::ptrace ((enum __ptrace_request) PTRACE_TRACEME, 0, 0, 0);
      if (errno != 0) {
	::perror ("ptrace.traceme");
	::_exit (errno);
      }
    }
    ::execvp (argv[0], argv);
    // This should not happen.
    ::perror ("execvp");
    ::_exit (errno);
  default:
    return pid;
  }
}

jint
frysk::sys::Fork::ptrace (jstring in, jstring out,
					       jstring err, jstringArray args)
{
  return spawn (in, out, err, args, true);
}

jint
frysk::sys::Fork::exec (jstring in, jstring out,
					     jstring err, jstringArray args)
{
  return spawn (in, out, err, args, false);
}


jint
frysk::sys::Fork::daemon (jstring in, jstring out,
					       jstring err, jstringArray args)
{
  volatile int pid = -1;
  register int v;
  errno = 0;
  v = vfork ();

  // This is executed by the child with the parent blocked, the final
  // process id ends up in PID.

  if (v == 0) {
    pid = spawn (in, out, err, args, false);
    _exit (0);
  }

  // This is executed after the child has exited (which helps
  // guarentee that the waitpid, below, doesn't block.

  if (v < 0)
    throwErrno (errno, "vfork");
  if (pid < 0)
    throwErrno (errno, "fork");
  
  // Consume the middle players wait.
  int status;
  errno = 0;
  if (waitpid (v, &status, 0) < 0)
    throwErrno (errno, "waitpid");

  // printf ("v %d pid %d\n", v, pid);

  return pid;
}
