// This file is part of the program FRYSK.
//
// Copyright 2005, 2006, Red Hat Inc.
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

#include <gcj/cni.h>
#include <gnu/gcj/RawDataManaged.h>

#include "frysk/sys/SignalSet.h"
#include "frysk/sys/Sig.h"
#include "frysk/sys/cni/SignalSet.hxx"
#include "frysk/sys/cni/Errno.hxx"

sigset_t *
getRawSet (frysk::sys::SignalSet* set)
{
  return (sigset_t*) set->getRawSet ();
}

frysk::sys::SignalSet*
frysk::sys::SignalSet::fill ()
{
  sigset_t *sigset = (sigset_t*) rawSet;
  ::sigfillset (sigset);
  return this;
}

frysk::sys::SignalSet*
frysk::sys::SignalSet::remove (frysk::sys::Sig* sig)
{
  sigset_t *sigset = (sigset_t*) rawSet;
  ::sigdelset (sigset, sig->hashCode ());
  return this;
}

frysk::sys::SignalSet*
frysk::sys::SignalSet::add (frysk::sys::Sig* sig)
{
  sigset_t *sigset = (sigset_t*) rawSet;
  ::sigaddset (sigset, sig->hashCode ());
  return this;
}

jboolean
frysk::sys::SignalSet::contains (frysk::sys::Sig* sig)
{
  sigset_t *sigset = (sigset_t*) rawSet;
  return ::sigismember (sigset, sig->hashCode ());
}

gnu::gcj::RawDataManaged *
frysk::sys::SignalSet::newRawSet ()
{
  sigset_t* sigset = (sigset_t*) JvAllocBytes (sizeof (sigset_t));
  ::sigemptyset (sigset);
  return (gnu::gcj::RawDataManaged*) sigset;
}

frysk::sys::SignalSet*
frysk::sys::SignalSet::empty ()
{
  sigset_t *sigset = (sigset_t*) rawSet;
  ::sigemptyset (sigset);
  return this;
}

frysk::sys::SignalSet*
frysk::sys::SignalSet::getPending ()
{
  sigset_t *sigset = (sigset_t*) rawSet;
  errno = 0;
  if (::sigpending (sigset) < 0)
    throwErrno (errno, "sigpending");
  return this;
}

frysk::sys::SignalSet*
frysk::sys::SignalSet::suspend()
{
  sigset_t *sigset = (sigset_t*) rawSet;
  errno = 0;
  ::sigsuspend (sigset); // always fails with EINTR.
  if (errno != EINTR)
    throwErrno (errno, "sigsuspend");
  return this;
}



frysk::sys::SignalSet*
frysk::sys::SignalSet::blockProcMask (frysk::sys::SignalSet* oset)
{
  sigset_t *set = (sigset_t*) rawSet;
  sigset_t* old = (sigset_t*) (oset == NULL ? NULL : oset->rawSet);
  errno = 0;
  if (::sigprocmask (SIG_BLOCK, set, old) < 0)
    throwErrno (errno, "sigprocmask.SIG_BLOCK");
  return this;
}

frysk::sys::SignalSet*
frysk::sys::SignalSet::unblockProcMask (frysk::sys::SignalSet* oset)
{
  sigset_t *set = (sigset_t*) rawSet;
  sigset_t* old = (sigset_t*) (oset == NULL ? NULL : oset->rawSet);
  errno = 0;
  if (::sigprocmask (SIG_UNBLOCK, set, old) < 0)
    throwErrno (errno, "sigprocmask.SIG_UNBLOCK");
  return this;
}

frysk::sys::SignalSet*
frysk::sys::SignalSet::setProcMask (frysk::sys::SignalSet* oset)
{
  sigset_t *set = (sigset_t*) rawSet;
  sigset_t* old = (sigset_t*) (oset == NULL ? NULL : oset->rawSet);
  errno = 0;
  if (::sigprocmask (SIG_SETMASK, set, old) < 0)
    throwErrno (errno, "sigprocmask.SIG_SETMASK");
  return this;
}

frysk::sys::SignalSet*
frysk::sys::SignalSet::getProcMask ()
{
  sigset_t *set = (sigset_t*) rawSet;
  errno = 0;
  if (::sigprocmask (SIG_SETMASK, NULL, set) < 0)
    throwErrno (errno, "sigprocmask.SIG_SETMASK");
  return this;
}
