#include <iostream>
using namespace std;

static void crash (){
  char* a = 0;
  a[0] = 0;
}

static int assign_ (int) __attribute__ ((noinline));
int
assign_ (int a)
{
  return 2;
}

class Base1
{
public:
  Base1 (const char *m):msg (m)
  {
    // std::cout << "Base1::Base1(" << msg << ")" << std::endl;
  }
  ~Base1 ()
  {
    // std::cout << "Base1::~Base1(" << msg << ")" << std::endl;
  }
  const char *msg;
};

class Base2
{
public:
  Base2 (const char *m):msg (m)
  {
    // std::cout << "Base2::Base2(" << msg << ")" << std::endl;
  }
  ~Base2 ()
  {
    // std::cout << "Base2::~Base2(" << msg << ")" << std::endl;
  }
  const char *msg;
};

class Type : public Base1, public Base2
{
public:
  Type(const char *m, const char *n, const char *o) : Base1(m), Base2(n), note(o)
  {
    // std::cout << "Type::Type(" << note << ")" << std::endl;
  }
  ~Type()
  {
    // std::cout << "Type::~Type(" << note << ")" << std::endl;
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
    // std::cout << t << std::endl;
    return t;
  }
};

static Type mb("static", "main", "mb");
static Type new_base = Type ("new base", "main", "new_base");
static Derived xyz;

static void func () __attribute__ ((noinline));
void
func ()
{
  // std::cout << "main" << std::endl;
  xyz.do_this ((char)'1');
  xyz.do_this ((short)2);
  xyz.do_this ((int)3);
  xyz.do_this ((float) 4.1);
  int x = xyz.do_this ((int)1);
  x = assign_(x);
  crash();
}

int
main (int argc, char **argv)
{
  func();
}
