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

extern char** strings2chars(::jnixx::env, ::jnixx::array<java::lang::String>);

extern ::jnixx::array<java::lang::String> chars2strings(::jnixx::env, char**);

class StringChars {
private:
  ::java::lang::String string;
  ::jnixx::env env;
  const char* p;
public:
  void operator=(const StringChars& src) {
    this->env = src.env;
    this->string = src.string;
    // Avoid double references by not copying P.
    this->p = NULL;
  }
  StringChars() {
    this->p = NULL;
  }
  StringChars(const StringChars& old) {
    this->operator=(old);
  }
  StringChars(::jnixx::env env, ::java::lang::String string) {
    this->env = env;
    this->string = string;
    this->p = NULL;
  }
  const char* elements() {
    if (p == NULL) {
      if (string != NULL) {
	this->p = string.GetStringUTFChars(env);
      }
    }
    return p;
  }
  void release() {
    if (p != NULL) {
      string.ReleaseStringUTFChars(env, p);
      p = NULL;
    }
  }
  ~StringChars() {
    release();
  }
};

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

class Bytes {
protected:
  jnixx::env env;
  jbyte* p;
  jsize l;
  Bytes() {
    p = NULL;
    l = -1;
  }
  virtual ~Bytes() {
  }
public:
  virtual jbyte* elements() = 0;
  virtual jsize length() = 0;
};

class FileBytes : public Bytes {
private:
  char file[FILENAME_MAX];
public:
  void operator=(const FileBytes& src);
  FileBytes(const FileBytes& old) {
    this->operator=(old);
  }
  FileBytes(jnixx::env env, const char* fmt, ...)
  __attribute__((format(printf, 3, 4)));
  FileBytes(jnixx::env env, int pid, const char* name);
  FileBytes(jnixx::env env, int pid, int tid, const char* name);
  jbyte* elements();
  jsize length();
  void release();
  ~FileBytes() {
    release();
  }
};

class ArrayBytes : public Bytes {
private:
  ::jnixx::jbyteArray bytes;
public:
  void operator=(const ArrayBytes& src) {
    release();
    this->env = src.env;
    this->bytes = src.bytes;
    // don't copy the pointer.
  }
  ArrayBytes() {
  }
  ArrayBytes(const ArrayBytes& old) {
    this->operator=(old);
  }
  ArrayBytes(::jnixx::env env, ::jnixx::jbyteArray bytes) {
    this->bytes = bytes;
    this->env = env;
  }
  jbyte* elements() {
    length();
    return p;
  }
  jsize length() {
    if (l < 0) {
      if (bytes != NULL) {
	this->l = bytes.GetArrayLength(env);
	this->p = bytes.GetByteArrayElements(env, NULL);
      } else {
	this->l = 0;
	this->p = NULL;
      }
    }
    return l;
  }
  void release() {
    if (p != NULL) {
      bytes.ReleaseByteArrayElements(env, p, 0);
      p = NULL;
    }
    l = -1;
  }
  ~ArrayBytes() {
    release();
  }
};
