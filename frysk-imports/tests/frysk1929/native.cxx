#include <gcj/cni.h>

#include "frysk1929/Parent.h"
#include "frysk1929/Child.h"
#include "frysk1929/Child$Nested.h"

#include <stdio.h>

void
frysk1929::Child$Nested::f ()
{
  printf ("%d\n", this$0->i);
}

