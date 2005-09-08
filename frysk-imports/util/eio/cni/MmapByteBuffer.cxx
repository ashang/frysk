// From the VENUS project.  Copyright 2004, 2005, Andrew Cagney
// Licenced under the terms of the Eclipse Public Licence.
// Licenced under the terms of the GNU CLASSPATH Licence.

#include <sys/mman.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <alloca.h>
#include <errno.h>
#include <stdio.h>
#include <stdlib.h>

#include <gcj/cni.h>
#include "util/eio/Buffer.h"
#include "util/eio/ByteBuffer.h"
#include "util/eio/MmapByteBuffer.h"
#include "util/eio/MmapByteBuffer$Mmap.h"

jlong
util::eio::MmapByteBuffer$Mmap::mmap (jstring file, jlong length)
{
  int pathlength = JvGetStringUTFLength (file);
  char *pathname = (char *) alloca (pathlength + 1);
  JvGetStringUTFRegion (file, 0, pathlength, pathname);
  pathname[pathlength] = '\0';

  errno = 0;
  int fd = open (pathname, O_RDONLY);
  if (errno != 0) {
    ::perror ("open");
    ::exit (errno);
  }

  errno = 0;
  void* buffer = ::mmap (NULL, length, PROT_READ | PROT_WRITE, MAP_PRIVATE,
			 fd, 0);
  if (errno != 0) {
    ::perror ("mmap");
    ::exit (errno);
  }

  return (jlong) (long) buffer;
}

jint
util::eio::MmapByteBuffer::peek (jlong caret)
{
  u_int8_t *p = (u_int8_t*) (long) this->map->byteData;
  return p[caret];
}

void
util::eio::MmapByteBuffer::poke (jlong caret, jint value)
{
  u_int8_t *p = (u_int8_t*) (long) this->map->byteData;
  p[caret] = value;
}

jlong
util::eio::MmapByteBuffer::peek (jlong caret, jbyteArray bytes, jlong off,
			     jlong len)
{
  u_int8_t *p = (u_int8_t*) (long) this->map->byteData;
  memcpy (elements (bytes) + off, p + caret, len);
  return len;
}
