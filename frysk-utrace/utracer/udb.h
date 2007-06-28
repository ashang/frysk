#ifndef UDB_H
#define UDB_H

extern void text_ui();
extern void text_ui_init();
extern void utace_attach_if(long pid);
extern void utrace_detach_if (long pid);
extern void utrace_readreg_if (long pid, int regset, int reg);
extern void set_prompt();
extern void register_utracer(pid_t pid);

pid_t		udb_pid;

char * prompt
#ifdef DO_UDB_INIT
= NULL;
#endif
;

long current_pid
#ifdef DO_UDB_INIT
= -1
#endif
;

int ctl_file_fd
#ifdef DO_UDB_INIT
= -1
#endif
;

int utracer_cmd_file_fd
#ifdef DO_UDB_INIT
= -1
#endif
;

int utracer_resp_file_fd
#ifdef DO_UDB_INIT
= -1
#endif
;

char * module_name
#ifdef DO_UDB_INIT
= NULL
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

#endif  /* UDB_H */
