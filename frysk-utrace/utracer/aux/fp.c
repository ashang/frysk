#include <stdio.h>

main()
{
  double result;
  
  asm ("FLDPI");
  asm ("FSTP %[result]" : [result] "=f" (result));
}
