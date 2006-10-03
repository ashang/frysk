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

#include "frysk/sys/Ptrace$PtraceThread.h"
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

static RegisterSetParams regSetParams[] =
  {{sizeof(user_regs_struct), PTRACE_GETREGS, PTRACE_SETREGS},
   {sizeof(user_fpregs_struct), PTRACE_GETFPREGS, PTRACE_SETFPREGS},
#if defined(__i386__)
   {sizeof(user_fpxregs_struct), PTRACE_GETFPXREGS, PTRACE_SETFPXREGS},
#endif
  };

int cpid;

static void
reopen (jstring file, const char *mode, FILE *stream);

/* If the operation involves a PTRACE_TRACEME, create a new child and exec as 
 * necessary. Otherwise, perform a ptrace operation on a currently running
 * process as a superior task. */
void
frysk::sys::Ptrace$PtraceThread::callPtrace ()
{
		
  if (request == PTRACE_TRACEME) /* New child creation */
    {
      result = fork ();
      if (result < 0) /* Error */
	{ 
	  perror ("Error: could not fork child process");
	  exit (EXIT_FAILURE);
	} 
      else if (result == 0) /* Child */
	{ 
      
	  setsid();
	  setpgid(0, 0);

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

	  if (in != NULL)
	    reopen (in, "r", stdin);
	  if (out != NULL)
	    reopen (out, "w", stdout);
	  if (err != NULL)
	    reopen (err, "w", stderr);
      
	  errno = 0;
	  result = ::ptrace ((enum __ptrace_request) request, \
			     pid, (void*) addr, (long) data);
	  error = errno;
	  ::execvp (argv[0], argv);
	  ::perror ("execvp");		
	}
      else
	cpid = result;
    } 
  else /* Operations on existing child or other process */
    { 
  	
      errno = 0;
      result = ::ptrace ((enum __ptrace_request) request, pid, \
      				(void*) addr, (long) data);
      error = errno;
    }
}

/* Delegates creating a new ptrace_thread, assigning data, and sending the
 * running thread to perform work */
static long
_callPtrace (int request, pid_t pid, void *addr, long data, const char *what)
{
  return frysk::sys::Ptrace::getPt () -> notifyPtraceThread \
  					(request, pid, (jlong)addr, data);
}

void
frysk::sys::Ptrace::attach (jint pid)
{
  _callPtrace(PTRACE_ATTACH, pid, NULL, 0, "ptrace.attach");
}

void
frysk::sys::Ptrace::detach (jint pid, jint sig)
{
  _callPtrace(PTRACE_DETACH, pid, NULL, sig, "ptrace.detach");
} 

void
frysk::sys::Ptrace::singleStep (jint pid, jint sig)
{
  _callPtrace(PTRACE_SINGLESTEP, pid, NULL, sig, "ptrace.singlestep");
} 

void
frysk::sys::Ptrace::cont (jint pid, jint sig)
{
  _callPtrace(PTRACE_CONT, pid, NULL, sig, "ptrace.cont");
}

void
frysk::sys::Ptrace::sysCall (jint pid, jint sig)
{
  _callPtrace(PTRACE_SYSCALL, pid, NULL, sig, "ptrace.syscall");
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
  _callPtrace(PTRACE_GETEVENTMSG, pid, NULL, (long) &msg,
	      			"ptrace.geteventmsg");
  return msg;
}

jlong
frysk::sys::Ptrace::peek(jint peekRequest, jint pid, jstring paddr)
{
  return _callPtrace((enum __ptrace_request) peekRequest, pid, (char *)paddr,
                                        0, "ptrace.peek");
}

void
frysk::sys::Ptrace::poke(jint peekRequest, jint pid, jstring paddr,
                                        jlong data)
{
  _callPtrace((enum __ptrace_request) peekRequest, pid, (char *)paddr,
                                        data, "ptrace.poke");
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
  _callPtrace(regSetParams[registerSet].peekRequest, pid, 0,
	      (long)elements(data), "ptrace.peekRegisters");
}

void
frysk::sys::Ptrace::pokeRegisters(jint registerSet, jint pid, jbyteArray data)
{
  _callPtrace(regSetParams[registerSet].pokeRequest, pid, 0,
	      (long)elements(data), "ptrace.pokeRegisters");
}

void
frysk::sys::Ptrace::setOptions (jint pid, jlong options)
{
  _callPtrace(PTRACE_SETOPTIONS, pid, 0, options, "ptrace.setoptions");
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
frysk::sys::Ptrace::child (jstring in, jstring out, jstring err,
			   		jstringArray args)
{
  
  frysk::sys::Ptrace::getPt () -> assignFileDescriptors (in, out, err, args);
  
  _callPtrace ((enum __ptrace_request) PTRACE_TRACEME, 0, 0, 0, "ptrace.traceme");
  return cpid;
}
