// This file is part of the program FRYSK.
//
// Copyright 2007, 2008, Red Hat Inc.
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

class class_accessibility {
 private:
  long int long_var;
  int int_var;
 public:
  short int short_var;
 protected:
  float float_var;
 public:
  void set_int_var (int int_var);
} class_accessibility;

// Test: Class
// Name: class_accessibility
// Type: class class_accessibility {
// Type:  private:
// Type:   long int long_var;
// Type:   int int_var;
// Type:  public:
// Type:   short int short_var;
// Type:  protected:
// Type:   float float_var;
// Type:  public:
// Type:   void set_int_var (int);
// Type: }

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

// Name: class_inherited
// Type: class Type {
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
// Type:  public:
// Type:   void Type (const char *,const char *,const char *);
// Type:   void ~Type ();
// Type: }

Type class_inherited("static", "main", "mb");

// Name: class_inherited_new
// Type: class Type {
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
// Type:  public:
// Type:   void Type (const char *,const char *,const char *);
// Type:   void ~Type ();
// Type: }

Type class_inherited_new = Type ("new base", "main", "new_base");

// Name: class_template
// Type: class Derived {
// Type:   struct Base3 {
// Type:     int (**_vptr.Base3) ();
// Type:     void Base3 (const Base3 &);
// Type:     void Base3 ();
// Type:     char do_this (char);
// Type:     short int do_this (short int);
// Type:     int do_this (int);
// Type:     float do_this (float);
// Type:     void ~Base3 ();
// Type:   } ;
// Type:   void Derived (const Derived &);
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

Derived class_template;

static void func () __attribute__ ((noinline));
void
func ()
{
  class_template.do_this ((char)'1');
  class_template.do_this ((short)2);
  class_template.do_this ((int)3);
  class_template.do_this ((float) 4.1);
  class_template.do_this ((int)1);
  crash();
}

class class_nested_class	
{
public:
  class nested
  {
   public:
    nested (int i) : z(i) {}
    int z;
  };
} class_nested_class_v;

// Name: class_nested_class_v
// Type: struct class_nested_class {
// Type: }

class class_ref_method
{
  int z;
    int get_z () {return z;}
  class class_ref_method & get_this () {return *this;} 
  class class_ref_method * also_get_this () {return this;} 
} class_ref_method_v;

// Name: class_ref_method_v
// Type: class class_ref_method {
// Type:  private:
// Type:   int z;
// Type:   int get_z ();
// Type:   class_ref_method & get_this ();
// Type:   class_ref_method * also_get_this ();
// Type: }

int
main (int argc, char **argv)
{
  func();
}
