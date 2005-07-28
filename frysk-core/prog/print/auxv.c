// This file is part of FRYSK.
//
// Copyright 2005, Red Hat Inc.
//
// FRYSK is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
//
// FRYSK is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with FRYSK; if not, write to the Free Software
// Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
#include <stdio.h>

struct auxv {
  long type;
  long value;
};

int
main (int argc, char **argv, char **envp)
{
  struct auxv *auxp;
  /* Auxv array starts after the terminal entry in envp */
  while (*envp != NULL)
    ++envp;
  ++envp; /* point pass terminating null entry */
  for (auxp = (struct auxv *)(envp); auxp->type != 0; auxp++) {
    printf ("%lu %lu\n", auxp->type, auxp->value);
  }
  return 0;
}
