/* Copyright (C) 2005 Red Hat, Inc.
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

#ifdef HAVE_CONFIG_H
# include <config.h>
#endif

#include <inttypes.h>
#include <assert.h>
#include ELFUTILS_HEADER(dwfl)
#include <argp.h>
#include <stdio.h>
#include <locale.h>
#include <stdlib.h>
#include <string.h>
#include <error.h>


static void
print_address (Dwfl_Module *mod, Dwarf_Addr address)
{
  int n = dwfl_module_relocations (mod);
  if (n < 0)
    error (0, 0, "dwfl_module_relocations: %s", dwfl_errmsg (-1));
  else if (n > 0)
    {
      int i = dwfl_module_relocate_address (mod, &address);
      if (i < 0)
	error (0, 0, "dwfl_module_relocate_address: %s", dwfl_errmsg (-1));
      else
	{
	  const char *modname = dwfl_module_info (mod, NULL, NULL, NULL,
						  NULL, NULL, NULL, NULL);
	  const char *secname = dwfl_module_relocation_info (mod, i, NULL);
	  if (n > 1 || secname[0] != '\0')
	    printf ("%s(%s)+%#" PRIx64, modname, secname, address);
	  else
	    printf ("%s+%#" PRIx64, modname, address);
	  return;
	}
    }

  printf ("%#" PRIx64, address);
}


struct args
{
  const char *arg;
  char *file;
  int line;
};

static int
handle_module (Dwfl_Module *mod __attribute__ ((unused)),
	       void **udata __attribute__ ((unused)),
	       const char *modname, Dwarf_Addr base __attribute__ ((unused)),
	       Dwarf *dbg __attribute__ ((unused)),
	       Dwarf_Addr bias __attribute__ ((unused)), void *arg)
{
  const struct args *const a = arg;

  Dwfl_Line **lines = NULL;
  size_t nlines = 0;

  if (dwfl_module_getsrc_file (mod, a->file, a->line, 0, &lines, &nlines) == 0)
    {
      for (size_t inner = 0; inner < nlines; ++inner)
	{
	  Dwarf_Addr addr;
	  int line = a->line, col = 0;
	  const char *file = dwfl_lineinfo (lines[inner], &addr, &line, &col,
					    NULL, NULL);
	  if (file != NULL)
	    {
	      printf ("%s -> ", a->arg);
	      print_address (mod, addr);
	      if (modname[0] != '\0')
		printf (" (%s:", modname);
	      if (strcmp (file, a->file) || line != a->line || col != 0)
		printf (" %s%s:%d", modname[0] != '\0' ? "" : "(",
			file, line);
	      if (col != 0)
		printf (":%d", col);
	      if (modname[0] != '\0'
		  || strcmp (file, a->file) || line != a->line || col != 0)
		puts (")");
	      else
		puts ("");
	    }
	}
      free (lines);
    }

  return DWARF_CB_OK;
}

int
main (int argc, char *argv[])
{
  int cnt;

  /* Set locale.  */
  (void) setlocale (LC_ALL, "");

  Dwfl *dwfl = NULL;
  (void) argp_parse (dwfl_standard_argp (), argc, argv, 0, &cnt, &dwfl);
  assert (dwfl != NULL);

  for (; cnt < argc; ++cnt)
    {
      struct args a = { .arg = argv[cnt] };
      char *c = strchr (a.arg, ':');

      if (a.arg == NULL) {
	printf ("ignored %s\n", a.arg);
	continue;
      } else if (c == NULL) {
	a.file = strdup (a.arg);
	a.line = 0;
      } else {
	a.file = strndup (a.arg, c - a.arg);
	a.line = strtol (c + 1, NULL, 10);
      }

      (void) dwfl_getdwarf (dwfl, &handle_module, &a, 0);

      free (a.file);
    }

  dwfl_end (dwfl);

  return 0;
}
