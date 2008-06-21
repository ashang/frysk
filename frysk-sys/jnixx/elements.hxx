// This file is part of the program FRYSK.
//
// Copyright 2008, Red Hat Inc.
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

#ifndef elements_hxx
#define elements_hxx

#include <stdarg.h>
#include <stdio.h>
#include <string.h>
#include <errno.h>

#include <jnixx/exceptions.hxx>

extern char** strings2chars(::jnixx::env, ::jnixx::array<java::lang::String>);
extern ::jnixx::array<java::lang::String> chars2strings(::jnixx::env, char**);

/**
 * Attempt to read the entire contents of the specified file.  Return
 * BUF and set LEN, or NULL should the open fail.  The BUF will be NUL
 * terminated but the NUL will not be included in the character count.
 */
extern jbyte* slurp(::jnixx::env, const char file[], jsize& len);

class StringArrayChars {
private:
  ::jnixx::env env;
  ::jnixx::array< ::java::lang::String> strings;
  char** p;
public:
  void operator=(const StringArrayChars& src) {
    this->env = src.env;
    this->strings = src.strings;
    // Avoid double references by no copying p.
    this->p = NULL;
  }
  StringArrayChars() {
    this->p = NULL;
  }
  StringArrayChars(const StringArrayChars& old) {
    this->operator=(old);
  }
  StringArrayChars(::jnixx::env env,
		   ::jnixx::array< ::java::lang::String> strings) {
    this->env = env;
    this->strings = strings;
    this->p = NULL;
  }
  char** elements() {
    if (p == NULL) {
      if (strings != NULL) {
	p = strings2chars(env, strings);
      }
    }
    return p;
  }
  void release() {
    if (p != NULL) {
      delete p;
      p = NULL;
    }
  }
  ~StringArrayChars() {
    release();
  }
};

/**
 * A buffer of primitive types that is automatically re-claimed when
 * the function returns, or an exception is thrown.
 */
template <typename type> class Elements {
private:
  jnixx::env env;
  type* buf;
  jsize len;
protected:
  Elements() {
    buf = NULL;
    // Use len<0 as a marker to indicate that the buffer hasn't been
    // read / extracted / converted.
    len = -1;
  }
  Elements(jnixx::env env) {
    this->env = env;
    this->len = -1;
    this->buf = NULL;
  }
  void copy(const Elements& old) {
    release();
    this->env = old.env;
    this->len = -1;
    this->buf = NULL;
  }
  virtual ~Elements() {
  }
  virtual void slurp(jnixx::env& env, type* (&buf), jsize &len) = 0;
  virtual void free(jnixx::env& env, type* buf, int mode) {
  }
public:
  type* elements() {
    if (len < 0) {
      slurp(env, buf, len);
    }
    return buf;
  };
  jsize length() {
    if (len < 0) {
      slurp(env, buf, len);
    }
    return len;
  };
  void release(int mode) {
    if (len >= 0) {
      if (buf != NULL) {
	free(env, buf, mode);
	buf = NULL;
      }
      len = -1;
    }
  }
  void release() {
    release(0);
  }
};
typedef Elements<jbyte> Bytes;

/**
 * Scratch UTF Chars.
 */
class jstringUTFChars : public Elements<const char> {
private:
  ::java::lang::String string;
public:
  void operator=(const jstringUTFChars& src) {
    this->copy(src);
    this->string = src.string;
  }
  jstringUTFChars() {
  }
  jstringUTFChars(const jstringUTFChars& old) {
    this->operator=(old);
  }
  jstringUTFChars(::jnixx::env env, ::java::lang::String string)
    : Elements<const char>(env) {
    this->string = string;
  }
  void slurp(jnixx::env& env, const char* (&buf), jsize &len) {
    if (string == NULL) {
      buf = NULL;
      len = 0;
    } else {
      buf = string.GetStringUTFChars(env);
      len = ::strlen(buf);
    }
  }
  void free(jnixx::env& env, const char* buf, int mode) {
    string.ReleaseStringUTFChars(env, buf);
  }
  ~jstringUTFChars() {
    release();
  }
};


/**
 * A scratch buffer containing the file's entire contents; the buffer
 * is recovered once this goes out of scope.
 */
template <typename type> class FileElements : public Elements<type> {
private:
  char file[FILENAME_MAX];
public:
  void operator=(const FileElements& src) {
    copy(src);
    ::strcpy(this->file, src.file);
    // Don't copy the pointer.
  }
  FileElements(const FileElements& src) {
    this->operator=(src);
  }
  FileElements(jnixx::env env, const char* fmt, ...)
  __attribute__((format(printf, 3, 4))) : Elements<type>(env) {
    va_list ap;
    va_start(ap, fmt);
    if (::vsnprintf(file, sizeof file, fmt, ap)
	>= (int) sizeof file) {
      errnoException(env, errno, "vsnprintf");
    }
    va_end(ap);
  }
  FileElements(jnixx::env env, int pid, const char* name)
    : Elements<type>(env) {
    // Convert the string into a file.
    if (::snprintf(file, sizeof file, "/proc/%d/%s", pid, name)
	>= (int) sizeof file) {
      errnoException(env, errno, "snprintf");
    }
  }
  FileElements(jnixx::env env, int pid, int tid, const char* name)
    : Elements<type>(env) {
    // Convert the string into a file.
    if (::snprintf(file, sizeof file, "/proc/%d/task/%d/%s", pid, tid, name)
	>= (int) sizeof file) {
      errnoException(env, errno, "snprintf");
    }
  }
  ~FileElements() {
    this->release();
  }
protected:
  void slurp(jnixx::env& env, type* (&buf), jsize &len) {
    jsize length;
    jbyte* buffer = ::slurp(env, file, length);
    buf = (type*)buffer;
    len = length / sizeof(type);
  }
  void free(jnixx::env& env, type* buf, int mode) {
    delete buf;
  }
};
typedef FileElements<jbyte> FileBytes;

/**
 * Provide access to a java primitive type array's elements.
 */
template <typename type, typename typeArray> class ArrayElements
  : public Elements<type> {
private:
  typeArray types;
public:
  void operator=(const ArrayElements& src) {
    this->copy(src);
    this->types = src.types;
  }
  ArrayElements() {
  }
  ArrayElements(const ArrayElements& old) {
    this->operator=(old);
  }
  ArrayElements(::jnixx::env env, typeArray types) : Elements<type>(env) {
    this->types = types;
  }
  ~ArrayElements() {
    this->release();
  }
protected:
  void slurp(jnixx::env& env, type* (&buf), jsize &len) {
    if (types != NULL) {
      len = types.GetArrayLength(env);
      buf = types.GetArrayElements(env, NULL);
    } else {
      len = 0;
      buf = NULL;
    }
  }
  void free(jnixx::env& env, type* buf, int mode) {
    types.ReleaseArrayElements(env, buf, mode);
  }
};
typedef ArrayElements<jbyte,::jnixx::jbyteArray> jbyteArrayElements;
typedef ArrayElements<jlong,::jnixx::jlongArray> jlongArrayElements;

#endif
