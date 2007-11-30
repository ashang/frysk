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

// Test: class
// Name: mb
// Type: class Type {
// Type:  public:
// Type:   struct Base1 {
// Type:     const char * msg;
// Type:     void Base1 (const char * );
// Type:     void ~Base1 ();
// Type:   } ;
// Type:   struct Base2 {
// Type:     const char * msg;
// Type:     void Base2 (const char * );
// Type:     void ~Base2 ();
// Type:   } ;
// Type:  private:
// Type:   const char * note;
// Type:   void Type (const char * ,const char * ,const char * );
// Type:   void ~Type ();
// Type: }

Type mb("static", "main", "mb");

// Name: new_base
// Type: class Type {
// Type:  public:
// Type:   struct Base1 {
// Type:     const char * msg;
// Type:     void Base1 (const char * );
// Type:     void ~Base1 ();
// Type:   } ;
// Type:   struct Base2 {
// Type:     const char * msg;
// Type:     void Base2 (const char * );
// Type:     void ~Base2 ();
// Type:   } ;
// Type:  private:
// Type:   const char * note;
// Type:   void Type (const char * ,const char * ,const char * );
// Type:   void ~Type ();
// Type: }

Type new_base = Type ("new base", "main", "new_base");

// Name: xyz
// Type: class Derived {
// Type:  public:
// Type:   struct Base3 {
// Type:     int  () * * _vptr.Base3;
// Type:     void Base3 (void * const & );
// Type:     void Base3 ();
// Type:     char do_this (char );
// Type:     short int do_this (short int );
// Type:     int do_this (int );
// Type:     float do_this (float );
// Type:     void ~Base3 ();
// Type:   } ;
// Type:   void Derived (void * const & );
// Type:   void Derived ();
// Type:   char do_this (char );
// Type:   short int do_this (short int );
// Type:   int do_this (int );
// Type:   float do_this (float );
// Type:   void ~Derived ();
// Type:  private:
// Type:   char do_this_impl<char> (char );
// Type:   short int do_this_impl<short int> (short int );
// Type:   int do_this_impl<int> (int );
// Type:   float do_this_impl<float> (float );
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
