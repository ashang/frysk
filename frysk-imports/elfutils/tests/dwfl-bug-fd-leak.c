/* Test program for libdwfl file decriptors leakage.
   Copyright (C) 2007 Red Hat, Inc.
   This file is part of Red Hat elfutils.

   Red Hat elfutils is free software; you can redistribute it and/or modify
   it under the terms of the GNU General Public License as published by the
   Free Software Foundation; version 2 of the License.

   Red Hat elfutils is distributed in the hope that it will be useful, but
   WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
   General Public License for more details.

   You should have received a copy of the GNU General Public License along
   with Red Hat elfutils; if not, write to the Free Software Foundation,
   Inc., 51 Franklin Street, Fifth Floor, Boston MA 02110-1301 USA.

   Red Hat elfutils is an included package of the Open Invention Network.
   An included package of the Open Invention Network is a package for which
   Open Invention Network licensees cross-license their patents.  No patent
   license is granted, either expressly or impliedly, by designation as an
   included package.  Should you wish to participate in the Open Invention
   Network licensing program, please visit www.openinventionnetwork.com
   <http://www.openinventionnetwork.com>.  */

#include <config.h>
#include <assert.h>
#include <inttypes.h>
#include <stdio.h>
#include <stdio_ext.h>
#include <locale.h>
#include <dirent.h>
#include <stdlib.h>
#include <errno.h>
#include <unistd.h>
#include <dwarf.h>
#include ELFUTILS_HEADER(dwfl)


static void
elfutils_open (pid_t pid, Dwarf_Addr ip, Dwfl **dwfl_return,
	       Dwarf_Die **funcdie_return)
{
  static char *debuginfo_path;
  static const Dwfl_Callbacks proc_callbacks =
    {
      .find_debuginfo = dwfl_standard_find_debuginfo,
      .debuginfo_path = &debuginfo_path,

      .find_elf = dwfl_linux_proc_find_elf,
    };
  Dwfl *dwfl = dwfl_begin (&proc_callbacks);
  assert (dwfl != NULL);
  *dwfl_return = dwfl;

  if (dwfl_linux_proc_report (dwfl, pid) != 0)
    abort ();

  if (dwfl_report_end (dwfl, NULL, NULL) != 0)
    abort ();

  Dwfl_Module *mod = dwfl_addrmodule (dwfl, ip);
  assert (mod != NULL);

  Dwarf_Addr bias = 0;
  Dwarf_Die *cudie = dwfl_module_addrdie (mod, ip, &bias);
  assert (cudie != NULL);

  Dwarf_Die *scopes;
  int nscopes = dwarf_getscopes (cudie, ip - bias, &scopes);
  assert (nscopes > 0);

  Dwarf_Die *funcdie = NULL;
  int i;
  for (i = 0; i < nscopes; ++i)
    if (dwarf_tag (&scopes[i]) == DW_TAG_subprogram)
      {
	funcdie = &scopes[i];
	break;
      }
  assert (funcdie != NULL);
  *funcdie_return = funcdie;
}

static void
elfutils_close (Dwfl *dwfl)
{
  dwfl_end (dwfl);
}

/* Function returns even "."/".." entries.
   We compare only that count did not change.  */

static int
fd_count (void)
{
  DIR *dir;
  int retval = 0;

  dir = opendir ("/proc/self/fd");
  assert (dir != NULL);

  while (errno = 0, readdir (dir) != 0)
    retval++;
  assert (errno == 0);

  if (closedir (dir) != 0)
    abort();

  return retval;
}

int
main (void)
{
  int fd_counted;

  fd_counted = fd_count ();

  /* We use no threads here which can interfere with handling a stream.  */
  (void) __fsetlocking (stdout, FSETLOCKING_BYCALLER);

  /* Set locale.  */
  (void) setlocale (LC_ALL, "");

  assert (fd_counted == fd_count ());

  Dwfl *dwfl;
  Dwarf_Die *funcdie;
  elfutils_open (getpid (), (Dwarf_Addr) main, &dwfl, &funcdie);
  
  elfutils_close (dwfl);

  assert (fd_counted == fd_count ());

  return 0;
}
