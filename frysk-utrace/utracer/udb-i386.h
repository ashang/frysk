#include <search.h>

#ifndef DO_UDB_INIT
extern
#endif
const char * i386_signals[]
#ifdef DO_UDB_INIT
= {
  "unused",			//  0
  "SIGHUP",			//  1
  "SIGINT",			//  2
  "SIGQUIT",			//  3
  "SIGILL",			//  4
  "SIGTRAP",			//  5
  "SIGABRT",			//  6
  "SIGBUS",			//  7
  "SIGFPE",			//  8
  "SIGKILL",			//  9
  "SIGUSR1",			// 10
  "SIGSEGV",			// 11
  "SIGUSR2",			// 12
  "SIGPIPE",			// 13
  "SIGALRM",			// 14
  "SIGTERM",			// 15
  "SIGSTKFLT",			// 16
  "SIGCHLD",			// 17
  "SIGCONT",			// 18
  "SIGSTOP",			// 19
  "SIGTSTP",			// 20
  "SIGTTIN",			// 21
  "SIGTTOU",			// 22
  "SIGURG",			// 23
  "SIGXCPU",			// 24
  "SIGXFSZ",			// 25
  "SIGVTALRM",			// 26
  "SIGPROF",			// 27
  "SIGWINCH",			// 28
  "SIGIO",			// 29
  "SIGPWR",			// 30
  "SIGUNUSED"			// 31
}
#endif
  ;

int nr_signals
#ifdef DO_UDB_INIT
= sizeof(i386_signals)/sizeof(char *)
#endif
  ;

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
