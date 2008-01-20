// This file is part of the program FRYSK.
//
// Copyright 2007, Red Hat Inc.
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

#include <iostream>
using namespace std;

static void crash (){
  char* a = 0;
  a[0] = 0;
}

class Base1
{
public:
  Base1 (const char *m):msg (m)
  {
  }
  ~Base1 ()
  {
  }
  const char *msg;
};

class Base2
{
public:
  Base2 (const char *m):msg (m)
  {
  }
  ~Base2 ()
  {
  }
  const char *msg;
};

class Type : public Base1, public Base2
{
public:
  Type(const char *m, const char *n, const char *o) : Base1(m), Base2(n), note(o)
  {
  }
  ~Type()
  {
  }
  private:
  const char *note;
};

class Base3 
{
public:
  virtual char do_this (char) =0;
  virtual short do_this (short) =0;
  virtual int do_this (int) =0;
  virtual float do_this (float) =0;
  virtual ~Base3()
  {
  }
};
class Derived : public Base3
{
public:
  virtual char do_this(char x) { return do_this_impl(x); }
  virtual short do_this(short x) { return do_this_impl(x); }
  virtual int do_this(int x) { return do_this_impl(x); }
  virtual float do_this(float x) { return do_this_impl(x); }

private:
  template<class TYPE> TYPE do_this_impl(TYPE t)
  {
    return t;
  }
};

// Test: Class
// Name: mb
// Type: class Type {
// Type:  public:
// Type:   struct Base1 {
// Type:     const char *msg;
// Type:     void Base1 (const char *);
// Type:     void ~Base1 ();
// Type:   } ;
// Type:   struct Base2 {
// Type:     const char *msg;
// Type:     void Base2 (const char *);
// Type:     void ~Base2 ();
// Type:   } ;
// Type:  private:
// Type:   const char *note;
// Type:   void Type (const char *,const char *,const char *);
// Type:   void ~Type ();
// Type: }

Type mb("static", "main", "mb");

// Name: new_base
// Type: class Type {
// Type:  public:
// Type:   struct Base1 {
// Type:     const char *msg;
// Type:     void Base1 (const char *);
// Type:     void ~Base1 ();
// Type:   } ;
// Type:   struct Base2 {
// Type:     const char *msg;
// Type:     void Base2 (const char *);
// Type:     void ~Base2 ();
// Type:   } ;
// Type:  private:
// Type:   const char *note;
// Type:   void Type (const char *,const char *,const char *);
// Type:   void ~Type ();
// Type: }

Type new_base = Type ("new base", "main", "new_base");

// Name: xyz
// Type: class Derived {
// Type:  public:
// Type:   struct Base3 {
// Type:     int (**_vptr.Base3) ();
// Type:     void Base3 (void * const &);
// Type:     void Base3 ();
// Type:     char do_this (char);
// Type:     short int do_this (short int);
// Type:     int do_this (int);
// Type:     float do_this (float);
// Type:     void ~Base3 ();
// Type:   } ;
// Type:   void Derived (void * const &);
// Type:   void Derived ();
// Type:   char do_this (char);
// Type:   short int do_this (short int);
// Type:   int do_this (int);
// Type:   float do_this (float);
// Type:   void ~Derived ();
// Type:  private:
// Type:   char do_this_impl<char> (char);
// Type:   short int do_this_impl<short int> (short int);
// Type:   int do_this_impl<int> (int);
// Type:   float do_this_impl<float> (float);
// Type: }

Derived xyz;

static void func () __attribute__ ((noinline));
void
func ()
{
  xyz.do_this ((char)'1');
  xyz.do_this ((short)2);
  xyz.do_this ((int)3);
  xyz.do_this ((float) 4.1);
  xyz.do_this ((int)1);
  crash();
}

int
main (int argc, char **argv)
{
  func();
}
