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

#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>

#include <gcj/cni.h>

#include "frysk/sys/proc/cni/slurp.hxx"
#include "frysk/sys/cni/Errno.hxx"
#include "frysk/sys/proc/AuxvBuilder.h"

/**
 * Extract the value as an unsigned.
 */

typedef int64_t (get_t) (const void *);

// Read bytes beg:end as either big or little endian; if B is NULL
// return the byte order (+ve == BE, -ve == le) and word size.

static int64_t
get (const void *b, int beg, int end, int delta)
{
  if (b == NULL)
    return beg - end;
  else {
    int64_t v = 0;
    const unsigned char *p = (const unsigned char *)b;
    for (int i = beg; i != end; i += delta) {
      v |= ((uint64_t)p[i]) << (8 * abs (beg - i));
    }
    return v;
  }
}

static int64_t
get32b (const void *b)
{
  return get (b, 3, -1, -1);
}

static int64_t
get64b (const void *b)
{
  return get (b, 7, -1, -1);
}

static int64_t
get32l (const void *b)
{
  return get (b, 0, 4, 1);
}

static int64_t
get64l (const void *b)
{
  return get (b, 0, 8, 1);
}

// Verify the auxiliary vector is both correctly sized, and contains
// only reasonable entries.
static bool
verify (jbyteArray buf, get_t *get)
{
  int wordSize = abs (get (NULL));
  // Buffer holds an exact multiple of entry-size (2*word)?
  if (buf->length % (wordSize * 2) != 0)
    return false;
  for (int i = 0; i < buf->length; i += 2 * wordSize) {
    jbyte *p = elements(buf) + i;
    int64_t type = get (p);
    // Reasonable value?
    if (type > 1024 || type < 0)
      return false;
    // AT_NULL value only at end of buffer?
    if (type == 0 && i + 2 * wordSize < buf->length)
      return false;
  }
  return true;
}

jboolean
frysk::sys::proc::AuxvBuilder::construct (jint pid)
{
  jbyteArray buf = slurp (pid, "auxv");
  if (buf == NULL)
    return false;
  buildBuffer (buf);
  return construct (buf);
}

jboolean
frysk::sys::proc::AuxvBuilder::construct (jbyteArray buf)
{
  static get_t *cvt[2][2][2][2] =
    {
      {
	{
	  { NULL, get64b },
	  { get32b, NULL },
	},
	{
	  { get64l, NULL },
	  { NULL, NULL },
	},
      },
      {
	  {
	    { get32l, NULL },
	    { NULL, NULL },
	  },
	  {
	    { NULL, NULL },
	    { NULL, NULL },
	  },
      }
    };

  // Figure out the word-size of the auxv.

  get_t *get = cvt
    [verify (buf, get32l)]
    [verify (buf, get64l)]
    [verify (buf, get32b)]
    [verify (buf, get64b)];
  if (get == NULL)
    throwRuntimeException ("unknown word size for auxv",
			   "1|32l|64l|32b|64b",
			   10000
			   + verify (buf, get32l) * 1000
			   + verify (buf, get64l) * 100
			   + verify (buf, get32b) * 10
			   + verify (buf, get64b));
  int wordSize = abs (get (NULL));
  bool bigEndian = get (NULL) > 0;
  int numberEntries = buf->length / wordSize / 2;
  buildDimensions (wordSize, bigEndian, numberEntries);
  
  // Unpack the corresponding entries.
  for (int i = 0; i < numberEntries; i++) {
    jbyte *p = elements (buf) + wordSize * i * 2;
    jint type = get (p + wordSize * 0);
    jlong value = get (p + wordSize * 1);
    buildAuxiliary (i, type, value);
  }

  return true;
}
