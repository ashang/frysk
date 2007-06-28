#include <search.h>

#ifndef DO_UDB_INIT
extern
#endif
ENTRY reg_mapping[]
#ifdef DO_UDB_INIT
= {
  {"ebx",	 (void *)0},
  {"ecx",	 (void *)1},
  {"edx",	 (void *)2},
  {"esi",	 (void *)3},
  {"edi",	 (void *)4},
  {"ebp",	 (void *)5},
  {"eax",	 (void *)6},
  {"ds",	 (void *)7},
  {"es",	 (void *)8},
  {"fs",	 (void *)9},
  {"gs",	(void *)10},
  {"cs",	(void *)13},
  {"orig_eax",	(void *)11},
  {"eip",	(void *)12},
  {"efl",	(void *)14},
  {"esp",	(void *)15},
  {"ss",	(void *)16}
}
#endif
  ;
int nr_regs
#ifdef DO_UDB_INIT
= sizeof(reg_mapping)/sizeof(ENTRY)
#endif
  ;
