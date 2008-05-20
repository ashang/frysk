// This file is part of the program FRYSK.
//
// Copyright 2005, 2008, Red Hat Inc.
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
#include <ctype.h>
#include <stdio.h>
#include <string.h>

#include "jni.hxx"

#include "jnixx/elements.hxx"
#include "jnixx/exceptions.hxx"
#include "jnixx/scan.hxx"

bool
construct(jnixx::env env, frysk::sys::proc::MapsBuilder* builder, Bytes& buf) {
  const char *start = (const char *) buf.elements;
  const char *end = start + buf.length;
  const char *p = start;
  while (p < end) {
    if (isspace (*p))
      p++;
    else if (*p == '\0')
      return true;
    else {
      // <address>-<address>
      jlong addressLow = scanJlong(env, &p, 16);
      if (*p++ != '-')
	runtimeException(env, "missing dash");
      jlong addressHigh = scanJlong(env, &p, 16);
      // <RWXSP>
      if (*p++ != ' ')
	runtimeException(env, "missing space");
      jboolean permRead = *p++ == 'r';
      jboolean permWrite = *p++ == 'w';
      jboolean permExecute = *p++ == 'x';
      jboolean shared = *p++ == 's';
      // <offset>
      jlong offset = scanJlong(env, &p, 16);
      // <major>:<minor>
      jint devMajor = scanJint(env, &p, 16);
      if (*p++ != ':')
	runtimeException(env, "missing colon");
      jint devMinor = scanJint(env, &p, 16);
      // <inode>
      jint inode = scanJint(env, &p, 10);
      // <filename-string>?
      while (isblank (*p))
	p++;
      jint pathnameOffset = p - start;
      while (*p != '\0' && *p != '\n') {
	p++;
      }
      int pathnameLength = p - start - pathnameOffset;
      builder->buildMap(env, addressLow, addressHigh,
			permRead, permWrite, permExecute, shared, 
			offset,
			devMajor, devMinor,
			inode,
			pathnameOffset, pathnameLength);
    }
  }
  runtimeException(env, "missing NUL");
  return false;
}

bool
frysk::sys::proc::MapsBuilder::construct(jnixx::env env, jnixx::byteArray buf) {
  ArrayBytes bytes = ArrayBytes(env, buf);
  bool ok = ::construct(env, this, bytes);
  bytes.release();
  return ok;
}

bool
frysk::sys::proc::MapsBuilder::construct(jnixx::env env, jint pid) {
  FileBytes bytes = FileBytes(env, pid, "maps");
  if (bytes.elements == NULL)
    return false;
  {
    jnixx::byteArray array = jnixx::byteArray::NewByteArray(env, bytes.length);
    ArrayBytes b = ArrayBytes(env, array);
    memcpy(b.elements, bytes.elements, bytes.length);
    b.release();
    buildBuffer(env, array);
    array.DeleteLocalRef(env);
  }
  bool ok = ::construct(env, this, bytes);
  bytes.release();
  return ok;
}
