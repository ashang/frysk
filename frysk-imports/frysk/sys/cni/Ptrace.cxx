// This file is part of the program FRYSK.
//
// Copyright 2005, 2007, Red Hat Inc.
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

#include <pthread.h>
#include <sys/types.h>
#include <sys/ptrace.h>
#include <sys/user.h>
#include "linux.ptrace.h"
#include <errno.h>
#include <linux/unistd.h>
#include <sys/wait.h>
#include <alloca.h>
#include <unistd.h>
#include <stdio.h>
#include <stdlib.h>
#include <signal.h>

#include <gcj/cni.h>

#include <gnu/gcj/RawData.h>

#include "frysk/sys/Ptrace$PtraceRequest.h"
#include "frysk/sys/Ptrace.h"
#include "frysk/sys/Errno.h"
#include "frysk/sys/Errno$Esrch.h"
#include "frysk/sys/cni/Errno.hxx"

struct RegisterSetParams
{
  int size;
  int peekRequest;
  int pokeRequest;
};

/* There structures and constants are x86-specific.  */
static RegisterSetParams regSetParams[] =
  {
#if defined(__i386__)|| defined(__x86_64__)
   {sizeof(user_regs_struct), PTRACE_GETREGS, PTRACE_SETREGS},
   {sizeof(user_fpregs_struct), PTRACE_GETFPREGS, PTRACE_SETFPREGS},
#if defined(__i386__)
   {sizeof(user_fpxregs_struct), PTRACE_GETFPXREGS, PTRACE_SETFPXREGS},
#endif
#endif
  };

int cpid;

/**
 * If the operation involves a PTRACE_TRACEME, create a new child and
 * exec as necessary. Otherwise, perform a ptrace operation on a
 * currently running process as a superior task.
 */

void
frysk::sys::Ptrace$PtraceRequest::execute ()
{
  errno = 0;
  result = ::ptrace ((enum __ptrace_request) op, pid,
		     (void*) addr, (long) data);
  if (errno != 0)
    throwErrno (errno, "ptrace");
}

void
frysk::sys::Ptrace::attach (jint pid)
{
  request(PTRACE_ATTACH, pid, NULL, 0);
}

void
frysk::sys::Ptrace::detach (jint pid, jint sig)
{
  request(PTRACE_DETACH, pid, NULL, sig);
} 

void
frysk::sys::Ptrace::singleStep (jint pid, jint sig)
{
  request(PTRACE_SINGLESTEP, pid, NULL, sig);
} 

void
frysk::sys::Ptrace::cont (jint pid, jint sig)
{
  request(PTRACE_CONT, pid, NULL, sig);
}

void
frysk::sys::Ptrace::sysCall (jint pid, jint sig)
{
  request(PTRACE_SYSCALL, pid, NULL, sig);
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
  request(PTRACE_GETEVENTMSG, pid, NULL, (long) &msg);
  return msg;
}

jint
frysk::sys::Ptrace::registerSetSize(jint set)
{
  if (set < (jint)(sizeof(regSetParams) / sizeof(regSetParams[0])))
    return regSetParams[set].size;
  else
    return 0;
}

void
frysk::sys::Ptrace::peekRegisters(jint registerSet, jint pid, jbyteArray data)
{
  request(regSetParams[registerSet].peekRequest, pid, 0, (long)elements(data));
}

void
frysk::sys::Ptrace::pokeRegisters(jint registerSet, jint pid, jbyteArray data)
{
  request(regSetParams[registerSet].pokeRequest, pid, 0, (long)elements(data));
}

void
frysk::sys::Ptrace::setOptions (jint pid, jlong options)
{
  request(PTRACE_SETOPTIONS, pid, 0, options);
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
