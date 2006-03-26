#include <gcj/cni.h>

#include "frysk2490/Parent.h"
#include "frysk2490/Child.h"
#include "frysk2490/Child$Nested.h"

#include <stdio.h>

void
frysk2490::Child$Nested::f ()
{
  printf ("%d\n", this$0->i);
}

