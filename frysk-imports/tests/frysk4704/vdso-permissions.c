/* Test VDSO permissions in `/proc/self/maps'.
   Copyright (C) 2005, 2007 Red Hat, Inc.
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

   In addition, as a special exception, Red Hat, Inc. gives You the
   additional right to link the code of Red Hat elfutils with code licensed
   under any Open Source Initiative certified open source license
   (http://www.opensource.org/licenses/index.php) which requires the
   distribution of source code with any binary distribution and to
   distribute linked combinations of the two.  Non-GPL Code permitted under
   this exception must only link to the code of Red Hat elfutils through
   those well defined interfaces identified in the file named EXCEPTION
   found in the source code files (the "Approved Interfaces").  The files
   of Non-GPL Code may instantiate templates or use macros or inline
   functions from the Approved Interfaces without causing the resulting
   work to be covered by the GNU General Public License.  Only Red Hat,
   Inc. may make changes or additions to the list of Approved Interfaces.
   Red Hat's grant of this exception is conditioned upon your not adding
   any new exceptions.  If you wish to add a new Approved Interface or
   exception, please contact Red Hat.  You must obey the GNU General Public
   License in all respects for all of the Red Hat elfutils code and other
   code used in conjunction with Red Hat elfutils except the Non-GPL Code
   covered by this exception.  If you modify this file, you may extend this
   exception to your version of the file, but you are not obligated to do
   so.  If you do not wish to provide this exception without modification,
   you must delete this exception statement from your version and license
   this file solely under the GPL without exception.

   Red Hat elfutils is an included package of the Open Invention Network.
   An included package of the Open Invention Network is a package for which
   Open Invention Network licensees cross-license their patents.  No patent
   license is granted, either expressly or impliedly, by designation as an
   included package.  Should you wish to participate in the Open Invention
   Network licensing program, please visit www.openinventionnetwork.com
   <http://www.openinventionnetwork.com>.  */

#define _GNU_SOURCE

#include <inttypes.h>
#include <sys/types.h>
#include <errno.h>
#include <stdio.h>
#include <stdio_ext.h>
#include <stdbool.h>
#include <string.h>
#include <stdlib.h>
#include <fcntl.h>
#include <unistd.h>
#include <assert.h>
#include <endian.h>

#include <gelf.h>


#define PROCMAPSFMT	"/proc/%d/maps"
#define PROCAUXVFMT	"/proc/%d/auxv"


/* Search /proc/PID/auxv for the AT_SYSINFO_EHDR tag.  */

static int
find_sysinfo_ehdr (pid_t pid, GElf_Addr *sysinfo_ehdr)
{
  char *fname;
  if (asprintf (&fname, PROCAUXVFMT, pid) < 0)
    return ENOMEM;

  int fd = open64 (fname, O_RDONLY);
  free (fname);
  if (fd < 0)
    return errno == ENOENT ? 0 : errno;

  ssize_t nread;
  do
    {
      union
      {
	char buffer[sizeof (long int) * 2 * 64];
	Elf64_auxv_t a64[sizeof (long int) * 2 * 64 / sizeof (Elf64_auxv_t)];
	Elf32_auxv_t a32[sizeof (long int) * 2 * 32 / sizeof (Elf32_auxv_t)];
      } d;
      nread = read (fd, &d, sizeof d);
      if (nread > 0)
	{
	  size_t i;

	  switch (sizeof (long int))
	    {
	    case 4:
	      for (i = 0; (char *) &d.a32[i] < &d.buffer[nread]; ++i)
		if (d.a32[i].a_type == AT_SYSINFO_EHDR)
		  {
		    *sysinfo_ehdr = d.a32[i].a_un.a_val;
		    nread = 0;
		    break;
		  }
	      break;
	    case 8:
	      for (i = 0; (char *) &d.a64[i] < &d.buffer[nread]; ++i)
		if (d.a64[i].a_type == AT_SYSINFO_EHDR)
		  {
		    *sysinfo_ehdr = d.a64[i].a_un.a_val;
		    nread = 0;
		    break;
		  }
	      break;
	    default:
	      abort ();
	      break;
	    }
	}
    }
  while (nread > 0);

  close (fd);

  return nread < 0 ? errno : 0;
}

static int
proc_maps_report (FILE *f, GElf_Addr sysinfo_ehdr, pid_t pid)
{
  char *line = NULL;
  size_t linesz;
  ssize_t len;
  char *perm = NULL;
  while ((len = getline (&line, &linesz, f)) > 0)
    {
      if (line[len - 1] == '\n')
	line[len - 1] = '\0';

      GElf_Addr start;
      int nread = -1;
      char *filename = NULL;
      /* FILENAME may be missing there but we must not stop on such entry.  */
      if (sscanf (line, "%" PRIx64 "-%*x %as %*x %*x:%*x %*i %n %as",
		  &start, &perm, &nread, &filename) < 2
	  || nread <= 0)
	{
	  free (line);
	  free (perm);
	  free (filename);
	  /* Should not happen.  Considered as FAIl.  */
	  return 1;
	}

      /* If this is the special mapping AT_SYSINFO_EHDR pointed us at,
	 report the last one and then this special one.  */
      if ((start == sysinfo_ehdr && start != 0)
/* VDSO memory without AT_SYSINFO_EHDR is present on systems where it is
   either disabled by `/proc/sys/kernel/vdso' or not needed.
   In these cases the VDSO is never present in backtraces anyway.
   We would fail on some systems which behave right on the real runs.  */
#if 0
	  || (filename != NULL && strcmp (filename, "[vdso]") == 0)
#endif
	  )
        {
	  int retval = strcmp (perm, "r-xp") == 0 ? 0 : 1;

	  free (line);
	  free (perm);
	  free (filename);
	  return retval;
	}
      free (filename);
      free (perm);
      perm = NULL;
    }
  free (line);

  /* VDSO has not been found so its state cannot be a Frysk showstopper.  */
  return 77;
}

int
main (void)
{
  pid_t pid = getpid ();
  int retval;

  /* We'll notice the AT_SYSINFO_EHDR address specially when we hit it.  */
  GElf_Addr sysinfo_ehdr = 0;
  int result = find_sysinfo_ehdr (pid, &sysinfo_ehdr);
  if (result != 0)
    return result;

  char *fname;
  if (asprintf (&fname, PROCMAPSFMT, pid) < 0)
    return ENOMEM;

  FILE *f = fopen (fname, "r");
  free (fname);
  if (f == NULL)
    return errno;

  (void) __fsetlocking (f, FSETLOCKING_BYCALLER);

  retval = proc_maps_report (f, sysinfo_ehdr, pid);

  fclose (f);

  return retval;
}
