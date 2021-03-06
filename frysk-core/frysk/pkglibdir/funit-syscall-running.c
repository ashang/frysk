// This file is part of the program FRYSK.
//
// Copyright 2006, Red Hat Inc.
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
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <netinet/ip.h>

int
main ()
{
  int fd;
  struct sockaddr_in addr;
  socklen_t len = sizeof (addr);

  fd = socket (AF_INET, SOCK_STREAM, 0);
  memset (&addr, 0, sizeof(addr));
  addr.sin_family = AF_INET;
  addr.sin_addr.s_addr = htonl (INADDR_ANY);
  if (bind (fd, (struct sockaddr *) &addr, sizeof(addr)) == -1)
    {
      perror ("bind");
      exit (-1);
    }
  if (getsockname (fd, (struct sockaddr *) &addr, &len) == -1)
    {
      perror ("getsockname");
      exit (-1);
    }
  if (listen (fd, 0) == -1)
    {
      perror ("listen");
      exit (-1);
    }
  printf("%d\n", ntohs (addr.sin_port));
  fflush(stdout);

  // Wait for the start sign
  getchar();
  
  // Accept connection.
  struct sockaddr_in sai;
  len = sizeof (sai);
  int f = accept (fd, (struct sockaddr *) &sai, &len);
  if (f == -1)
    {
      perror ("accept");
      exit (-1);
    }

  return f;
}
