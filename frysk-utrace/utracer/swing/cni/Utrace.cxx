#include <stdio.h>
#include <stdlib.h>
#include <time.h>

#include <gcj/cni.h>

#include "Utrace.h"

static int seeded = 0;	// temp

void
Utrace::getregs (jintArray vals)
{
  int i;
  
  if (!seeded) {
    srand48 ((long int)time (NULL));
    seeded = 1;
  }

  for (i = 0; i < (int)JvGetArrayLength(vals); i++)
    (elements (vals))[i] = (int)lrand48();
    
}
