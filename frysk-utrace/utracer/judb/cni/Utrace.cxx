// This file is part of the Utracer kernel module and it's userspace
// interfaces. 
//
// Copyright 2007, Red Hat Inc.
//
// Utracer is free software; you can redistribute it and/or modify it
// under the terms of the GNU General Public License as published by
// the Free Software Foundation; version 2 of the License.
//
// Utracer is distributed in the hope that it will be useful, but
// WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
// General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with Utracer; if not, write to the Free Software Foundation,
// Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
//
// In addition, as a special exception, Red Hat, Inc. gives You the
// additional right to link the code of Utracer with code not covered
// under the GNU General Public License ("Non-GPL Code") and to
// distribute linked combinations including the two, subject to the
// limitations in this paragraph. Non-GPL Code permitted under this
// exception must only link to the code of Utracer through those well
// defined interfaces identified in the file named EXCEPTION found in
// the source code files (the "Approved Interfaces"). The files of
// Non-GPL Code may instantiate templates or use macros or inline
// functions from the Approved Interfaces without causing the
// resulting work to be covered by the GNU General Public
// License. Only Red Hat, Inc. may make changes or additions to the
// list of Approved Interfaces. You must obey the GNU General Public
// License in all respects for all of the Utracer code and other code
// used in conjunction with Utracer except the Non-GPL Code covered by
// this exception. If you modify this file, you may extend this
// exception to your version of the file, but you are not obligated to
// do so. If you do not wish to provide this exception without
// modification, you must delete this exception statement from your
// version and license this file solely under the GPL without
// exception.

#define _XOPEN_SOURCE 500
#include <stdio.h>
#include <stdlib.h>
#include <time.h>
#include <unistd.h>

#include <gcj/cni.h>

#include <utracer.h>


#include "Utrace.h"

jlong
Utrace::open ()
{
  //  fprintf (stderr, "in open()\n");
  return utracer_open();
}

jint
Utrace::unregister (jlong client_pid)
{
  //  fprintf (stderr, "in unregister( %ld )\n", (long)client_pid);
  return utracer_unregister ((long)client_pid);
}

jint
Utrace::attach (jlong client_pid, jlong pid,
		jlong quiesce, jlong exec_quiesce)
{
  //  fprintf (stderr, "in attach( )\n");
  return utracer_attach (client_pid, pid, quiesce, exec_quiesce);
}

jint
Utrace::detach (jlong client_pid, jlong pid)
{
  //  fprintf (stderr, "in detach( )\n");
  return utracer_detach (client_pid, pid);
}

jint
Utrace::run (jlong client_pid, jlong pid)
{
  //  fprintf (stderr, "in run( )\n");
  return utracer_run (client_pid, pid);
}

jint
Utrace::quiesce (jlong client_pid, jlong pid)
{
  //  fprintf (stderr, "in quiesce( )\n");
  return utracer_quiesce (client_pid, pid);
}

jint
Utrace::sync (jlong client_pid, jlong type)
{
  //  fprintf (stderr, "in quiesce( )\n");
  return utracer_sync (client_pid, type);
}

jlongArray
Utrace::get_gprs (jlong client_pid, jlong pid)
{
  jlongArray rc;
  jlong * rc_elements;
  void * regsinfo = NULL;
  unsigned int nr_regs;
  unsigned int reg_size;
  int irc;

  // fprintf (stderr, "in get_gprs( )\n");

  irc = utracer_get_regs (client_pid, pid, 0, &regsinfo,
			  &nr_regs, &reg_size);

  if (regsinfo) {
    rc = JvNewLongArray (nr_regs);
    rc_elements = elements (rc);

    for (int i = 0; i < 17; i++) rc_elements[i] = ((long *)regsinfo)[i];
  }
  else rc = NULL;
  
  return rc;
}

static if_resp_u if_resp;

jint
Utrace::read_response ()
{
  int sz;
  int rc;
  
  fprintf (stderr, "in read_response( )\n");

  sz = pread (utracer_resp_file_fd(), &if_resp,
	      sizeof(if_resp), 0);

  rc = (-1 == sz) ? -1 : if_resp.type;
  
  return rc;
}

jlong
Utrace::read_response_sync_type ()
{
  sync_resp_s sync_resp = if_resp.sync_resp;
  
  fprintf (stderr, "in read_response_sync_type( )\n");
  
  return sync_resp.sync_type;
}

