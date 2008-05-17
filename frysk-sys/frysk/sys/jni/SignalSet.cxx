// This file is part of the program FRYSK.
//
// Copyright 2005, 2006, 2007, 2008, Red Hat Inc.
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

#include <signal.h>
#include <errno.h>
#include <stdio.h>

#include "jni.hxx"

#include "frysk/sys/jni/SignalSet.hxx"
#include "jnixx/exceptions.hxx"

sigset_t *
getRawSet(jnixx::env env, frysk::sys::SignalSet set) {
  return (sigset_t*) set.getRawSet(env);
}

void
frysk::sys::SignalSet::fill(jnixx::env env, jlong rawSet) {
  sigset_t *sigset = (sigset_t*) rawSet;
  ::sigfillset(sigset);
}

void
frysk::sys::SignalSet::remove(jnixx::env env, jlong rawSet, jint sig) {
  sigset_t *sigset = (sigset_t*) rawSet;
  ::sigdelset(sigset, sig);
}

void
frysk::sys::SignalSet::add(jnixx::env env, jlong rawSet, jint sig) {
  sigset_t *sigset = (sigset_t*) rawSet;
  ::sigaddset(sigset, sig);
}

bool
frysk::sys::SignalSet::contains(jnixx::env env, jlong rawSet, jint sig) {
  sigset_t *sigset = (sigset_t*) rawSet;
  return ::sigismember(sigset, sig);
}

jlong
frysk::sys::SignalSet::malloc(jnixx::env env) {
  sigset_t* sigset = new sigset_t();
  if (sigset == NULL) {
    errnoException(env, errno, "malloc");
  }
  ::sigemptyset(sigset);
  return (long)(void*)sigset;
}

void
frysk::sys::SignalSet::free(jnixx::env env, jlong rawSet) {
  sigset_t *sigset = (sigset_t*) rawSet;
  delete sigset;
}

void
frysk::sys::SignalSet::empty(jnixx::env env, jlong rawSet) {
  sigset_t *sigset = (sigset_t*) rawSet;
  ::sigemptyset(sigset);
}

void
frysk::sys::SignalSet::getPending(jnixx::env env, jlong rawSet) {
  sigset_t *sigset = (sigset_t*) rawSet;
  errno = 0;
  if (::sigpending(sigset) < 0)
    errnoException(env, errno, "sigpending");
}

void
frysk::sys::SignalSet::suspend(jnixx::env env, jlong rawSet) {
  sigset_t *sigset = (sigset_t*) rawSet;
  errno = 0;
  ::sigsuspend(sigset); // always fails with EINTR.
  if (errno != EINTR)
    errnoException(env, errno, "sigsuspend");
}



void
frysk::sys::SignalSet::blockProcMask(jnixx::env env, jlong rawSet, jlong oset) {
  sigset_t *set = (sigset_t*) rawSet;
  sigset_t* old = (sigset_t*) oset;
  errno = 0;
  if (::sigprocmask(SIG_BLOCK, set, old) < 0)
    errnoException(env, errno, "sigprocmask.SIG_BLOCK");
}

void
frysk::sys::SignalSet::unblockProcMask(jnixx::env env, jlong rawSet,
				       jlong oset) {
  sigset_t *set = (sigset_t*) rawSet;
  sigset_t* old = (sigset_t*) oset;
  errno = 0;
  if (::sigprocmask (SIG_UNBLOCK, set, old) < 0)
    errnoException(env, errno, "sigprocmask.SIG_UNBLOCK");
}

void
frysk::sys::SignalSet::setProcMask(jnixx::env env, jlong rawSet, jlong oset) {
  sigset_t *set = (sigset_t*) rawSet;
  sigset_t* old = (sigset_t*) oset;
  errno = 0;
  if (::sigprocmask (SIG_SETMASK, set, old) < 0)
    errnoException(env, errno, "sigprocmask.SIG_SETMASK");
}

void
frysk::sys::SignalSet::getProcMask(jnixx::env env, jlong rawSet) {
  sigset_t *set = (sigset_t*) rawSet;
  errno = 0;
  if (::sigprocmask (SIG_SETMASK, NULL, set) < 0)
    errnoException(env, errno, "sigprocmask.SIG_SETMASK");
}

jint
frysk::sys::SignalSet::size(jnixx::env env, jlong rawSet) {
  sigset_t *set = (sigset_t*) rawSet;
  int numSigs = 0;
  // Count the number of signals
  for (int i = 1; i < NSIG; i++) {
    if (sigismember (set, i))
      numSigs++;
  }
  return numSigs;
}
