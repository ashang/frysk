#include <gcj/cni.h>

#include "cniinner/Parent.h"
#include "cniinner/Child.h"
#include "cniinner/Child$Nested.h"

#include <stdio.h>

void
cniinner::Child$Nested::f ()
{
  printf ("%d\n", this$0->i);
}

