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

#include <sys/types.h>
#include <sys/ptrace.h>
#include "linux.ptrace.h"
#include <errno.h>
#include <alloca.h>
#include <unistd.h>
#include <stdio.h>
#include <stdlib.h>

#include <gcj/cni.h>

#include "frysk/sys/Ptrace.h"
#include "frysk/sys/Errno.h"
#include "frysk/sys/Errno$Esrch.h"
#include "frysk/sys/cni/Errno.hxx"

static long
callPtrace (int request, pid_t pid, void *addr, long data, const char *what)
{
  errno = 0;
  long result = ::ptrace ((enum __ptrace_request) request, pid, addr, data);
  if (errno != 0)
    throwErrno (errno, what);
  return result;
}

void
frysk::sys::Ptrace::attach (jint pid)
{
  callPtrace (PTRACE_ATTACH, pid, NULL, 0, "ptrace.attach");
}

void
frysk::sys::Ptrace::detach (jint pid, jint sig)
{
  callPtrace (PTRACE_DETACH, pid, NULL, sig, "ptrace.detach");
}

void
frysk::sys::Ptrace::singleStep (jint pid, jint sig)
{
  callPtrace (PTRACE_SINGLESTEP, pid, NULL, sig, "ptrace.singlestep");
}

void
frysk::sys::Ptrace::cont (jint pid, jint sig)
{
  callPtrace (PTRACE_CONT, pid, NULL, sig, "ptrace.cont");
}

void
frysk::sys::Ptrace::sysCall (jint pid, jint sig)
{
  callPtrace (PTRACE_SYSCALL, pid, NULL, sig, "ptrace.syscall");
}

jlong
frysk::sys::Ptrace::getEventMsg (jint pid)
{
  /* Note: PTRACE_GETEVENTMSG ends up calling the function
     kernel/ptrace.c: ptrace_request() and that uses put_user to store
     child->ptrace_message write sizeof(ptrace_message) bytes into the
     MESSAGE parameter.  include/linux/sched.h declares ptrace_message
     as a long.  */
  long msg;
  callPtrace (PTRACE_GETEVENTMSG, pid, NULL, (long) &msg,
	      "ptrace.geteventmsg");
  return msg;
}


void
frysk::sys::Ptrace::setOptions (jint pid, jlong options)
{
  callPtrace (PTRACE_SETOPTIONS, pid, 0, options, "ptrace.setoptions");
}

jlong
frysk::sys::Ptrace::optionTraceClone ()
{
  return PTRACE_O_TRACECLONE;
}
jlong
frysk::sys::Ptrace::optionTraceFork ()
{
  return PTRACE_O_TRACEFORK;
}
jlong
frysk::sys::Ptrace::optionTraceExit ()
{
  return PTRACE_O_TRACEEXIT;
}
jlong
frysk::sys::Ptrace::optionTraceSysgood ()
{
  return PTRACE_O_TRACESYSGOOD;
}
jlong
frysk::sys::Ptrace::optionTraceExec ()
{
  return PTRACE_O_TRACEEXEC;
}


static void
reopen (jstring file, const char *mode, FILE *stream)
{
  int len = JvGetStringUTFLength (file);
  char *fileName = (char *) alloca (len + 1);
  JvGetStringUTFRegion (file, 0, file->length (), fileName);
  fileName[len] = '\0';
  errno = 0;
  ::freopen (fileName, mode, stream);
  if (errno != 0) {
    ::perror ("freopen");
    ::exit (errno);
  }
}

jint
frysk::sys::Ptrace::child (jstring in, jstring out,
						jstring err,
						jstringArray args)
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
  pid_t pid = fork ();
  if (pid == 0) {
    // Child
    if (in != NULL)
      reopen (in, "r", stdin);
    if (out != NULL)
      reopen (out, "w", stdout);
    if (err != NULL)
      reopen (err, "w", stderr);
    errno = 0;
    ::ptrace ((enum __ptrace_request) PTRACE_TRACEME, 0, 0, 0);
    if (errno != 0) {
      ::perror ("ptrace.traceme");
      ::_exit (errno);
    }
    ::execvp (argv[0], argv);
    ::perror ("execvp");
    ::_exit (errno);
  }

  return pid;
}
